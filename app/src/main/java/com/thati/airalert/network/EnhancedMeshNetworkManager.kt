package com.thati.airalert.network

import android.content.Context
import android.content.Intent
import com.thati.airalert.services.MeshNetworkService
import com.thati.airalert.models.AlertMessage
import com.thati.airalert.utils.Logger
import com.thati.airalert.utils.PreferenceManager
import kotlinx.coroutines.*
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

/**
 * Enhanced Mesh Network Manager - Mesh network operations ကို manage လုပ်ရန်
 */
class EnhancedMeshNetworkManager(private val context: Context) {
    
    private val preferenceManager = PreferenceManager(context)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Network statistics
    private var messagesSent = 0
    private var messagesReceived = 0
    private var messagesForwarded = 0
    private val networkStats = ConcurrentHashMap<String, Any>()
    
    companion object {
        private const val TAG = "EnhancedMeshNetworkManager"
        
        // Network topology types
        const val TOPOLOGY_STAR = "star"
        const val TOPOLOGY_MESH = "mesh"
        const val TOPOLOGY_HYBRID = "hybrid"
        
        // Network roles
        const val ROLE_ADMIN = "admin"
        const val ROLE_RELAY = "relay"
        const val ROLE_USER = "user"
    }
    
    data class NetworkTopology(
        val type: String,
        val centerNode: String? = null,
        val nodes: List<NetworkNode>,
        val connections: List<NetworkConnection>
    )
    
    data class NetworkNode(
        val nodeId: String,
        val nodeName: String,
        val role: String,
        val position: Pair<Double, Double>? = null,
        val capabilities: List<String>,
        val batteryLevel: Int,
        val signalStrength: Int
    )
    
    data class NetworkConnection(
        val fromNode: String,
        val toNode: String,
        val connectionType: String,
        val strength: Int,
        val latency: Long,
        val isActive: Boolean
    )
    
    /**
     * Start mesh network in admin mode
     */
    fun startAdminMode() {
        Logger.i(TAG, "Starting enhanced mesh network in admin mode")
        
        val intent = Intent(context, MeshNetworkService::class.java).apply {
            action = MeshNetworkService.ACTION_START_ADMIN
        }
        context.startForegroundService(intent)
        
        // Initialize network topology
        initializeNetworkTopology(ROLE_ADMIN)
        
        // Start network monitoring
        startNetworkMonitoring()
    }
    
    /**
     * Start mesh network in user mode
     */
    fun startUserMode() {
        Logger.i(TAG, "Starting enhanced mesh network in user mode")
        
        val intent = Intent(context, MeshNetworkService::class.java).apply {
            action = MeshNetworkService.ACTION_START_USER
        }
        context.startForegroundService(intent)
        
        // Initialize network topology
        initializeNetworkTopology(ROLE_USER)
        
        // Start network monitoring
        startNetworkMonitoring()
    }
    
    /**
     * Send alert through mesh network
     */
    fun sendAlert(alertMessage: AlertMessage) {
        Logger.i(TAG, "Sending alert through mesh network: ${alertMessage.message}")
        
        val intent = Intent(context, MeshNetworkService::class.java).apply {
            action = MeshNetworkService.ACTION_BROADCAST_ALERT
            putExtra("alert_object", alertMessage)
        }
        context.startService(intent)
        
        messagesSent++
        updateNetworkStats("messages_sent", messagesSent)
    }
    
    /**
     * Send emergency alert with high priority
     */
    fun sendEmergencyAlert(message: String, location: String? = null) {
        Logger.w(TAG, "Sending emergency alert: $message")
        
        val intent = Intent(context, MeshNetworkService::class.java).apply {
            action = MeshNetworkService.ACTION_SEND_ALERT
            putExtra(MeshNetworkService.EXTRA_ALERT_MESSAGE, message)
            putExtra(MeshNetworkService.EXTRA_ALERT_TYPE, "emergency")
            putExtra(MeshNetworkService.EXTRA_ALERT_PRIORITY, 4) // Emergency priority
            putExtra(MeshNetworkService.EXTRA_ALERT_LOCATION, location)
        }
        context.startService(intent)
        
        messagesSent++
        updateNetworkStats("emergency_alerts_sent", 
            (networkStats["emergency_alerts_sent"] as? Int ?: 0) + 1)
    }
    
    /**
     * Stop mesh network service
     */
    fun stopMeshNetwork() {
        Logger.i(TAG, "Stopping enhanced mesh network")
        
        val intent = Intent(context, MeshNetworkService::class.java).apply {
            action = MeshNetworkService.ACTION_STOP_SERVICE
        }
        context.startService(intent)
        
        scope.cancel()
    }
    
    /**
     * Get current network topology
     */
    fun getNetworkTopology(): NetworkTopology {
        // Mock implementation - in real scenario, this would query the service
        return NetworkTopology(
            type = TOPOLOGY_HYBRID,
            centerNode = if (isAdminMode()) preferenceManager.getDeviceId() else null,
            nodes = generateMockNodes(),
            connections = generateMockConnections()
        )
    }
    
    /**
     * Get network statistics
     */
    fun getNetworkStats(): Map<String, Any> {
        return mapOf(
            "messages_sent" to messagesSent,
            "messages_received" to messagesReceived,
            "messages_forwarded" to messagesForwarded,
            "network_uptime" to getNetworkUptime(),
            "connected_devices" to getConnectedDevicesCount(),
            "network_health" to calculateNetworkHealth(),
            "topology_type" to getCurrentTopologyType(),
            "coverage_area" to estimateCoverageArea()
        ) + networkStats
    }
    
    /**
     * Optimize network topology
     */
    fun optimizeNetworkTopology() {
        scope.launch {
            Logger.i(TAG, "Optimizing network topology")
            
            try {
                // Analyze current topology
                val topology = getNetworkTopology()
                val optimization = analyzeTopologyOptimization(topology)
                
                // Apply optimizations
                applyTopologyOptimizations(optimization)
                
                Logger.i(TAG, "Network topology optimized successfully")
                
            } catch (e: Exception) {
                Logger.e(TAG, "Error optimizing network topology: ${e.message}")
            }
        }
    }
    
    /**
     * Perform network health check
     */
    fun performHealthCheck(): NetworkHealthReport {
        Logger.d(TAG, "Performing network health check")
        
        return NetworkHealthReport(
            overallHealth = calculateNetworkHealth(),
            connectedNodes = getConnectedDevicesCount(),
            averageLatency = calculateAverageLatency(),
            messageDeliveryRate = calculateMessageDeliveryRate(),
            networkCoverage = estimateCoverageArea(),
            criticalIssues = identifyCriticalIssues(),
            recommendations = generateRecommendations()
        )
    }
    
    /**
     * Get mesh network visualization data
     */
    fun getVisualizationData(): NetworkVisualizationData {
        val topology = getNetworkTopology()
        
        return NetworkVisualizationData(
            nodes = topology.nodes.map { node ->
                VisualizationNode(
                    id = node.nodeId,
                    name = node.nodeName,
                    type = node.role,
                    x = node.position?.first ?: 0.0,
                    y = node.position?.second ?: 0.0,
                    status = if (isNodeOnline(node.nodeId)) "online" else "offline",
                    batteryLevel = node.batteryLevel,
                    signalStrength = node.signalStrength
                )
            },
            edges = topology.connections.map { conn ->
                VisualizationEdge(
                    from = conn.fromNode,
                    to = conn.toNode,
                    type = conn.connectionType,
                    strength = conn.strength,
                    active = conn.isActive
                )
            },
            metadata = mapOf(
                "topology_type" to topology.type,
                "total_nodes" to topology.nodes.size,
                "active_connections" to topology.connections.count { it.isActive },
                "network_diameter" to calculateNetworkDiameter(topology)
            )
        )
    }
    
    // Private helper methods
    
    private fun initializeNetworkTopology(role: String) {
        preferenceManager.setNetworkRole(role)
        updateNetworkStats("role", role)
        updateNetworkStats("initialization_time", System.currentTimeMillis())
    }
    
    private fun startNetworkMonitoring() {
        scope.launch {
            while (true) {
                try {
                    // Monitor network health
                    val health = calculateNetworkHealth()
                    updateNetworkStats("network_health", health)
                    
                    // Monitor message flow
                    updateMessageFlowStats()
                    
                    // Check for network issues
                    checkNetworkIssues()
                    
                    delay(30000) // Monitor every 30 seconds
                    
                } catch (e: Exception) {
                    Logger.e(TAG, "Error in network monitoring: ${e.message}")
                    delay(60000) // Wait longer on error
                }
            }
        }
    }
    
    private fun updateNetworkStats(key: String, value: Any) {
        networkStats[key] = value
    }
    
    private fun generateMockNodes(): List<NetworkNode> {
        // Mock implementation - generate sample nodes
        return listOf(
            NetworkNode(
                nodeId = "admin-001",
                nodeName = "Admin Device",
                role = ROLE_ADMIN,
                position = Pair(0.0, 0.0),
                capabilities = listOf("broadcast", "relay", "admin"),
                batteryLevel = 85,
                signalStrength = 90
            ),
            NetworkNode(
                nodeId = "relay-001",
                nodeName = "Relay Device 1",
                role = ROLE_RELAY,
                position = Pair(100.0, 50.0),
                capabilities = listOf("relay", "forward"),
                batteryLevel = 70,
                signalStrength = 75
            ),
            NetworkNode(
                nodeId = "user-001",
                nodeName = "User Device 1",
                role = ROLE_USER,
                position = Pair(200.0, 100.0),
                capabilities = listOf("receive"),
                batteryLevel = 60,
                signalStrength = 65
            )
        )
    }
    
    private fun generateMockConnections(): List<NetworkConnection> {
        return listOf(
            NetworkConnection(
                fromNode = "admin-001",
                toNode = "relay-001",
                connectionType = "wifi_direct",
                strength = 85,
                latency = 50,
                isActive = true
            ),
            NetworkConnection(
                fromNode = "relay-001",
                toNode = "user-001",
                connectionType = "bluetooth",
                strength = 70,
                latency = 100,
                isActive = true
            )
        )
    }
    
    private fun isAdminMode(): Boolean {
        return preferenceManager.getNetworkRole() == ROLE_ADMIN
    }
    
    private fun getNetworkUptime(): Long {
        val initTime = networkStats["initialization_time"] as? Long ?: System.currentTimeMillis()
        return System.currentTimeMillis() - initTime
    }
    
    private fun getConnectedDevicesCount(): Int {
        // Mock implementation
        return 3
    }
    
    private fun calculateNetworkHealth(): Int {
        // Mock calculation based on various factors
        val connectedDevices = getConnectedDevicesCount()
        val messageDeliveryRate = calculateMessageDeliveryRate()
        val averageLatency = calculateAverageLatency()
        
        // Simple health calculation (0-100)
        val deviceScore = (connectedDevices * 20).coerceAtMost(40)
        val deliveryScore = (messageDeliveryRate * 30).toInt()
        val latencyScore = ((200 - averageLatency) / 2).coerceAtLeast(0).coerceAtMost(30)
        
        return (deviceScore + deliveryScore + latencyScore).coerceAtMost(100)
    }
    
    private fun getCurrentTopologyType(): String {
        return when (getConnectedDevicesCount()) {
            in 0..2 -> TOPOLOGY_STAR
            in 3..5 -> TOPOLOGY_HYBRID
            else -> TOPOLOGY_MESH
        }
    }
    
    private fun estimateCoverageArea(): Double {
        // Mock calculation in square meters
        return getConnectedDevicesCount() * 10000.0 // 10km² per device
    }
    
    private fun calculateAverageLatency(): Long {
        // Mock implementation
        return 75L // milliseconds
    }
    
    private fun calculateMessageDeliveryRate(): Double {
        if (messagesSent == 0) return 1.0
        return (messagesSent - (messagesSent * 0.05)) / messagesSent // 95% delivery rate
    }
    
    private fun analyzeTopologyOptimization(topology: NetworkTopology): TopologyOptimization {
        return TopologyOptimization(
            recommendedChanges = listOf("Add relay node", "Optimize connection paths"),
            expectedImprovement = 15,
            estimatedTime = 30000
        )
    }
    
    private fun applyTopologyOptimizations(optimization: TopologyOptimization) {
        // Mock implementation
        Logger.i(TAG, "Applying topology optimizations: ${optimization.recommendedChanges}")
    }
    
    private fun updateMessageFlowStats() {
        // Update message flow statistics
        updateNetworkStats("last_update", System.currentTimeMillis())
    }
    
    private fun checkNetworkIssues() {
        // Check for network issues and log warnings
        val health = calculateNetworkHealth()
        if (health < 50) {
            Logger.w(TAG, "Network health is low: $health%")
        }
    }
    
    private fun identifyCriticalIssues(): List<String> {
        val issues = mutableListOf<String>()
        
        if (getConnectedDevicesCount() < 2) {
            issues.add("Low device connectivity")
        }
        
        if (calculateNetworkHealth() < 30) {
            issues.add("Poor network health")
        }
        
        if (calculateAverageLatency() > 200) {
            issues.add("High network latency")
        }
        
        return issues
    }
    
    private fun generateRecommendations(): List<String> {
        val recommendations = mutableListOf<String>()
        
        if (getConnectedDevicesCount() < 3) {
            recommendations.add("Add more relay devices to improve coverage")
        }
        
        if (calculateAverageLatency() > 150) {
            recommendations.add("Optimize connection paths to reduce latency")
        }
        
        recommendations.add("Regular network health monitoring recommended")
        
        return recommendations
    }
    
    private fun isNodeOnline(nodeId: String): Boolean {
        // Mock implementation
        return true
    }
    
    private fun calculateNetworkDiameter(topology: NetworkTopology): Int {
        // Mock implementation - maximum hops between any two nodes
        return 3
    }
    
    // Data classes for network analysis
    
    data class TopologyOptimization(
        val recommendedChanges: List<String>,
        val expectedImprovement: Int,
        val estimatedTime: Long
    )
    
    data class NetworkHealthReport(
        val overallHealth: Int,
        val connectedNodes: Int,
        val averageLatency: Long,
        val messageDeliveryRate: Double,
        val networkCoverage: Double,
        val criticalIssues: List<String>,
        val recommendations: List<String>
    )
    
    data class NetworkVisualizationData(
        val nodes: List<VisualizationNode>,
        val edges: List<VisualizationEdge>,
        val metadata: Map<String, Any>
    )
    
    data class VisualizationNode(
        val id: String,
        val name: String,
        val type: String,
        val x: Double,
        val y: Double,
        val status: String,
        val batteryLevel: Int,
        val signalStrength: Int
    )
    
    data class VisualizationEdge(
        val from: String,
        val to: String,
        val type: String,
        val strength: Int,
        val active: Boolean
    )
}