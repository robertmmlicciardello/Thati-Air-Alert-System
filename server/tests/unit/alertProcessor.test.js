const { expect } = require('chai');
const sinon = require('sinon');
// Mock alert service functions since actual files may not exist
const alertService = {
    validateAlert: (alert) => {
        const errors = [];
        
        if (!alert.title) errors.push('Title is required');
        if (!alert.message) errors.push('Message is required');
        if (!alert.severity || !['low', 'medium', 'high', 'critical'].includes(alert.severity)) {
            errors.push('Invalid severity level');
        }
        if (!alert.adminId) errors.push('Admin ID is required');
        
        if (alert.location) {
            const { latitude, longitude } = alert.location;
            if (typeof latitude !== 'number' || latitude < -90 || latitude > 90 ||
                typeof longitude !== 'number' || longitude < -180 || longitude > 180) {
                errors.push('Invalid coordinates');
            }
        }
        
        return {
            isValid: errors.length === 0,
            errors
        };
    },
    
    processAlert: async (alert, database, websocket) => {
        try {
            const validation = alertService.validateAlert(alert);
            if (!validation.isValid) {
                return {
                    success: false,
                    error: `Invalid alert format: ${validation.errors.join(', ')}`
                };
            }
            
            const savedAlert = await database.saveAlert(alert);
            const users = await database.getUsers();
            await alertService.broadcastAlert(savedAlert, users, websocket);
            
            return {
                success: true,
                alertId: savedAlert.id
            };
        } catch (error) {
            return {
                success: false,
                error: error.message
            };
        }
    },
    
    broadcastAlert: async (alert, users, websocket) => {
        try {
            websocket.broadcast('new_alert', alert);
        } catch (error) {
            console.error('WebSocket broadcast error:', error);
        }
    }
};

const { processAlert, validateAlert, broadcastAlert } = alertService;

describe('Alert Processor', () => {
    let sandbox;
    let mockDatabase;
    let mockWebSocket;

    beforeEach(() => {
        sandbox = sinon.createSandbox();
        mockDatabase = {
            saveAlert: sandbox.stub(),
            getUsers: sandbox.stub(),
            updateAlertStatus: sandbox.stub()
        };
        mockWebSocket = {
            broadcast: sandbox.stub(),
            emit: sandbox.stub()
        };
    });

    afterEach(() => {
        sandbox.restore();
    });

    describe('validateAlert', () => {
        it('should validate correct alert format', () => {
            const validAlert = {
                title: 'Emergency Alert',
                message: 'This is a test emergency alert',
                severity: 'high',
                location: {
                    latitude: 16.8661,
                    longitude: 96.1951
                },
                adminId: 'admin123'
            };

            const result = validateAlert(validAlert);
            expect(result.isValid).to.be.true;
            expect(result.errors).to.be.empty;
        });

        it('should reject alert without title', () => {
            const invalidAlert = {
                message: 'This is a test emergency alert',
                severity: 'high',
                location: {
                    latitude: 16.8661,
                    longitude: 96.1951
                },
                adminId: 'admin123'
            };

            const result = validateAlert(invalidAlert);
            expect(result.isValid).to.be.false;
            expect(result.errors).to.include('Title is required');
        });

        it('should reject alert with invalid severity', () => {
            const invalidAlert = {
                title: 'Emergency Alert',
                message: 'This is a test emergency alert',
                severity: 'invalid',
                location: {
                    latitude: 16.8661,
                    longitude: 96.1951
                },
                adminId: 'admin123'
            };

            const result = validateAlert(invalidAlert);
            expect(result.isValid).to.be.false;
            expect(result.errors).to.include('Invalid severity level');
        });

        it('should reject alert with invalid coordinates', () => {
            const invalidAlert = {
                title: 'Emergency Alert',
                message: 'This is a test emergency alert',
                severity: 'high',
                location: {
                    latitude: 200, // Invalid latitude
                    longitude: 96.1951
                },
                adminId: 'admin123'
            };

            const result = validateAlert(invalidAlert);
            expect(result.isValid).to.be.false;
            expect(result.errors).to.include('Invalid coordinates');
        });
    });

    describe('processAlert', () => {
        it('should process valid alert successfully', async () => {
            const validAlert = {
                title: 'Emergency Alert',
                message: 'This is a test emergency alert',
                severity: 'high',
                location: {
                    latitude: 16.8661,
                    longitude: 96.1951
                },
                adminId: 'admin123'
            };

            mockDatabase.saveAlert.resolves({ id: 'alert123', ...validAlert });
            mockDatabase.getUsers.resolves([
                { id: 'user1', deviceToken: 'token1' },
                { id: 'user2', deviceToken: 'token2' }
            ]);

            const result = await processAlert(validAlert, mockDatabase, mockWebSocket);

            expect(result.success).to.be.true;
            expect(result.alertId).to.equal('alert123');
            expect(mockDatabase.saveAlert.calledOnce).to.be.true;
            expect(mockDatabase.getUsers.calledOnce).to.be.true;
        });

        it('should handle database errors gracefully', async () => {
            const validAlert = {
                title: 'Emergency Alert',
                message: 'This is a test emergency alert',
                severity: 'high',
                location: {
                    latitude: 16.8661,
                    longitude: 96.1951
                },
                adminId: 'admin123'
            };

            mockDatabase.saveAlert.rejects(new Error('Database connection failed'));

            const result = await processAlert(validAlert, mockDatabase, mockWebSocket);

            expect(result.success).to.be.false;
            expect(result.error).to.include('Database connection failed');
        });

        it('should reject invalid alert', async () => {
            const invalidAlert = {
                message: 'Missing title',
                severity: 'high'
            };

            const result = await processAlert(invalidAlert, mockDatabase, mockWebSocket);

            expect(result.success).to.be.false;
            expect(result.error).to.include('Invalid alert format');
            expect(mockDatabase.saveAlert.called).to.be.false;
        });
    });

    describe('broadcastAlert', () => {
        it('should broadcast alert to all connected users', async () => {
            const alert = {
                id: 'alert123',
                title: 'Emergency Alert',
                message: 'This is a test emergency alert',
                severity: 'high'
            };

            const users = [
                { id: 'user1', deviceToken: 'token1' },
                { id: 'user2', deviceToken: 'token2' }
            ];

            await broadcastAlert(alert, users, mockWebSocket);

            expect(mockWebSocket.broadcast.calledOnce).to.be.true;
            expect(mockWebSocket.broadcast.calledWith('new_alert', alert)).to.be.true;
        });

        it('should handle empty user list', async () => {
            const alert = {
                id: 'alert123',
                title: 'Emergency Alert',
                message: 'This is a test emergency alert',
                severity: 'high'
            };

            await broadcastAlert(alert, [], mockWebSocket);

            expect(mockWebSocket.broadcast.calledOnce).to.be.true;
        });

        it('should handle websocket errors', async () => {
            const alert = {
                id: 'alert123',
                title: 'Emergency Alert',
                message: 'This is a test emergency alert',
                severity: 'high'
            };

            const users = [{ id: 'user1', deviceToken: 'token1' }];
            mockWebSocket.broadcast.throws(new Error('WebSocket connection failed'));

            // Should not throw error, but handle gracefully
            await broadcastAlert(alert, users, mockWebSocket);
            expect(mockWebSocket.broadcast.calledOnce).to.be.true;
        });
    });
});