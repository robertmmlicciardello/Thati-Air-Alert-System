const jwt = require('jsonwebtoken');
const database = require('../database/sqlite');

/**
 * JWT Authentication Middleware
 */
const authenticateToken = async (req, res, next) => {
    try {
        const authHeader = req.headers['authorization'];
        const token = authHeader && authHeader.split(' ')[1]; // Bearer TOKEN

        if (!token) {
            return res.status(401).json({
                success: false,
                message: 'Access token required'
            });
        }

        // Verify JWT token
        const decoded = jwt.verify(token, process.env.JWT_SECRET);

        // Check if user exists and is active
        const user = await database.get(
            'SELECT id, username, role, region, is_active FROM users WHERE id = ?',
            [decoded.userId]
        );

        if (!user || !user.is_active) {
            return res.status(401).json({
                success: false,
                message: 'Invalid or expired token'
            });
        }

        // Check if session is active
        const tokenHash = require('crypto').createHash('sha256').update(token).digest('hex');
        const session = await database.get(
            'SELECT id FROM sessions WHERE token_hash = ? AND is_active = 1 AND expires_at > datetime("now")',
            [tokenHash]
        );

        if (!session) {
            return res.status(401).json({
                success: false,
                message: 'Session expired'
            });
        }

        // Add user info to request
        req.user = {
            userId: user.id,
            username: user.username,
            role: user.role,
            region: user.region
        };

        next();

    } catch (error) {
        if (error.name === 'JsonWebTokenError') {
            return res.status(401).json({
                success: false,
                message: 'Invalid token'
            });
        }

        if (error.name === 'TokenExpiredError') {
            return res.status(401).json({
                success: false,
                message: 'Token expired'
            });
        }

        console.error('Auth middleware error:', error);
        return res.status(500).json({
            success: false,
            message: 'Authentication error'
        });
    }
};

/**
 * Role-based authorization middleware
 */
const requireRole = (roles) => {
    return (req, res, next) => {
        if (!req.user) {
            return res.status(401).json({
                success: false,
                message: 'Authentication required'
            });
        }

        const userRoles = Array.isArray(roles) ? roles : [roles];
        
        if (!userRoles.includes(req.user.role)) {
            return res.status(403).json({
                success: false,
                message: 'Insufficient permissions'
            });
        }

        next();
    };
};

/**
 * Region-based authorization middleware
 */
const requireRegion = (req, res, next) => {
    if (!req.user) {
        return res.status(401).json({
            success: false,
            message: 'Authentication required'
        });
    }

    // Admin can access all regions
    if (req.user.role === 'admin') {
        return next();
    }

    // Check if user has access to requested region
    const requestedRegion = req.params.region || req.body.region || req.query.region;
    
    if (requestedRegion && requestedRegion !== req.user.region) {
        return res.status(403).json({
            success: false,
            message: 'Access denied for this region'
        });
    }

    next();
};

module.exports = {
    authenticateToken,
    requireRole,
    requireRegion
};