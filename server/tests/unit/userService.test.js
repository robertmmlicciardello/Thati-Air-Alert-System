const { expect } = require('chai');
const sinon = require('sinon');

describe('User Service', () => {
    let sandbox;
    let mockDatabase;
    let userService;

    beforeEach(() => {
        sandbox = sinon.createSandbox();
        mockDatabase = {
            query: sandbox.stub(),
            insert: sandbox.stub(),
            update: sandbox.stub(),
            delete: sandbox.stub()
        };

        // Mock user service functions
        userService = {
            createUser: async (userData) => {
                const validation = validateUserData(userData);
                if (!validation.isValid) {
                    throw new Error(validation.errors.join(', '));
                }
                
                const hashedPassword = await require('bcryptjs').hash(userData.password, 10);
                const user = {
                    id: 'user123',
                    ...userData,
                    password: hashedPassword,
                    createdAt: new Date()
                };
                
                await mockDatabase.insert('users', user);
                return { ...user, password: undefined };
            },

            getUserById: async (userId) => {
                const result = await mockDatabase.query('SELECT * FROM users WHERE id = ?', [userId]);
                return result[0] || null;
            },

            updateUser: async (userId, updateData) => {
                const validation = validateUpdateData(updateData);
                if (!validation.isValid) {
                    throw new Error(validation.errors.join(', '));
                }
                
                await mockDatabase.update('users', updateData, { id: userId });
                return { id: userId, ...updateData };
            },

            deleteUser: async (userId) => {
                await mockDatabase.delete('users', { id: userId });
                return { success: true };
            },

            getUsersByRole: async (role) => {
                const result = await mockDatabase.query('SELECT * FROM users WHERE role = ?', [role]);
                return result.map(user => ({ ...user, password: undefined }));
            }
        };
    });

    afterEach(() => {
        sandbox.restore();
    });

    describe('createUser', () => {
        it('should create user with valid data', async () => {
            const userData = {
                username: 'testuser',
                email: 'test@example.com',
                password: 'password123',
                role: 'user',
                location: {
                    latitude: 16.8661,
                    longitude: 96.1951
                }
            };

            mockDatabase.insert.resolves();

            const result = await userService.createUser(userData);

            expect(result.id).to.equal('user123');
            expect(result.username).to.equal('testuser');
            expect(result.email).to.equal('test@example.com');
            expect(result.password).to.be.undefined; // Password should be excluded
            expect(mockDatabase.insert.calledOnce).to.be.true;
        });

        it('should reject user with invalid email', async () => {
            const userData = {
                username: 'testuser',
                email: 'invalid-email',
                password: 'password123',
                role: 'user'
            };

            try {
                await userService.createUser(userData);
                expect.fail('Should have thrown validation error');
            } catch (error) {
                expect(error.message).to.include('Invalid email format');
            }
        });

        it('should reject user with weak password', async () => {
            const userData = {
                username: 'testuser',
                email: 'test@example.com',
                password: '123',
                role: 'user'
            };

            try {
                await userService.createUser(userData);
                expect.fail('Should have thrown validation error');
            } catch (error) {
                expect(error.message).to.include('Password too weak');
            }
        });

        it('should handle database errors', async () => {
            const userData = {
                username: 'testuser',
                email: 'test@example.com',
                password: 'password123',
                role: 'user'
            };

            mockDatabase.insert.rejects(new Error('Database connection failed'));

            try {
                await userService.createUser(userData);
                expect.fail('Should have thrown database error');
            } catch (error) {
                expect(error.message).to.include('Database connection failed');
            }
        });
    });

    describe('getUserById', () => {
        it('should return user when found', async () => {
            const mockUser = {
                id: 'user123',
                username: 'testuser',
                email: 'test@example.com',
                role: 'user'
            };

            mockDatabase.query.resolves([mockUser]);

            const result = await userService.getUserById('user123');

            expect(result).to.deep.equal(mockUser);
            expect(mockDatabase.query.calledWith('SELECT * FROM users WHERE id = ?', ['user123'])).to.be.true;
        });

        it('should return null when user not found', async () => {
            mockDatabase.query.resolves([]);

            const result = await userService.getUserById('nonexistent');

            expect(result).to.be.null;
        });
    });

    describe('updateUser', () => {
        it('should update user with valid data', async () => {
            const updateData = {
                username: 'updateduser',
                email: 'updated@example.com'
            };

            mockDatabase.update.resolves();

            const result = await userService.updateUser('user123', updateData);

            expect(result.id).to.equal('user123');
            expect(result.username).to.equal('updateduser');
            expect(result.email).to.equal('updated@example.com');
            expect(mockDatabase.update.calledOnce).to.be.true;
        });

        it('should reject invalid update data', async () => {
            const updateData = {
                email: 'invalid-email'
            };

            try {
                await userService.updateUser('user123', updateData);
                expect.fail('Should have thrown validation error');
            } catch (error) {
                expect(error.message).to.include('Invalid email format');
            }
        });
    });

    describe('deleteUser', () => {
        it('should delete user successfully', async () => {
            mockDatabase.delete.resolves();

            const result = await userService.deleteUser('user123');

            expect(result.success).to.be.true;
            expect(mockDatabase.delete.calledWith('users', { id: 'user123' })).to.be.true;
        });
    });

    describe('getUsersByRole', () => {
        it('should return users with specified role', async () => {
            const mockUsers = [
                { id: 'user1', username: 'admin1', role: 'admin', password: 'hash1' },
                { id: 'user2', username: 'admin2', role: 'admin', password: 'hash2' }
            ];

            mockDatabase.query.resolves(mockUsers);

            const result = await userService.getUsersByRole('admin');

            expect(result).to.have.length(2);
            expect(result[0].password).to.be.undefined;
            expect(result[1].password).to.be.undefined;
            expect(result[0].role).to.equal('admin');
            expect(result[1].role).to.equal('admin');
        });

        it('should return empty array when no users found', async () => {
            mockDatabase.query.resolves([]);

            const result = await userService.getUsersByRole('nonexistent');

            expect(result).to.be.an('array').that.is.empty;
        });
    });
});

// Helper validation functions
function validateUserData(userData) {
    const errors = [];

    if (!userData.username || userData.username.length < 3) {
        errors.push('Username must be at least 3 characters');
    }

    if (!userData.email || !isValidEmail(userData.email)) {
        errors.push('Invalid email format');
    }

    if (!userData.password || userData.password.length < 6) {
        errors.push('Password too weak');
    }

    if (!userData.role || !['user', 'admin', 'regional_admin'].includes(userData.role)) {
        errors.push('Invalid role');
    }

    return {
        isValid: errors.length === 0,
        errors
    };
}

function validateUpdateData(updateData) {
    const errors = [];

    if (updateData.username && updateData.username.length < 3) {
        errors.push('Username must be at least 3 characters');
    }

    if (updateData.email && !isValidEmail(updateData.email)) {
        errors.push('Invalid email format');
    }

    if (updateData.password && updateData.password.length < 6) {
        errors.push('Password too weak');
    }

    return {
        isValid: errors.length === 0,
        errors
    };
}

function isValidEmail(email) {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(email);
}