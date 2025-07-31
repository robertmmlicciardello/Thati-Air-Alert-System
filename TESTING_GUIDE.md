# 📱 Thati Air Alert - Testing Guide

## 🎯 Debug APK Testing Instructions

### ✅ APK Information:
- **File**: `app/build/outputs/apk/debug/app-debug.apk`
- **Size**: 9.71 MB
- **Version**: 1.0-debug
- **Package**: `com.thati.airalert.debug`
- **Build Date**: July 31, 2025 - 11:59 AM

## 📲 Installation Steps

### 1. Enable Developer Options
1. Go to **Settings** → **About Phone**
2. Tap **Build Number** 7 times
3. Developer Options will be enabled

### 2. Enable Unknown Sources
1. Go to **Settings** → **Security** (or **Privacy**)
2. Enable **Unknown Sources** or **Install unknown apps**
3. For newer Android: **Settings** → **Apps** → **Special access** → **Install unknown apps**

### 3. Install APK
1. Transfer `app-debug.apk` to your Android device
2. Use file manager to locate the APK
3. Tap the APK file to install
4. Grant installation permission if prompted
5. Tap **Install** when prompted

### 4. Grant Permissions
When you first open the app, grant these permissions:
- **Location** - For GPS tracking and emergency location
- **Nearby devices** - For Bluetooth mesh networking
- **Notifications** - For emergency alerts
- **Phone** - For emergency calling features

## 🧪 Testing Features

### 🚨 Emergency Alert System
1. **Open the app**
2. **Select user mode** or **admin mode**
3. **Test alert broadcasting** (admin mode)
4. **Test alert receiving** (user mode)
5. **Check notification system**

### 🌐 Mesh Network Testing
1. **Enable Wi-Fi and Bluetooth**
2. **Start mesh network** in app
3. **Test with multiple devices** (if available)
4. **Check network status** in app
5. **Test offline communication**

### 📍 Location Services
1. **Enable GPS** on device
2. **Grant location permission**
3. **Test location sharing** in emergency mode
4. **Check location accuracy**

### 🔋 Battery Optimization
1. **Check battery usage** in device settings
2. **Test background operation**
3. **Verify power-saving features**

## 🐛 Troubleshooting

### Installation Issues:
- **"App not installed"**: Enable unknown sources
- **"Parse error"**: Re-download APK, check file integrity
- **"Insufficient storage"**: Free up device storage (need ~20MB)

### Permission Issues:
- **Location not working**: Check GPS settings, grant precise location
- **Bluetooth issues**: Enable Bluetooth, grant nearby devices permission
- **Notifications not showing**: Check notification settings for the app

### App Crashes:
- **Check device compatibility**: Android 7.0+ required
- **Clear app data**: Settings → Apps → Thati Air Alert → Storage → Clear Data
- **Restart device** and try again

## 📊 Testing Checklist

### ✅ Basic Functionality:
- [ ] App launches successfully
- [ ] Login screen appears
- [ ] User/Admin mode selection works
- [ ] Main interface loads properly

### ✅ Emergency Features:
- [ ] Alert creation works
- [ ] Alert broadcasting functions
- [ ] Alert receiving works
- [ ] Sound notifications play
- [ ] Visual alerts display

### ✅ Network Features:
- [ ] Wi-Fi Direct connection
- [ ] Bluetooth pairing
- [ ] Mesh network formation
- [ ] Message forwarding
- [ ] Network health monitoring

### ✅ Location Features:
- [ ] GPS location detection
- [ ] Location sharing
- [ ] Map integration (if available)
- [ ] Emergency location broadcast

### ✅ UI/UX:
- [ ] Dark mode support
- [ ] Responsive design
- [ ] Button interactions
- [ ] Navigation works
- [ ] Settings accessible

## 📝 Bug Reporting

If you encounter issues, please note:
1. **Device model and Android version**
2. **Steps to reproduce the issue**
3. **Error messages (if any)**
4. **Screenshots of the problem**
5. **App behavior vs expected behavior**

## 🔄 Testing Scenarios

### Scenario 1: Single Device Testing
1. Install APK on one device
2. Test all basic features
3. Check UI responsiveness
4. Test notification system

### Scenario 2: Multi-Device Testing (Recommended)
1. Install APK on 2+ devices
2. Test mesh network formation
3. Test alert broadcasting between devices
4. Test offline communication
5. Test network healing (disconnect/reconnect devices)

### Scenario 3: Emergency Simulation
1. Turn off internet/cellular
2. Use only mesh network
3. Test emergency alert propagation
4. Test location sharing
5. Verify offline functionality

## 🎯 Expected Results

### ✅ Successful Testing Should Show:
- App launches without crashes
- All permissions granted successfully
- Mesh network forms between devices
- Alerts propagate through network
- Location services work accurately
- Battery usage remains reasonable
- UI is responsive and intuitive

### ⚠️ Known Limitations (Debug Version):
- Detailed logging enabled (may affect performance)
- Debug symbols included (larger file size)
- Some features may show debug information
- Network simulation data for testing

## 📞 Support

For testing support or questions:
- Check app logs in device settings
- Review this testing guide
- Test with different device configurations
- Document any issues for future fixes

---

**Happy Testing! 🚀**  
**Thati Air Alert - Emergency Preparedness for Myanmar Communities**