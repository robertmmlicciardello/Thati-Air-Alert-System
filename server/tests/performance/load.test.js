const autocannon = require('autocannon');
const { expect } = require('chai');
const express = require('express');
const http = require('http');

// Mock server app for performance testing
const app = express();
app.use(express.json());

// Mock routes for performance testing
app.post('/api/alerts/send', (req, res) => {
    // Simulate processing delay
    setTimeout(() => {
        res.json({
            id: `alert-${Date.now()}`,
            message: req.body.message || 'Test alert',
            status: 'sent',
            timestamp: new Date().toISOString()
        });
    }, Math.random() * 10); // Random delay 0-10ms
});

app.get('/api/alerts', (req, res) => {
    // Simulate database query delay
    setTimeout(() => {
        const alerts = Array.from({ length: 50 }, (_, i) => ({
            id: `alert-${i}`,
            message: `Test alert ${i}`,
            status: 'sent',
            timestamp: new Date().toISOString()
        }));
        res.json(alerts);
    }, Math.random() * 20); // Random delay 0-20ms
});

app.get('/api/health', (req, res) => {
    res.json({
        status: 'healthy',
        timestamp: new Date().toISOString(),
        uptime: process.uptime()
    });
});

app.post('/api/auth/login', (req, res) => {
    // Simulate authentication delay
    setTimeout(() => {
        res.json({
            token: 'mock-jwt-token',
            user: { id: 'test-user', username: req.body.username }
        });
    }, Math.random() * 50); // Random delay 0-50ms
});

// Create HTTP server
const server = http.createServer(app);

/**
 * Performance and Load Tests
 * Tests system performance under various load conditions
 */
describe('Performance Tests', () => {
    let server;
    const baseURL = 'http://localhost:3001';
    
    before((done) => {
        server.listen(3001, done);
    });
    
    after((done) => {
        server.close(done);
    });
    
    describe('Alert Sending Performance', () => {
        it('should handle 100 concurrent alert requests', async function() {
            this.timeout(30000); // 30 second timeout
            
            const result = await autocannon({
                url: `${baseURL}/api/alerts/send`,
                connections: 10,
                pipelining: 1,
                duration: 10,
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': 'Bearer test-token'
                },
                body: JSON.stringify({
                    message: 'Performance test alert',
                    type: 'aircraft',
                    priority: 'medium',
                    region: 'yangon'
                })
            });
            
            console.log('Alert sending performance results:');
            console.log(`Requests per second: ${result.requests.average}`);
            console.log(`Average latency: ${result.latency.average}ms`);
            console.log(`Error rate: ${(result.errors / result.requests.total * 100).toFixed(2)}%`);
            
            // Performance assertions
            expect(result.requests.average).to.be.above(50); // At least 50 RPS
            expect(result.latency.average).to.be.below(500); // Under 500ms average
            expect(result.errors).to.equal(0); // No errors
        });
    });
    
    describe('Alert History Performance', () => {
        it('should handle concurrent history requests efficiently', async function() {
            this.timeout(20000);
            
            const result = await autocannon({
                url: `${baseURL}/api/alerts/history`,
                connections: 20,
                pipelining: 1,
                duration: 10,
                headers: {
                    'Authorization': 'Bearer test-token'
                }
            });
            
            console.log('Alert history performance results:');
            console.log(`Requests per second: ${result.requests.average}`);
            console.log(`Average latency: ${result.latency.average}ms`);
            
            expect(result.requests.average).to.be.above(100); // At least 100 RPS
            expect(result.latency.average).to.be.below(200); // Under 200ms average
        });
    });
    
    describe('WebSocket Connection Performance', () => {
        it('should handle multiple WebSocket connections', function(done) {
            this.timeout(15000);
            
            const WebSocket = require('ws');
            const connections = [];
            const targetConnections = 50;
            let connectedCount = 0;
            let messagesReceived = 0;
            
            // Create multiple WebSocket connections
            for (let i = 0; i < targetConnections; i++) {
                const ws = new WebSocket(`ws://localhost:3001/ws?token=test-token`);
                
                ws.on('open', () => {
                    connectedCount++;
                    if (connectedCount === targetConnections) {
                        // All connections established, start test
                        setTimeout(() => {
                            // Send a test message to trigger broadcasts
                            const testWs = new WebSocket(`ws://localhost:3001/ws?token=test-token`);
                            testWs.on('open', () => {
                                testWs.send(JSON.stringify({
                                    type: 'test_broadcast',
                                    data: { message: 'Performance test' }
                                }));
                                testWs.close();
                            });
                        }, 1000);
                    }
                });
                
                ws.on('message', (data) => {
                    messagesReceived++;
                    if (messagesReceived >= targetConnections) {
                        // All connections received the message
                        connections.forEach(conn => conn.close());
                        
                        expect(connectedCount).to.equal(targetConnections);
                        expect(messagesReceived).to.be.at.least(targetConnections);
                        done();
                    }
                });
                
                ws.on('error', (error) => {
                    console.error('WebSocket error:', error);
                    done(error);
                });
                
                connections.push(ws);
            }
        });
    });
    
    describe('Database Performance', () => {
        it('should handle concurrent database queries efficiently', async function() {
            this.timeout(10000);
            
            const db = require('../../src/database/connection');
            const startTime = Date.now();
            const concurrentQueries = 100;
            
            const queries = Array(concurrentQueries).fill().map(() => 
                db.query('SELECT COUNT(*) FROM alerts WHERE created_at > NOW() - INTERVAL \'1 day\'')
            );
            
            const results = await Promise.all(queries);
            const endTime = Date.now();
            const totalTime = endTime - startTime;
            
            console.log(`Database performance: ${concurrentQueries} queries in ${totalTime}ms`);
            console.log(`Average query time: ${(totalTime / concurrentQueries).toFixed(2)}ms`);
            
            expect(results.length).to.equal(concurrentQueries);
            expect(totalTime).to.be.below(5000); // Under 5 seconds total
            expect(totalTime / concurrentQueries).to.be.below(100); // Under 100ms per query
        });
    });
    
    describe('Memory Usage', () => {
        it('should maintain stable memory usage under load', async function() {
            this.timeout(20000);
            
            const initialMemory = process.memoryUsage();
            console.log('Initial memory usage:', initialMemory);
            
            // Simulate load by creating many alert objects
            const alerts = [];
            for (let i = 0; i < 10000; i++) {
                alerts.push({
                    id: `alert-${i}`,
                    message: `Test alert message ${i}`,
                    type: 'aircraft',
                    priority: 'medium',
                    region: 'yangon',
                    timestamp: new Date().toISOString()
                });
            }
            
            // Force garbage collection if available
            if (global.gc) {
                global.gc();
            }
            
            const finalMemory = process.memoryUsage();
            console.log('Final memory usage:', finalMemory);
            
            const memoryIncrease = finalMemory.heapUsed - initialMemory.heapUsed;
            const memoryIncreaseMB = memoryIncrease / 1024 / 1024;
            
            console.log(`Memory increase: ${memoryIncreaseMB.toFixed(2)}MB`);
            
            // Memory increase should be reasonable (less than 100MB for this test)
            expect(memoryIncreaseMB).to.be.below(100);
        });
    });
    
    describe('Response Time Distribution', () => {
        it('should have consistent response times', async function() {
            this.timeout(15000);
            
            const responseTimes = [];
            const requestCount = 100;
            
            for (let i = 0; i < requestCount; i++) {
                const startTime = Date.now();
                
                try {
                    await new Promise((resolve, reject) => {
                        const req = require('http').request({
                            hostname: 'localhost',
                            port: 3001,
                            path: '/api/alerts/statistics',
                            method: 'GET',
                            headers: {
                                'Authorization': 'Bearer test-token'
                            }
                        }, (res) => {
                            res.on('data', () => {});
                            res.on('end', resolve);
                        });
                        
                        req.on('error', reject);
                        req.end();
                    });
                    
                    const responseTime = Date.now() - startTime;
                    responseTimes.push(responseTime);
                } catch (error) {
                    console.error('Request failed:', error);
                }
            }
            
            // Calculate statistics
            const avgResponseTime = responseTimes.reduce((a, b) => a + b, 0) / responseTimes.length;
            const sortedTimes = responseTimes.sort((a, b) => a - b);
            const p95ResponseTime = sortedTimes[Math.floor(sortedTimes.length * 0.95)];
            const p99ResponseTime = sortedTimes[Math.floor(sortedTimes.length * 0.99)];
            
            console.log(`Average response time: ${avgResponseTime.toFixed(2)}ms`);
            console.log(`95th percentile: ${p95ResponseTime}ms`);
            console.log(`99th percentile: ${p99ResponseTime}ms`);
            
            expect(avgResponseTime).to.be.below(200); // Average under 200ms
            expect(p95ResponseTime).to.be.below(500); // 95% under 500ms
            expect(p99ResponseTime).to.be.below(1000); // 99% under 1s
        });
    });
});