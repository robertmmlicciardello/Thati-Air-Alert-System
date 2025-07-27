const jwt = require('jsonwebtoken');
const database = require('../database/sqlite');

/**
 * WebSocket Setup for Real-time Communication
 */
function setupWebSocket(io) {
    // Authentication middleware for WebSocket
    io.use(async (socket, next) => {
        try {
            const token = socket.handshake.auth.token;
            
            if (!token) {
                return next(new Error('Authentication error: No token provided'));
            }

            // Verify JWT token
            const decoded = jwt.verify(token, process.env.JWT_SECRET);
            
            // Get user info
            const user = await database.get(
                'SELECT id, username, role, region FROM users WHERE id = ? AND is_active = 1',
                [decoded.userId]
            );

            if (!user) {
                return next(new Error('Authentication error: Invalid user'));
            }

            socket.user = user;
            next();

        } catch (error) {
            console.error('WebSocket auth error:', error);
            next(new Error('Authentication error'));
        }
    });

    // Handle connections
    io.on('connection', (socket) => {
        console.log(`User connected: ${socket.user.username} (${socket.user.role})`);

        // Join user to their region room
        if (socket.user.region) {
            socket.join(`region_${socket.user.region}`);
        }

        // Join admin users to admin room
        if (socket.user.role === 'admin') {
            socket.join('admin');
        }

        // Handle device status updates
        socket.on('device_status', async (data) => {
            try {
                const { deviceId, status, location, batteryLevel } = data;

                // Update device status in database
                await database.run(`
                    UPDATE devices SET
                        is_online = ?,
                        location_lat = ?,
                        location_lng = ?,
                        battery_level = ?,
                        last_seen = CURRENT_TIMESTAMP
                    WHERE device_id = ?
                `, [
                    status === 'online' ? 1 : 0,
                    location?.latitude,
                    location?.longitude,
                    batteryLevel,
                    deviceId
                ]);

                // Broadcast to admin users
                socket.to('admin').emit('device_status_update', {
                    deviceId,
                    status,
                    location,
                    batteryLevel,
                    timestamp: new Date().toISOString()
                });

            } catch (error) {
                console.error('Device status update error:', error);
                socket.emit('error', { message: 'Failed to update device status' });
            }
        });

        // Handle alert acknowledgments
        socket.on('alert_acknowledge', async (data) => {
            try {
                const { alertId, deviceId, location } = data;

                // Find alert and device
                const alert = await database.get('SELECT id FROM alerts WHERE alert_id = ?', [alertId]);
                const device = await database.get('SELECT id FROM devices WHERE device_id = ?', [deviceId]);

                if (alert && device) {
                    // Update delivery status
                    await database.run(`
                        UPDATE alert_deliveries 
                        SET status = 'acknowledged', acknowledged_at = CURRENT_TIMESTAMP
                        WHERE alert_id = ? AND device_id = ?
                    `, [alert.id, device.id]);

                    // Broadcast acknowledgment to admin
                    socket.to('admin').emit('alert_acknowledged', {
                        alertId,
                        deviceId,
                        location,
                        timestamp: new Date().toISOString(),
                        user: socket.user.username
                    });

                    socket.emit('acknowledge_success', { alertId });
                }

            } catch (error) {
                console.error('Alert acknowledge error:', error);
                socket.emit('error', { message: 'Failed to acknowledge alert' });
            }
        });

        // Handle real-time chat/messaging
        socket.on('send_message', (data) => {
            const { message, targetRegion } = data;

            // Broadcast message to target region or all regions
            if (targetRegion) {
                socket.to(`region_${targetRegion}`).emit('new_message', {
                    message,
                    sender: socket.user.username,
                    region: targetRegion,
                    timestamp: new Date().toISOString()
                });
            } else {
                socket.broadcast.emit('new_message', {
                    message,
                    sender: socket.user.username,
                    timestamp: new Date().toISOString()
                });
            }
        });

        // Handle disconnection
        socket.on('disconnect', () => {
            console.log(`User disconnected: ${socket.user.username}`);
        });

        // Send welcome message
        socket.emit('connected', {
            message: 'Connected to Thati Alert Server',
            user: {
                username: socket.user.username,
                role: socket.user.role,
                region: socket.user.region
            },
            timestamp: new Date().toISOString()
        });
    });

    // Helper function to broadcast alerts
    io.broadcastAlert = (alert) => {
        // Broadcast to specific region
        if (alert.region && alert.region !== 'all') {
            io.to(`region_${alert.region}`).emit('new_alert', alert);
        } else {
            // Broadcast to all connected clients
            io.emit('new_alert', alert);
        }

        // Always send to admin room
        io.to('admin').emit('alert_sent', {
            ...alert,
            adminNotification: true
        });
    };

    // Helper function to broadcast system status
    io.broadcastSystemStatus = (status) => {
        io.to('admin').emit('system_status', {
            ...status,
            timestamp: new Date().toISOString()
        });
    };

    console.log('WebSocket server initialized');
    return io;
}

module.exports = { setupWebSocket };