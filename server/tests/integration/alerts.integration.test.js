const request = require('supertest');
const { expect } = require('chai');
const app = require('../../src/server');
const db = require('../../src/database/connection');

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