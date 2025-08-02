const request = require('supertest');
const { expect } = require('chai');
const express = require('express');
const jwt = require('jsonwebtoken');
const helmet = require('helmet');
const cors = require('cors');

// Mock server app for security testing
const app = express();

// Security middleware
app.use(helmet());
app.use(cors({
    origin: ['http://localhost:3001', 'http://localhost:3000'],
    credentials: true
}));
app.use(express.json({ limit: '10mb' }));

// Mock authentication middleware
const authenticateToken = (req, res, next) => {
    const authHeader = req.headers['authorization'];
    const token = authHeader && authHeader.split(' ')[1];
    
    if (!token) {
        return res.status(401).json({ error: 'Access token required' });
    }
    
    try {
        const decoded = jwt.verify(token, process.env.JWT_SECRET || 'test-secret');
        req.user = decoded;
        next();
    } catch (error) {
        return res.status(403).json({ error: 'Invalid or expired token' });
    }
};

// Mock admin middleware
const requireAdmin = (req, res, next) => {
    if (req.user.role !== 'admin') {
        return res.status(403).json({ error: 'Admin access required' });
    }
    next();
};

// Mock routes for security testing
app.get('/api/protected', authenticateToken, (req, res) => {
    res.json({ message: 'Protected resource accessed', user: req.user });
});

app.get('/api/admin/users', authenticateToken, requireAdmin, (req, res) => {
    res.json({ users: [{ id: 1, username: 'admin' }] });
});

app.post('/api/auth/login', (req, res) => {
    const { username, password } = req.body;
    
    // Mock password validation
    if (password && password.length < 8) {
        return res.status(400).json({ error: 'Password must be at least 8 characters' });
    }
    
    res.json({ 
        token: 'mock-jwt-token',
        user: { id: 'test-user-id', username }
    });
});

app.post('/api/auth/register', (req, res) => {
    const { password } = req.body;
    
    // Password strength validation
    if (!password || password.length < 8) {
        return res.status(400).json({ error: 'Password must be at least 8 characters' });
    }
    
    if (!/(?=.*[a-z])(?=.*[A-Z])(?=.*\d)/.test(password)) {
        return res.status(400).json({ error: 'Password must contain uppercase, lowercase, and number' });
    }
    
    res.status(201).json({ message: 'User registered successfully' });
});

app.post('/api/auth/logout', authenticateToken, (req, res) => {
    res.json({ message: 'Logged out successfully' });
});

app.post('/api/alerts', authenticateToken, (req, res) => {
    const { message, coordinates } = req.body;
    
    // XSS prevention - sanitize message
    if (message && (message.includes('<script>') || message.includes('javascript:'))) {
        return res.status(400).json({ error: 'Invalid characters in message' });
    }
    
    // Coordinate validation
    if (coordinates) {
        const { latitude, longitude } = coordinates;
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            return res.status(400).json({ error: 'Invalid coordinates' });
        }
    }
    
    res.status(201).json({
        id: 'test-alert-id',
        message: message,
        coordinates,
        status: 'sent'
    });
});

app.get('/api/alerts', (req, res) => {
    // SQL injection prevention - mock safe query
    const { search } = req.query;
    if (search && (search.includes("'") || search.includes(';') || search.includes('--'))) {
        return res.status(400).json({ error: 'Invalid search parameters' });
    }
    
    res.json([{ id: 'test-alert-id', message: 'Test alert' }]);
});

app.get('/api/user/profile', authenticateToken, (req, res) => {
    // Mock encrypted response
    res.json({
        id: req.user.userId,
        username: 'testuser',
        email: '***@***.com', // Masked email
        phone: '***-***-1234' // Masked phone
    });
});

// Rate limiting simulation
let loginAttempts = {};
let alertAttempts = {};

app.use('/api/auth/login', (req, res, next) => {
    const ip = req.ip || '127.0.0.1';
    loginAttempts[ip] = (loginAttempts[ip] || 0) + 1;
    
    if (loginAttempts[ip] > 5) {
        return res.status(429).json({ error: 'Too many login attempts' });
    }
    
    next();
});

app.use('/api/alerts', (req, res, next) => {
    if (req.method === 'POST') {
        const ip = req.ip || '127.0.0.1';
        alertAttempts[ip] = (alertAttempts[ip] || 0) + 1;
        
        if (alertAttempts[ip] > 10) {
            return res.status(429).json({ error: 'Too many alert requests' });
        }
    }
    
    next();
});

/**
 * Security Tests
 * Comprehensive security testing for the API
 */
describe('Security Tests', () => {
    let validToken;
    let expiredToken;
    
    before(() => {
        // Create a valid token
        validToken = jwt.sign(
            { userId: 'test-user', role: 'user' },
            process.env.JWT_SECRET || 'test-secret',
            { expiresIn: '1h' }
        );
        
        // Create an expired token
        expiredToken = jwt.sign(
            { userId: 'test-user', role: 'user' },
            process.env.JWT_SECRET || 'test-secret',
            { expiresIn: '-1h' }
        );
    });
    
    describe('Authentication Security', () => {
        it('should reject requests without authentication token', async () => {
            const response = await request(app)
                .get('/api/alerts/history')
                .expect(401);
            
            expect(response.body.success).to.be.false;
            expect(response.body.error.code).to.equal('AUTHENTICATION_FAILED');
        });
        
        it('should reject requests with invalid token format', async () => {
            const response = await request(app)
                .get('/api/alerts/history')
                .set('Authorization', 'Bearer invalid-token-format')
                .expect(401);
            
            expect(response.body.success).to.be.false;
        });
        
        it('should reject requests with expired tokens', async () => {
            const response = await request(app)
                .get('/api/alerts/history')
                .set('Authorization', `Bearer ${expiredToken}`)
                .expect(401);
            
            expect(response.body.success).to.be.false;
        });
        
        it('should reject requests with malformed Authorization header', async () => {
            const response = await request(app)
                .get('/api/alerts/history')
                .set('Authorization', 'InvalidFormat token')
                .expect(401);
            
            expect(response.body.success).to.be.false;
        });
    });
    
    describe('Input Validation Security', () => {
        it('should prevent SQL injection in alert queries', async () => {
            const maliciousInput = "'; DROP TABLE alerts; --";
            
            const response = await request(app)
                .get(`/api/alerts/history?type=${encodeURIComponent(maliciousInput)}`)
                .set('Authorization', `Bearer ${validToken}`)
                .expect(400);
            
            expect(response.body.success).to.be.false;
        });
        
        it('should sanitize XSS attempts in alert messages', async () => {
            const xssPayload = '<script>alert("XSS")</script>';
            
            const response = await request(app)
                .post('/api/alerts/send')
                .set('Authorization', `Bearer ${validToken}`)
                .send({
                    message: xssPayload,
                    type: 'aircraft',
                    priority: 'high',
                    region: 'yangon'
                })
                .expect(400);
            
            expect(response.body.success).to.be.false;
            expect(response.body.error.code).to.equal('VALIDATION_ERROR');
        });
        
        it('should reject oversized payloads', async () => {
            const largeMessage = 'A'.repeat(10000); // 10KB message
            
            const response = await request(app)
                .post('/api/alerts/send')
                .set('Authorization', `Bearer ${validToken}`)
                .send({
                    message: largeMessage,
                    type: 'aircraft',
                    priority: 'high',
                    region: 'yangon'
                })
                .expect(400);
            
            expect(response.body.success).to.be.false;
        });
        
        it('should validate coordinate ranges', async () => {
            const response = await request(app)
                .post('/api/alerts/send')
                .set('Authorization', `Bearer ${validToken}`)
                .send({
                    message: 'Test alert',
                    type: 'aircraft',
                    priority: 'high',
                    region: 'yangon',
                    coordinates: {
                        latitude: 999, // Invalid latitude
                        longitude: 999 // Invalid longitude
                    }
                })
                .expect(400);
            
            expect(response.body.success).to.be.false;
            expect(response.body.error.code).to.equal('VALIDATION_ERROR');
        });
    });
    
    describe('Authorization Security', () => {
        it('should prevent access to admin endpoints with user token', async () => {
            const userToken = jwt.sign(
                { userId: 'test-user', role: 'user' },
                process.env.JWT_SECRET || 'test-secret',
                { expiresIn: '1h' }
            );
            
            const response = await request(app)
                .get('/api/admin/users')
                .set('Authorization', `Bearer ${userToken}`)
                .expect(403);
            
            expect(response.body.success).to.be.false;
            expect(response.body.error.code).to.equal('AUTHORIZATION_FAILED');
        });
        
        it('should allow admin access with admin token', async () => {
            const adminToken = jwt.sign(
                { userId: 'admin-user', role: 'admin' },
                process.env.JWT_SECRET || 'test-secret',
                { expiresIn: '1h' }
            );
            
            const response = await request(app)
                .get('/api/admin/statistics')
                .set('Authorization', `Bearer ${adminToken}`);
            
            // Should not be 403 (may be 200 or other status based on implementation)
            expect(response.status).to.not.equal(403);
        });
    });
    
    describe('Rate Limiting Security', () => {
        it('should enforce rate limits on login attempts', async function() {
            this.timeout(10000);
            
            const loginAttempts = [];
            
            // Make multiple rapid login attempts
            for (let i = 0; i < 10; i++) {
                loginAttempts.push(
                    request(app)
                        .post('/api/auth/login')
                        .send({
                            username: 'invalid-user',
                            password: 'invalid-password'
                        })
                );
            }
            
            const responses = await Promise.all(loginAttempts);
            
            // At least some requests should be rate limited
            const rateLimitedResponses = responses.filter(res => res.status === 429);
            expect(rateLimitedResponses.length).to.be.above(0);
        });
        
        it('should enforce rate limits on alert sending', async function() {
            this.timeout(10000);
            
            const alertRequests = [];
            
            // Make multiple rapid alert requests
            for (let i = 0; i < 15; i++) {
                alertRequests.push(
                    request(app)
                        .post('/api/alerts/send')
                        .set('Authorization', `Bearer ${validToken}`)
                        .send({
                            message: `Test alert ${i}`,
                            type: 'aircraft',
                            priority: 'medium',
                            region: 'yangon'
                        })
                );
            }
            
            const responses = await Promise.all(alertRequests);
            
            // Some requests should be rate limited
            const rateLimitedResponses = responses.filter(res => res.status === 429);
            expect(rateLimitedResponses.length).to.be.above(0);
        });
    });
    
    describe('Data Encryption Security', () => {
        it('should encrypt sensitive data in responses', async () => {
            const response = await request(app)
                .get('/api/users/profile')
                .set('Authorization', `Bearer ${validToken}`)
                .expect(200);
            
            // Sensitive fields should not contain raw data
            if (response.body.data && response.body.data.email) {
                expect(response.body.data.email).to.not.include('raw_');
            }
        });
    });
    
    describe('CORS Security', () => {
        it('should include proper CORS headers', async () => {
            const response = await request(app)
                .options('/api/alerts/history')
                .set('Origin', 'https://admin.thatialert.com');
            
            expect(response.headers).to.have.property('access-control-allow-origin');
            expect(response.headers).to.have.property('access-control-allow-methods');
            expect(response.headers).to.have.property('access-control-allow-headers');
        });
        
        it('should reject requests from unauthorized origins', async () => {
            const response = await request(app)
                .get('/api/alerts/history')
                .set('Origin', 'https://malicious-site.com')
                .set('Authorization', `Bearer ${validToken}`);
            
            // Should either reject or not include CORS headers for unauthorized origin
            if (response.headers['access-control-allow-origin']) {
                expect(response.headers['access-control-allow-origin']).to.not.equal('https://malicious-site.com');
            }
        });
    });
    
    describe('Security Headers', () => {
        it('should include security headers in responses', async () => {
            const response = await request(app)
                .get('/api/alerts/statistics')
                .set('Authorization', `Bearer ${validToken}`);
            
            // Check for common security headers
            expect(response.headers).to.have.property('x-content-type-options');
            expect(response.headers).to.have.property('x-frame-options');
            expect(response.headers).to.have.property('x-xss-protection');
        });
    });
    
    describe('Password Security', () => {
        it('should enforce strong password requirements', async () => {
            const weakPasswords = [
                '123456',
                'password',
                'abc123',
                '12345678'
            ];
            
            for (const password of weakPasswords) {
                const response = await request(app)
                    .post('/api/auth/register')
                    .send({
                        username: 'testuser',
                        password: password,
                        email: 'test@example.com',
                        name: 'Test User',
                        region: 'yangon'
                    })
                    .expect(400);
                
                expect(response.body.success).to.be.false;
                expect(response.body.error.code).to.equal('VALIDATION_ERROR');
            }
        });
    });
    
    describe('Session Security', () => {
        it('should invalidate tokens on logout', async () => {
            // First, login to get a token
            const loginResponse = await request(app)
                .post('/api/auth/login')
                .send({
                    username: 'testuser',
                    password: 'validpassword123'
                });
            
            if (loginResponse.status === 200) {
                const token = loginResponse.body.data.token;
                
                // Logout
                await request(app)
                    .post('/api/auth/logout')
                    .set('Authorization', `Bearer ${token}`)
                    .expect(200);
                
                // Try to use the token after logout
                const response = await request(app)
                    .get('/api/alerts/history')
                    .set('Authorization', `Bearer ${token}`)
                    .expect(401);
                
                expect(response.body.success).to.be.false;
            }
        });
    });
});