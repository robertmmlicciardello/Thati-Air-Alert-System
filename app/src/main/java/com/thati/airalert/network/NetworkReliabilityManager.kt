package com.thati.airalert.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.util.Log
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Network Reliability Manager - ကွန်ယက် ယုံကြည်စိတ်ချရမှု တိုးတက်စေရန်
 * Production အတွက် network reliability improvements
 */
class NetworkReliabilityManager(private val context: Context) {
    
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val reliabilityScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    // Message retry mechanism
    private val pendingMessages = ConcurrentHashMap<String, PendingMessage>()
    private val retryAttempts = ConcurrentHashMap<String, AtomicInteger>()
    
    // Network health tracking
    private val networkHealthHistory = mutableListOf<NetworkHealthSnapshot>()
    private val connectedDevices = ConcurrentHashMap<String, DeviceConnection>()
    
    companion object {
        private const val TAG = "NetworkReliability"
        private const val MAX_RETRY_ATTEMPTS = 5
        private const val RETRY_DELAY_BASE = 2000L // 2 seconds
        private const val HEALTH_HISTORY_SIZE = 100
        private const val CONNECTION_TIMEOUT = 30000L // 30 seconds
    }
    
    /**
     * Message Delivery with Retry Mechanism
     */
    fun sendMessageWithRetry(
        messageId: String,
        message: String,
        targetDevices: List<String>,
        onSuccess: (String) -> Unit,
        onFailure: (String, String) -> Unit
    ) {
        val pendingMessage = PendingMessage(
            id = messageId,
            content = message,
            targetDevices = targetDevices.toMutableSet(),
            timestamp = System.currentTimeMillis(),
            onSuccess = onSuccess,
            onFailure = onFailure
        )
        
        pendingMessages[messageId] = pendingMessage
        retryAttempts[messageId] = AtomicInteger(0)
        
        attemptMessageDelivery(messageId)
    }
    
    /**
     * Network Health Monitoring
     */
    fun startNetworkHealthMonitoring(onHealthChanged: (NetworkHealth) -> Unit) {
        reliabilityScope.launch {
            while (true) {
                val health = assessNetworkHealth()
                onHealthChanged(health)
                
                // Store health history
                synchronized(networkHealthHistory) {
                    networkHealthHistory.add(
                        NetworkHealthSnapshot(
                            timestamp = System.currentTimeMillis(),
                            health = health
                        )
                    )
                    
                    // Keep only recent history
                    if (networkHealthHistory.size > HEALTH_HISTORY_SIZE) {
                        networkHealthHistory.removeAt(0)
                    }
                }
                
                delay(15000) // Check every 15 seconds
            }
        }
    }
    
    /**
     * Adaptive Connection Strategy
     */
    fun getOptimalConnectionStrategy(networkHealth: NetworkHealth): ConnectionStrategy {
        return when (networkHealth.overallScore) {
            in 80..100 -> ConnectionStrategy(
                preferredProtocol = Protocol.WIFI_DIRECT,
                fallbackProtocol = Protocol.BLUETOOTH,
                connectionTimeout = 10000L,
                retryInterval = 2000L,
                maxConcurrentConnections = 8
            )
            in 60..79 -> ConnectionStrategy(
                preferredProtocol = Protocol.BLUETOOTH,
                fallbackProtocol = Protocol.WIFI_DIRECT,
                connectionTimeout = 15000L,
                retryInterval = 3000L,
                maxConcurrentConnections = 6
            )
            in 40..59 -> ConnectionStrategy(
                preferredProtocol = Protocol.BLUETOOTH,
                fallbackProtocol = Protocol.WIFI_DIRECT,
                connectionTimeout = 20000L,
                retryInterval = 5000L,
                maxConcurrentConnections = 4
            )
            else -> ConnectionStrategy(
                preferredProtocol = Protocol.BLUETOOTH,
                fallbackProtocol = null,
                connectionTimeout = 30000L,
                retryInterval = 10000L,
                maxConcurrentConnections = 2
            )
        }
    }
    
    /**
     * Device Connection Management
     */
    fun registerDeviceConnection(deviceId: String, protocol: Protocol, signalStrength: Int) {
        val connection = DeviceConnection(
            deviceId = deviceId,
            protocol = protocol,
            signalStrength = signalStrength,
            lastSeen = System.currentTimeMillis(),
            isActive = true,
            messagesSent = 0,
            messagesReceived = 0
        )
        
        connectedDevices[deviceId] = connection
        Log.d(TAG, "Device registered: $deviceId via $protocol (Signal: $signalStrength%)")
    }
    
    /**
     * Connection Health Check
     */
    fun performConnectionHealthCheck() {
        reliabilityScope.launch {
            val currentTime = System.currentTimeMillis()
            val devicesToRemove = mutableListOf<String>()
            
            connectedDevices.forEach { (deviceId, connection) ->
                val timeSinceLastSeen = currentTime - connection.lastSeen
                
                if (timeSinceLastSeen > CONNECTION_TIMEOUT) {
                    devicesToRemove.add(deviceId)
                    Log.w(TAG, "Device $deviceId timed out, removing from active connections")
                }
            }
            
            devicesToRemove.forEach { deviceId ->
                connectedDevices.remove(deviceId)
            }
        }
    }
    
    /**
     * Message Acknowledgment Handling
     */
    fun handleMessageAcknowledgment(messageId: String, fromDevice: String) {
        pendingMessages[messageId]?.let { pendingMessage ->
            pendingMessage.targetDevices.remove(fromDevice)
            
            if (pendingMessage.targetDevices.isEmpty()) {
                // All devices acknowledged
                pendingMessage.onSuccess(messageId)
                pendingMessages.remove(messageId)
                retryAttempts.remove(messageId)
                Log.d(TAG, "Message $messageId delivered successfully to all targets")
            }
        }
    }
    
    /**
     * Network Quality Assessment
     */
    private fun assessNetworkHealth(): NetworkHealth {
        val activeNetwork = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        
        val wifiScore = assessWifiHealth(networkCapabilities)
        val bluetoothScore = assessBluetoothHealth()
        val deviceConnectivity = assessDeviceConnectivity()
        val messageDeliveryRate = calculateMessageDeliveryRate()
        
        val overallScore = (wifiScore + bluetoothScore + deviceConnectivity + messageDeliveryRate) / 4
        
        return NetworkHealth(
            wifiScore = wifiScore,
            bluetoothScore = bluetoothScore,
            deviceConnectivity = deviceConnectivity,
            messageDeliveryRate = messageDeliveryRate,
            overallScore = overallScore,
            connectedDevicesCount = connectedDevices.size,
            timestamp = System.currentTimeMillis()
        )
    }
    
    private fun assessWifiHealth(capabilities: NetworkCapabilities?): Int {
        return when {
            capabilities == null -> 0
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                val signalStrength = capabilities.signalStrength
                when {
                    signalStrength >= -50 -> 100
                    signalStrength >= -60 -> 80
                    signalStrength >= -70 -> 60
                    signalStrength >= -80 -> 40
                    else -> 20
                }
            }
            else -> 30 // Has network but not WiFi
        }
    }
    
    private fun assessBluetoothHealth(): Int {
        val bluetoothDevices = connectedDevices.values.filter { it.protocol == Protocol.BLUETOOTH }
        return when {
            bluetoothDevices.isEmpty() -> 0
            bluetoothDevices.size >= 4 -> 100
            bluetoothDevices.size >= 3 -> 80
            bluetoothDevices.size >= 2 -> 60
            else -> 40
        }
    }
    
    private fun assessDeviceConnectivity(): Int {
        val totalDevices = connectedDevices.size
        val activeDevices = connectedDevices.values.count { it.isActive }
        
        return if (totalDevices == 0) 0 else (activeDevices * 100) / totalDevices
    }
    
    private fun calculateMessageDeliveryRate(): Int {
        val recentMessages = pendingMessages.values.filter { 
            System.currentTimeMillis() - it.timestamp < 300000 // Last 5 minutes
        }
        
        return if (recentMessages.isEmpty()) 100 else {
            val successfulMessages = recentMessages.count { it.targetDevices.isEmpty() }
            (successfulMessages * 100) / recentMessages.size
        }
    }
    
    private fun attemptMessageDelivery(messageId: String) {
        reliabilityScope.launch {
            val pendingMessage = pendingMessages[messageId] ?: return@launch
            val attempts = retryAttempts[messageId] ?: return@launch
            
            if (attempts.get() >= MAX_RETRY_ATTEMPTS) {
                pendingMessage.onFailure(messageId, "Max retry attempts exceeded")
                pendingMessages.remove(messageId)
                retryAttempts.remove(messageId)
                return@launch
            }
            
            try {
                // Attempt to send message to remaining target devices
                val success = sendToTargetDevices(pendingMessage)
                
                if (!success) {
                    // Schedule retry with exponential backoff
                    val retryDelay = RETRY_DELAY_BASE * (1L shl attempts.get())
                    delay(retryDelay)
                    
                    attempts.incrementAndGet()
                    attemptMessageDelivery(messageId)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Message delivery attempt failed: ${e.message}")
                attempts.incrementAndGet()
                
                val retryDelay = RETRY_DELAY_BASE * (1L shl attempts.get())
                delay(retryDelay)
                attemptMessageDelivery(messageId)
            }
        }
    }
    
    private suspend fun sendToTargetDevices(pendingMessage: PendingMessage): Boolean {
        // This would integrate with actual WiFi Direct and Bluetooth managers
        // For now, simulate the sending process
        return try {
            // Simulate network operation
            delay(1000)
            
            // Simulate some success rate based on network health
            val currentHealth = assessNetworkHealth()
            val successRate = currentHealth.overallScore / 100.0
            
            Math.random() < successRate
        } catch (e: Exception) {
            false
        }
    }
    
    fun stopReliabilityManager() {
        reliabilityScope.cancel()
        pendingMessages.clear()
        retryAttempts.clear()
        connectedDevices.clear()
        Log.d(TAG, "Network reliability manager stopped")
    }
}

/**
 * Data Classes for Network Reliability
 */
data class PendingMessage(
    val id: String,
    val content: String,
    val targetDevices: MutableSet<String>,
    val timestamp: Long,
    val onSuccess: (String) -> Unit,
    val onFailure: (String, String) -> Unit
)

data class NetworkHealth(
    val wifiScore: Int,
    val bluetoothScore: Int,
    val deviceConnectivity: Int,
    val messageDeliveryRate: Int,
    val overallScore: Int,
    val connectedDevicesCount: Int,
    val timestamp: Long
)

data class NetworkHealthSnapshot(
    val timestamp: Long,
    val health: NetworkHealth
)

data class DeviceConnection(
    val deviceId: String,
    val protocol: Protocol,
    val signalStrength: Int,
    val lastSeen: Long,
    val isActive: Boolean,
    val messagesSent: Int,
    val messagesReceived: Int
)

data class ConnectionStrategy(
    val preferredProtocol: Protocol,
    val fallbackProtocol: Protocol?,
    val connectionTimeout: Long,
    val retryInterval: Long,
    val maxConcurrentConnections: Int
)

enum class Protocol {
    WIFI_DIRECT, BLUETOOTH
}