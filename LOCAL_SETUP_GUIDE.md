# 🚀 Local Setup Guide - Thati Air Alert Web Admin

## 📋 Quick Start (5 Minutes)

### 1. Open Two Command Prompts/Terminals

#### Terminal 1 - Start Backend Server:
```bash
cd server
npm start
```

#### Terminal 2 - Start Admin Dashboard:
```bash
cd admin-dashboard
npm start
```

### 2. Access the Application:
- **Admin Dashboard**: http://localhost:3001
- **API Server**: http://localhost:3000

---

## 🔧 Detailed Setup Instructions

### Prerequisites:
- Node.js installed (version 14+)
- npm or yarn package manager

### Step 1: Install Dependencies

#### For Server:
```bash
cd server
npm install
```

#### For Admin Dashboard:
```bash
cd admin-dashboard
npm install
```

### Step 2: Environment Configuration

#### Server (.env already created):
- Port: 3000
- Database: SQLite (local file)
- JWT secrets configured
- CORS enabled for localhost:3001

#### Admin Dashboard (.env already created):
- Port: 3001
- API URL: http://localhost:3000/api
- WebSocket URL: http://localhost:3000

### Step 3: Start Services

#### Start Backend Server:
```bash
cd server
npm start
```
**Expected Output:**
```
Thati Alert Server running on localhost:3000
Environment: development
Admin Dashboard: http://localhost:3001
```

#### Start Admin Dashboard (New Terminal):
```bash
cd admin-dashboard
npm start
```
**Expected Output:**
```
Local:            http://localhost:3001
On Your Network:  http://192.168.x.x:3001
```

---

## 🌐 Using the Admin Panel

### Default Login:
- **Username**: admin
- **Password**: admin123
- **Role**: Administrator

### Available Features:
1. **Dashboard** - Overview and statistics
2. **Alerts** - Send and manage emergency alerts
3. **Network** - Mesh network visualization
4. **Users** - User management
5. **Devices** - Device monitoring
6. **Analytics** - System analytics
7. **Settings** - Configuration

---

## 🐛 Troubleshooting

### Common Issues:

#### 1. Port Already in Use:
```bash
# Kill process on port 3000
npx kill-port 3000

# Kill process on port 3001
npx kill-port 3001
```

#### 2. Dependencies Issues:
```bash
# Clear npm cache
npm cache clean --force

# Delete node_modules and reinstall
rm -rf node_modules
npm install
```

#### 3. CORS Errors:
- Check if server is running on port 3000
- Verify .env files are configured correctly

#### 4. Database Issues:
```bash
# Server will create SQLite database automatically
# Check server/database.sqlite file is created
```

---

## 📱 Testing with Android APK

### Connect Android App to Local Server:

1. **Find Your Computer's IP Address:**
   ```bash
   ipconfig  # Windows
   ifconfig  # Mac/Linux
   ```

2. **Update Android App Server URL:**
   - In Android app, change server URL to: `http://YOUR_IP:3000`
   - Example: `http://192.168.1.100:3000`

3. **Ensure Firewall Allows Connections:**
   - Allow port 3000 and 3001 in Windows Firewall
   - Or temporarily disable firewall for testing

---

## 🔄 Development Workflow

### 1. Start Both Services:
```bash
# Terminal 1
cd server && npm start

# Terminal 2  
cd admin-dashboard && npm start
```

### 2. Make Changes:
- Server changes: Auto-restart with nodemon
- Dashboard changes: Hot reload enabled

### 3. Test Features:
- Login to admin panel
- Send test alerts
- Monitor network status
- Check device connections

---

## 📊 Available Scripts

### Server Scripts:
```bash
npm start          # Start development server
npm run dev        # Start with nodemon (auto-restart)
npm test           # Run tests
npm run build      # Build for production
```

### Admin Dashboard Scripts:
```bash
npm start          # Start development server
npm run build      # Build for production
npm test           # Run tests
npm run eject      # Eject from Create React App
```

---

## 🎯 Quick Test Checklist

### ✅ Server Running:
- [ ] Server starts without errors
- [ ] Database connection successful
- [ ] API endpoints responding
- [ ] WebSocket connection active

### ✅ Admin Dashboard:
- [ ] Dashboard loads at localhost:3001
- [ ] Login page appears
- [ ] Can login with admin/admin123
- [ ] All menu items accessible
- [ ] No console errors

### ✅ Integration:
- [ ] Dashboard connects to server API
- [ ] Real-time updates working
- [ ] Alert sending functional
- [ ] Network monitoring active

---

## 🚨 Emergency Commands

### Stop All Services:
```bash
# Windows
taskkill /f /im node.exe

# Mac/Linux
pkill -f node
```

### Reset Everything:
```bash
# Delete databases and logs
rm -f server/database.sqlite
rm -rf server/logs

# Reinstall dependencies
cd server && rm -rf node_modules && npm install
cd admin-dashboard && rm -rf node_modules && npm install
```

---

## 📞 Need Help?

### Check Logs:
- **Server logs**: Console output or `server/logs/app.log`
- **Browser logs**: F12 → Console tab
- **Network requests**: F12 → Network tab

### Common URLs:
- **Admin Panel**: http://localhost:3001
- **API Health**: http://localhost:3000/health
- **API Docs**: http://localhost:3000/api/docs (if enabled)

---

**Happy Development! 🎉**