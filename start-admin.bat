@echo off
title Thati Air Alert - Admin Dashboard
color 0A

echo.
echo ========================================
echo   🌐 THATI AIR ALERT - ADMIN DASHBOARD
echo ========================================
echo   Starting React development server...
echo ========================================
echo.

cd admin-dashboard

echo 📦 Checking dependencies...
if not exist "node_modules" (
    echo Installing dependencies...
    npm install
)

echo.
echo 🎯 Starting admin dashboard on http://localhost:3001
echo 🔗 Make sure backend server is running on port 3000
echo.
echo Login credentials:
echo Username: admin
echo Password: admin123
echo.
echo Press Ctrl+C to stop the dashboard
echo.

npm start

pause