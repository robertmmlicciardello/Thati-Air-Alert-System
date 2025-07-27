const sqlite3 = require('sqlite3').verbose();
const path = require('path');
const fs = require('fs');

/**
 * SQLite Database Setup for Local Development
 */
class SQLiteDatabase {
    constructor() {
        this.db = null;
        this.dbPath = process.env.DB_PATH || './data/thati_alert.db';
    }

    async connect() {
        try {
            // Ensure data directory exists
            const dataDir = path.dirname(this.dbPath);
            if (!fs.existsSync(dataDir)) {
                fs.mkdirSync(dataDir, { recursive: true });
            }

            return new Promise((resolve, reject) => {
                this.db = new sqlite3.Database(this.dbPath, (err) => {
                    if (err) {
                        console.error('Error opening SQLite database:', err);
                        reject(err);
                    } else {
                        console.log('Connected to SQLite database');
                        this.initializeTables().then(resolve).catch(reject);
                    }
                });
            });
        } catch (error) {
            console.error('Database connection error:', error);
            throw error;
        }
    }

    async initializeTables() {
        const tables = [
            // Users table
            `CREATE TABLE IF NOT EXISTS users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT UNIQUE NOT NULL,
                email TEXT UNIQUE,
                password_hash TEXT NOT NULL,
                role TEXT DEFAULT 'user',
                region TEXT,
                is_active BOOLEAN DEFAULT 1,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                last_login DATETIME
            )`,

            // Devices table
            `CREATE TABLE IF NOT EXISTS devices (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                device_id TEXT UNIQUE NOT NULL,
                user_id INTEGER,
                device_name TEXT,
                device_type TEXT,
                os_version TEXT,
                app_version TEXT,
                fcm_token TEXT,
                is_online BOOLEAN DEFAULT 0,
                last_seen DATETIME DEFAULT CURRENT_TIMESTAMP,
                location_lat REAL,
                location_lng REAL,
                battery_level INTEGER,
                network_type TEXT,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (user_id) REFERENCES users (id)
            )`,

            // Alerts table
            `CREATE TABLE IF NOT EXISTS alerts (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                alert_id TEXT UNIQUE NOT NULL,
                message TEXT NOT NULL,
                type TEXT NOT NULL,
                priority TEXT NOT NULL,
                region TEXT,
                sender_id INTEGER,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                expires_at DATETIME,
                is_active BOOLEAN DEFAULT 1,
                metadata TEXT,
                FOREIGN KEY (sender_id) REFERENCES users (id)
            )`,

            // Alert deliveries table
            `CREATE TABLE IF NOT EXISTS alert_deliveries (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                alert_id INTEGER NOT NULL,
                device_id INTEGER NOT NULL,
                status TEXT DEFAULT 'pending',
                delivered_at DATETIME,
                acknowledged_at DATETIME,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (alert_id) REFERENCES alerts (id),
                FOREIGN KEY (device_id) REFERENCES devices (id)
            )`,

            // Sessions table
            `CREATE TABLE IF NOT EXISTS sessions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER NOT NULL,
                token_hash TEXT NOT NULL,
                refresh_token_hash TEXT,
                expires_at DATETIME NOT NULL,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                is_active BOOLEAN DEFAULT 1,
                FOREIGN KEY (user_id) REFERENCES users (id)
            )`
        ];

        for (const tableSQL of tables) {
            await this.run(tableSQL);
        }

        // Insert default admin user
        await this.insertDefaultData();
    }

    async insertDefaultData() {
        const bcrypt = require('bcryptjs');
        
        // Check if admin user exists
        const adminExists = await this.get('SELECT id FROM users WHERE username = ?', ['admin']);
        
        if (!adminExists) {
            const hashedPassword = await bcrypt.hash('admin123', 10);
            await this.run(
                'INSERT INTO users (username, email, password_hash, role, region) VALUES (?, ?, ?, ?, ?)',
                ['admin', 'admin@thatialert.com', hashedPassword, 'admin', 'all']
            );
            console.log('Default admin user created: admin / admin123');
        }

        // Insert regional admins
        const regionalAdmins = [
            { username: 'yangon_admin', password: 'yangon123', region: 'yangon' },
            { username: 'mandalay_admin', password: 'mandalay123', region: 'mandalay' },
            { username: 'naypyidaw_admin', password: 'naypyidaw123', region: 'naypyidaw' }
        ];

        for (const admin of regionalAdmins) {
            const exists = await this.get('SELECT id FROM users WHERE username = ?', [admin.username]);
            if (!exists) {
                const hashedPassword = await bcrypt.hash(admin.password, 10);
                await this.run(
                    'INSERT INTO users (username, password_hash, role, region) VALUES (?, ?, ?, ?)',
                    [admin.username, hashedPassword, 'regional_admin', admin.region]
                );
            }
        }
    }

    async run(sql, params = []) {
        return new Promise((resolve, reject) => {
            this.db.run(sql, params, function(err) {
                if (err) {
                    reject(err);
                } else {
                    resolve({ id: this.lastID, changes: this.changes });
                }
            });
        });
    }

    async get(sql, params = []) {
        return new Promise((resolve, reject) => {
            this.db.get(sql, params, (err, row) => {
                if (err) {
                    reject(err);
                } else {
                    resolve(row);
                }
            });
        });
    }

    async all(sql, params = []) {
        return new Promise((resolve, reject) => {
            this.db.all(sql, params, (err, rows) => {
                if (err) {
                    reject(err);
                } else {
                    resolve(rows);
                }
            });
        });
    }

    async close() {
        return new Promise((resolve, reject) => {
            if (this.db) {
                this.db.close((err) => {
                    if (err) {
                        reject(err);
                    } else {
                        console.log('SQLite database connection closed');
                        resolve();
                    }
                });
            } else {
                resolve();
            }
        });
    }
}

module.exports = new SQLiteDatabase();