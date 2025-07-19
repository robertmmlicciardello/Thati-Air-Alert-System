package com.thati.airalert.database

import androidx.room.*
import androidx.room.Database
import com.thati.airalert.models.AlertMessage
import com.thati.airalert.models.AlertPriority
import com.thati.airalert.models.AlertType

/**
 * Room Database configuration
 * Alert messages များကို local storage မှာ သိမ်းဆည်းရန်
 */
@Database(
    entities = [AlertMessage::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AlertDatabase : RoomDatabase() {
    abstract fun alertDao(): AlertDao
    
    companion object {
        @Volatile
        private var INSTANCE: AlertDatabase? = null
        
        fun getDatabase(context: android.content.Context): AlertDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AlertDatabase::class.java,
                    "alert_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

/**
 * Data Access Object for Alert Messages
 * Database operations များ လုပ်ဆောင်ရန်
 */
@Dao
interface AlertDao {
    
    @Query("SELECT * FROM alert_messages ORDER BY timestamp DESC")
    suspend fun getAllAlerts(): List<AlertMessage>
    
    @Query("SELECT * FROM alert_messages WHERE id = :alertId")
    suspend fun getAlertById(alertId: String): AlertMessage?
    
    @Query("SELECT * FROM alert_messages WHERE timestamp > :since ORDER BY timestamp DESC")
    suspend fun getRecentAlerts(since: Long): List<AlertMessage>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: AlertMessage)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlerts(alerts: List<AlertMessage>)
    
    @Query("DELETE FROM alert_messages WHERE id = :alertId")
    suspend fun deleteAlert(alertId: String)
    
    @Query("DELETE FROM alert_messages WHERE timestamp < :before")
    suspend fun deleteOldAlerts(before: Long)
    
    @Query("SELECT COUNT(*) FROM alert_messages WHERE id = :alertId")
    suspend fun isAlertExists(alertId: String): Int
    
    @Query("DELETE FROM alert_messages")
    suspend fun clearAllAlerts()
}

/**
 * Type converters for Room Database
 * Enum များကို database မှာ သိမ်းဆည်းရန်
 */
class Converters {
    
    @TypeConverter
    fun fromAlertPriority(priority: AlertPriority): String {
        return priority.name
    }
    
    @TypeConverter
    fun toAlertPriority(priority: String): AlertPriority {
        return AlertPriority.valueOf(priority)
    }
    
    @TypeConverter
    fun fromAlertType(type: AlertType): String {
        return type.name
    }
    
    @TypeConverter
    fun toAlertType(type: String): AlertType {
        return AlertType.valueOf(type)
    }
}