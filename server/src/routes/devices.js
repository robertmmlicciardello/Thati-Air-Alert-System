const express = require('express');
const database = require('../database/sqlite');
const { authenticateToken } = require('../middleware/auth');

const router = express.Router();

/**
 * GET /api/devices
 * Get devices list
 */
router.get('/', authenticateToken, async (req, res) => {
    try {
        const { page = 1, limit = 20, status, type, region } = req.query;
        const offset = (page - 1) * limit;

        let whereClause = 'WHERE 1=1';
        let params = [];

        if (status) {
            whereClause += ' AND is_online = ?';
            params.push(status === 'online' ? 1 : 0);
        }

        if (type) {
            whereClause += ' AND device_type = ?';
            params.push(type);
        }

        // Filter by region for non-admin users
        if (req.user.role !== 'admin') {
            whereClause += ' AND u.region = ?';
            params.push(req.user.region);
        } else if (region) {
            whereClause += ' AND u.region = ?';
            params.push(region);
        }

        const devices = await database.all(`
            SELECT d.*, u.username, u.region
            FROM devices d
            LEFT JOIN users u ON d.user_id = u.id
            ${whereClause}
            ORDER BY d.last_seen DESC
            LIMIT ? OFFSET ?
        `, [...params, parseInt(limit), parseInt(offset)]);

        const totalResult = await database.get(`
            SELECT COUNT(*) as total 
            FROM devices d
            LEFT JOIN users u ON d.user_id = u.id
            ${whereClause}
        `, params);

        res.json({
            success: true,
            data: {
                devices,
                pagination: {
                    page: parseInt(page),
                    limit: parseInt(limit),
                    total: totalResult.total,
                    totalPages: Math.ceil(totalResult.total / limit)
                }
            }
        });

    } catch (error) {
        console.error('Get devices error:', error);
        res.status(500).json({
            success: false,
            message: 'Failed to get devices'
        });
    }
});

/**
 * POST /api/devices/register
 * Register new device
 */
router.post('/register', async (req, res) => {
    try {
        const {
            deviceId,
            deviceName,
            deviceType,
            osVersion,
            appVersion,
            fcmToken,
            userId
        } = req.body;

        // Check if device already exists
        const existingDevice = await database.get(
            'SELECT id FROM devices WHERE device_id = ?',
            [deviceId]
        );

        if (existingDevice) {
            // Update existing device
            await database.run(`
                UPDATE devices SET
                    device_name = ?,
                    device_type = ?,
                    os_version = ?,
                    app_version = ?,
                    fcm_token = ?,
                    is_online = 1,
                    last_seen = CURRENT_TIMESTAMP
                WHERE device_id = ?
            `, [deviceName, deviceType, osVersion, appVersion, fcmToken, deviceId]);

            res.json({
                success: true,
                message: 'Device updated successfully',
                deviceId: existingDevice.id
            });
        } else {
            // Register new device
            const result = await database.run(`
                INSERT INTO devices (
                    device_id, user_id, device_name, device_type,
                    os_version, app_version, fcm_token, is_online
                ) VALUES (?, ?, ?, ?, ?, ?, ?, 1)
            `, [deviceId, userId, deviceName, deviceType, osVersion, appVersion, fcmToken]);

            res.json({
                success: true,
                message: 'Device registered successfully',
                deviceId: result.id
            });
        }

    } catch (error) {
        console.error('Device registration error:', error);
        res.status(500).json({
            success: false,
            message: 'Failed to register device'
        });
    }
});

/**
 * PUT /api/devices/:deviceId/status
 * Update device status
 */
router.put('/:deviceId/status', async (req, res) => {
    try {
        const { deviceId } = req.params;
        const { status, location, batteryLevel, networkInfo } = req.body;

        await database.run(`
            UPDATE devices SET
                is_online = ?,
                location_lat = ?,
                location_lng = ?,
                battery_level = ?,
                network_type = ?,
                last_seen = CURRENT_TIMESTAMP
            WHERE device_id = ?
        `, [
            status === 'online' ? 1 : 0,
            location?.latitude,
            location?.longitude,
            batteryLevel,
            networkInfo?.type,
            deviceId
        ]);

        res.json({
            success: true,
            message: 'Device status updated successfully'
        });

    } catch (error) {
        console.error('Update device status error:', error);
        res.status(500).json({
            success: false,
            message: 'Failed to update device status'
        });
    }
});

module.exports = router;