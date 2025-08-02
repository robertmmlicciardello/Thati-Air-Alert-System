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

// Global test hooks - will be available when mocha loads this file
if (typeof before !== 'undefined') {
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
}

/**
 * Setup test database
 */
async function setupTestDatabase() {
    try {
        // For unit tests, we'll use mocked database
        // Integration tests will use real database
        console.log('✅ Test database setup complete (mocked for unit tests)');
    } catch (error) {
        console.error('❌ Test database setup failed:', error);
        // Don't throw error for unit tests
    }
}

/**
 * Setup test Redis
 */
async function setupTestRedis() {
    try {
        // For unit tests, we'll use mocked Redis
        // Integration tests will use real Redis
        console.log('✅ Test Redis setup complete (mocked for unit tests)');
    } catch (error) {
        console.error('❌ Test Redis setup failed:', error);
        // Don't throw error if Redis is not available in test environment
    }
}

/**
 * Seed test data
 */
async function seedTestData() {
    try {
        // For unit tests, we'll use mocked data
        // Integration tests will use real database seeding
        console.log('✅ Test data seeding complete (mocked for unit tests)');
    } catch (error) {
        console.error('❌ Test data seeding failed:', error);
        // Don't throw error for unit tests
    }
}

/**
 * Cleanup test database
 */
async function cleanupTestDatabase() {
    try {
        // For unit tests, cleanup is handled by mocks
        console.log('✅ Test database cleanup complete (mocked for unit tests)');
    } catch (error) {
        console.error('❌ Test database cleanup failed:', error);
    }
}

/**
 * Cleanup test Redis
 */
async function cleanupTestRedis() {
    try {
        // For unit tests, cleanup is handled by mocks
        console.log('✅ Test Redis cleanup complete (mocked for unit tests)');
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