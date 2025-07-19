package com.thati.airalert.optimization

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager
import android.util.Log
import kotlinx.coroutines.*

/**
 * Power Optimizer - စွမ်းအင် ချွေတာမှု လျှော့ချရန်
 * Production အတွက် battery optimization
 */
class PowerOptimizer(private val context: Context) {
    
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private val optimizationScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    companion object {
        private const val TAG = "PowerOptimizer"
        private const val LOW_BATTERY_THRESHOLD = 20
        private const val CRITICAL_BATTERY_THRESHOLD = 10
    }
    
    /**
     * Battery Level Monitoring
     */
    fun startBatteryMonitoring(onBatteryLevelChanged: (BatteryLevel) -> Unit) {
        optimizationScope.launch {
            while (true) {
                val batteryLevel = getCurrentBatteryLevel()
                onBatteryLevelChanged(batteryLevel)
                
                when (batteryLevel.level) {
                    in 0..CRITICAL_BATTERY_THRESHOLD -> {
                        applyCriticalPowerSaving()
                    }
                    in CRITICAL_BATTERY_THRESHOLD..LOW_BATTERY_THRESHOLD -> {
                        applyAggressivePowerSaving()
                    }
                    in LOW_BATTERY_THRESHOLD..50 -> {
                        applyModeratePowerSaving()
                    }
                    else -> {
                        applyNormalOperation()
                    }
                }
                
                delay(30000) // Check every 30 seconds
            }
        }
    }
    
    /**
     * Adaptive Scanning Intervals
     */
    fun getOptimalScanInterval(batteryLevel: Int, networkActivity: NetworkActivity): Long {
        return when {
            batteryLevel <= CRITICAL_BATTERY_THRESHOLD -> {
                when (networkActivity) {
                    NetworkActivity.HIGH -> 60000L // 1 minute
                    NetworkActivity.MEDIUM -> 120000L // 2 minutes
                    NetworkActivity.LOW -> 300000L // 5 minutes
                }
            }
            batteryLevel <= LOW_BATTERY_THRESHOLD -> {
                when (networkActivity) {
                    NetworkActivity.HIGH -> 30000L // 30 seconds
                    NetworkActivity.MEDIUM -> 60000L // 1 minute
                    NetworkActivity.LOW -> 120000L // 2 minutes
                }
            }
            else -> {
                when (networkActivity) {
                    NetworkActivity.HIGH -> 15000L // 15 seconds
                    NetworkActivity.MEDIUM -> 30000L // 30 seconds
                    NetworkActivity.LOW -> 60000L // 1 minute
                }
            }
        }
    }
    
    /**
     * Smart Wake Lock Management
     */
    fun acquireSmartWakeLock(duration: Long): PowerManager.WakeLock? {
        return try {
            val wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "ThatiAlert::SmartWakeLock"
            )
            wakeLock.acquire(duration)
            Log.d(TAG, "Smart wake lock acquired for ${duration}ms")
            wakeLock
        } catch (e: Exception) {
            Log.e(TAG, "Failed to acquire wake lock: ${e.message}")
            null
        }
    }
    
    /**
     * Background Task Optimization
     */
    fun optimizeBackgroundTasks(batteryLevel: Int): BackgroundTaskConfig {
        return when {
            batteryLevel <= CRITICAL_BATTERY_THRESHOLD -> BackgroundTaskConfig(
                enableBluetoothScanning = false,
                enableWifiDirectScanning = true, // Keep for emergency
                scanInterval = 300000L, // 5 minutes
                enableLocationUpdates = false,
                enableNetworkMonitoring = true
            )
            batteryLevel <= LOW_BATTERY_THRESHOLD -> BackgroundTaskConfig(
                enableBluetoothScanning = true,
                enableWifiDirectScanning = true,
                scanInterval = 120000L, // 2 minutes
                enableLocationUpdates = false,
                enableNetworkMonitoring = true
            )
            else -> BackgroundTaskConfig(
                enableBluetoothScanning = true,
                enableWifiDirectScanning = true,
                scanInterval = 30000L, // 30 seconds
                enableLocationUpdates = true,
                enableNetworkMonitoring = true
            )
        }
    }
    
    private fun getCurrentBatteryLevel(): BatteryLevel {
        val batteryStatus = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        
        return batteryStatus?.let { intent ->
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val temperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)
            
            val batteryPct = (level * 100 / scale.toFloat()).toInt()
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                           status == BatteryManager.BATTERY_STATUS_FULL
            
            BatteryLevel(
                level = batteryPct,
                isCharging = isCharging,
                temperature = temperature / 10.0f, // Convert to Celsius
                status = getBatteryStatus(status)
            )
        } ?: BatteryLevel(100, false, 25.0f, "Unknown")
    }
    
    private fun getBatteryStatus(status: Int): String {
        return when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
            BatteryManager.BATTERY_STATUS_FULL -> "Full"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not Charging"
            else -> "Unknown"
        }
    }
    
    private fun applyCriticalPowerSaving() {
        Log.w(TAG, "Applying critical power saving mode")
        // Reduce all non-essential operations
        // Keep only emergency alert functionality
    }
    
    private fun applyAggressivePowerSaving() {
        Log.i(TAG, "Applying aggressive power saving mode")
        // Reduce scanning frequency
        // Disable location updates
        // Minimize background operations
    }
    
    private fun applyModeratePowerSaving() {
        Log.i(TAG, "Applying moderate power saving mode")
        // Slightly reduce scanning frequency
        // Optimize network operations
    }
    
    private fun applyNormalOperation() {
        Log.d(TAG, "Normal operation mode")
        // Full functionality enabled
    }
    
    fun stopOptimization() {
        optimizationScope.cancel()
        Log.d(TAG, "Power optimization stopped")
    }
}

/**
 * Data Classes for Power Optimization
 */
data class BatteryLevel(
    val level: Int,
    val isCharging: Boolean,
    val temperature: Float,
    val status: String
)

data class BackgroundTaskConfig(
    val enableBluetoothScanning: Boolean,
    val enableWifiDirectScanning: Boolean,
    val scanInterval: Long,
    val enableLocationUpdates: Boolean,
    val enableNetworkMonitoring: Boolean
)

enum class NetworkActivity {
    LOW, MEDIUM, HIGH
}