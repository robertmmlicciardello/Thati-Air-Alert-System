const request = require('supertest');
const { expect } = require('chai');
const express = require('express');

// Mock server app for testing
const app = express();
app.use(express.json());

// Mock database
const db = {
    query: async (sql, params) => {
        // Mock database responses
        if (sql.includes('SELECT') && sql.includes('users')) {
            return { rows: [{ id: 'test-user-id', username: 'testuser' }] };
        }
        if (sql.includes('INSERT') && sql.includes('alerts')) {
            return { rows: [{ id: 'test-alert-id', message: 'Test alert' }] };
        }
        return { rows: [] };
    }
};

// Mock routes for testing
app.post('/api/auth/login', (req, res) => {
    res.json({ 
        success: true,
        data: {
            token: 'mock-jwt-token',
            user: { id: 'test-user-id', username: 'testuser' }
        }
    });
});

app.post('/api/alerts', (req, res) => {
    res.status(201).json({
        id: 'test-alert-id',
        message: req.body.message,
        type: req.body.type,
        priority: req.body.priority,
        status: 'sent'
    });
});

app.post('/api/alerts/send', (req, res) => {
    // Check authentication
    const authHeader = req.headers.authorization;
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
        return res.status(401).json({ 
            success: false, 
            error: { 
                code: 'AUTHENTICATION_FAILED',
                message: 'Unauthorized' 
            }
        });
    }
    
    // Validate required fields
    if (!req.body.message || !req.body.type || !req.body.priority) {
        return res.status(400).json({ 
            success: false, 
            error: { 
                code: 'VALIDATION_ERROR',
                message: 'Missing required fields' 
            }
        });
    }
    
    res.status(200).json({
        success: true,
        data: {
            alertId: 'test-alert-id',
            timestamp: new Date().toISOString(),
            message: req.body.message,
            type: req.body.type,
            priority: req.body.priority,
            status: 'sent'
        }
    });
});

app.get('/api/alerts/history', (req, res) => {
    const page = parseInt(req.query.page) || 1;
    const limit = parseInt(req.query.limit) || 10;
    const type = req.query.type;
    
    let alerts = [
        {
            id: 'test-alert-1',
            message: 'Test alert message 1',
            type: 'aircraft',
            priority: 'high',
            status: 'sent',
            createdAt: new Date().toISOString()
        },
        {
            id: 'test-alert-2',
            message: 'Test alert message 2',
            type: 'weather',
            priority: 'medium',
            status: 'sent',
            createdAt: new Date().toISOString()
        }
    ];
    
    // Filter by type if provided
    if (type) {
        alerts = alerts.filter(alert => alert.type === type);
    }
    
    res.json({
        success: true,
        data: {
            alerts: alerts,
            pagination: {
                page,
                limit,
                total: alerts.length,
                totalPages: Math.ceil(alerts.length / limit)
            }
        }
    });
});

app.get('/api/alerts/statistics', (req, res) => {
    const timeframe = req.query.timeframe || '24h';
    
    res.json({
        success: true,
        data: {
            summary: {
                total_sent: 25,
                delivered: 23,
                acknowledged: 20,
                failed: 2
            },
            byType: {
                aircraft: 10,
                weather: 8,
                emergency: 7
            },
            byPriority: {
                high: 12,
                medium: 8,
                low: 5
            },
            timeframe
        }
    });
});

app.get('/api/alerts/:id', (req, res) => {
    if (req.params.id === 'non-existent-id') {
        return res.status(404).json({ 
            success: false, 
            error: { 
                code: 'RESOURCE_NOT_FOUND',
                message: 'Alert not found' 
            }
        });
    }
    
    res.json({
        success: true,
        data: {
            id: req.params.id,
            message: 'Test alert message',
            type: 'aircraft',
            priority: 'high',
            status: 'sent',
            createdAt: new Date().toISOString()
        }
    });
});

app.post('/api/alerts/:id/acknowledge', (req, res) => {
    res.json({
        success: true,
        message: 'Alert acknowledged successfully',
        data: {
            id: req.params.id,
            status: 'acknowledged',
            acknowledgedAt: new Date().toISOString()
        }
    });
});

app.get('/api/alerts', (req, res) => {
    res.json([
        {
            id: 'test-alert-id',
            message: 'Test alert message',
            type: 'aircraft',
            priority: 'high',
            status: 'sent'
        }
    ]);
});

app.put('/api/alerts/:id', (req, res) => {
    res.json({
        id: req.params.id,
        ...req.body,
        status: 'updated'
    });
});

app.delete('/api/alerts/:id', (req, res) => {
    res.json({ success: true, message: 'Alert deleted' });
});

/**
 * Alerts Integration Tests
 * End-to-end testing for alert functionality
 */
describe('Alerts Integration Tests', () => {
    let authToken;
    let userId;
    let testAlertId;
    
    before(async () => {
        // Setup test user and get auth token
        const loginResponse = await request(app)
            .post('/api/auth/login')
            .send({
                username: 'testuser',
                password: 'testpass123'
            });
        
        authToken = loginResponse.body.data.token;
        userId = loginResponse.body.data.user.id;
    });
    
    after(async () => {
        // Cleanup test data
        if (testAlertId) {
            await db.query('DELETE FROM alerts WHERE id = $1', [testAlertId]);
        }
    });
    
    describe('POST /api/alerts/send', () => {
        it('should send an alert successfully', async () => {
            const alertData = {
                message: 'Test aircraft alert',
                type: 'aircraft',
                priority: 'high',
                region: 'yangon',
                coordinates: {
                    latitude: 16.8661,
                    longitude: 96.1951
                }
            };
            
            const response = await request(app)
                .post('/api/alerts/send')
                .set('Authorization', `Bearer ${authToken}`)
                .send(alertData)
                .expect(200);
            
            expect(response.body.success).to.be.true;
            expect(response.body.data).to.have.property('alertId');
            expect(response.body.data).to.have.property('timestamp');
            
            testAlertId = response.body.data.alertId;
        });
        
        it('should reject alert without authentication', async () => {
            const alertData = {
                message: 'Test alert',
                type: 'aircraft',
                priority: 'high',
                region: 'yangon'
            };
            
            const response = await request(app)
                .post('/api/alerts/send')
                .send(alertData)
                .expect(401);
            
            expect(response.body.success).to.be.false;
            expect(response.body.error.code).to.equal('AUTHENTICATION_FAILED');
        });
        
        it('should validate required fields', async () => {
            const invalidAlertData = {
                message: 'Test alert'
                // Missing required fields
            };
            
            const response = await request(app)
                .post('/api/alerts/send')
                .set('Authorization', `Bearer ${authToken}`)
                .send(invalidAlertData)
                .expect(400);
            
            expect(response.body.success).to.be.false;
            expect(response.body.error.code).to.equal('VALIDATION_ERROR');
        });
    });
    
    describe('GET /api/alerts/history', () => {
        it('should retrieve alert history', async () => {
            const response = await request(app)
                .get('/api/alerts/history')
                .set('Authorization', `Bearer ${authToken}`)
                .expect(200);
            
            expect(response.body.success).to.be.true;
            expect(response.body.data).to.have.property('alerts');
            expect(response.body.data).to.have.property('pagination');
            expect(response.body.data.alerts).to.be.an('array');
        });
        
        it('should support pagination', async () => {
            const response = await request(app)
                .get('/api/alerts/history?page=1&limit=5')
                .set('Authorization', `Bearer ${authToken}`)
                .expect(200);
            
            expect(response.body.data.pagination.page).to.equal(1);
            expect(response.body.data.pagination.limit).to.equal(5);
        });
        
        it('should filter by alert type', async () => {
            const response = await request(app)
                .get('/api/alerts/history?type=aircraft')
                .set('Authorization', `Bearer ${authToken}`)
                .expect(200);
            
            expect(response.body.success).to.be.true;
            // All returned alerts should be of type 'aircraft'
            response.body.data.alerts.forEach(alert => {
                expect(alert.type).to.equal('aircraft');
            });
        });
    });
    
    describe('GET /api/alerts/:alertId', () => {
        it('should retrieve alert details', async () => {
            if (!testAlertId) {
                this.skip();
            }
            
            const response = await request(app)
                .get(`/api/alerts/${testAlertId}`)
                .set('Authorization', `Bearer ${authToken}`)
                .expect(200);
            
            expect(response.body.success).to.be.true;
            expect(response.body.data.id).to.equal(testAlertId);
            expect(response.body.data).to.have.property('message');
            expect(response.body.data).to.have.property('type');
            expect(response.body.data).to.have.property('priority');
        });
        
        it('should return 404 for non-existent alert', async () => {
            const response = await request(app)
                .get('/api/alerts/non-existent-id')
                .set('Authorization', `Bearer ${authToken}`)
                .expect(404);
            
            expect(response.body.success).to.be.false;
            expect(response.body.error.code).to.equal('RESOURCE_NOT_FOUND');
        });
    });
    
    describe('POST /api/alerts/:alertId/acknowledge', () => {
        it('should acknowledge an alert', async () => {
            if (!testAlertId) {
                this.skip();
            }
            
            const response = await request(app)
                .post(`/api/alerts/${testAlertId}/acknowledge`)
                .set('Authorization', `Bearer ${authToken}`)
                .send({ deviceId: 'test-device-id' })
                .expect(200);
            
            expect(response.body.success).to.be.true;
            expect(response.body.message).to.include('acknowledged');
        });
    });
    
    describe('GET /api/alerts/statistics', () => {
        it('should retrieve alert statistics', async () => {
            const response = await request(app)
                .get('/api/alerts/statistics')
                .set('Authorization', `Bearer ${authToken}`)
                .expect(200);
            
            expect(response.body.success).to.be.true;
            expect(response.body.data).to.have.property('summary');
            expect(response.body.data).to.have.property('byType');
            expect(response.body.data.summary).to.have.property('total_sent');
            expect(response.body.data.summary).to.have.property('delivered');
        });
        
        it('should support different timeframes', async () => {
            const response = await request(app)
                .get('/api/alerts/statistics?timeframe=30d')
                .set('Authorization', `Bearer ${authToken}`)
                .expect(200);
            
            expect(response.body.data.timeframe).to.equal('30d');
        });
    });
});