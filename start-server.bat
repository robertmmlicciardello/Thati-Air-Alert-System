@echo off
title Thati Air Alert - Backend Server
color 0B

echo.
echo ========================================
echo   🚀 THATI AIR ALERT - BACKEND SERVER
echo ========================================
echo   Starting Node.js server...
echo ========================================
echo.

cd server

echo 📦 Checking dependencies...
if not exist "node_modules" (
    echo Installing dependencies...
    npm install
)

echo.
echo 🔥 Starting server on http://localhost:3000
echo 📊 Admin Dashboard will be at http://localhost:3001
echo.
echo Press Ctrl+C to stop the server
echo.

npm start

pause