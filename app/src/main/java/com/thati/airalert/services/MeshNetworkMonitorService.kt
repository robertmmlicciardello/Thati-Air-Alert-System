package com.thati.airalert.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.thati.airalert.R
import com.thati.airalert.network.EnhancedMeshNetworkManager
import com.thati.airalert.utils.Logger
import com.thati.airalert.utils.PreferenceManager
import kotlinx.coroutines.*
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

/**
 * Mesh Network Monitor Service - Real-time mesh network monitoring နဲ့ analytics
 */
class MeshNetworkMonitorService : Service() {
    
    private lateinit var meshNetworkManager: EnhancedMeshNetworkManager
    private lateinit var preferenceManager: PreferenceManager
    
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isMonitoring = false
    
    // Network metrics
    private val networkMetrics = ConcurrentHashMap<String, Any>()
    private val performanceHistory = mutableListOf<NetworkPerformanceSnapshot>()
    private val alertHistory = mutableListOf<NetworkAlert>()
    
    companion object {
        private const val TAG = "MeshNetworkMonitorService"
        private const val NOTIFICATION_ID = 2002
        private const val CHANNEL_ID = "mesh_monitor_channel"
        
        const val ACTION_START_MONITORING = "start_monitoring"
        const val ACTION_STOP_MONITORING = "stop_monitoring"
        const val ACTION_GET_METRICS = "get_metrics"
        const val ACTION_OPTIMIZE_NETWORK = "optimize_network"
        
        // Monitoring intervals
        private const val HEALTH_CHECK_INTERVAL = 30000L // 30 seconds
        private const val PERFORMANCE_LOG_INTERVAL = 60000L // 1 minute
        private const val OPTIMIZATION_CHECK_INTERVAL = 300000L // 5 minutes
    }
    
    data class NetworkPerformanceSnapshot(
        val timestamp: Long,
        val connectedDevices: Int,
        val messagesSent: Int,
        val messagesReceived: Int,
        val averageLatency: Long,
        val networkHealth: Int,
        val batteryUsage: Double,
        val signalStrength: Int
    )
    
    data class NetworkAlert(
        val id: String,
        val type: String, // "performance", "connectivity", "battery", "security"
        val severity: String, // "low", "medium", "high", "critical"
        val message: String,
        val timestamp: Long,
        val resolved: Boolean = false
    )
    
    override fun onCreate() {
        super.onCreate()
        Logger.d(TAG, "MeshNetworkMonitorService created")
        createNotificationChannel()
        initializeComponents()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_MONITORING -> startMonitoring()
            ACTION_STOP_MONITORING -> stopMonitoring()
            ACTION_GET_METRICS -> sendMetricsBroadcast()
            ACTION_OPTIMIZE_NETWORK -> optimizeNetwork()
        }
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Mesh Network Monitor",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Mesh network monitoring and analytics"
                setShowBadge(false)
                enableVibration(false)
                setSound(null, null)
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun initializeComponents() {
        preferenceManager = PreferenceManager(this)
        meshNetworkManager = EnhancedMeshNetworkManager(this)
        
        // Initialize metrics
        networkMetrics["service_started"] = System.currentTimeMillis()
        networkMetrics["monitoring_active"] = false
    }
    
    private fun startMonitoring() {
        if (isMonitoring) return
        
        Logger.i(TAG, "Starting mesh network monitoring")
        isMonitoring = true
        networkMetrics["monitoring_active"] = true
        
        startForeground(NOTIFICATION_ID, createNotification("Monitoring mesh network..."))
        
        // Start monitoring tasks
        startHealthMonitoring()
        startPerformanceLogging()
        startOptimizationChecks()
        startNetworkAnalytics()
        
        Logger.i(TAG, "Mesh network monitoring started successfully")
    }
    
    private fun stopMonitoring() {
        if (!isMonitoring) return
        
        Logger.i(TAG, "Stopping mesh network monitoring")
        isMonitoring = false
        networkMetrics["monitoring_active"] = false
        
        serviceScope.cancel()
        stopForeground(true)
        stopSelf()
    }
    
    private fun startHealthMonitoring() {
        serviceScope.launch {
            while (isMonitoring) {
                try {
                    performHealthCheck()
                    delay(HEALTH_CHECK_INTERVAL)
                } catch (e: Exception) {
                    Logger.e(TAG, "Error in health monitoring: ${e.message}")
                    delay(HEALTH_CHECK_INTERVAL * 2) // Wait longer on error
                }
            }
        }
    }
    
    private fun startPerformanceLogging() {
        serviceScope.launch {
            while (isMonitoring) {
                try {
                    logPerformanceSnapshot()
                    delay(PERFORMANCE_LOG_INTERVAL)
                } catch (e: Exception) {
                    Logger.e(TAG, "Error in performance logging: ${e.message}")
                    delay(PERFORMANCE_LOG_INTERVAL)
                }
            }
        }
    }
    
    private fun startOptimizationChecks() {
        serviceScope.launch {
            while (isMonitoring) {
                try {
                    checkForOptimizationOpportunities()
                    delay(OPTIMIZATION_CHECK_INTERVAL)
                } catch (e: Exception) {
                    Logger.e(TAG, "Error in optimization checks: ${e.message}")
                    delay(OPTIMIZATION_CHECK_INTERVAL)
                }
            }
        }
    }
    
    private fun startNetworkAnalytics() {
        serviceScope.launch {
            while (isMonitoring) {
                try {
                    analyzeNetworkPatterns()
                    cleanupOldData()
                    delay(120000) // 2 minutes
                } catch (e: Exception) {
                    Logger.e(TAG, "Error in network analytics: ${e.message}")
                    delay(120000)
                }
            }
        }
    }
    
    private suspend fun performHealthCheck() {
        val healthReport = meshNetworkManager.performHealthCheck()
        
        // Update metrics
        networkMetrics["last_health_check"] = System.currentTimeMillis()
        networkMetrics["network_health"] = healthReport.overallHealth
        networkMetrics["connected_nodes"] = healthReport.connectedNodes
        networkMetrics["average_latency"] = healthReport.averageLatency
        networkMetrics["message_delivery_rate"] = healthReport.messageDeliveryRate
        
        // Check for issues
        checkHealthIssues(healthReport)
        
        Logger.d(TAG, "Health check completed - Health: ${healthReport.overallHealth}%")
    }
    
    private fun checkHealthIssues(healthReport: EnhancedMeshNetworkManager.NetworkHealthReport) {
        // Check for critical issues
        if (healthReport.overallHealth < 30) {
            createAlert(
                type = "performance",
                severity = "critical",
                message = "Network health critically low: ${healthReport.overallHealth}%"
            )
        } else if (healthReport.overallHealth < 50) {
            createAlert(
                type = "performance",
                severity = "high",
                message = "Network health degraded: ${healthReport.overallHealth}%"
            )
        }
        
        // Check connectivity issues
        if (healthReport.connectedNodes < 2) {
            createAlert(
                type = "connectivity",
                severity = "high",
                message = "Low device connectivity: ${healthReport.connectedNodes} devices"
            )
        }
        
        // Check latency issues
        if (healthReport.averageLatency > 200) {
            createAlert(
                type = "performance",
                severity = "medium",
                message = "High network latency: ${healthReport.averageLatency}ms"
            )
        }
        
        // Check message delivery issues
        if (healthReport.messageDeliveryRate < 0.9) {
            createAlert(
                type = "performance",
                severity = "high",
                message = "Low message delivery rate: ${(healthReport.messageDeliveryRate * 100).toInt()}%"
            )
        }
    }
    
    private fun logPerformanceSnapshot() {
        val stats = meshNetworkManager.getNetworkStats()
        
        val snapshot = NetworkPerformanceSnapshot(
            timestamp = System.currentTimeMillis(),
            connectedDevices = stats["connected_devices"] as? Int ?: 0,
            messagesSent = stats["messages_sent"] as? Int ?: 0,
            messagesReceived = stats["messages_received"] as? Int ?: 0,
            averageLatency = (stats["average_latency"] as? Number)?.toLong() ?: 0L,
            networkHealth = stats["network_health"] as? Int ?: 0,
            batteryUsage = calculateBatteryUsage(),
            signalStrength = calculateAverageSignalStrength()
        )
        
        performanceHistory.add(snapshot)
        
        // Keep only last 100 snapshots
        if (performanceHistory.size > 100) {
            performanceHistory.removeAt(0)
        }
        
        // Update current metrics
        networkMetrics["current_performance"] = snapshot
        
        Logger.d(TAG, "Performance snapshot logged - Health: ${snapshot.networkHealth}%")
    }
    
    private fun checkForOptimizationOpportunities() {
        val stats = meshNetworkManager.getNetworkStats()
        val currentHealth = stats["network_health"] as? Int ?: 0
        
        // Check if optimization is needed
        if (currentHealth < 70) {
            Logger.i(TAG, "Network optimization recommended - Health: $currentHealth%")
            
            // Trigger automatic optimization if enabled
            if (preferenceManager.isAutoOptimizationEnabled()) {
                meshNetworkManager.optimizeNetworkTopology()
                
                createAlert(
                    type = "performance",
                    severity = "low",
                    message = "Network topology automatically optimized"
                )
            } else {
                createAlert(
                    type = "performance",
                    severity = "medium",
                    message = "Network optimization recommended - Health: $currentHealth%"
                )
            }
        }
    }
    
    private fun analyzeNetworkPatterns() {
        if (performanceHistory.size < 10) return
        
        // Analyze performance trends
        val recentSnapshots = performanceHistory.takeLast(10)
        val healthTrend = calculateTrend(recentSnapshots.map { it.networkHealth })
        val latencyTrend = calculateTrend(recentSnapshots.map { it.averageLatency.toInt() })
        
        networkMetrics["health_trend"] = healthTrend
        networkMetrics["latency_trend"] = latencyTrend
        
        // Detect patterns
        if (healthTrend < -5) {
            createAlert(
                type = "performance",
                severity = "medium",
                message = "Network health declining trend detected"
            )
        }
        
        if (latencyTrend > 10) {
            createAlert(
                type = "performance",
                severity = "medium",
                message = "Network latency increasing trend detected"
            )
        }
        
        Logger.d(TAG, "Network patterns analyzed - Health trend: $healthTrend, Latency trend: $latencyTrend")
    }
    
    private fun calculateTrend(values: List<Int>): Double {
        if (values.size < 2) return 0.0
        
        val n = values.size
        val sumX = (0 until n).sum()
        val sumY = values.sum()
        val sumXY = values.mapIndexed { index, value -> index * value }.sum()
        val sumX2 = (0 until n).map { it * it }.sum()
        
        return (n * sumXY - sumX * sumY).toDouble() / (n * sumX2 - sumX * sumX)
    }
    
    private fun createAlert(type: String, severity: String, message: String) {
        val alert = NetworkAlert(
            id = "alert_${System.currentTimeMillis()}",
            type = type,
            severity = severity,
            message = message,
            timestamp = System.currentTimeMillis()
        )
        
        alertHistory.add(alert)
        
        // Keep only last 50 alerts
        if (alertHistory.size > 50) {
            alertHistory.removeAt(0)
        }
        
        // Broadcast alert
        val intent = Intent("com.thati.airalert.NETWORK_ALERT").apply {
            putExtra("alert_type", type)
            putExtra("alert_severity", severity)
            putExtra("alert_message", message)
            putExtra("alert_timestamp", alert.timestamp)
        }
        sendBroadcast(intent)
        
        Logger.w(TAG, "Network alert created: [$severity] $message")
    }
    
    private fun optimizeNetwork() {
        serviceScope.launch {
            try {
                Logger.i(TAG, "Starting network optimization")
                meshNetworkManager.optimizeNetworkTopology()
                
                createAlert(
                    type = "performance",
                    severity = "low",
                    message = "Network optimization completed successfully"
                )
                
            } catch (e: Exception) {
                Logger.e(TAG, "Error during network optimization: ${e.message}")
                
                createAlert(
                    type = "performance",
                    severity = "medium",
                    message = "Network optimization failed: ${e.message}"
                )
            }
        }
    }
    
    private fun sendMetricsBroadcast() {
        val metricsJson = JSONObject().apply {
            networkMetrics.forEach { (key, value) ->
                put(key, value)
            }
            
            // Add performance history summary
            if (performanceHistory.isNotEmpty()) {
                val recent = performanceHistory.last()
                put("latest_performance", JSONObject().apply {
                    put("timestamp", recent.timestamp)
                    put("connected_devices", recent.connectedDevices)
                    put("network_health", recent.networkHealth)
                    put("average_latency", recent.averageLatency)
                    put("battery_usage", recent.batteryUsage)
                })
            }
            
            // Add alert summary
            val activeAlerts = alertHistory.filter { !it.resolved }
            put("active_alerts", activeAlerts.size)
            put("total_alerts", alertHistory.size)
        }
        
        val intent = Intent("com.thati.airalert.NETWORK_METRICS").apply {
            putExtra("metrics_json", metricsJson.toString())
        }
        sendBroadcast(intent)
    }
    
    private fun cleanupOldData() {
        val cutoffTime = System.currentTimeMillis() - (24 * 60 * 60 * 1000) // 24 hours
        
        // Clean old performance snapshots
        performanceHistory.removeAll { it.timestamp < cutoffTime }
        
        // Clean old resolved alerts
        alertHistory.removeAll { it.resolved && it.timestamp < cutoffTime }
    }
    
    private fun calculateBatteryUsage(): Double {
        // Mock implementation - in real scenario, calculate actual battery usage
        return Math.random() * 5.0 // 0-5% battery usage
    }
    
    private fun calculateAverageSignalStrength(): Int {
        // Mock implementation - in real scenario, calculate from connected devices
        return (70..90).random()
    }
    
    private fun createNotification(contentText: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Mesh Network Monitor")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }
    
    // Public methods for external access
    fun getCurrentMetrics(): Map<String, Any> = networkMetrics.toMap()
    fun getPerformanceHistory(): List<NetworkPerformanceSnapshot> = performanceHistory.toList()
    fun getActiveAlerts(): List<NetworkAlert> = alertHistory.filter { !it.resolved }
    fun getAllAlerts(): List<NetworkAlert> = alertHistory.toList()
    fun isMonitoringActive(): Boolean = isMonitoring
}