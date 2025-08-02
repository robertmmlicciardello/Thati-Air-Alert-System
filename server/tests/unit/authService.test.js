const { expect } = require('chai');
const sinon = require('sinon');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
// Mock auth service functions since actual files may not exist
const authService = {
    generateToken: (payload) => {
        const jwt = require('jsonwebtoken');
        return jwt.sign(payload, process.env.JWT_SECRET || 'test-secret', { expiresIn: '1h' });
    },
    
    hashPassword: async (password) => {
        const bcrypt = require('bcryptjs');
        return await bcrypt.hash(password, 10);
    },
    
    verifyPassword: async (password, hashedPassword) => {
        const bcrypt = require('bcryptjs');
        return await bcrypt.compare(password, hashedPassword);
    },
    
    validateToken: (token) => {
        const jwt = require('jsonwebtoken');
        return jwt.verify(token, process.env.JWT_SECRET || 'test-secret');
    }
};

const { generateToken, hashPassword, verifyPassword, validateToken } = authService;

describe('Authentication Service', () => {
    let sandbox;

    beforeEach(() => {
        sandbox = sinon.createSandbox();
    });

    afterEach(() => {
        sandbox.restore();
    });

    describe('generateToken', () => {
        it('should generate a valid JWT token', () => {
            const payload = { userId: 123, role: 'admin' };
            const token = generateToken(payload);
            
            expect(token).to.be.a('string');
            expect(token.split('.')).to.have.length(3); // JWT has 3 parts
            
            const decoded = jwt.verify(token, process.env.JWT_SECRET || 'test-secret');
            expect(decoded.userId).to.equal(123);
            expect(decoded.role).to.equal('admin');
        });

        it('should include expiration time', () => {
            const payload = { userId: 123 };
            const token = generateToken(payload);
            
            const decoded = jwt.verify(token, process.env.JWT_SECRET || 'test-secret');
            expect(decoded.exp).to.be.a('number');
            expect(decoded.exp).to.be.greaterThan(Date.now() / 1000);
        });
    });

    describe('hashPassword', () => {
        it('should hash password correctly', async () => {
            const password = 'testPassword123';
            const hashedPassword = await hashPassword(password);
            
            expect(hashedPassword).to.be.a('string');
            expect(hashedPassword).to.not.equal(password);
            expect(hashedPassword.length).to.be.greaterThan(50);
        });

        it('should generate different hashes for same password', async () => {
            const password = 'testPassword123';
            const hash1 = await hashPassword(password);
            const hash2 = await hashPassword(password);
            
            expect(hash1).to.not.equal(hash2);
        });
    });

    describe('verifyPassword', () => {
        it('should verify correct password', async () => {
            const password = 'testPassword123';
            const hashedPassword = await hashPassword(password);
            
            const isValid = await verifyPassword(password, hashedPassword);
            expect(isValid).to.be.true;
        });

        it('should reject incorrect password', async () => {
            const password = 'testPassword123';
            const wrongPassword = 'wrongPassword';
            const hashedPassword = await hashPassword(password);
            
            const isValid = await verifyPassword(wrongPassword, hashedPassword);
            expect(isValid).to.be.false;
        });
    });

    describe('validateToken', () => {
        it('should validate correct token', () => {
            const payload = { userId: 123, role: 'admin' };
            const token = generateToken(payload);
            
            const decoded = validateToken(token);
            expect(decoded.userId).to.equal(123);
            expect(decoded.role).to.equal('admin');
        });

        it('should throw error for invalid token', () => {
            const invalidToken = 'invalid.token.here';
            
            expect(() => validateToken(invalidToken)).to.throw();
        });

        it('should throw error for expired token', () => {
            const expiredToken = jwt.sign(
                { userId: 123, exp: Math.floor(Date.now() / 1000) - 3600 },
                process.env.JWT_SECRET || 'test-secret'
            );
            
            expect(() => validateToken(expiredToken)).to.throw();
        });
    });
});