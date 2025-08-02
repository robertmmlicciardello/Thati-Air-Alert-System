@echo off
title Thati Air Alert - Full Stack Launcher
color 0E

echo.
echo ========================================
echo   🚀 THATI AIR ALERT - FULL STACK
echo ========================================
echo   Starting both server and dashboard...
echo ========================================
echo.

echo 📦 Installing dependencies if needed...

cd server
if not exist "node_modules" (
    echo Installing server dependencies...
    npm install
)
cd ..

cd admin-dashboard
if not exist "node_modules" (
    echo Installing dashboard dependencies...
    npm install
)
cd ..

echo.
echo 🔥 Starting backend server...
start "Thati Server" cmd /k "cd server && npm start"

echo ⏳ Waiting 5 seconds for server to start...
timeout /t 5 /nobreak >nul

echo 🌐 Starting admin dashboard...
start "Thati Admin" cmd /k "cd admin-dashboard && npm start"

echo.
echo ========================================
echo   ✅ BOTH SERVICES STARTING...
echo ========================================
echo.
echo 🔗 Backend Server: http://localhost:3000
echo 🎯 Admin Dashboard: http://localhost:3001
echo.
echo Login credentials:
echo Username: admin
echo Password: admin123
echo.
echo Close this window to keep services running
echo Or press any key to exit
echo.

pause