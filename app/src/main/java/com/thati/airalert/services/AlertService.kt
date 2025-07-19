package com.thati.airalert.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.thati.airalert.R
import com.thati.airalert.database.AlertDatabase
import com.thati.airalert.models.AlertMessage
import com.thati.airalert.models.toAlertMessage
import com.thati.airalert.models.toNetworkPacket
import com.thati.airalert.utils.AlarmPlayer
import kotlinx.coroutines.*
import java.util.*

/**
 * Alert Service
 * Background မှာ အလုပ်လုပ်ပြီး alert messages များ လက်ခံ/ပို့ဆောင်ရန်
 */
class AlertService : Service() {
    
    private lateinit var wifiDirectManager: WifiDirectManager
    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var alarmPlayer: AlarmPlayer
    private lateinit var database: AlertDatabase
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private var isAdminMode = false
    private val processedAlerts = mutableSetOf<String>() // Duplicate alerts ကို ရှောင်ရန်
    
    companion object {
        private const val TAG = "AlertService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "ThatiAlertChannel"
        
        const val ACTION_START_ADMIN = "START_ADMIN"
        const val ACTION_START_USER = "START_USER"
        const val ACTION_SEND_ALERT = "SEND_ALERT"
        const val ACTION_STOP_SERVICE = "STOP_SERVICE"
        
        const val EXTRA_ALERT_MESSAGE = "alert_message"
        const val EXTRA_IS_ADMIN = "is_admin"
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "AlertService created")
        
        // Database ကို initialize လုပ်ပါ
        database = AlertDatabase.getDatabase(this)
        
        // Alarm player ကို initialize လုပ်ပါ
        alarmPlayer = AlarmPlayer(this)
        
        // Network managers များကို initialize လုပ်ပါ
        initializeNetworkManagers()
        
        // Notification channel ကို ဖန်တီးပါ
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "AlertService started with action: ${intent?.action}")
        
        when (intent?.action) {
            ACTION_START_ADMIN -> {
                startAdminMode()
            }
            ACTION_START_USER -> {
                startUserMode()
            }
            ACTION_SEND_ALERT -> {
                val message = intent.getStringExtra(EXTRA_ALERT_MESSAGE)
                message?.let { sendAlert(it) }
            }
            ACTION_STOP_SERVICE -> {
                stopSelf()
            }
        }
        
        return START_STICKY // Service ကို system က restart လုပ်စေရန်
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    /**
     * Network managers များကို initialize လုပ်ခြင်း
     */
    private fun initializeNetworkManagers() {
        // Wi-Fi Direct Manager
        wifiDirectManager = WifiDirectManager(this) { message ->
            handleReceivedMessage(message)
        }
        
        // Bluetooth Manager
        bluetoothManager = BluetoothManager(this) { message ->
            handleReceivedMessage(message)
        }
        
        // Initialize လုပ်ပါ
        if (!wifiDirectManager.initialize()) {
            Log.w(TAG, "Wi-Fi Direct initialization failed")
        }
        
        if (!bluetoothManager.initialize()) {
            Log.w(TAG, "Bluetooth initialization failed")
        }
    }
    
    /**
     * Admin mode ကို စတင်ခြင်း
     */
    private fun startAdminMode() {
        isAdminMode = true
        Log.d(TAG, "Starting Admin Mode")
        
        // Foreground service အဖြစ် run ပါ
        startForeground(NOTIFICATION_ID, createNotification("Admin Mode - Ready to send alerts"))
        
        // Wi-Fi Direct group ဖန်တီးပါ
        wifiDirectManager.createGroup()
        
        // BLE advertising စတင်ပါ
        bluetoothManager.startAdvertising()
        
        // Discovery စတင်ပါ
        wifiDirectManager.startDiscovery()
    }
    
    /**
     * User mode ကို စတင်ခြင်း
     */
    private fun startUserMode() {
        isAdminMode = false
        Log.d(TAG, "Starting User Mode")
        
        // Foreground service အဖြစ် run ပါ
        startForeground(NOTIFICATION_ID, createNotification("User Mode - Listening for alerts"))
        
        // Wi-Fi Direct discovery စတင်ပါ
        wifiDirectManager.startDiscovery()
        
        // BLE scanning စတင်ပါ
        bluetoothManager.startScanning()
    }
    
    /**
     * Alert ပို့ခြင်း (Admin mode မှသာ)
     */
    private fun sendAlert(message: String) {
        if (!isAdminMode) {
            Log.w(TAG, "Cannot send alert - not in admin mode")
            return
        }
        
        serviceScope.launch {
            try {
                // Alert message object ဖန်တီးပါ
                val alertMessage = AlertMessage(
                    id = UUID.randomUUID().toString(),
                    message = message,
                    timestamp = System.currentTimeMillis(),
                    senderDeviceId = getUniqueDeviceId(),
                    hopCount = 0
                )
                
                // Database မှာ သိမ်းဆည်းပါ
                database.alertDao().insertAlert(alertMessage)
                
                // Network packet အဖြစ် ပြောင်းပါ
                val packet = alertMessage.toNetworkPacket()
                
                // Wi-Fi Direct မှတစ်ဆင့် broadcast လုပ်ပါ
                wifiDirectManager.broadcastMessage(packet)
                
                // BLE မှတစ်ဆင့်လည်း ပို့ပါ (nearby devices များကို)
                // Note: BLE broadcast implementation ကို ပိုမို develop လုပ်ရမည်
                
                Log.d(TAG, "Alert sent: $message")
                
                // Notification ကို update လုပ်ပါ
                updateNotification("Alert sent: $message")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error sending alert", e)
            }
        }
    }
    
    /**
     * လက်ခံရရှိသော message ကို handle လုပ်ခြင်း
     */
    private fun handleReceivedMessage(message: String) {
        serviceScope.launch {
            try {
                // JSON string မှ AlertMessage အဖြစ် ပြောင်းပါ
                val alertMessage = message.toAlertMessage()
                if (alertMessage == null) {
                    Log.w(TAG, "Invalid alert message format")
                    return@launch
                }
                
                // Duplicate check လုပ်ပါ
                if (processedAlerts.contains(alertMessage.id)) {
                    Log.d(TAG, "Duplicate alert ignored: ${alertMessage.id}")
                    return@launch
                }
                
                // Database မှာ ရှိပြီးလား စစ်ပါ
                val existingAlert = database.alertDao().getAlertById(alertMessage.id)
                if (existingAlert != null) {
                    Log.d(TAG, "Alert already exists in database: ${alertMessage.id}")
                    return@launch
                }
                
                // Alert ကို process လုပ်ပါ
                processAlert(alertMessage)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error handling received message", e)
            }
        }
    }
    
    /**
     * Alert ကို process လုပ်ခြင်း
     */
    private suspend fun processAlert(alertMessage: AlertMessage) {
        try {
            // Processed list မှာ ထည့်ပါ
            processedAlerts.add(alertMessage.id)
            
            // Database မှာ သိမ်းဆည်းပါ
            val receivedAlert = alertMessage.copy(isReceived = true)
            database.alertDao().insertAlert(receivedAlert)
            
            Log.d(TAG, "Processing alert: ${alertMessage.message}")
            
            // Alarm မြည်စေပါ
            withContext(Dispatchers.Main) {
                alarmPlayer.startAlarm(alertMessage.message)
            }
            
            // Notification ပြပါ
            showAlertNotification(alertMessage)
            
            // Mesh network: Alert ကို relay လုပ်ပါ (hop count စစ်ပြီး)
            if (alertMessage.hopCount < alertMessage.maxHops) {
                relayAlert(alertMessage)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing alert", e)
        }
    }
    
    /**
     * Alert ကို relay လုပ်ခြင်း (Mesh network အတွက်)
     */
    private fun relayAlert(alertMessage: AlertMessage) {
        serviceScope.launch {
            try {
                // Hop count ကို တိုးပါ
                val relayMessage = alertMessage.copy(hopCount = alertMessage.hopCount + 1)
                val packet = relayMessage.toNetworkPacket()
                
                // Wi-Fi Direct မှတစ်ဆင့် relay လုပ်ပါ
                wifiDirectManager.broadcastMessage(packet)
                
                Log.d(TAG, "Alert relayed: ${alertMessage.id} (hop ${relayMessage.hopCount})")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error relaying alert", e)
            }
        }
    }
    
    /**
     * Alert notification ပြခြင်း
     */
    private fun showAlertNotification(alertMessage: AlertMessage) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🚨 သတိပေးချက်!")
            .setContentText(alertMessage.message)
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 1000, 500, 1000))
            .build()
        
        notificationManager.notify(alertMessage.id.hashCode(), notification)
    }
    
    /**
     * Notification channel ဖန်တီးခြင်း
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Thati Alert Channel",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel for Thati air alert notifications"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 1000, 500, 1000)
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * Foreground service notification ဖန်တီးခြင်း
     */
    private fun createNotification(content: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Thati Alert Service")
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }
    
    /**
     * Notification ကို update လုပ်ခြင်း
     */
    private fun updateNotification(content: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, createNotification(content))
    }
    
    /**
     * Device ID ရယူခြင်း
     */
    private fun getUniqueDeviceId(): String {
        return android.provider.Settings.Secure.getString(
            contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        ) ?: "unknown"
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "AlertService destroyed")
        
        // Resources များကို သန့်ရှင်းပါ
        serviceScope.cancel()
        alarmPlayer.cleanup()
        wifiDirectManager.cleanup()
        bluetoothManager.cleanup()
    }
}