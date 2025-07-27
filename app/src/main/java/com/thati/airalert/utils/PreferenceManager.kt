package com.thati.airalert.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Secure Preference Manager
 * Handles encrypted storage of sensitive user data
 */
class PreferenceManager(private val context: Context) {
    
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "thati_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    // User Authentication
    fun saveAuthToken(token: String) {
        sharedPreferences.edit().putString(Constants.PREF_AUTH_TOKEN, token).apply()
    }
    
    fun getAuthToken(): String? {
        return sharedPreferences.getString(Constants.PREF_AUTH_TOKEN, null)
    }
    
    fun clearAuthToken() {
        sharedPreferences.edit().remove(Constants.PREF_AUTH_TOKEN).apply()
    }
    
    // User Information
    fun saveUserInfo(userId: String, username: String, role: String, region: String) {
        sharedPreferences.edit().apply {
            putString(Constants.PREF_USER_ID, userId)
            putString(Constants.PREF_USERNAME, username)
            putString(Constants.PREF_USER_ROLE, role)
            putString(Constants.PREF_USER_REGION, region)
            apply()
        }
    }
    
    fun getUserId(): String? = sharedPreferences.getString(Constants.PREF_USER_ID, null)
    fun getUsername(): String? = sharedPreferences.getString(Constants.PREF_USERNAME, null)
    fun getUserRole(): String? = sharedPreferences.getString(Constants.PREF_USER_ROLE, null)
    fun getUserRegion(): String? = sharedPreferences.getString(Constants.PREF_USER_REGION, null)
    
    // Device Information
    fun saveDeviceId(deviceId: String) {
        sharedPreferences.edit().putString(Constants.PREF_DEVICE_ID, deviceId).apply()
    }
    
    fun getDeviceId(): String? = sharedPreferences.getString(Constants.PREF_DEVICE_ID, null)
    
    // App Settings
    fun setNotificationEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(Constants.PREF_NOTIFICATION_ENABLED, enabled).apply()
    }
    
    fun isNotificationEnabled(): Boolean = 
        sharedPreferences.getBoolean(Constants.PREF_NOTIFICATION_ENABLED, true)
    
    fun setSoundEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(Constants.PREF_SOUND_ENABLED, enabled).apply()
    }
    
    fun isSoundEnabled(): Boolean = 
        sharedPreferences.getBoolean(Constants.PREF_SOUND_ENABLED, true)
    
    fun setVibrationEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(Constants.PREF_VIBRATION_ENABLED, enabled).apply()
    }
    
    fun isVibrationEnabled(): Boolean = 
        sharedPreferences.getBoolean(Constants.PREF_VIBRATION_ENABLED, true)
    
    // Authentication Status
    fun isLoggedIn(): Boolean = getAuthToken() != null && getUserId() != null
    
    fun isAdmin(): Boolean {
        val role = getUserRole()
        return role == Constants.ROLE_REGIONAL_ADMIN || role == Constants.ROLE_MAIN_ADMIN
    }
    
    fun isMainAdmin(): Boolean = getUserRole() == Constants.ROLE_MAIN_ADMIN
    
    // Clear All Data
    fun clearAllData() {
        sharedPreferences.edit().clear().apply()
    }
    
    // Language Preference
    fun setLanguage(language: String) {
        sharedPreferences.edit().putString("language", language).apply()
    }
    
    fun getLanguage(): String = sharedPreferences.getString("language", "en") ?: "en"
    
    // First Launch
    fun setFirstLaunch(isFirst: Boolean) {
        sharedPreferences.edit().putBoolean("first_launch", isFirst).apply()
    }
    
    fun isFirstLaunch(): Boolean = sharedPreferences.getBoolean("first_launch", true)
}