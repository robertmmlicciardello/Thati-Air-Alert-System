/**
 * Test Helpers
 * Utility functions for testing
 */

const jwt = require('jsonwebtoken');
const bcrypt = require('bcrypt');
const { v4: uuidv4 } = require('uuid');

/**
 * Authentication helpers
 */
const authHelpers = {
    /**
     * Generate a valid JWT token
     */
    generateToken(payload = {}, options = {}) {
        const defaultPayload = {
            userId: uuidv4(),
            username: 'testuser',
            role: 'user',
            region: 'yangon'
        };
        
        const defaultOptions = {
            expiresIn: '1h'
        };
        
        return jwt.sign(
            { ...defaultPayload, ...payload },
            process.env.JWT_SECRET || 'test-secret',
            { ...defaultOptions, ...options }
        );
    },
    
    /**
     * Generate an expired JWT token
     */
    generateExpiredToken(payload = {}) {
        return this.generateToken(payload, { expiresIn: '-1h' });
    },
    
    /**
     * Generate an admin token
     */
    generateAdminToken(payload = {}) {
        return this.generateToken({ ...payload, role: 'admin' });
    },
    
    /**
     * Hash a password
     */
    async hashPassword(password) {
        return await bcrypt.hash(password, 10);
    },
    
    /**
     * Verify a password
     */
    async verifyPassword(password, hash) {
        return await bcrypt.compare(password, hash);
    }
};

/**
 * Data generation helpers
 */
const dataHelpers = {
    /**
     * Generate test user data
     */
    generateUserData(overrides = {}) {
        const timestamp = Date.now();
        return {
            id: uuidv4(),
            username: `testuser_${timestamp}`,
            email: `test_${timestamp}@example.com`,
            name: 'Test User',
            role: 'user',
            region: 'yangon',
            status: 'active',
            created_at: new Date().toISOString(),
            ...overrides
        };
    },
    
    /**
     * Generate test alert data
     */
    generateAlertData(overrides = {}) {
        return {
            id: uuidv4(),
            message: 'Test alert message',
            type: 'aircraft',
            priority: 'medium',
            region: 'yangon',
            coordinates: {
                latitude: 16.8661,
                longitude: 96.1951
            },
            created_at: new Date().toISOString(),
            status: 'pending',
            delivery_count: 0,
            ...overrides
        };
    },
    
    /**
     * Generate test device data
     */
    generateDeviceData(overrides = {}) {
        const timestamp = Date.now();
        return {
            id: uuidv4(),
            device_id: `test_device_${timestamp}`,
            name: 'Test Device',
            type: 'android',
            model: 'Test Model',
            status: 'offline',
            battery_level: 85,
            is_charging: false,
            network_type: 'wifi',
            signal_strength: 90,
            created_at: new Date().toISOString(),
            ...overrides
        };
    },
    
    /**
     * Generate coordinates within Myanmar
     */
    generateMyanmarCoordinates() {
        // Myanmar approximate bounds
        const minLat = 9.5;
        const maxLat = 28.5;
        const minLng = 92.2;
        const maxLng = 101.2;
        
        return {
            latitude: minLat + Math.random() * (maxLat - minLat),
            longitude: minLng + Math.random() * (maxLng - minLng)
        };
    },
    
    /**
     * Generate random string
     */
    generateRandomString(length = 10) {
        const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
        let result = '';
        for (let i = 0; i < length; i++) {
            result += chars.charAt(Math.floor(Math.random() * chars.length));
        }
        return result;
    }
};

/**
 * Database helpers
 */
const dbHelpers = {
    /**
     * Clean up test data by pattern
     */
    async cleanupTestData(db, pattern = 'test%') {
        try {
            await db.query('DELETE FROM alert_deliveries WHERE 1=1');
            await db.query('DELETE FROM alerts WHERE message LIKE $1', [`%${pattern}%`]);
            await db.query('DELETE FROM devices WHERE name LIKE $1', [`%${pattern}%`]);
            await db.query('DELETE FROM users WHERE username LIKE $1', [pattern]);
        } catch (error) {
            console.error('Cleanup failed:', error);
        }
    },
    
    /**
     * Insert test user
     */
    async insertTestUser(db, userData = {}) {
        const user = dataHelpers.generateUserData(userData);
        if (user.password) {
            user.password_hash = await authHelpers.hashPassword(user.password);
            delete user.password;
        }
        
        const result = await db.query(`
            INSERT INTO users (id, username, email, password_hash, name, role, region, status, created_at)
            VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)
            RETURNING *
        `, [
            user.id, user.username, user.email, user.password_hash || 'dummy_hash',
            user.name, user.role, user.region, user.status, user.created_at
        ]);
        
        return result.rows[0];
    },
    
    /**
     * Insert test alert
     */
    async insertTestAlert(db, userId, alertData = {}) {
        const alert = dataHelpers.generateAlertData({ user_id: userId, ...alertData });
        
        const result = await db.query(`
            INSERT INTO alerts (id, user_id, message, type, priority, region, coordinates, created_at, status, delivery_count)
            VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10)
            RETURNING *
        `, [
            alert.id, alert.user_id, alert.message, alert.type, alert.priority,
            alert.region, JSON.stringify(alert.coordinates), alert.created_at,
            alert.status, alert.delivery_count
        ]);
        
        return result.rows[0];
    },
    
    /**
     * Insert test device
     */
    async insertTestDevice(db, userId, deviceData = {}) {
        const device = dataHelpers.generateDeviceData({ user_id: userId, ...deviceData });
        
        const result = await db.query(`
            INSERT INTO devices (id, user_id, device_id, name, type, model, status, battery_level, is_charging, network_type, signal_strength, created_at)
            VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12)
            RETURNING *
        `, [
            device.id, device.user_id, device.device_id, device.name, device.type,
            device.model, device.status, device.battery_level, device.is_charging,
            device.network_type, device.signal_strength, device.created_at
        ]);
        
        return result.rows[0];
    }
};

/**
 * HTTP helpers
 */
const httpHelpers = {
    /**
     * Create authorization header
     */
    createAuthHeader(token) {
        return { Authorization: `Bearer ${token}` };
    },
    
    /**
     * Create request with auth
     */
    createAuthenticatedRequest(request, token) {
        return request.set('Authorization', `Bearer ${token}`);
    },
    
    /**
     * Extract error from response
     */
    extractError(response) {
        return response.body.error || { code: 'UNKNOWN', message: 'Unknown error' };
    },
    
    /**
     * Check if response is successful
     */
    isSuccessResponse(response) {
        return response.body.success === true;
    },
    
    /**
     * Check if response has error
     */
    hasError(response) {
        return response.body.success === false && response.body.error;
    }
};

/**
 * WebSocket helpers
 */
const wsHelpers = {
    /**
     * Create WebSocket connection for testing
     */
    createTestWebSocket(url, token) {
        const WebSocket = require('ws');
        const wsUrl = token ? `${url}?token=${token}` : url;
        return new WebSocket(wsUrl);
    },
    
    /**
     * Wait for WebSocket message
     */
    waitForMessage(ws, timeout = 5000) {
        return new Promise((resolve, reject) => {
            const timer = setTimeout(() => {
                reject(new Error('WebSocket message timeout'));
            }, timeout);
            
            ws.once('message', (data) => {
                clearTimeout(timer);
                try {
                    resolve(JSON.parse(data));
                } catch (error) {
                    resolve(data);
                }
            });
        });
    },
    
    /**
     * Send WebSocket message
     */
    sendMessage(ws, message) {
        const data = typeof message === 'string' ? message : JSON.stringify(message);
        ws.send(data);
    }
};

/**
 * Time helpers
 */
const timeHelpers = {
    /**
     * Wait for specified milliseconds
     */
    async wait(ms) {
        return new Promise(resolve => setTimeout(resolve, ms));
    },
    
    /**
     * Get ISO string for date offset
     */
    getDateOffset(days = 0, hours = 0, minutes = 0) {
        const date = new Date();
        date.setDate(date.getDate() + days);
        date.setHours(date.getHours() + hours);
        date.setMinutes(date.getMinutes() + minutes);
        return date.toISOString();
    },
    
    /**
     * Check if date is within range
     */
    isDateWithinRange(dateStr, startDate, endDate) {
        const date = new Date(dateStr);
        return date >= new Date(startDate) && date <= new Date(endDate);
    }
};

/**
 * Validation helpers
 */
const validationHelpers = {
    /**
     * Validate email format
     */
    isValidEmail(email) {
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        return emailRegex.test(email);
    },
    
    /**
     * Validate UUID format
     */
    isValidUUID(uuid) {
        const uuidRegex = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;
        return uuidRegex.test(uuid);
    },
    
    /**
     * Validate coordinates
     */
    isValidCoordinates(lat, lng) {
        return lat >= -90 && lat <= 90 && lng >= -180 && lng <= 180;
    },
    
    /**
     * Validate Myanmar coordinates
     */
    isValidMyanmarCoordinates(lat, lng) {
        return lat >= 9.5 && lat <= 28.5 && lng >= 92.2 && lng <= 101.2;
    }
};

module.exports = {
    authHelpers,
    dataHelpers,
    dbHelpers,
    httpHelpers,
    wsHelpers,
    timeHelpers,
    validationHelpers
};