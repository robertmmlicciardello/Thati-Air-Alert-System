package com.thati.airalert.services

import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.*

/**
 * Battery Optimization Service
 * ဘက်ထရီ အသုံးပြုမှု ကို ထိန်းချုပ်ပြီး အကောင်းဆုံး စွမ်းအင် သုံးစွဲမှု ရရှိရန်
 */
class BatteryOptimizationService : Service() {
    
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var batteryLevel = 100
    private var isCharging = false
    private var batteryImpactLevel = "နည်း" // နည်း, အလယ်အလတ်, များ
    
    companion object {
        private const val TAG = "BatteryOptimization"
        const val ACTION_BATTERY_STATUS = "com.thati.airalert.BATTERY_STATUS"
        const val EXTRA_BATTERY_LEVEL = "battery_level"
        const val EXTRA_BATTERY_IMPACT = "battery_impact"
        const val EXTRA_IS_CHARGING = "is_charging"
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Battery Optimization Service စတင်ခဲ့သည်")
        startBatteryMonitoring()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }
    
    private fun startBatteryMonitoring() {
        serviceScope.launch {
            while (true) {
                updateBatteryStatus()
                optimizeBatteryUsage()
                broadcastBatteryStatus()
                delay(30000) // Check every 30 seconds
            }
        }
    }
    
    private fun updateBatteryStatus() {
        val batteryStatus = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        batteryStatus?.let { intent ->
            // Get battery level
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            batteryLevel = (level * 100 / scale.toFloat()).toInt()
            
            // Check if charging
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL
            
            // Calculate battery impact based on usage
            calculateBatteryImpact()
            
            Log.d(TAG, "ဘက်ထရီ အခြေအနေ: $batteryLevel%, အားသွင်းနေ: $isCharging, သက်ရောက်မှု: $batteryImpactLevel")
        }
    }
    
    private fun calculateBatteryImpact() {
        batteryImpactLevel = when {
            batteryLevel > 80 -> "နည်း"
            batteryLevel > 50 -> "အလယ်အလတ်"
            batteryLevel > 20 -> "များ"
            else -> "အလွန်များ"
        }
    }
    
    private fun optimizeBatteryUsage() {
        when (batteryImpactLevel) {
            "များ", "အလွန်များ" -> {
                // Reduce background activities
                reduceBluetooth()
                reduceWifiDirect()
                reduceLocationUpdates()
                Log.d(TAG, "ဘက်ထရီ ချွေတာမှု လျှော့ချခဲ့သည်")
            }
            "အလယ်အလတ်" -> {
                // Moderate optimization
                moderateOptimization()
                Log.d(TAG, "အလယ်အလတ် ဘက်ထရီ ပြုပြင်မှု")
            }
            else -> {
                // Normal operation
                normalOperation()
            }
        }
    }
    
    private fun reduceBluetooth() {
        // Reduce Bluetooth scanning frequency
        val intent = Intent("com.thati.airalert.REDUCE_BLUETOOTH")
        sendBroadcast(intent)
    }
    
    private fun reduceWifiDirect() {
        // Reduce WiFi Direct discovery frequency
        val intent = Intent("com.thati.airalert.REDUCE_WIFI_DIRECT")
        sendBroadcast(intent)
    }
    
    private fun reduceLocationUpdates() {
        // Reduce location update frequency
        val intent = Intent("com.thati.airalert.REDUCE_LOCATION")
        sendBroadcast(intent)
    }
    
    private fun moderateOptimization() {
        // Implement moderate battery saving measures
        val intent = Intent("com.thati.airalert.MODERATE_OPTIMIZATION")
        sendBroadcast(intent)
    }
    
    private fun normalOperation() {
        // Resume normal operation
        val intent = Intent("com.thati.airalert.NORMAL_OPERATION")
        sendBroadcast(intent)
    }
    
    private fun broadcastBatteryStatus() {
        val intent = Intent(ACTION_BATTERY_STATUS).apply {
            putExtra(EXTRA_BATTERY_LEVEL, batteryLevel)
            putExtra(EXTRA_BATTERY_IMPACT, batteryImpactLevel)
            putExtra(EXTRA_IS_CHARGING, isCharging)
        }
        sendBroadcast(intent)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        Log.d(TAG, "Battery Optimization Service ရပ်တန့်ခဲ့သည်")
    }
}