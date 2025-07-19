# 🚀 Thati Air Alert - Complete Deployment Guide

## 📋 Table of Contents
1. [Prerequisites](#prerequisites)
2. [Mobile App Deployment](#mobile-app-deployment)
3. [Server Infrastructure Setup](#server-infrastructure-setup)
4. [Admin Dashboard Deployment](#admin-dashboard-deployment)
5. [Production Configuration](#production-configuration)
6. [Monitoring & Maintenance](#monitoring--maintenance)
7. [Troubleshooting](#troubleshooting)

---

## 🔧 Prerequisites

### System Requirements
- **Server:** Ubuntu 22.04 LTS (minimum 4GB RAM, 2 CPU cores)
- **Database:** PostgreSQL 14+
- **Cache:** Redis 6+
- **Node.js:** v18+ with npm
- **Domain:** SSL certificate (Let's Encrypt recommended)
- **Mobile:** Android 5.0+ (API level 21+)

### Required Accounts
- **Cloud Provider:** AWS/GCP/DigitalOcean
- **Domain Registrar:** Namecheap/GoDaddy
- **CDN:** CloudFlare (Free tier sufficient)
- **Push Notifications:** Firebase Console
- **SMS Service:** Twilio (optional)
- **Email Service:** SendGrid (optional)

---

## 📱 Mobile App Deployment

### 1. Development Setup
```bash
# Clone repository
git clone https://github.com/your-org/thati-air-alert.git
cd thati-air-alert

# Setup Android development environment
# Install Android Studio
# Install Android SDK API 34
# Setup device/emulator
```

### 2. Build Configuration
```kotlin
// app/build.gradle.kts
android {
    compileSdk = 34
    
    defaultConfig {
        applicationId = "com.thati.airalert"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }
    
    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
}
```

### 3. Signing Configuration
```bash
# Generate keystore
keytool -genkey -v -keystore thati-release-key.keystore \
  -alias thati-key -keyalg RSA -keysize 2048 -validity 10000

# Add to gradle.properties
MYAPP_RELEASE_STORE_FILE=thati-release-key.keystore
MYAPP_RELEASE_KEY_ALIAS=thati-key
MYAPP_RELEASE_STORE_PASSWORD=your_store_password
MYAPP_RELEASE_KEY_PASSWORD=your_key_password
```

### 4. Build Release APK
```bash
# Clean and build
./gradlew clean
./gradlew assembleRelease

# APK location: app/build/outputs/apk/release/app-release.apk
```

### 5. Google Play Store Deployment
```bash
# Create App Bundle (recommended)
./gradlew bundleRelease

# Upload to Google Play Console
# - Create app listing
# - Upload AAB file
# - Configure store listing
# - Submit for review
```

---

## 🌐 Server Infrastructure Setup

### 1. Server Provisioning

#### Option A: DigitalOcean Droplet ($40/month)
```bash
# Create droplet
doctl compute droplet create thati-server \
  --size s-2vcpu-4gb \
  --image ubuntu-22-04-x64 \
  --region sgp1 \
  --ssh-keys your-ssh-key-id
```

#### Option B: AWS EC2 ($45/month)
```bash
# Launch EC2 instance
aws ec2 run-instances \
  --image-id ami-0c02fb55956c7d316 \
  --instance-type t3.medium \
  --key-name your-key-pair \
  --security-group-ids sg-xxxxxxxxx \
  --subnet-id subnet-xxxxxxxxx
```

### 2. Initial Server Setup
```bash
# Connect to server
ssh root@your-server-ip

# Update system
apt update && apt upgrade -y

# Install essential packages
apt install -y curl wget git nginx certbot python3-certbot-nginx

# Install Node.js 18
curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash -
apt install -y nodejs

# Install PostgreSQL
apt install -y postgresql postgresql-contrib

# Install Redis
apt install -y redis-server

# Install PM2 for process management
npm install -g pm2
```

### 3. Database Setup
```bash
# Switch to postgres user
sudo -u postgres psql

# Create database and user
CREATE DATABASE thati_alert;
CREATE USER thati_user WITH PASSWORD 'secure_random_password_here';
GRANT ALL PRIVILEGES ON DATABASE thati_alert TO thati_user;
ALTER USER thati_user CREATEDB;
\q

# Configure PostgreSQL
nano /etc/postgresql/14/main/postgresql.conf
# Uncomment: listen_addresses = 'localhost'

nano /etc/postgresql/14/main/pg_hba.conf
# Add: local   thati_alert   thati_user   md5

# Restart PostgreSQL
systemctl restart postgresql
```

### 4. Redis Configuration
```bash
# Configure Redis
nano /etc/redis/redis.conf
# Uncomment: requirepass your_redis_password_here
# Change: bind 127.0.0.1

# Restart Redis
systemctl restart redis-server
```

### 5. Application Deployment
```bash
# Create application directory
mkdir -p /var/www/thati-alert
cd /var/www/thati-alert

# Clone repository
git clone https://github.com/your-org/thati-air-alert.git .

# Install server dependencies
cd server
npm install --production

# Create environment file
cp .env.example .env
nano .env
```

### 6. Environment Configuration
```env
# .env file
NODE_ENV=production
PORT=3000

# Database
DATABASE_URL=postgresql://thati_user:secure_password@localhost:5432/thati_alert

# Redis
REDIS_URL=redis://:redis_password@localhost:6379

# Security
JWT_SECRET=your_super_secure_jwt_secret_here
ENCRYPTION_KEY=your_32_character_encryption_key

# External Services
FCM_SERVER_KEY=your_firebase_server_key
TWILIO_ACCOUNT_SID=your_twilio_sid
TWILIO_AUTH_TOKEN=your_twilio_token

# Domain
DOMAIN=yourdomain.com
ALLOWED_ORIGINS=https://yourdomain.com,https://admin.yourdomain.com
```

### 7. Database Migration
```bash
# Run database migrations
npm run migrate

# Seed initial data
npm run seed
```

### 8. PM2 Process Management
```bash
# Create PM2 ecosystem file
nano ecosystem.config.js
```

```javascript
module.exports = {
  apps: [{
    name: 'thati-alert-server',
    script: 'src/server.js',
    instances: 'max',
    exec_mode: 'cluster',
    env: {
      NODE_ENV: 'production',
      PORT: 3000
    },
    error_file: '/var/log/thati-alert/error.log',
    out_file: '/var/log/thati-alert/out.log',
    log_file: '/var/log/thati-alert/combined.log',
    time: true
  }]
};
```

```bash
# Start application with PM2
pm2 start ecosystem.config.js
pm2 save
pm2 startup
```

### 9. Nginx Configuration
```bash
# Create Nginx configuration
nano /etc/nginx/sites-available/thati-alert
```

```nginx
server {
    listen 80;
    server_name yourdomain.com www.yourdomain.com;
    
    location / {
        proxy_pass http://localhost:3000;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_cache_bypass $http_upgrade;
    }
    
    # WebSocket support
    location /socket.io/ {
        proxy_pass http://localhost:3000;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

```bash
# Enable site
ln -s /etc/nginx/sites-available/thati-alert /etc/nginx/sites-enabled/
nginx -t
systemctl reload nginx
```

### 10. SSL Certificate
```bash
# Install SSL certificate
certbot --nginx -d yourdomain.com -d www.yourdomain.com

# Auto-renewal
crontab -e
# Add: 0 12 * * * /usr/bin/certbot renew --quiet
```

---

## 🎛️ Admin Dashboard Deployment

### 1. Build Admin Dashboard
```bash
# Navigate to admin directory
cd /var/www/thati-alert/admin-dashboard

# Install dependencies
npm install

# Build for production
npm run build
```

### 2. Nginx Configuration for Admin
```nginx
server {
    listen 443 ssl;
    server_name admin.yourdomain.com;
    
    ssl_certificate /etc/letsencrypt/live/yourdomain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/yourdomain.com/privkey.pem;
    
    root /var/www/thati-alert/admin-dashboard/build;
    index index.html;
    
    location / {
        try_files $uri $uri/ /index.html;
    }
    
    location /api/ {
        proxy_pass http://localhost:3000/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

---

## 📊 Production Configuration

### 1. Firewall Setup
```bash
# Configure UFW
ufw allow ssh
ufw allow 'Nginx Full'
ufw enable
```

### 2. Log Management
```bash
# Create log directories
mkdir -p /var/log/thati-alert

# Configure logrotate
nano /etc/logrotate.d/thati-alert
```

```
/var/log/thati-alert/*.log {
    daily
    missingok
    rotate 52
    compress
    delaycompress
    notifempty
    create 644 www-data www-data
    postrotate
        pm2 reload thati-alert-server
    endscript
}
```

### 3. Monitoring Setup
```bash
# Install monitoring tools
npm install -g clinic
apt install -y htop iotop nethogs

# Setup health check endpoint
curl https://yourdomain.com/health
```

### 4. Backup Configuration
```bash
# Create backup script
nano /usr/local/bin/backup-thati.sh
```

```bash
#!/bin/bash
BACKUP_DIR="/var/backups/thati-alert"
DATE=$(date +%Y%m%d_%H%M%S)

# Create backup directory
mkdir -p $BACKUP_DIR

# Database backup
pg_dump -U thati_user -h localhost thati_alert > $BACKUP_DIR/db_$DATE.sql

# Application backup
tar -czf $BACKUP_DIR/app_$DATE.tar.gz /var/www/thati-alert

# Keep only last 7 days
find $BACKUP_DIR -name "*.sql" -mtime +7 -delete
find $BACKUP_DIR -name "*.tar.gz" -mtime +7 -delete
```

```bash
# Make executable and schedule
chmod +x /usr/local/bin/backup-thati.sh
crontab -e
# Add: 0 2 * * * /usr/local/bin/backup-thati.sh
```

---

## 📊 Monitoring & Maintenance

### 1. Health Monitoring
```bash
# Install monitoring tools
npm install -g pm2-logrotate
pm2 install pm2-server-monit
```

### 2. Performance Monitoring
```javascript
// Add to server.js
const prometheus = require('prom-client');
const collectDefaultMetrics = prometheus.collectDefaultMetrics;
collectDefaultMetrics();

app.get('/metrics', (req, res) => {
    res.set('Content-Type', prometheus.register.contentType);
    res.end(prometheus.register.metrics());
});
```

### 3. Uptime Monitoring
- **UptimeRobot:** Free monitoring service
- **Pingdom:** Professional monitoring
- **New Relic:** Application performance monitoring

---

## 🔧 Troubleshooting

### Common Issues

#### 1. Database Connection Issues
```bash
# Check PostgreSQL status
systemctl status postgresql

# Check connections
sudo -u postgres psql -c "SELECT * FROM pg_stat_activity;"

# Reset connections
sudo -u postgres psql -c "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = 'thati_alert';"
```

#### 2. Redis Connection Issues
```bash
# Check Redis status
systemctl status redis-server

# Test Redis connection
redis-cli ping

# Check Redis logs
tail -f /var/log/redis/redis-server.log
```

#### 3. Application Issues
```bash
# Check PM2 status
pm2 status

# View logs
pm2 logs thati-alert-server

# Restart application
pm2 restart thati-alert-server
```

#### 4. Nginx Issues
```bash
# Check Nginx status
systemctl status nginx

# Test configuration
nginx -t

# Check error logs
tail -f /var/log/nginx/error.log
```

### Performance Optimization

#### 1. Database Optimization
```sql
-- Add indexes for better performance
CREATE INDEX idx_alerts_created_at ON alerts(created_at);
CREATE INDEX idx_alerts_user_id ON alerts(user_id);
CREATE INDEX idx_devices_user_id ON devices(user_id);
```

#### 2. Redis Caching
```javascript
// Implement caching for frequently accessed data
const redis = require('redis');
const client = redis.createClient();

// Cache user sessions
app.use(session({
    store: new RedisStore({ client: client }),
    secret: process.env.SESSION_SECRET,
    resave: false,
    saveUninitialized: false
}));
```

#### 3. CDN Configuration
```bash
# Configure CloudFlare
# - Enable caching for static assets
# - Enable Brotli compression
# - Enable HTTP/2
# - Configure security settings
```

---

## 📈 Scaling Considerations

### Horizontal Scaling
- **Load Balancer:** Nginx/HAProxy
- **Multiple App Instances:** PM2 cluster mode
- **Database Replication:** Master-slave setup
- **Redis Clustering:** For high availability

### Vertical Scaling
- **Increase server resources:** More CPU/RAM
- **SSD storage:** For better I/O performance
- **Dedicated database server:** Separate DB instance

---

## 🔒 Security Checklist

- ✅ SSL/TLS certificates installed
- ✅ Firewall configured (UFW/iptables)
- ✅ Database access restricted
- ✅ Redis password protected
- ✅ Rate limiting enabled
- ✅ Input validation implemented
- ✅ CORS properly configured
- ✅ Security headers set
- ✅ Regular security updates
- ✅ Backup strategy implemented

---

## 📞 Support & Maintenance

### Regular Maintenance Tasks
- **Daily:** Check application logs
- **Weekly:** Review performance metrics
- **Monthly:** Update dependencies
- **Quarterly:** Security audit
- **Annually:** Infrastructure review

### Emergency Procedures
1. **Server Down:** Check PM2, Nginx, database
2. **High Load:** Scale resources, check bottlenecks
3. **Security Breach:** Isolate, investigate, patch
4. **Data Loss:** Restore from backups

This deployment guide ensures a robust, scalable, and maintainable production environment for the Thati Air Alert system.