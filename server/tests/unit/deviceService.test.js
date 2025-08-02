const { expect } = require('chai');
const sinon = require('sinon');

describe('Device Service', () => {
    let sandbox;
    let mockDatabase;
    let deviceService;

    beforeEach(() => {
        sandbox = sinon.createSandbox();
        mockDatabase = {
            query: sandbox.stub(),
            insert: sandbox.stub(),
            update: sandbox.stub(),
            delete: sandbox.stub()
        };

        // Mock device service functions
        deviceService = {
            registerDevice: async (deviceData) => {
                const validation = validateDeviceData(deviceData);
                if (!validation.isValid) {
                    throw new Error(validation.errors.join(', '));
                }
                
                const device = {
                    id: 'device123',
                    ...deviceData,
                    registeredAt: new Date(),
                    lastSeen: new Date(),
                    status: 'active'
                };
                
                await mockDatabase.insert('devices', device);
                return device;
            },

            updateDeviceLocation: async (deviceId, location) => {
                if (!isValidLocation(location)) {
                    throw new Error('Invalid location coordinates');
                }
                
                const updateData = {
                    latitude: location.latitude,
                    longitude: location.longitude,
                    lastSeen: new Date()
                };
                
                await mockDatabase.update('devices', updateData, { id: deviceId });
                return { deviceId, ...updateData };
            },

            getDevicesByUser: async (userId) => {
                const result = await mockDatabase.query('SELECT * FROM devices WHERE userId = ?', [userId]);
                return result;
            },

            updateDeviceStatus: async (deviceId, status) => {
                if (!['active', 'inactive', 'offline'].includes(status)) {
                    throw new Error('Invalid device status');
                }
                
                await mockDatabase.update('devices', { status, lastSeen: new Date() }, { id: deviceId });
                return { deviceId, status };
            },

            getActiveDevices: async () => {
                const result = await mockDatabase.query('SELECT * FROM devices WHERE status = ?', ['active']);
                return result;
            },

            deleteDevice: async (deviceId) => {
                await mockDatabase.delete('devices', { id: deviceId });
                return { success: true };
            },

            updateDeviceToken: async (deviceId, deviceToken) => {
                if (!deviceToken || deviceToken.length < 10) {
                    throw new Error('Invalid device token');
                }
                
                await mockDatabase.update('devices', { deviceToken, lastSeen: new Date() }, { id: deviceId });
                return { deviceId, deviceToken };
            }
        };
    });

    afterEach(() => {
        sandbox.restore();
    });

    describe('registerDevice', () => {
        it('should register device with valid data', async () => {
            const deviceData = {
                userId: 'user123',
                deviceToken: 'firebase_token_123',
                deviceType: 'android',
                deviceModel: 'Samsung Galaxy S21',
                osVersion: '11.0',
                appVersion: '1.0.0',
                location: {
                    latitude: 16.8661,
                    longitude: 96.1951
                }
            };

            mockDatabase.insert.resolves();

            const result = await deviceService.registerDevice(deviceData);

            expect(result.id).to.equal('device123');
            expect(result.userId).to.equal('user123');
            expect(result.deviceToken).to.equal('firebase_token_123');
            expect(result.status).to.equal('active');
            expect(mockDatabase.insert.calledOnce).to.be.true;
        });

        it('should reject device without userId', async () => {
            const deviceData = {
                deviceToken: 'firebase_token_123',
                deviceType: 'android'
            };

            try {
                await deviceService.registerDevice(deviceData);
                expect.fail('Should have thrown validation error');
            } catch (error) {
                expect(error.message).to.include('User ID is required');
            }
        });

        it('should reject device with invalid location', async () => {
            const deviceData = {
                userId: 'user123',
                deviceToken: 'firebase_token_123',
                deviceType: 'android',
                location: {
                    latitude: 200, // Invalid latitude
                    longitude: 96.1951
                }
            };

            try {
                await deviceService.registerDevice(deviceData);
                expect.fail('Should have thrown validation error');
            } catch (error) {
                expect(error.message).to.include('Invalid location coordinates');
            }
        });
    });

    describe('updateDeviceLocation', () => {
        it('should update device location with valid coordinates', async () => {
            const location = {
                latitude: 16.8661,
                longitude: 96.1951
            };

            mockDatabase.update.resolves();

            const result = await deviceService.updateDeviceLocation('device123', location);

            expect(result.deviceId).to.equal('device123');
            expect(result.latitude).to.equal(16.8661);
            expect(result.longitude).to.equal(96.1951);
            expect(mockDatabase.update.calledOnce).to.be.true;
        });

        it('should reject invalid coordinates', async () => {
            const location = {
                latitude: 200,
                longitude: 96.1951
            };

            try {
                await deviceService.updateDeviceLocation('device123', location);
                expect.fail('Should have thrown validation error');
            } catch (error) {
                expect(error.message).to.include('Invalid location coordinates');
            }
        });
    });

    describe('getDevicesByUser', () => {
        it('should return devices for user', async () => {
            const mockDevices = [
                { id: 'device1', userId: 'user123', deviceType: 'android' },
                { id: 'device2', userId: 'user123', deviceType: 'ios' }
            ];

            mockDatabase.query.resolves(mockDevices);

            const result = await deviceService.getDevicesByUser('user123');

            expect(result).to.have.length(2);
            expect(result[0].userId).to.equal('user123');
            expect(result[1].userId).to.equal('user123');
            expect(mockDatabase.query.calledWith('SELECT * FROM devices WHERE userId = ?', ['user123'])).to.be.true;
        });

        it('should return empty array when no devices found', async () => {
            mockDatabase.query.resolves([]);

            const result = await deviceService.getDevicesByUser('user123');

            expect(result).to.be.an('array').that.is.empty;
        });
    });

    describe('updateDeviceStatus', () => {
        it('should update device status with valid status', async () => {
            mockDatabase.update.resolves();

            const result = await deviceService.updateDeviceStatus('device123', 'inactive');

            expect(result.deviceId).to.equal('device123');
            expect(result.status).to.equal('inactive');
            expect(mockDatabase.update.calledOnce).to.be.true;
        });

        it('should reject invalid status', async () => {
            try {
                await deviceService.updateDeviceStatus('device123', 'invalid_status');
                expect.fail('Should have thrown validation error');
            } catch (error) {
                expect(error.message).to.include('Invalid device status');
            }
        });
    });

    describe('getActiveDevices', () => {
        it('should return only active devices', async () => {
            const mockDevices = [
                { id: 'device1', status: 'active', userId: 'user1' },
                { id: 'device2', status: 'active', userId: 'user2' }
            ];

            mockDatabase.query.resolves(mockDevices);

            const result = await deviceService.getActiveDevices();

            expect(result).to.have.length(2);
            expect(result[0].status).to.equal('active');
            expect(result[1].status).to.equal('active');
            expect(mockDatabase.query.calledWith('SELECT * FROM devices WHERE status = ?', ['active'])).to.be.true;
        });
    });

    describe('deleteDevice', () => {
        it('should delete device successfully', async () => {
            mockDatabase.delete.resolves();

            const result = await deviceService.deleteDevice('device123');

            expect(result.success).to.be.true;
            expect(mockDatabase.delete.calledWith('devices', { id: 'device123' })).to.be.true;
        });
    });

    describe('updateDeviceToken', () => {
        it('should update device token with valid token', async () => {
            const deviceToken = 'new_firebase_token_456';
            mockDatabase.update.resolves();

            const result = await deviceService.updateDeviceToken('device123', deviceToken);

            expect(result.deviceId).to.equal('device123');
            expect(result.deviceToken).to.equal(deviceToken);
            expect(mockDatabase.update.calledOnce).to.be.true;
        });

        it('should reject invalid device token', async () => {
            const invalidToken = 'short';

            try {
                await deviceService.updateDeviceToken('device123', invalidToken);
                expect.fail('Should have thrown validation error');
            } catch (error) {
                expect(error.message).to.include('Invalid device token');
            }
        });
    });
});

// Helper validation functions
function validateDeviceData(deviceData) {
    const errors = [];

    if (!deviceData.userId) {
        errors.push('User ID is required');
    }

    if (!deviceData.deviceToken || deviceData.deviceToken.length < 10) {
        errors.push('Valid device token is required');
    }

    if (!deviceData.deviceType || !['android', 'ios'].includes(deviceData.deviceType)) {
        errors.push('Valid device type is required');
    }

    if (deviceData.location && !isValidLocation(deviceData.location)) {
        errors.push('Invalid location coordinates');
    }

    return {
        isValid: errors.length === 0,
        errors
    };
}

function isValidLocation(location) {
    if (!location || typeof location.latitude !== 'number' || typeof location.longitude !== 'number') {
        return false;
    }
    
    return location.latitude >= -90 && location.latitude <= 90 &&
           location.longitude >= -180 && location.longitude <= 180;
}