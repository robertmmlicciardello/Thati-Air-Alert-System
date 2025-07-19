const express = require('express');
const { body, validationResult } = require('express-validator');
const { v4: uuidv4 } = require('uuid');
const db = require('../database/connection');
const { publishAlert } = require('../services/alertProcessor');
const { encryptMessage, decryptMessage } = require('../utils/encryption');
const logger = require('../utils/logger');

const router = express.Router();

/**
 * POST /api/alerts/send
 * Send a new alert
 */
router.post('/send', [
    body('message').isLength({ min: 1, max: 500 }).withMessage('Message must be 1-500 characters'),
    body('type').isIn(['aircraft', 'attack', 'general', 'evacuation', 'all_clear']).withMessage('Invalid alert type'),
    body('priority').isIn(['low', 'medium', 'high', 'critical']).withMessage('Invalid priority'),
    body('region').optional().isString().withMessage('Region must be a string'),
    body('coordinates').optional().isObject().withMessage('Coordinates must be an object')
], async (req, res) => {
    try {
        const errors = validationResult(req);
        if (!errors.isEmpty()) {
            return res.status(400).json({
                error: 'Validation failed',
                details: errors.array()
            });
        }

        const { message, type, priority, region, coordinates } = req.body;
        const userId = req.user.id;
        const alertId = uuidv4();

        // Encrypt the message
        const encryptedMessage = encryptMessage(message);

        // Store alert in database
        const alertData = {
            id: alertId,
            user_id: userId,
            message: encryptedMessage.encryptedData,
            message_iv: encryptedMessage.iv,
            type,
            priority,
            region: region || null,
            coordinates: coordinates ? JSON.stringify(coordinates) : null,
            created_at: new Date(),
            status: 'pending'
        };

        await db.query(`
            INSERT INTO alerts (id, user_id, message, message_iv, type, priority, region, coordinates, created_at, status)
            VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10)
        `, [
            alertData.id,
            alertData.user_id,
            alertData.message,
            alertData.message_iv,
            alertData.type,
            alertData.priority,
            alertData.region,
            alertData.coordinates,
            alertData.created_at,
            alertData.status
        ]);

        // Publish alert to processing queue
        await publishAlert({
            id: alertId,
            message: message, // Use original message for processing
            type,
            priority,
            region,
            coordinates,
            userId,
            timestamp: new Date().toISOString()
        });

        logger.info(`Alert sent: ${alertId} by user ${userId}`);

        res.status(201).json({
            success: true,
            alertId,
            message: 'Alert sent successfully',
            timestamp: new Date().toISOString()
        });

    } catch (error) {
        logger.error('Error sending alert:', error);
        res.status(500).json({
            error: 'Failed to send alert',
            message: 'Internal server error'
        });
    }
});

/**
 * GET /api/alerts/history
 * Get alert history for the user
 */
router.get('/history', async (req, res) => {
    try {
        const userId = req.user.id;
        const page = parseInt(req.query.page) || 1;
        const limit = parseInt(req.query.limit) || 20;
        const offset = (page - 1) * limit;

        const result = await db.query(`
            SELECT id, type, priority, region, coordinates, created_at, status, delivery_count
            FROM alerts 
            WHERE user_id = $1 
            ORDER BY created_at DESC 
            LIMIT $2 OFFSET $3
        `, [userId, limit, offset]);

        const countResult = await db.query(`
            SELECT COUNT(*) as total FROM alerts WHERE user_id = $1
        `, [userId]);

        const total = parseInt(countResult.rows[0].total);
        const totalPages = Math.ceil(total / limit);

        res.json({
            alerts: result.rows,
            pagination: {
                page,
                limit,
                total,
                totalPages,
                hasNext: page < totalPages,
                hasPrev: page > 1
            }
        });

    } catch (error) {
        logger.error('Error fetching alert history:', error);
        res.status(500).json({
            error: 'Failed to fetch alert history',
            message: 'Internal server error'
        });
    }
});

/**
 * GET /api/alerts/received
 * Get alerts received by the user's devices
 */
router.get('/received', async (req, res) => {
    try {
        const userId = req.user.id;
        const page = parseInt(req.query.page) || 1;
        const limit = parseInt(req.query.limit) || 20;
        const offset = (page - 1) * limit;

        const result = await db.query(`
            SELECT ar.id, ar.alert_id, ar.device_id, ar.received_at, ar.acknowledged_at,
                   a.type, a.priority, a.region, a.created_at as alert_created_at
            FROM alert_receipts ar
            JOIN alerts a ON ar.alert_id = a.id
            JOIN devices d ON ar.device_id = d.id
            WHERE d.user_id = $1
            ORDER BY ar.received_at DESC
            LIMIT $2 OFFSET $3
        `, [userId, limit, offset]);

        res.json({
            receivedAlerts: result.rows,
            pagination: {
                page,
                limit,
                total: result.rows.length
            }
        });

    } catch (error) {
        logger.error('Error fetching received alerts:', error);
        res.status(500).json({
            error: 'Failed to fetch received alerts',
            message: 'Internal server error'
        });
    }
});

/**
 * POST /api/alerts/:alertId/acknowledge
 * Acknowledge receipt of an alert
 */
router.post('/:alertId/acknowledge', async (req, res) => {
    try {
        const { alertId } = req.params;
        const { deviceId } = req.body;
        const userId = req.user.id;

        // Verify device belongs to user
        const deviceResult = await db.query(`
            SELECT id FROM devices WHERE id = $1 AND user_id = $2
        `, [deviceId, userId]);

        if (deviceResult.rows.length === 0) {
            return res.status(403).json({
                error: 'Device not found or access denied'
            });
        }

        // Record acknowledgment
        await db.query(`
            UPDATE alert_receipts 
            SET acknowledged_at = NOW() 
            WHERE alert_id = $1 AND device_id = $2
        `, [alertId, deviceId]);

        logger.info(`Alert acknowledged: ${alertId} by device ${deviceId}`);

        res.json({
            success: true,
            message: 'Alert acknowledged successfully'
        });

    } catch (error) {
        logger.error('Error acknowledging alert:', error);
        res.status(500).json({
            error: 'Failed to acknowledge alert',
            message: 'Internal server error'
        });
    }
});

/**
 * GET /api/alerts/statistics
 * Get alert statistics for the user
 */
router.get('/statistics', async (req, res) => {
    try {
        const userId = req.user.id;
        const timeframe = req.query.timeframe || '7d'; // 1d, 7d, 30d

        let timeCondition = '';
        switch (timeframe) {
            case '1d':
                timeCondition = "AND created_at >= NOW() - INTERVAL '1 day'";
                break;
            case '7d':
                timeCondition = "AND created_at >= NOW() - INTERVAL '7 days'";
                break;
            case '30d':
                timeCondition = "AND created_at >= NOW() - INTERVAL '30 days'";
                break;
        }

        const stats = await db.query(`
            SELECT 
                COUNT(*) as total_sent,
                COUNT(CASE WHEN status = 'delivered' THEN 1 END) as delivered,
                COUNT(CASE WHEN status = 'failed' THEN 1 END) as failed,
                COUNT(CASE WHEN priority = 'critical' THEN 1 END) as critical_alerts,
                AVG(delivery_count) as avg_delivery_count
            FROM alerts 
            WHERE user_id = $1 ${timeCondition}
        `, [userId]);

        const typeStats = await db.query(`
            SELECT type, COUNT(*) as count
            FROM alerts 
            WHERE user_id = $1 ${timeCondition}
            GROUP BY type
            ORDER BY count DESC
        `, [userId]);

        res.json({
            timeframe,
            summary: stats.rows[0],
            byType: typeStats.rows
        });

    } catch (error) {
        logger.error('Error fetching alert statistics:', error);
        res.status(500).json({
            error: 'Failed to fetch statistics',
            message: 'Internal server error'
        });
    }
});

module.exports = router;