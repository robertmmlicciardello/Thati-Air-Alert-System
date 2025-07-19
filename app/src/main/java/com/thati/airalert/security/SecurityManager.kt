package com.thati.airalert.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Security Manager - လုံခြုံရေး စီမံခန့်ခွဲမှု
 * Production အတွက် security features များ
 */
class SecurityManager(private val context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("security_prefs", Context.MODE_PRIVATE)
    private val keyAlias = "thati_master_key"
    
    companion object {
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_LENGTH = 256
        private const val IV_LENGTH = 12
        private const val TAG_LENGTH = 16
    }
    
    /**
     * Device ID နှင့် Security Token ဖန်တီးခြင်း
     */
    fun generateDeviceSecurityToken(): String {
        val deviceId = getDeviceId()
        val timestamp = System.currentTimeMillis()
        val random = SecureRandom().nextInt(999999)
        
        val tokenData = "$deviceId:$timestamp:$random"
        return hashSHA256(tokenData)
    }
    
    /**
     * Alert Message Encryption
     */
    fun encryptAlertMessage(message: String): EncryptedMessage {
        try {
            val secretKey = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            
            // Generate random IV
            val iv = ByteArray(IV_LENGTH)
            SecureRandom().nextBytes(iv)
            val gcmSpec = GCMParameterSpec(TAG_LENGTH * 8, iv)
            
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)
            val encryptedData = cipher.doFinal(message.toByteArray(Charsets.UTF_8))
            
            return EncryptedMessage(
                encryptedData = Base64.encodeToString(encryptedData, Base64.DEFAULT),
                iv = Base64.encodeToString(iv, Base64.DEFAULT),
                timestamp = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            throw SecurityException("Encryption failed: ${e.message}")
        }
    }
    
    /**
     * Alert Message Decryption
     */
    fun decryptAlertMessage(encryptedMessage: EncryptedMessage): String {
        try {
            val secretKey = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            
            val iv = Base64.decode(encryptedMessage.iv, Base64.DEFAULT)
            val encryptedData = Base64.decode(encryptedMessage.encryptedData, Base64.DEFAULT)
            val gcmSpec = GCMParameterSpec(TAG_LENGTH * 8, iv)
            
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)
            val decryptedData = cipher.doFinal(encryptedData)
            
            return String(decryptedData, Charsets.UTF_8)
        } catch (e: Exception) {
            throw SecurityException("Decryption failed: ${e.message}")
        }
    }
    
    /**
     * Message Integrity Verification
     */
    fun verifyMessageIntegrity(message: String, signature: String): Boolean {
        val computedSignature = hashSHA256(message + getDeviceId())
        return computedSignature == signature
    }
    
    /**
     * Generate Message Signature
     */
    fun generateMessageSignature(message: String): String {
        return hashSHA256(message + getDeviceId())
    }
    
    /**
     * Admin Authentication
     */
    fun authenticateAdmin(username: String, password: String): AuthResult {
        val hashedPassword = hashSHA256(password + "thati_salt_2024")
        val storedHash = prefs.getString("admin_$username", null)
        
        return if (storedHash == hashedPassword) {
            val sessionToken = generateSessionToken()
            prefs.edit().putString("session_token", sessionToken).apply()
            AuthResult.Success(sessionToken)
        } else {
            AuthResult.Failed("Invalid credentials")
        }
    }
    
    /**
     * Session Token Validation
     */
    fun validateSessionToken(token: String): Boolean {
        val storedToken = prefs.getString("session_token", null)
        val tokenTimestamp = prefs.getLong("token_timestamp", 0)
        val currentTime = System.currentTimeMillis()
        
        // Token expires after 24 hours
        return storedToken == token && (currentTime - tokenTimestamp) < 24 * 60 * 60 * 1000
    }
    
    /**
     * Rate Limiting for Alert Sending
     */
    fun checkRateLimit(deviceId: String): RateLimitResult {
        val key = "rate_limit_$deviceId"
        val lastSentTime = prefs.getLong(key, 0)
        val currentTime = System.currentTimeMillis()
        val timeDiff = currentTime - lastSentTime
        
        // Allow maximum 1 alert per 30 seconds
        return if (timeDiff >= 30000) {
            prefs.edit().putLong(key, currentTime).apply()
            RateLimitResult.Allowed
        } else {
            val remainingTime = (30000 - timeDiff) / 1000
            RateLimitResult.Limited(remainingTime)
        }
    }
    
    /**
     * Secure Device ID Generation
     */
    private fun getDeviceId(): String {
        var deviceId = prefs.getString("secure_device_id", null)
        if (deviceId == null) {
            deviceId = generateSecureDeviceId()
            prefs.edit().putString("secure_device_id", deviceId).apply()
        }
        return deviceId
    }
    
    private fun generateSecureDeviceId(): String {
        val random = SecureRandom()
        val bytes = ByteArray(16)
        random.nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }
    
    private fun getOrCreateSecretKey(): SecretKey {
        val keyString = prefs.getString("encryption_key", null)
        return if (keyString != null) {
            val keyBytes = Base64.decode(keyString, Base64.DEFAULT)
            SecretKeySpec(keyBytes, "AES")
        } else {
            val keyGenerator = KeyGenerator.getInstance("AES")
            keyGenerator.init(KEY_LENGTH)
            val secretKey = keyGenerator.generateKey()
            
            val encodedKey = Base64.encodeToString(secretKey.encoded, Base64.DEFAULT)
            prefs.edit().putString("encryption_key", encodedKey).apply()
            
            secretKey
        }
    }
    
    private fun hashSHA256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(hashBytes, Base64.NO_WRAP)
    }
    
    private fun generateSessionToken(): String {
        val random = SecureRandom()
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        val token = Base64.encodeToString(bytes, Base64.NO_WRAP)
        
        prefs.edit().putLong("token_timestamp", System.currentTimeMillis()).apply()
        return token
    }
}

/**
 * Data Classes for Security
 */
data class EncryptedMessage(
    val encryptedData: String,
    val iv: String,
    val timestamp: Long
)

sealed class AuthResult {
    data class Success(val sessionToken: String) : AuthResult()
    data class Failed(val reason: String) : AuthResult()
}

sealed class RateLimitResult {
    object Allowed : RateLimitResult()
    data class Limited(val remainingSeconds: Long) : RateLimitResult()
}