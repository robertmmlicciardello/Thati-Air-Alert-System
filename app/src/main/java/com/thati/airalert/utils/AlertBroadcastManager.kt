package com.thati.airalert.utils

import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.thati.airalert.models.AlertMessage
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Alert Broadcast Manager - Admin နှင့် User အကြား alert communication
 */
object AlertBroadcastManager {
    
    const val ACTION_NEW_ALERT = "com.thati.airalert.NEW_ALERT"
    const val EXTRA_ALERT_MESSAGE = "alert_message"
    const val EXTRA_ALERT_TYPE = "alert_type"
    const val EXTRA_ALERT_PRIORITY = "alert_priority"
    const val EXTRA_ALERT_REGION = "alert_region"
    const val EXTRA_ALERT_TIMESTAMP = "alert_timestamp"
    
    private val _alertFlow = MutableSharedFlow<AlertMessage>(replay = 10)
    val alertFlow: SharedFlow<AlertMessage> = _alertFlow.asSharedFlow()
    
    private val alertHistory = mutableListOf<AlertMessage>()
    
    /**
     * Admin မှ alert ပို့ခြင်း
     */
    fun sendAlert(
        context: Context,
        message: String,
        type: String,
        priority: String,
        region: String = "All Regions"
    ) {
        val alertMessage = AlertMessage(
            id = "alert_${System.currentTimeMillis()}",
            message = message,
            timestamp = System.currentTimeMillis(),
            sender = "Regional Admin",
            type = type,
            priority = priority,
            location = region
        )
        
        // Add to history
        alertHistory.add(0, alertMessage)
        if (alertHistory.size > 50) {
            alertHistory.removeAt(alertHistory.size - 1)
        }
        
        // Emit to flow
        _alertFlow.tryEmit(alertMessage)
        
        // Send local broadcast
        val intent = Intent(ACTION_NEW_ALERT).apply {
            putExtra(EXTRA_ALERT_MESSAGE, message)
            putExtra(EXTRA_ALERT_TYPE, type)
            putExtra(EXTRA_ALERT_PRIORITY, priority)
            putExtra(EXTRA_ALERT_REGION, region)
            putExtra(EXTRA_ALERT_TIMESTAMP, System.currentTimeMillis())
        }
        
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
        
        // Show notification
        try {
            val notificationManager = SimpleNotificationManager(context)
            notificationManager.showAlert(alertMessage)
        } catch (e: Exception) {
            // Handle notification error
        }
    }
    
    /**
     * Alert history ရယူခြင်း
     */
    fun getAlertHistory(): List<AlertMessage> {
        return alertHistory.toList()
    }
    
    /**
     * Alert history ရှင်းလင်းခြင်း
     */
    fun clearAlertHistory() {
        alertHistory.clear()
    }
    
    /**
     * Simulate receiving alert from mesh network
     */
    fun simulateIncomingAlert(context: Context) {
        val sampleMessages = listOf(
            "လေယာဉ် သတိပေးချက် - မြောက်ဘက်မှ ချဉ်းကပ်လာနေ",
            "အရေးပေါ် - ချက်ချင်း ရွှေ့ပြောင်းပါ",
            "ဘေးကင်းပြီ - ပုံမှန်အခြေအနေ ပြန်လည်ရောက်ရှိ",
            "စမ်းသပ်ချက် - Alert စနစ် စစ်ဆေးမှု"
        )
        
        val types = listOf("လေယာဉ်", "အရေးပေါ်", "ဘေးကင်းပြီ", "စမ်းသပ်ချက်")
        val priorities = listOf("အရေးကြီး", "မြင့်", "အလယ်အလတ်", "နိမ့်")
        
        sendAlert(
            context = context,
            message = sampleMessages.random(),
            type = types.random(),
            priority = priorities.random(),
            region = "Mesh Network"
        )
    }
}

enum class AlertPriority {
    CRITICAL, HIGH, MEDIUM, LOW
}

enum class AlertType {
    AIRCRAFT, ATTACK, EVACUATION, ALL_CLEAR, TEST, EMERGENCY
}