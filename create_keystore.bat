@echo off
title Thati Air Alert - Create Signing Keystore
color 0A

echo.
echo ========================================
echo   🔐 THATI AIR ALERT KEYSTORE CREATOR
echo ========================================
echo   Creating signing key for release APK
echo ========================================
echo.

REM Check if Java keytool is available
echo 🔍 Checking Java keytool...
keytool -help >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ ERROR: Java keytool not found
    echo Please ensure Java JDK is installed and in PATH
    echo.
    pause
    exit /b 1
)
echo ✅ Java keytool found

echo.
echo 🔑 Creating keystore for Thati Air Alert...
echo.
echo Please provide the following information:
echo (Press Enter for default values in brackets)
echo.

set /p KEYSTORE_PASSWORD="Enter keystore password [thati123]: "
if "%KEYSTORE_PASSWORD%"=="" set KEYSTORE_PASSWORD=thati123

set /p KEY_PASSWORD="Enter key password [thati123]: "
if "%KEY_PASSWORD%"=="" set KEY_PASSWORD=thati123

set /p FIRST_NAME="Enter your first name [Thati]: "
if "%FIRST_NAME%"=="" set FIRST_NAME=Thati

set /p LAST_NAME="Enter your last name [Developer]: "
if "%LAST_NAME%"=="" set LAST_NAME=Developer

set /p ORGANIZATION="Enter organization [Thati Air Alert]: "
if "%ORGANIZATION%"=="" set ORGANIZATION=Thati Air Alert

set /p CITY="Enter city [Yangon]: "
if "%CITY%"=="" set CITY=Yangon

set /p STATE="Enter state [Yangon]: "
if "%STATE%"=="" set STATE=Yangon

set /p COUNTRY="Enter country code [MM]: "
if "%COUNTRY%"=="" set COUNTRY=MM

echo.
echo 🔨 Creating keystore...

keytool -genkey -v -keystore thati-release-key.keystore -alias thati-key -keyalg RSA -keysize 2048 -validity 10000 -storepass "%KEYSTORE_PASSWORD%" -keypass "%KEY_PASSWORD%" -dname "CN=%FIRST_NAME% %LAST_NAME%, OU=%ORGANIZATION%, O=%ORGANIZATION%, L=%CITY%, S=%STATE%, C=%COUNTRY%"

if %errorlevel% equ 0 (
    echo.
    echo ✅ Keystore created successfully!
    echo.
    echo 📁 Keystore file: thati-release-key.keystore
    echo 🔑 Alias: thati-key
    echo 🔒 Store password: %KEYSTORE_PASSWORD%
    echo 🔒 Key password: %KEY_PASSWORD%
    echo.
    echo ⚠️  IMPORTANT: Keep these passwords safe!
    echo    You'll need them to sign future releases.
    echo.
    echo 📝 Keystore info saved to keystore-info.txt
    echo.
    
    REM Save keystore info to file
    echo Thati Air Alert - Keystore Information > keystore-info.txt
    echo ============================================ >> keystore-info.txt
    echo Keystore file: thati-release-key.keystore >> keystore-info.txt
    echo Alias: thati-key >> keystore-info.txt
    echo Store password: %KEYSTORE_PASSWORD% >> keystore-info.txt
    echo Key password: %KEY_PASSWORD% >> keystore-info.txt
    echo Created: %date% %time% >> keystore-info.txt
    echo ============================================ >> keystore-info.txt
    echo. >> keystore-info.txt
    echo KEEP THIS FILE SECURE! >> keystore-info.txt
    echo You need these credentials to sign release APKs. >> keystore-info.txt
    
    echo 🎯 Next steps:
    echo 1. Update app/build.gradle.kts with signing config
    echo 2. Build signed release APK
    echo 3. Test installation on device
    
) else (
    echo.
    echo ❌ Failed to create keystore!
    echo Please check the error messages above.
)

echo.
pause