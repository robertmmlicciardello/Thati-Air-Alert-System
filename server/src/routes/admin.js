const express = require('express');
const bcrypt = require('bcryptjs');
const database = require('../database/sqlite');
const { authenticateToken, requireRole } = require('../middleware/auth');
const { body, validationResult } = require('express-validator');

const router = express.Router();

/**
 * GET /api/admin/dashboard
 * Get dashboard statistics
 */
router.get('/dashboard', authenticateToken, requireRole(['admin', 'regional_admin']), async (req, res) => {
    try {
        // Get basic stats
        const stats = await Promise.all([
            database.get('SELECT COUNT(*) as total FROM alerts WHERE created_at >= date("now", "-7 days")'),
            database.get('SELECT COUNT(*) as total FROM users WHERE is_active = 1'),
            database.get('SELECT COUNT(*) as total FROM devices WHERE is_online = 1'),
            database.get('SELECT COUNT(*) as total FROM alerts WHERE priority = "critical" AND created_at >= date("now", "-24 hours")')
        ]);

        // Get alert trends (last 7 days)
        const alertTrends = await database.all(`
            SELECT 
                date(created_at) as date,
                COUNT(*) as alerts,
                COUNT(CASE WHEN priority = 'critical' THEN 1 END) as critical,
                COUNT(CASE WHEN priority = 'high' THEN 1 END) as high
            FROM alerts 
            WHERE created_at >= date('now', '-7 days')
            GROUP BY date(created_at)
            ORDER BY date
        `);

        // Get alert types distribution
        const alertTypes = await database.all(`
            SELECT 
                type,
                COUNT(*) as count,
                ROUND(COUNT(*) * 100.0 / (SELECT COUNT(*) FROM alerts WHERE created_at >= date('now', '-30 days')), 1) as percentage
            FROM alerts 
            WHERE created_at >= date('now', '-30 days')
            GROUP BY type
            ORDER BY count DESC
        `);

        // Get regional stats (for admin only)
        let regionalStats = [];
        if (req.user.role === 'admin') {
            regionalStats = await database.all(`
                SELECT 
                    u.region,
                    COUNT(DISTINCT u.id) as users,
                    COUNT(DISTINCT d.id) as devices,
                    COUNT(DISTINCT a.id) as alerts,
                    ROUND(AVG(CASE WHEN ad.status = 'delivered' THEN 100.0 ELSE 0.0 END), 1) as delivery_rate
                FROM users u
                LEFT JOIN devices d ON u.id = d.user_id
                LEFT JOIN alerts a ON u.region = a.region AND a.created_at >= date('now', '-30 days')
                LEFT JOIN alert_deliveries ad ON a.id = ad.alert_id
                WHERE u.region IS NOT NULL
                GROUP BY u.region
                ORDER BY users DESC
            `);
        }

        res.json({
            success: true,
            data: {
                summary: {
                    totalAlerts: stats[0].total,
                    activeUsers: stats[1].total,
                    connectedDevices: stats[2].total,
                    criticalAlerts: stats[3].total
                },
                charts: {
                    alertTrends,
                    alertTypes,
                    regionalStats
                }
            }
        });

    } catch (error) {
        console.error('Dashboard error:', error);
        res.status(500).json({
            success: false,
            message: 'Failed to get dashboard data'
        });
    }
});

/**
 * GET /api/admin/users
 * Get users list (admin only)
 */
router.get('/users', authenticateToken, requireRole('admin'), async (req, res) => {
    try {
        const { page = 1, limit = 20, role, region, status } = req.query;
        const offset = (page - 1) * limit;

        let whereClause = 'WHERE 1=1';
        let params = [];

        if (role) {
            whereClause += ' AND role = ?';
            params.push(role);
        }

        if (region) {
            whereClause += ' AND region = ?';
            params.push(region);
        }

        if (status) {
            whereClause += ' AND is_active = ?';
            params.push(status === 'active' ? 1 : 0);
        }

        const users = await database.all(`
            SELECT 
                u.*,
                COUNT(DISTINCT d.id) as device_count,
                COUNT(DISTINCT a.id) as alert_count
            FROM users u
            LEFT JOIN devices d ON u.id = d.user_id
            LEFT JOIN alerts a ON u.id = a.sender_id
            ${whereClause}
            GROUP BY u.id
            ORDER BY u.created_at DESC
            LIMIT ? OFFSET ?
        `, [...params, parseInt(limit), parseInt(offset)]);

        const totalResult = await database.get(`
            SELECT COUNT(*) as total FROM users ${whereClause}
        `, params);

        res.json({
            success: true,
            data: {
                users: users.map(user => ({
                    ...user,
                    password_hash: undefined // Don't send password hash
                })),
                pagination: {
                    page: parseInt(page),
                    limit: parseInt(limit),
                    total: totalResult.total,
                    totalPages: Math.ceil(totalResult.total / limit)
                }
            }
        });

    } catch (error) {
        console.error('Get users error:', error);
        res.status(500).json({
            success: false,
            message: 'Failed to get users'
        });
    }
});

/**
 * POST /api/admin/users
 * Create new user (admin only)
 */
router.post('/users', authenticateToken, requireRole('admin'), [
    body('username').notEmpty().withMessage('Username is required'),
    body('password').isLength({ min: 6 }).withMessage('Password must be at least 6 characters'),
    body('role').isIn(['admin', 'regional_admin', 'user']).withMessage('Invalid role'),
    body('email').optional().isEmail().withMessage('Invalid email format')
], async (req, res) => {
    try {
        const errors = validationResult(req);
        if (!errors.isEmpty()) {
            return res.status(400).json({
                success: false,
                errors: errors.array()
            });
        }

        const { username, email, password, role, region } = req.body;

        // Check if username already exists
        const existingUser = await database.get(
            'SELECT id FROM users WHERE username = ?',
            [username]
        );

        if (existingUser) {
            return res.status(409).json({
                success: false,
                message: 'Username already exists'
            });
        }

        // Hash password
        const passwordHash = await bcrypt.hash(password, parseInt(process.env.BCRYPT_ROUNDS) || 10);

        // Create user
        const result = await database.run(
            'INSERT INTO users (username, email, password_hash, role, region) VALUES (?, ?, ?, ?, ?)',
            [username, email, passwordHash, role, region]
        );

        res.json({
            success: true,
            data: {
                id: result.id,
                username,
                email,
                role,
                region,
                message: 'User created successfully'
            }
        });

    } catch (error) {
        console.error('Create user error:', error);
        res.status(500).json({
            success: false,
            message: 'Failed to create user'
        });
    }
});

/**
 * PUT /api/admin/users/:userId
 * Update user (admin only)
 */
router.put('/users/:userId', authenticateToken, requireRole('admin'), async (req, res) => {
    try {
        const { userId } = req.params;
        const { email, role, region, is_active } = req.body;

        await database.run(
            'UPDATE users SET email = ?, role = ?, region = ?, is_active = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?',
            [email, role, region, is_active ? 1 : 0, userId]
        );

        res.json({
            success: true,
            message: 'User updated successfully'
        });

    } catch (error) {
        console.error('Update user error:', error);
        res.status(500).json({
            success: false,
            message: 'Failed to update user'
        });
    }
});

/**
 * DELETE /api/admin/users/:userId
 * Delete user (admin only)
 */
router.delete('/users/:userId', authenticateToken, requireRole('admin'), async (req, res) => {
    try {
        const { userId } = req.params;

        // Soft delete by setting is_active to false
        await database.run(
            'UPDATE users SET is_active = 0, updated_at = CURRENT_TIMESTAMP WHERE id = ?',
            [userId]
        );

        res.json({
            success: true,
            message: 'User deleted successfully'
        });

    } catch (error) {
        console.error('Delete user error:', error);
        res.status(500).json({
            success: false,
            message: 'Failed to delete user'
        });
    }
});

/**
 * GET /api/admin/system/health
 * Get system health status
 */
router.get('/system/health', authenticateToken, requireRole('admin'), async (req, res) => {
    try {
        const health = {
            status: 'healthy',
            timestamp: new Date().toISOString(),
            services: {
                database: 'healthy',
                server: 'healthy'
            },
            metrics: {
                uptime: process.uptime(),
                memory: process.memoryUsage(),
                cpu: process.cpuUsage()
            }
        };

        res.json({
            success: true,
            data: health
        });

    } catch (error) {
        console.error('Health check error:', error);
        res.status(500).json({
            success: false,
            message: 'Health check failed'
        });
    }
});

module.exports = router;