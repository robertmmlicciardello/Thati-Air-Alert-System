@echo off
title Thati Air Alert - Production Build System
color 0A

echo.
echo ========================================
echo   🚨 THATI AIR ALERT BUILD SYSTEM
echo ========================================
echo   Production-Ready APK Builder
echo   Version: 1.0.0
echo ========================================
echo.

REM Check if Java is installed
echo 🔍 Checking Java installation...
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ ERROR: Java is not installed or not in PATH
    echo Please install Java JDK 11 or higher
    echo Download from: https://adoptium.net/
    echo.
    pause
    exit /b 1
)
echo ✅ Java found

REM Check if Android SDK is set
echo 🔍 Checking Android SDK...
if "%ANDROID_HOME%"=="" (
    echo ⚠️  WARNING: ANDROID_HOME environment variable is not set
    echo Please set ANDROID_HOME to your Android SDK path
    echo Example: C:\Users\%USERNAME%\AppData\Local\Android\Sdk
    echo.
    echo Continuing with build attempt...
) else (
    echo ✅ Android SDK found at: %ANDROID_HOME%
)
echo.

REM Clean previous builds
echo 🧹 Cleaning previous builds...
gradlew clean
if %errorlevel% neq 0 (
    echo ❌ Clean failed!
    pause
    exit /b 1
)
echo ✅ Clean completed
echo.

REM Build debug APK
echo 🔨 Building debug APK...
gradlew assembleDebug

if %errorlevel% equ 0 (
    echo ✅ Debug APK built successfully!
    echo.
    
    REM Try to build release APK
    echo 🔨 Attempting release build...
    gradlew assembleRelease
    if %errorlevel% equ 0 (
        echo ✅ Release APK built successfully!
    ) else (
        echo ⚠️  Release build failed (normal without signing key)
    )
    
    echo.
    echo ========================================
    echo   📦 BUILD RESULTS
    echo ========================================
    echo.
    
    if exist "app\build\outputs\apk\debug\app-debug.apk" (
        echo ✅ Debug APK: app\build\outputs\apk\debug\app-debug.apk
        for %%I in ("app\build\outputs\apk\debug\app-debug.apk") do echo    📏 Size: %%~zI bytes
    )
    
    if exist "app\build\outputs\apk\release\app-release-unsigned.apk" (
        echo ✅ Release APK: app\build\outputs\apk\release\app-release-unsigned.apk
        for %%I in ("app\build\outputs\apk\release\app-release-unsigned.apk") do echo    📏 Size: %%~zI bytes
    )
    
    echo.
    echo ========================================
    echo   📱 INSTALLATION GUIDE
    echo ========================================
    echo.
    echo To install on Android device:
    echo 1. 📱 Enable "Install unknown apps" in Settings
    echo 2. 📂 Transfer APK file to your device
    echo 3. 📲 Tap the APK file to install
    echo 4. ✅ Grant permissions when prompted
    echo.
    echo 🔐 Required Permissions:
    echo   • Location (GPS + Wi-Fi Direct)
    echo   • Nearby devices (Bluetooth mesh)
    echo   • Notifications (Emergency alerts)
    echo   • Phone (Emergency features)
    echo   • Storage (Log files)
    echo.
    echo ========================================
    echo   🚨 THATI AIR ALERT READY FOR MYANMAR!
    echo ========================================
    echo   Emergency preparedness system ready
    echo   to serve Myanmar communities! 🇲🇲
    echo ========================================
    
) else (
    echo.
    echo ❌ Build failed!
    echo Please check the error messages above
    echo.
    echo Common solutions:
    echo 1. Ensure Android SDK is properly installed
    echo 2. Check ANDROID_HOME environment variable
    echo 3. Verify Java JDK 11+ is installed
    echo 4. Run 'gradlew --version' to check setup
)

echo.
pause