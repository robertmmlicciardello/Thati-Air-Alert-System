package com.thati.airalert.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Alert Message ဒေတာ မော်ဒယ်
 * Wi-Fi Direct နှင့် BLE မှတစ်ဆင့် ပို့ဆောင်မည့် သတိပေးချက် စာသားများ
 */
@Serializable
@Entity(tableName = "alert_messages")
data class AlertMessage(
    @PrimaryKey
    val id: String, // Unique identifier (UUID)
    val message: String, // သတိပေးချက် စာသား
    val timestamp: Long, // Unix timestamp (milliseconds)
    val senderDeviceId: String, // ပို့သူ device ၏ ID
    val priority: AlertPriority = AlertPriority.HIGH, // ဦးစားပေးမှု အဆင့်
    val alertType: AlertType = AlertType.AIR_RAID, // သတိပေးချက် အမျိုးအစား
    val hopCount: Int = 0, // Mesh network မှာ ဘယ်နှစ်ဆင့် ကူးလာပြီးလဲ
    val maxHops: Int = 5, // အများဆုံး ဘယ်နှစ်ဆင့် ကူးခွင့်ပြုမလဲ
    val isReceived: Boolean = false // လက်ခံရရှိပြီးလား
)

/**
 * သတိပေးချက် ဦးစားပေးမှု အဆင့်များ
 */
@Serializable
enum class AlertPriority(val level: Int) {
    LOW(1),
    MEDIUM(2), 
    HIGH(3),
    CRITICAL(4)
}

/**
 * သတိပေးချက် အမျိုးအစားများ
 */
@Serializable
enum class AlertType {
    AIR_RAID,        // လေကြောင်းတိုက်ခိုက်မှု
    EMERGENCY,       // အရေးပေါ်အခြေအနေ
    EVACUATION,      // ရွှေ့ပြောင်းရန်
    ALL_CLEAR,       // ဘေးကင်းပြီ
    TEST             // စမ်းသပ်ချက်
}

/**
 * Network မှတစ်ဆင့် ပို့ဆောင်ရန်အတွက် AlertMessage ကို JSON string အဖြစ် ပြောင်းလဲခြင်း
 */
fun AlertMessage.toNetworkPacket(): String {
    return kotlinx.serialization.json.Json.encodeToString(AlertMessage.serializer(), this)
}

/**
 * JSON string မှ AlertMessage အဖြစ် ပြန်လည်ပြောင်းလဲခြင်း
 */
fun String.toAlertMessage(): AlertMessage? {
    return try {
        kotlinx.serialization.json.Json.decodeFromString(AlertMessage.serializer(), this)
    } catch (e: Exception) {
        null
    }
}