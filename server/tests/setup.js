/**
 * Test Setup and Configuration
 * Global test setup for the Thati Air Alert server
 */

const { expect } = require('chai');
const sinon = require('sinon');

// Global test configuration
process.env.NODE_ENV = 'test';
process.env.JWT_SECRET = 'test-jwt-secret-key';
process.env.DB_HOST = 'localhost';
process.env.DB_PORT = '5432';
process.env.DB_NAME = 'thati_alert_test';
process.env.DB_USER = 'test_user';
process.env.DB_PASSWORD = 'test_password';
process.env.REDIS_URL = 'redis://localhost:6379/1';

// Global test hooks
before(async function() {
    console.log('🚀 Starting test suite...');
    
    // Setup test database
    await setupTestDatabase();
    
    // Setup test Redis
    await setupTestRedis();
    
    // Setup test data
    await seedTestData();
});

after(async function() {
    console.log('🧹 Cleaning up test suite...');
    
    // Cleanup test database
    await cleanupTestDatabase();
    
    // Cleanup test Redis
    await cleanupTestRedis();
});

beforeEach(function() {
    // Reset all stubs before each test
    sinon.restore();
});

afterEach(function() {
    // Additional cleanup after each test
    sinon.restore();
});

/**
 * Setup test database
 */
async function setupTestDatabase() {
    const db = require('../src/database/connection');
    
    try {
        // Create test tables if they don't exist
        await db.query(`
            CREATE TABLE IF NOT EXISTS users (
                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                username VARCHAR(50) UNIQUE NOT NULL,
                email VARCHAR(100) UNIQUE NOT NULL,
                password_hash VARCHAR(255) NOT NULL,
                name VARCHAR(100) NOT NULL,
                role VARCHAR(20) DEFAULT 'user',
                region VARCHAR(50) NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                last_login TIMESTAMP,
                status VARCHAR(20) DEFAULT 'active'
            )
        `);
        
        await db.query(`
            CREATE TABLE IF NOT EXISTS alerts (
                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                user_id UUID REFERENCES users(id),
                message TEXT NOT NULL,
                message_iv VARCHAR(32),
                type VARCHAR(20) NOT NULL,
                priority VARCHAR(20) NOT NULL,
                region VARCHAR(50) NOT NULL,
                coordinates JSONB,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                status VARCHAR(20) DEFAULT 'pending',
                delivery_count INTEGER DEFAULT 0
            )
        `);
        
        await db.query(`
            CREATE TABLE IF NOT EXISTS devices (
                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                user_id UUID REFERENCES users(id),
                device_id VARCHAR(100) UNIQUE NOT NULL,
                name VARCHAR(100) NOT NULL,
                type VARCHAR(20) NOT NULL,
                model VARCHAR(100),
                fcm_token TEXT,
                security_token VARCHAR(255),
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                last_seen TIMESTAMP,
                status VARCHAR(20) DEFAULT 'offline',
                battery_level INTEGER,
                is_charging BOOLEAN DEFAULT false,
                network_type VARCHAR(20),
                signal_strength INTEGER
            )
        `);
        
        await db.query(`
            CREATE TABLE IF NOT EXISTS alert_deliveries (
                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                alert_id UUID REFERENCES alerts(id),
                device_id UUID REFERENCES devices(id),
                delivered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                acknowledged_at TIMESTAMP,
                status VARCHAR(20) DEFAULT 'delivered'
            )
        `);
        
        console.log('✅ Test database setup complete');
    } catch (error) {
        console.error('❌ Test database setup failed:', error);
        throw error;
    }
}

/**
 * Setup test Redis
 */
async function setupTestRedis() {
    try {
        const redis = require('../src/services/redis');
        await redis.client.flushdb(); // Clear test database
        console.log('✅ Test Redis setup complete');
    } catch (error) {
        console.error('❌ Test Redis setup failed:', error);
        // Don't throw error if Redis is not available in test environment
    }
}

/**
 * Seed test data
 */
async function seedTestData() {
    const db = require('../src/database/connection');
    const bcrypt = require('bcrypt');
    
    try {
        // Create test users
        const hashedPassword = await bcrypt.hash('testpass123', 10);
        
        await db.query(`
            INSERT INTO users (username, email, password_hash, name, role, region)
            VALUES 
                ('testuser', 'test@example.com', $1, 'Test User', 'user', 'yangon'),
                ('testadmin', 'admin@example.com', $1, 'Test Admin', 'admin', 'yangon')
            ON CONFLICT (username) DO NOTHING
        `, [hashedPassword]);
        
        // Get test user ID
        const userResult = await db.query('SELECT id FROM users WHERE username = $1', ['testuser']);
        if (userResult.rows.length > 0) {
            const userId = userResult.rows[0].id;
            
            // Create test devices
            await db.query(`
                INSERT INTO devices (user_id, device_id, name, type, model, status)
                VALUES 
                    ($1, 'test-device-1', 'Test Phone 1', 'android', 'Samsung Galaxy', 'online'),
                    ($1, 'test-device-2', 'Test Phone 2', 'android', 'Google Pixel', 'offline')
                ON CONFLICT (device_id) DO NOTHING
            `, [userId]);
            
            // Create test alerts
            await db.query(`
                INSERT INTO alerts (user_id, message, type, priority, region, coordinates)
                VALUES 
                    ($1, 'Test aircraft alert', 'aircraft', 'high', 'yangon', '{"latitude": 16.8661, "longitude": 96.1951}'),
                    ($1, 'Test general alert', 'general', 'medium', 'yangon', '{"latitude": 16.8661, "longitude": 96.1951}')
            `, [userId]);
        }
        
        console.log('✅ Test data seeding complete');
    } catch (error) {
        console.error('❌ Test data seeding failed:', error);
        throw error;
    }
}

/**
 * Cleanup test database
 */
async function cleanupTestDatabase() {
    const db = require('../src/database/connection');
    
    try {
        // Clean up test data
        await db.query('DELETE FROM alert_deliveries WHERE 1=1');
        await db.query('DELETE FROM alerts WHERE 1=1');
        await db.query('DELETE FROM devices WHERE 1=1');
        await db.query('DELETE FROM users WHERE username LIKE $1', ['test%']);
        
        console.log('✅ Test database cleanup complete');
    } catch (error) {
        console.error('❌ Test database cleanup failed:', error);
    }
}

/**
 * Cleanup test Redis
 */
async function cleanupTestRedis() {
    try {
        const redis = require('../src/services/redis');
        await redis.client.flushdb();
        console.log('✅ Test Redis cleanup complete');
    } catch (error) {
        console.error('❌ Test Redis cleanup failed:', error);
    }
}

/**
 * Test utilities
 */
global.testUtils = {
    /**
     * Create a test user
     */
    async createTestUser(userData = {}) {
        const db = require('../src/database/connection');
        const bcrypt = require('bcrypt');
        
        const defaultData = {
            username: `testuser_${Date.now()}`,
            email: `test_${Date.now()}@example.com`,
            password: 'testpass123',
            name: 'Test User',
            role: 'user',
            region: 'yangon'
        };
        
        const user = { ...defaultData, ...userData };
        user.password_hash = await bcrypt.hash(user.password, 10);
        delete user.password;
        
        const result = await db.query(`
            INSERT INTO users (username, email, password_hash, name, role, region)
            VALUES ($1, $2, $3, $4, $5, $6)
            RETURNING *
        `, [user.username, user.email, user.password_hash, user.name, user.role, user.region]);
        
        return result.rows[0];
    },
    
    /**
     * Create a test alert
     */
    async createTestAlert(userId, alertData = {}) {
        const db = require('../src/database/connection');
        
        const defaultData = {
            message: 'Test alert message',
            type: 'aircraft',
            priority: 'medium',
            region: 'yangon',
            coordinates: { latitude: 16.8661, longitude: 96.1951 }
        };
        
        const alert = { ...defaultData, ...alertData };
        
        const result = await db.query(`
            INSERT INTO alerts (user_id, message, type, priority, region, coordinates)
            VALUES ($1, $2, $3, $4, $5, $6)
            RETURNING *
        `, [userId, alert.message, alert.type, alert.priority, alert.region, JSON.stringify(alert.coordinates)]);
        
        return result.rows[0];
    },
    
    /**
     * Generate JWT token for testing
     */
    generateTestToken(payload = {}) {
        const jwt = require('jsonwebtoken');
        
        const defaultPayload = {
            userId: 'test-user-id',
            role: 'user'
        };
        
        return jwt.sign(
            { ...defaultPayload, ...payload },
            process.env.JWT_SECRET,
            { expiresIn: '1h' }
        );
    },
    
    /**
     * Wait for a specified amount of time
     */
    async wait(ms) {
        return new Promise(resolve => setTimeout(resolve, ms));
    }
};

// Export for use in other test files
module.exports = {
    setupTestDatabase,
    setupTestRedis,
    seedTestData,
    cleanupTestDatabase,
    cleanupTestRedis
};