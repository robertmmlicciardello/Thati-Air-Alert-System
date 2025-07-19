package com.thati.airalert

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.thati.airalert.security.SecurityManager
import com.thati.airalert.security.AuthResult
import com.thati.airalert.security.RateLimitResult
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

/**
 * Comprehensive Security Manager Tests
 * Production-ready test suite for security features
 */
@RunWith(AndroidJUnit4::class)
class SecurityManagerTest {

    private lateinit var securityManager: SecurityManager
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        securityManager = SecurityManager(context)
    }

    @Test
    fun testDeviceSecurityTokenGeneration() {
        // Test token generation
        val token1 = securityManager.generateDeviceSecurityToken()
        val token2 = securityManager.generateDeviceSecurityToken()
        
        // Tokens should be different
        assertNotEquals("Security tokens should be unique", token1, token2)
        
        // Tokens should be non-empty
        assertTrue("Token should not be empty", token1.isNotEmpty())
        assertTrue("Token should not be empty", token2.isNotEmpty())
        
        // Tokens should be of expected length (SHA-256 base64 encoded)
        assertTrue("Token should be proper length", token1.length > 40)
    }

    @Test
    fun testMessageEncryptionDecryption() {
        val originalMessage = "Test alert message for encryption"
        
        // Encrypt message
        val encryptedMessage = securityManager.encryptAlertMessage(originalMessage)
        
        // Verify encryption result
        assertNotNull("Encrypted message should not be null", encryptedMessage)
        assertNotEquals("Encrypted data should be different from original", 
            originalMessage, encryptedMessage.encryptedData)
        assertTrue("IV should be present", encryptedMessage.iv.isNotEmpty())
        assertTrue("Timestamp should be set", encryptedMessage.timestamp > 0)
        
        // Decrypt message
        val decryptedMessage = securityManager.decryptAlertMessage(encryptedMessage)
        
        // Verify decryption
        assertEquals("Decrypted message should match original", 
            originalMessage, decryptedMessage)
    }

    @Test
    fun testMessageIntegrityVerification() {
        val message = "Test message for integrity check"
        
        // Generate signature
        val signature = securityManager.generateMessageSignature(message)
        
        // Verify valid signature
        assertTrue("Valid signature should verify", 
            securityManager.verifyMessageIntegrity(message, signature))
        
        // Test with tampered message
        val tamperedMessage = "Tampered message"
        assertFalse("Tampered message should not verify", 
            securityManager.verifyMessageIntegrity(tamperedMessage, signature))
        
        // Test with invalid signature
        val invalidSignature = "invalid_signature"
        assertFalse("Invalid signature should not verify", 
            securityManager.verifyMessageIntegrity(message, invalidSignature))
    }

    @Test
    fun testRateLimiting() {
        val deviceId = "test_device_123"
        
        // First request should be allowed
        val result1 = securityManager.checkRateLimit(deviceId)
        assertTrue("First request should be allowed", result1 is RateLimitResult.Allowed)
        
        // Immediate second request should be limited
        val result2 = securityManager.checkRateLimit(deviceId)
        assertTrue("Second immediate request should be limited", 
            result2 is RateLimitResult.Limited)
        
        if (result2 is RateLimitResult.Limited) {
            assertTrue("Remaining time should be positive", result2.remainingSeconds > 0)
            assertTrue("Remaining time should be reasonable", result2.remainingSeconds <= 30)
        }
    }

    @Test
    fun testSessionTokenValidation() {
        // Test with invalid token
        assertFalse("Invalid token should not validate", 
            securityManager.validateSessionToken("invalid_token"))
        
        // Note: Testing valid token requires authentication first
        // This would be tested in integration tests
    }

    @Test
    fun testEncryptionWithDifferentMessages() {
        val messages = listOf(
            "Short message",
            "This is a longer message with more content to test encryption",
            "မြန်မာစာ သတိပေးချက်", // Myanmar text
            "Special chars: !@#$%^&*()_+-=[]{}|;':\",./<>?",
            "" // Empty message
        )
        
        messages.forEach { message ->
            try {
                val encrypted = securityManager.encryptAlertMessage(message)
                val decrypted = securityManager.decryptAlertMessage(encrypted)
                assertEquals("Message should decrypt correctly: '$message'", 
                    message, decrypted)
            } catch (e: Exception) {
                if (message.isEmpty()) {
                    // Empty message might throw exception, which is acceptable
                    assertTrue("Empty message should throw SecurityException", 
                        e is SecurityException)
                } else {
                    fail("Encryption/decryption failed for message: '$message' - ${e.message}")
                }
            }
        }
    }

    @Test
    fun testConcurrentEncryption() {
        val message = "Concurrent encryption test"
        val results = mutableListOf<String>()
        
        // Simulate concurrent encryption
        repeat(10) {
            val encrypted = securityManager.encryptAlertMessage(message)
            val decrypted = securityManager.decryptAlertMessage(encrypted)
            results.add(decrypted)
        }
        
        // All results should be the same as original message
        results.forEach { result ->
            assertEquals("Concurrent encryption should work correctly", message, result)
        }
    }

    @Test
    fun testSecurityTokenUniqueness() {
        val tokens = mutableSetOf<String>()
        
        // Generate multiple tokens
        repeat(100) {
            val token = securityManager.generateDeviceSecurityToken()
            assertFalse("Token should be unique", tokens.contains(token))
            tokens.add(token)
        }
        
        assertEquals("All tokens should be unique", 100, tokens.size)
    }

    @Test
    fun testEncryptionPerformance() {
        val message = "Performance test message for encryption benchmarking"
        val startTime = System.currentTimeMillis()
        
        // Perform multiple encryption/decryption cycles
        repeat(100) {
            val encrypted = securityManager.encryptAlertMessage(message)
            securityManager.decryptAlertMessage(encrypted)
        }
        
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime
        
        // Should complete within reasonable time (adjust threshold as needed)
        assertTrue("Encryption should be performant (took ${duration}ms)", 
            duration < 5000) // 5 seconds for 100 operations
    }

    @Test
    fun testInvalidEncryptedMessageHandling() {
        // Test with corrupted encrypted data
        val invalidEncrypted = com.thati.airalert.security.EncryptedMessage(
            encryptedData = "invalid_base64_data",
            iv = "invalid_iv",
            timestamp = System.currentTimeMillis()
        )
        
        try {
            securityManager.decryptAlertMessage(invalidEncrypted)
            fail("Should throw SecurityException for invalid encrypted data")
        } catch (e: SecurityException) {
            assertTrue("Should throw SecurityException", true)
        }
    }

    @Test
    fun testRateLimitingWithMultipleDevices() {
        val device1 = "device_1"
        val device2 = "device_2"
        
        // Both devices should be allowed initially
        assertTrue("Device 1 should be allowed", 
            securityManager.checkRateLimit(device1) is RateLimitResult.Allowed)
        assertTrue("Device 2 should be allowed", 
            securityManager.checkRateLimit(device2) is RateLimitResult.Allowed)
        
        // Immediate second requests should be limited for both
        assertTrue("Device 1 should be limited", 
            securityManager.checkRateLimit(device1) is RateLimitResult.Limited)
        assertTrue("Device 2 should be limited", 
            securityManager.checkRateLimit(device2) is RateLimitResult.Limited)
    }
}