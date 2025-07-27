const express = require('express');
const database = require('../database/sqlite');
const { body, validationResult } = require('express-validator');
const { authenticateToken } = require('../middleware/auth');

const router = express.Router();

/**
 * POST /api/alerts/send
 * Send new alert
 */
router.post('/send', authenticateToken, [
    body('message').notEmpty().withMessage('Message is required'),
    body('type').notEmpty().withMessage('Type is required'),
    body('priority').isIn(['low', 'medium', 'high', 'critical']).withMessage('Invalid priority')
], async (req, res) => {
    try {
        const errors = validationResult(req);
        if (!errors.isEmpty()) {
            return res.status(400).json({
                success: false,
                errors: errors.array()
            });
        }

        const { message, type, priority, region, metadata } = req.body;
        const alertId = `alert_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;

        // Insert alert into database
        const result = await database.run(
            'INSERT INTO alerts (alert_id, message, type, priority, region, sender_id, metadata) VALUES (?, ?, ?, ?, ?, ?, ?)',
            [alertId, message, type, priority, region || req.user.region, req.user.userId, JSON.stringify(metadata || {})]
        );

        // Get active devices in the region
        const devices = await database.all(
            'SELECT * FROM devices WHERE is_online = 1 AND (? = "all" OR ? IS NULL)',
            [region || req.user.region, region]
        );

        // Create delivery records
        for (const device of devices) {
            await database.run(
                'INSERT INTO alert_deliveries (alert_id, device_id, status) VALUES (?, ?, ?)',
                [result.id, device.id, 'pending']
            );
        }

        // Emit to WebSocket clients (if available)
        if (req.app.get('io')) {
            req.app.get('io').emit('new_alert', {
                id: alertId,
                message,
                type,
                priority,
                region: region || req.user.region,
                timestamp: new Date().toISOString(),
                sender: req.user.username
            });
        }

        res.json({
            success: true,
            data: {
                alertId,
                message,
                type,
                priority,
                region: region || req.user.region,
                targetDevices: devices.length,
                timestamp: new Date().toISOString()
            }
        });

    } catch (error) {
        console.error('Send alert error:', error);
        res.status(500).json({
            success: false,
            message: 'Failed to send alert'
        });
    }
});

/**
 * GET /api/alerts/history
 * Get alert history
 */
router.get('/history', authenticateToken, async (req, res) => {
    try {
        const { page = 1, limit = 20, type, priority, region } = req.query;
        const offset = (page - 1) * limit;

        let whereClause = 'WHERE 1=1';
        let params = [];

        // Filter by user's region if not admin
        if (req.user.role !== 'admin') {
            whereClause += ' AND (region = ? OR region IS NULL)';
            params.push(req.user.region);
        }

        if (type) {
            whereClause += ' AND type = ?';
            params.push(type);
        }

        if (priority) {
            whereClause += ' AND priority = ?';
            params.push(priority);
        }

        if (region && req.user.role === 'admin') {
            whereClause += ' AND region = ?';
            params.push(region);
        }

        // Get alerts with sender info
        const alerts = await database.all(`
            SELECT a.*, u.username as sender_username,
                   COUNT(ad.id) as total_deliveries,
                   COUNT(CASE WHEN ad.status = 'delivered' THEN 1 END) as successful_deliveries
            FROM alerts a
            LEFT JOIN users u ON a.sender_id = u.id
            LEFT JOIN alert_deliveries ad ON a.id = ad.alert_id
            ${whereClause}
            GROUP BY a.id
            ORDER BY a.created_at DESC
            LIMIT ? OFFSET ?
        `, [...params, parseInt(limit), parseInt(offset)]);

        // Get total count
        const totalResult = await database.get(`
            SELECT COUNT(*) as total FROM alerts a ${whereClause}
        `, params);

        res.json({
            success: true,
            data: {
                alerts: alerts.map(alert => ({
                    ...alert,
                    metadata: alert.metadata ? JSON.parse(alert.metadata) : {},
                    deliveryRate: alert.total_deliveries > 0 
                        ? Math.round((alert.successful_deliveries / alert.total_deliveries) * 100) 
                        : 0
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
        console.error('Get alert history error:', error);
        res.status(500).json({
            success: false,
            message: 'Failed to get alert history'
        });
    }
});

/**
 * GET /api/alerts/:alertId
 * Get specific alert details
 */
router.get('/:alertId', authenticateToken, async (req, res) => {
    try {
        const { alertId } = req.params;

        const alert = await database.get(`
            SELECT a.*, u.username as sender_username
            FROM alerts a
            LEFT JOIN users u ON a.sender_id = u.id
            WHERE a.alert_id = ?
        `, [alertId]);

        if (!alert) {
            return res.status(404).json({
                success: false,
                message: 'Alert not found'
            });
        }

        // Check permissions
        if (req.user.role !== 'admin' && alert.region !== req.user.region) {
            return res.status(403).json({
                success: false,
                message: 'Access denied'
            });
        }

        // Get delivery details
        const deliveries = await database.all(`
            SELECT ad.*, d.device_name, d.device_type
            FROM alert_deliveries ad
            LEFT JOIN devices d ON ad.device_id = d.id
            WHERE ad.alert_id = ?
            ORDER BY ad.created_at DESC
        `, [alert.id]);

        res.json({
            success: true,
            data: {
                ...alert,
                metadata: alert.metadata ? JSON.parse(alert.metadata) : {},
                deliveries
            }
        });

    } catch (error) {
        console.error('Get alert details error:', error);
        res.status(500).json({
            success: false,
            message: 'Failed to get alert details'
        });
    }
});

/**
 * POST /api/alerts/:alertId/acknowledge
 * Acknowledge alert receipt
 */
router.post('/:alertId/acknowledge', async (req, res) => {
    try {
        const { alertId } = req.params;
        const { deviceId, location } = req.body;

        // Find the alert
        const alert = await database.get('SELECT id FROM alerts WHERE alert_id = ?', [alertId]);
        if (!alert) {
            return res.status(404).json({
                success: false,
                message: 'Alert not found'
            });
        }

        // Find the device
        const device = await database.get('SELECT id FROM devices WHERE device_id = ?', [deviceId]);
        if (!device) {
            return res.status(404).json({
                success: false,
                message: 'Device not found'
            });
        }

        // Update delivery status
        await database.run(`
            UPDATE alert_deliveries 
            SET status = 'acknowledged', acknowledged_at = CURRENT_TIMESTAMP
            WHERE alert_id = ? AND device_id = ?
        `, [alert.id, device.id]);

        // Update device location if provided
        if (location) {
            await database.run(
                'UPDATE devices SET location_lat = ?, location_lng = ?, last_seen = CURRENT_TIMESTAMP WHERE id = ?',
                [location.latitude, location.longitude, device.id]
            );
        }

        res.json({
            success: true,
            message: 'Alert acknowledged successfully'
        });

    } catch (error) {
        console.error('Acknowledge alert error:', error);
        res.status(500).json({
            success: false,
            message: 'Failed to acknowledge alert'
        });
    }
});

module.exports = router;