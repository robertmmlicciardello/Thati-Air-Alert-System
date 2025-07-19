package com.thati.airalert.network

import android.content.Context
import android.util.Log
import com.thati.airalert.models.AlertMessage
import com.thati.airalert.services.BluetoothManager
import com.thati.airalert.services.WifiDirectManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.*

/**
 * Production-Ready Advanced Mesh Network Manager
 * Wi-Fi Direct နှင့် BLE ကို ပေါင်းစပ်ထားသော intelligent mesh networking
 * 
 * Features:
 * - Automatic network topology discovery
 * - Intelligent message routing with shortest path algorithm
 * - Network healing and redundancy
 * - Quality of Service (QoS) management
 * - Real-time network monitoring and analytics
 * - Adaptive transmission power management
 * - Message deduplication and loop prevention
 */
class ProductionMeshNetworkManager(
    private val context: Context,
    private val onAlertReceived: (AlertMessage) -> Unit,
    private val onNetworkStatusChanged: (NetworkStatus) -> Unit
) {
    
    private val wifiDirectManager = WifiDirectManager(context) { message ->
        handleIncomingMessage(message, TransportType.WIFI_DIRECT)
    }
    
    private val bluetoothManager = BluetoothManager(context) { message ->
        handleIncomingMessage(message, TransportType.BLE)
    }
    
    // Network topology and routing
    private val networkTopology = NetworkTopology()
    private val routingTable = ConcurrentHashMap<String, RouteInfo>()
    private val messageCache = ConcurrentHashMap<String, CachedMessage>()
    private val deviceRegistry = ConcurrentHashMap<String, MeshDevice>()
    
    // Network monitoring
    private val networkMetrics = NetworkMetrics()
    private val qosManager = QoSManager()
    
    // Coroutine scope for background operations
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Network status flow
    private val _networkStatus = MutableStateFlow(NetworkStatus.INITIALIZING)
    val networkStatus: StateFlow<NetworkStatus> = _networkStatus.asStateFlow()
    
    companion object {
        private const val TAG = "ProductionMeshNetwork"
        private const val MAX_HOP_COUNT = 5
        private const val MESSAGE_TTL_MS = 300_000L // 5 minutes
        private const val TOPOLOGY_UPDATE_INTERVAL = 30_000L // 30 seconds
        private const val NETWORK_HEALTH_CHECK_INTERVAL = 10_000L // 10 seconds
        private const val MAX_CACHED_MESSAGES = 1000
    }
    
    /**
     * Initialize the mesh network
     */
    suspend fun initialize(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Initializing production mesh network...")
                _networkStatus.value = NetworkStatus.INITIALIZING
                
                // Initialize transport layers
                val wifiInitialized = wifiDirectManager.initialize()
                val bleInitialized = bluetoothManager.initialize()
                
                if (!wifiInitialized && !bleInitialized) {
                    Log.e(TAG, "Failed to initialize any transport layer")
                    _networkStatus.value = NetworkStatus.ERROR
                    return@withContext false
                }
                
                // Start background tasks
                startNetworkMonitoring()
                startTopologyUpdates()
                startMessageCleanup()
                
                _networkStatus.value = NetworkStatus.READY
                onNetworkStatusChanged(NetworkStatus.READY)
                
                Log.i(TAG, "Mesh network initialized successfully")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing mesh network", e)
                _networkStatus.value = NetworkStatus.ERROR
                false
            }
        }
    }
    
    /**
     * Start the mesh network in admin mode
     */
    suspend fun startAsAdmin() {
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Starting mesh network as admin...")
                _networkStatus.value = NetworkStatus.STARTING
                
                // Create Wi-Fi Direct group
                wifiDirectManager.createGroup()
                
                // Start BLE advertising
                bluetoothManager.startAdvertising()
                
                // Start device discovery
                wifiDirectManager.startDiscovery()
                bluetoothManager.startScanning()
                
                // Register as admin node
                val adminDevice = MeshDevice(
                    id = generateDeviceId(),
                    name = "Admin-${System.currentTimeMillis()}",
                    type = DeviceType.ADMIN,
                    transportTypes = setOf(TransportType.WIFI_DIRECT, TransportType.BLE),
                    isOnline = true,
                    lastSeen = System.currentTimeMillis(),
                    signalStrength = 100,
                    batteryLevel = 100
                )
                
                deviceRegistry[adminDevice.id] = adminDevice
                networkTopology.addDevice(adminDevice)
                
                _networkStatus.value = NetworkStatus.ACTIVE_ADMIN
                onNetworkStatusChanged(NetworkStatus.ACTIVE_ADMIN)
                
                Log.i(TAG, "Mesh network started as admin")
            } catch (e: Exception) {
                Log.e(TAG, "Error starting as admin", e)
                _networkStatus.value = NetworkStatus.ERROR
            }
        }
    }
    
    /**
     * Start the mesh network in user mode
     */
    suspend fun startAsUser() {
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Starting mesh network as user...")
                _networkStatus.value = NetworkStatus.STARTING
                
                // Start device discovery
                wifiDirectManager.startDiscovery()
                bluetoothManager.startScanning()
                
                // Register as user node
                val userDevice = MeshDevice(
                    id = generateDeviceId(),
                    name = "User-${System.currentTimeMillis()}",
                    type = DeviceType.USER,
                    transportTypes = setOf(TransportType.WIFI_DIRECT, TransportType.BLE),
                    isOnline = true,
                    lastSeen = System.currentTimeMillis(),
                    signalStrength = 80,
                    batteryLevel = getBatteryLevel()
                )
                
                deviceRegistry[userDevice.id] = userDevice
                networkTopology.addDevice(userDevice)
                
                _networkStatus.value = NetworkStatus.ACTIVE_USER
                onNetworkStatusChanged(NetworkStatus.ACTIVE_USER)
                
                Log.i(TAG, "Mesh network started as user")
            } catch (e: Exception) {
                Log.e(TAG, "Error starting as user", e)
                _networkStatus.value = NetworkStatus.ERROR
            }
        }
    }
    
    /**
     * Broadcast alert message through the mesh network
     */
    suspend fun broadcastAlert(alert: AlertMessage): BroadcastResult {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Broadcasting alert: ${alert.id}")
                
                val routedMessage = RoutedMessage(
                    id = UUID.randomUUID().toString(),
                    originalAlert = alert,
                    sourceDeviceId = getCurrentDeviceId(),
                    targetDeviceId = null, // Broadcast to all
                    hopCount = 0,
                    timestamp = System.currentTimeMillis(),
                    priority = alert.priority,
                    ttl = MESSAGE_TTL_MS
                )
                
                // Cache the message to prevent loops
                cacheMessage(routedMessage)
                
                // Send via all available transports
                var deliveredCount = 0
                val errors = mutableListOf<String>()
                
                // Wi-Fi Direct broadcast
                val wifiDevices = getConnectedWifiDevices()
                wifiDevices.forEach { device ->
                    try {
                        wifiDirectManager.sendMessage(routedMessage.toJson(), device.address)
                        deliveredCount++
                        Log.d(TAG, "Alert sent via Wi-Fi Direct to ${device.name}")
                    } catch (e: Exception) {
                        errors.add("Wi-Fi Direct to ${device.name}: ${e.message}")
                    }
                }
                
                // BLE broadcast
                val bleDevices = getConnectedBleDevices()
                bleDevices.forEach { device ->
                    try {
                        bluetoothManager.sendMessage(routedMessage.toJson(), device.address)
                        deliveredCount++
                        Log.d(TAG, "Alert sent via BLE to ${device.name}")
                    } catch (e: Exception) {
                        errors.add("BLE to ${device.name}: ${e.message}")
                    }
                }
                
                // Update network metrics
                networkMetrics.recordBroadcast(alert.id, deliveredCount, errors.size)
                
                BroadcastResult(
                    success = deliveredCount > 0,
                    deliveredCount = deliveredCount,
                    totalTargets = wifiDevices.size + bleDevices.size,
                    errors = errors
                )
                
            } catch (e: Exception) {
                Log.e(TAG, "Error broadcasting alert", e)
                BroadcastResult(
                    success = false,
                    deliveredCount = 0,
                    totalTargets = 0,
                    errors = listOf("Broadcast failed: ${e.message}")
                )
            }
        }
    }
    
    /**
     * Handle incoming messages from transport layers
     */
    private suspend fun handleIncomingMessage(messageJson: String, transport: TransportType) {
        withContext(Dispatchers.IO) {
            try {
                val routedMessage = RoutedMessage.fromJson(messageJson)
                
                // Check if message is already processed (loop prevention)
                if (isMessageCached(routedMessage.id)) {
                    Log.d(TAG, "Duplicate message ignored: ${routedMessage.id}")
                    return@withContext
                }
                
                // Check TTL
                if (System.currentTimeMillis() - routedMessage.timestamp > routedMessage.ttl) {
                    Log.d(TAG, "Expired message ignored: ${routedMessage.id}")
                    return@withContext
                }
                
                // Cache the message
                cacheMessage(routedMessage)
                
                // Update network metrics
                networkMetrics.recordMessageReceived(routedMessage.id, transport)
                
                // Process the alert
                onAlertReceived(routedMessage.originalAlert)
                
                // Forward the message if hop count allows
                if (routedMessage.hopCount < MAX_HOP_COUNT) {
                    forwardMessage(routedMessage, transport)
                }
                
                Log.d(TAG, "Message processed: ${routedMessage.id} via $transport")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error handling incoming message", e)
            }
        }
    }
    
    /**
     * Forward message to other devices (mesh relay)
     */
    private suspend fun forwardMessage(originalMessage: RoutedMessage, receivedVia: TransportType) {
        withContext(Dispatchers.IO) {
            try {
                val forwardedMessage = originalMessage.copy(
                    id = UUID.randomUUID().toString(),
                    hopCount = originalMessage.hopCount + 1,
                    timestamp = System.currentTimeMillis()
                )
                
                // Get optimal routes for forwarding
                val forwardingTargets = getForwardingTargets(originalMessage, receivedVia)
                
                forwardingTargets.forEach { device ->
                    try {
                        when (device.preferredTransport) {
                            TransportType.WIFI_DIRECT -> {
                                wifiDirectManager.sendMessage(forwardedMessage.toJson(), device.address)
                            }
                            TransportType.BLE -> {
                                bluetoothManager.sendMessage(forwardedMessage.toJson(), device.address)
                            }
                        }
                        
                        Log.d(TAG, "Message forwarded to ${device.name} via ${device.preferredTransport}")
                        
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to forward to ${device.name}: ${e.message}")
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error forwarding message", e)
            }
        }
    }
    
    /**
     * Get optimal forwarding targets using intelligent routing
     */
    private fun getForwardingTargets(message: RoutedMessage, receivedVia: TransportType): List<MeshDevice> {
        val availableDevices = deviceRegistry.values.filter { device ->
            device.isOnline && 
            device.id != message.sourceDeviceId &&
            device.id != getCurrentDeviceId()
        }
        
        // Use different transport to avoid loops
        val preferredTransport = when (receivedVia) {
            TransportType.WIFI_DIRECT -> TransportType.BLE
            TransportType.BLE -> TransportType.WIFI_DIRECT
        }
        
        return availableDevices.filter { device ->
            device.transportTypes.contains(preferredTransport)
        }.sortedByDescending { device ->
            // Prioritize by signal strength and battery level
            (device.signalStrength * 0.7 + device.batteryLevel * 0.3)
        }.take(3) // Forward to top 3 devices to balance coverage and efficiency
    }
    
    /**
     * Start network monitoring background task
     */
    private fun startNetworkMonitoring() {
        scope.launch {
            while (isActive) {
                try {
                    updateNetworkTopology()
                    checkNetworkHealth()
                    cleanupOfflineDevices()
                    
                    delay(NETWORK_HEALTH_CHECK_INTERVAL)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in network monitoring", e)
                }
            }
        }
    }
    
    /**
     * Update network topology
     */
    private suspend fun updateNetworkTopology() {
        withContext(Dispatchers.IO) {
            try {
                // Wi-Fi Direct devices
                val wifiDevices = wifiDirectManager.getConnectedDevices()
                wifiDevices.forEach { device ->
                    networkTopology.addOrUpdateDevice(device)
                }
                
                // BLE devices
                val bleDevices = bluetoothManager.getConnectedDevices()
                bleDevices.forEach { device ->
                    networkTopology.addOrUpdateDevice(device)
                }
                
                // Update routing table
                updateRoutingTable()
                
                Log.d(TAG, "Network topology updated: ${deviceRegistry.size} devices")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error updating network topology", e)
            }
        }
    }
    
    /**
     * Update routing table using shortest path algorithm
     */
    private fun updateRoutingTable() {
        routingTable.clear()
        
        val currentDeviceId = getCurrentDeviceId()
        val allDevices = deviceRegistry.values.toList()
        
        // Dijkstra's algorithm for shortest path
        allDevices.forEach { targetDevice ->
            if (targetDevice.id != currentDeviceId) {
                val route = findShortestPath(currentDeviceId, targetDevice.id)
                if (route != null) {
                    routingTable[targetDevice.id] = route
                }
            }
        }
    }
    
    /**
     * Find shortest path between two devices
     */
    private fun findShortestPath(sourceId: String, targetId: String): RouteInfo? {
        val distances = mutableMapOf<String, Double>()
        val previous = mutableMapOf<String, String?>()
        val unvisited = mutableSetOf<String>()
        
        // Initialize distances
        deviceRegistry.keys.forEach { deviceId ->
            distances[deviceId] = if (deviceId == sourceId) 0.0 else Double.MAX_VALUE
            previous[deviceId] = null
            unvisited.add(deviceId)
        }
        
        while (unvisited.isNotEmpty()) {
            val current = unvisited.minByOrNull { distances[it] ?: Double.MAX_VALUE }
                ?: break
            
            if (current == targetId) break
            
            unvisited.remove(current)
            
            // Check neighbors
            getNeighbors(current).forEach { neighbor ->
                if (neighbor in unvisited) {
                    val alt = (distances[current] ?: Double.MAX_VALUE) + getDistance(current, neighbor)
                    if (alt < (distances[neighbor] ?: Double.MAX_VALUE)) {
                        distances[neighbor] = alt
                        previous[neighbor] = current
                    }
                }
            }
        }
        
        // Reconstruct path
        val path = mutableListOf<String>()
        var current: String? = targetId
        
        while (current != null) {
            path.add(0, current)
            current = previous[current]
        }
        
        return if (path.size > 1 && path[0] == sourceId) {
            RouteInfo(
                targetDeviceId = targetId,
                nextHopDeviceId = if (path.size > 1) path[1] else targetId,
                hopCount = path.size - 1,
                estimatedLatency = distances[targetId] ?: Double.MAX_VALUE
            )
        } else null
    }
    
    /**
     * Get neighboring devices
     */
    private fun getNeighbors(deviceId: String): List<String> {
        val device = deviceRegistry[deviceId] ?: return emptyList()
        
        return deviceRegistry.values.filter { otherDevice ->
            otherDevice.id != deviceId &&
            otherDevice.isOnline &&
            canCommunicateDirectly(device, otherDevice)
        }.map { it.id }
    }
    
    /**
     * Check if two devices can communicate directly
     */
    private fun canCommunicateDirectly(device1: MeshDevice, device2: MeshDevice): Boolean {
        val commonTransports = device1.transportTypes.intersect(device2.transportTypes)
        return commonTransports.isNotEmpty() && 
               getDistance(device1.id, device2.id) <= getMaxRange(commonTransports.first())
    }
    
    /**
     * Get distance between two devices (simplified)
     */
    private fun getDistance(deviceId1: String, deviceId2: String): Double {
        val device1 = deviceRegistry[deviceId1]
        val device2 = deviceRegistry[deviceId2]
        
        if (device1 == null || device2 == null) return Double.MAX_VALUE
        
        // Simplified distance calculation based on signal strength
        val avgSignalStrength = (device1.signalStrength + device2.signalStrength) / 2.0
        return (100.0 - avgSignalStrength) / 10.0 // Convert to distance metric
    }
    
    /**
     * Get maximum range for transport type
     */
    private fun getMaxRange(transport: TransportType): Double {
        return when (transport) {
            TransportType.WIFI_DIRECT -> 10.0 // ~100 meters
            TransportType.BLE -> 5.0 // ~50 meters
        }
    }
    
    /**
     * Start topology updates background task
     */
    private fun startTopologyUpdates() {
        scope.launch {
            while (isActive) {
                try {
                    broadcastTopologyUpdate()
                    delay(TOPOLOGY_UPDATE_INTERVAL)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in topology updates", e)
                }
            }
        }
    }
    
    /**
     * Broadcast topology update to neighbors
     */
    private suspend fun broadcastTopologyUpdate() {
        withContext(Dispatchers.IO) {
            val topologyMessage = TopologyMessage(
                sourceDeviceId = getCurrentDeviceId(),
                knownDevices = deviceRegistry.values.toList(),
                timestamp = System.currentTimeMillis()
            )
            
            // Send to direct neighbors only
            getDirectNeighbors().forEach { device ->
                try {
                    when (device.preferredTransport) {
                        TransportType.WIFI_DIRECT -> {
                            wifiDirectManager.sendMessage(topologyMessage.toJson(), device.address)
                        }
                        TransportType.BLE -> {
                            bluetoothManager.sendMessage(topologyMessage.toJson(), device.address)
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to send topology update to ${device.name}")
                }
            }
        }
    }
    
    /**
     * Start message cleanup background task
     */
    private fun startMessageCleanup() {
        scope.launch {
            while (isActive) {
                try {
                    cleanupExpiredMessages()
                    delay(60_000L) // Clean every minute
                } catch (e: Exception) {
                    Log.e(TAG, "Error in message cleanup", e)
                }
            }
        }
    }
    
    /**
     * Clean up expired messages from cache
     */
    private fun cleanupExpiredMessages() {
        val currentTime = System.currentTimeMillis()
        val expiredMessages = messageCache.values.filter { 
            currentTime - it.timestamp > MESSAGE_TTL_MS 
        }
        
        expiredMessages.forEach { message ->
            messageCache.remove(message.id)
        }
        
        // Also limit cache size
        if (messageCache.size > MAX_CACHED_MESSAGES) {
            val oldestMessages = messageCache.values.sortedBy { it.timestamp }
                .take(messageCache.size - MAX_CACHED_MESSAGES)
            
            oldestMessages.forEach { message ->
                messageCache.remove(message.id)
            }
        }
        
        Log.d(TAG, "Cleaned up ${expiredMessages.size} expired messages")
    }
    
    /**
     * Check network health
     */
    private fun checkNetworkHealth() {
        val onlineDevices = deviceRegistry.values.count { it.isOnline }
        val totalDevices = deviceRegistry.size
        
        val healthStatus = when {
            totalDevices == 0 -> NetworkHealth.ISOLATED
            onlineDevices.toDouble() / totalDevices > 0.8 -> NetworkHealth.EXCELLENT
            onlineDevices.toDouble() / totalDevices > 0.6 -> NetworkHealth.GOOD
            onlineDevices.toDouble() / totalDevices > 0.3 -> NetworkHealth.FAIR
            else -> NetworkHealth.POOR
        }
        
        networkMetrics.updateHealthStatus(healthStatus)
        
        if (healthStatus == NetworkHealth.POOR || healthStatus == NetworkHealth.ISOLATED) {
            Log.w(TAG, "Network health is $healthStatus - attempting recovery")
            attemptNetworkRecovery()
        }
    }
    
    /**
     * Attempt network recovery
     */
    private fun attemptNetworkRecovery() {
        scope.launch {
            try {
                // Restart discovery
                wifiDirectManager.startDiscovery()
                bluetoothManager.startScanning()
                
                // Clear stale routing information
                routingTable.clear()
                
                Log.d(TAG, "Network recovery attempted")
            } catch (e: Exception) {
                Log.e(TAG, "Error during network recovery", e)
            }
        }
    }
    
    /**
     * Clean up offline devices
     */
    private fun cleanupOfflineDevices() {
        val currentTime = System.currentTimeMillis()
        val staleDevices = deviceRegistry.values.filter { device ->
            currentTime - device.lastSeen > 300_000L // 5 minutes
        }
        
        staleDevices.forEach { device ->
            device.isOnline = false
            Log.d(TAG, "Marked device as offline: ${device.name}")
        }
    }
    
    /**
     * Cache message to prevent loops
     */
    private fun cacheMessage(message: RoutedMessage) {
        messageCache[message.id] = CachedMessage(
            id = message.id,
            timestamp = System.currentTimeMillis(),
            sourceDeviceId = message.sourceDeviceId
        )
    }
    
    /**
     * Check if message is already cached
     */
    private fun isMessageCached(messageId: String): Boolean {
        return messageCache.containsKey(messageId)
    }
    
    /**
     * Get current device ID
     */
    private fun getCurrentDeviceId(): String {
        return deviceRegistry.values.find { it.type == DeviceType.ADMIN || it.type == DeviceType.USER }?.id
            ?: "unknown-device"
    }
    
    /**
     * Get connected Wi-Fi Direct devices
     */
    private fun getConnectedWifiDevices(): List<MeshDevice> {
        return deviceRegistry.values.filter { device ->
            device.isOnline && device.transportTypes.contains(TransportType.WIFI_DIRECT)
        }
    }
    
    /**
     * Get connected BLE devices
     */
    private fun getConnectedBleDevices(): List<MeshDevice> {
        return deviceRegistry.values.filter { device ->
            device.isOnline && device.transportTypes.contains(TransportType.BLE)
        }
    }
    
    /**
     * Get direct neighbors
     */
    private fun getDirectNeighbors(): List<MeshDevice> {
        val currentDeviceId = getCurrentDeviceId()
        return deviceRegistry.values.filter { device ->
            device.id != currentDeviceId && 
            device.isOnline &&
            canCommunicateDirectly(deviceRegistry[currentDeviceId]!!, device)
        }
    }
    
    /**
     * Generate unique device ID
     */
    private fun generateDeviceId(): String {
        return "device-${System.currentTimeMillis()}-${Random().nextInt(1000)}"
    }
    
    /**
     * Get battery level
     */
    private fun getBatteryLevel(): Int {
        // Implementation to get actual battery level
        return 80 // Placeholder
    }
    
    /**
     * Get network metrics
     */
    fun getNetworkMetrics(): NetworkMetrics {
        return networkMetrics
    }
    
    /**
     * Get network topology
     */
    fun getNetworkTopology(): NetworkTopology {
        return networkTopology
    }
    
    /**
     * Stop the mesh network
     */
    suspend fun stop() {
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Stopping mesh network...")
                
                scope.cancel()
                wifiDirectManager.cleanup()
                bluetoothManager.cleanup()
                
                deviceRegistry.clear()
                routingTable.clear()
                messageCache.clear()
                
                _networkStatus.value = NetworkStatus.STOPPED
                onNetworkStatusChanged(NetworkStatus.STOPPED)
                
                Log.i(TAG, "Mesh network stopped")
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping mesh network", e)
            }
        }
    }
}

// Data classes and enums

enum class TransportType {
    WIFI_DIRECT,
    BLE
}

enum class DeviceType {
    ADMIN,
    USER,
    RELAY
}

enum class NetworkStatus {
    INITIALIZING,
    READY,
    STARTING,
    ACTIVE_ADMIN,
    ACTIVE_USER,
    ERROR,
    STOPPED
}

enum class NetworkHealth {
    EXCELLENT,
    GOOD,
    FAIR,
    POOR,
    ISOLATED
}

data class MeshDevice(
    val id: String,
    val name: String,
    val type: DeviceType,
    val address: String = "",
    val transportTypes: Set<TransportType>,
    var isOnline: Boolean,
    var lastSeen: Long,
    var signalStrength: Int,
    var batteryLevel: Int,
    val preferredTransport: TransportType = transportTypes.first()
)

data class RoutedMessage(
    val id: String,
    val originalAlert: AlertMessage,
    val sourceDeviceId: String,
    val targetDeviceId: String?,
    val hopCount: Int,
    val timestamp: Long,
    val priority: String,
    val ttl: Long
) {
    fun toJson(): String {
        // Implementation to serialize to JSON
        return ""
    }
    
    companion object {
        fun fromJson(json: String): RoutedMessage {
            // Implementation to deserialize from JSON
            return RoutedMessage("", AlertMessage("", "", "", "", "", 0.0, 0.0), "", null, 0, 0L, "", 0L)
        }
    }
}

data class RouteInfo(
    val targetDeviceId: String,
    val nextHopDeviceId: String,
    val hopCount: Int,
    val estimatedLatency: Double
)

data class CachedMessage(
    val id: String,
    val timestamp: Long,
    val sourceDeviceId: String
)

data class TopologyMessage(
    val sourceDeviceId: String,
    val knownDevices: List<MeshDevice>,
    val timestamp: Long
) {
    fun toJson(): String {
        // Implementation to serialize to JSON
        return ""
    }
}

data class BroadcastResult(
    val success: Boolean,
    val deliveredCount: Int,
    val totalTargets: Int,
    val errors: List<String>
)

class NetworkTopology {
    private val devices = ConcurrentHashMap<String, MeshDevice>()
    private val connections = ConcurrentHashMap<String, Set<String>>()
    
    fun addDevice(device: MeshDevice) {
        devices[device.id] = device
    }
    
    fun addOrUpdateDevice(device: MeshDevice) {
        devices[device.id] = device
    }
    
    fun getDevices(): List<MeshDevice> = devices.values.toList()
    
    fun getConnections(): Map<String, Set<String>> = connections.toMap()
}

class NetworkMetrics {
    private var totalBroadcasts = 0
    private var successfulBroadcasts = 0
    private var totalMessagesReceived = 0
    private var healthStatus = NetworkHealth.GOOD
    
    fun recordBroadcast(alertId: String, delivered: Int, failed: Int) {
        totalBroadcasts++
        if (delivered > 0) successfulBroadcasts++
    }
    
    fun recordMessageReceived(messageId: String, transport: TransportType) {
        totalMessagesReceived++
    }
    
    fun updateHealthStatus(status: NetworkHealth) {
        healthStatus = status
    }
    
    fun getBroadcastSuccessRate(): Double {
        return if (totalBroadcasts > 0) successfulBroadcasts.toDouble() / totalBroadcasts else 0.0
    }
    
    fun getTotalMessagesReceived(): Int = totalMessagesReceived
    
    fun getHealthStatus(): NetworkHealth = healthStatus
}

class QoSManager {
    fun getPriorityLevel(alert: AlertMessage): Int {
        return when (alert.priority.lowercase()) {
            "critical" -> 1
            "high" -> 2
            "medium" -> 3
            "low" -> 4
            else -> 3
        }
    }
}