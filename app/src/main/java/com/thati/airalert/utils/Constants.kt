package com.thati.airalert.utils

/**
 * Application Constants
 * Central location for all app constants
 */
object Constants {
    
    // App Configuration
    const val APP_NAME = "Thati Air Alert"
    const val APP_VERSION = "1.0.0"
    const val DATABASE_VERSION = 1
    const val DATABASE_NAME = "thati_alert.db"
    
    // Network Configuration
    const val API_BASE_URL = "https://api.thatialert.com"
    const val WEBSOCKET_URL = "wss://api.thatialert.com/ws"
    const val CONNECTION_TIMEOUT = 30000L
    const val READ_TIMEOUT = 30000L
    
    // Mesh Network Settings
    const val WIFI_DIRECT_PORT = 8888
    const val BLE_SERVICE_UUID = "12345678-1234-1234-1234-123456789abc"
    const val BLE_CHARACTERISTIC_UUID = "87654321-4321-4321-4321-cba987654321"
    const val MESH_DISCOVERY_INTERVAL = 10000L // 10 seconds
    const val MESH_CONNECTION_TIMEOUT = 15000L // 15 seconds
    
    // Alert Configuration
    const val MAX_ALERT_MESSAGE_LENGTH = 500
    const val ALERT_SOUND_DURATION = 30000L // 30 seconds
    const val CRITICAL_ALERT_REPEAT_COUNT = 3
    val ALERT_VIBRATION_PATTERN = longArrayOf(0, 1000, 500, 1000, 500, 1000)
    
    // User Roles
    const val ROLE_USER = "user"
    const val ROLE_REGIONAL_ADMIN = "regional_admin"
    const val ROLE_MAIN_ADMIN = "main_admin"
    
    // Alert Types
    const val ALERT_TYPE_AIRCRAFT = "aircraft"
    const val ALERT_TYPE_ATTACK = "attack"
    const val ALERT_TYPE_GENERAL = "general"
    const val ALERT_TYPE_CRITICAL = "critical"
    
    // Alert Priorities
    const val PRIORITY_CRITICAL = "critical"
    const val PRIORITY_HIGH = "high"
    const val PRIORITY_MEDIUM = "medium"
    const val PRIORITY_LOW = "low"
    
    // Myanmar Regions
    val MYANMAR_REGIONS = listOf(
        "yangon", "mandalay", "naypyitaw", "bago", "magway",
        "tanintharyi", "ayeyarwady", "sagaing", "mon", "kayin",
        "kayah", "chin", "shan", "rakhine", "kachin"
    )
    
    // Preferences Keys
    const val PREF_USER_ID = "user_id"
    const val PREF_USERNAME = "username"
    const val PREF_USER_ROLE = "user_role"
    const val PREF_USER_REGION = "user_region"
    const val PREF_AUTH_TOKEN = "auth_token"
    const val PREF_DEVICE_ID = "device_id"
    const val PREF_NOTIFICATION_ENABLED = "notification_enabled"
    const val PREF_SOUND_ENABLED = "sound_enabled"
    const val PREF_VIBRATION_ENABLED = "vibration_enabled"
    
    // Error Messages
    const val ERROR_NETWORK_UNAVAILABLE = "Network connection unavailable"
    const val ERROR_AUTHENTICATION_FAILED = "Authentication failed"
    const val ERROR_PERMISSION_DENIED = "Permission denied"
    const val ERROR_LOCATION_UNAVAILABLE = "Location services unavailable"
    const val ERROR_BLUETOOTH_UNAVAILABLE = "Bluetooth unavailable"
    const val ERROR_WIFI_UNAVAILABLE = "Wi-Fi unavailable"
    
    // Success Messages
    const val SUCCESS_ALERT_SENT = "Alert sent successfully"
    const val SUCCESS_LOGIN = "Login successful"
    const val SUCCESS_REGISTRATION = "Registration successful"
    const val SUCCESS_DEVICE_REGISTERED = "Device registered successfully"
    
    // Myanmar Language Support
    val ALERT_TYPES_MM = mapOf(
        ALERT_TYPE_AIRCRAFT to "လေယာဉ်",
        ALERT_TYPE_ATTACK to "တိုက်ခိုက်မှု",
        ALERT_TYPE_GENERAL to "ယေဘုယျ",
        ALERT_TYPE_CRITICAL to "အရေးကြီး"
    )
    
    val PRIORITIES_MM = mapOf(
        PRIORITY_CRITICAL to "အရေးကြီးဆုံး",
        PRIORITY_HIGH to "အရေးကြီး",
        PRIORITY_MEDIUM to "အလတ်အလတ်",
        PRIORITY_LOW to "နိမ့်"
    )
    
    val REGIONS_MM = mapOf(
        "yangon" to "ရန်ကုန်",
        "mandalay" to "မန္တလေး",
        "naypyitaw" to "နေပြည်တော်",
        "bago" to "ပဲခူး",
        "magway" to "မကွေး",
        "tanintharyi" to "တနင်္သာရီ",
        "ayeyarwady" to "ဧရာဝတီ",
        "sagaing" to "စစ်ကိုင်း",
        "mon" to "မွန်",
        "kayin" to "ကရင်",
        "kayah" to "ကယား",
        "chin" to "ချင်း",
        "shan" to "ရှမ်း",
        "rakhine" to "ရခိုင်",
        "kachin" to "ကချင်"
    )
}