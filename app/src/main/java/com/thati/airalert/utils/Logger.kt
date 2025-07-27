package com.thati.airalert.utils

import android.util.Log
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 * Enhanced Logger
 * Provides structured logging with file output and different log levels
 */
object Logger {
    
    private const val TAG = "ThatiAlert"
    private var logToFile = false
    private var logFile: File? = null
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    
    enum class LogLevel {
        VERBOSE, DEBUG, INFO, WARN, ERROR
    }
    
    /**
     * Initialize logger with file logging option
     */
    fun init(logDirectory: File? = null, enableFileLogging: Boolean = false) {
        logToFile = enableFileLogging
        if (logToFile && logDirectory != null) {
            logFile = File(logDirectory, "thati_alert_${getCurrentDate()}.log")
            logFile?.parentFile?.mkdirs()
        }
    }
    
    /**
     * Verbose logging
     */
    fun v(message: String, tag: String = TAG, throwable: Throwable? = null) {
        log(LogLevel.VERBOSE, tag, message, throwable)
    }
    
    /**
     * Debug logging
     */
    fun d(message: String, tag: String = TAG, throwable: Throwable? = null) {
        log(LogLevel.DEBUG, tag, message, throwable)
    }
    
    /**
     * Info logging
     */
    fun i(message: String, tag: String = TAG, throwable: Throwable? = null) {
        log(LogLevel.INFO, tag, message, throwable)
    }
    
    /**
     * Warning logging
     */
    fun w(message: String, tag: String = TAG, throwable: Throwable? = null) {
        log(LogLevel.WARN, tag, message, throwable)
    }
    
    /**
     * Error logging
     */
    fun e(message: String, tag: String = TAG, throwable: Throwable? = null) {
        log(LogLevel.ERROR, tag, message, throwable)
    }
    
    /**
     * Log network events
     */
    fun logNetworkEvent(event: String, details: Map<String, Any> = emptyMap()) {
        val message = "NETWORK_EVENT: $event ${formatDetails(details)}"
        i(message, "Network")
    }
    
    /**
     * Log mesh network events
     */
    fun logMeshEvent(event: String, deviceId: String? = null, details: Map<String, Any> = emptyMap()) {
        val message = "MESH_EVENT: $event ${deviceId?.let { "Device: $it" } ?: ""} ${formatDetails(details)}"
        i(message, "Mesh")
    }
    
    /**
     * Log alert events
     */
    fun logAlertEvent(event: String, alertId: String? = null, details: Map<String, Any> = emptyMap()) {
        val message = "ALERT_EVENT: $event ${alertId?.let { "Alert: $it" } ?: ""} ${formatDetails(details)}"
        i(message, "Alert")
    }
    
    /**
     * Log security events
     */
    fun logSecurityEvent(event: String, details: Map<String, Any> = emptyMap()) {
        val message = "SECURITY_EVENT: $event ${formatDetails(details)}"
        w(message, "Security")
    }
    
    /**
     * Log performance metrics
     */
    fun logPerformance(operation: String, duration: Long, details: Map<String, Any> = emptyMap()) {
        val message = "PERFORMANCE: $operation took ${duration}ms ${formatDetails(details)}"
        d(message, "Performance")
    }
    
    /**
     * Log user actions
     */
    fun logUserAction(action: String, userId: String? = null, details: Map<String, Any> = emptyMap()) {
        val message = "USER_ACTION: $action ${userId?.let { "User: $it" } ?: ""} ${formatDetails(details)}"
        i(message, "UserAction")
    }
    
    /**
     * Core logging function
     */
    private fun log(level: LogLevel, tag: String, message: String, throwable: Throwable? = null) {
        val timestamp = dateFormat.format(Date())
        val logMessage = "[$timestamp] [$level] [$tag] $message"
        
        // Log to Android Log
        when (level) {
            LogLevel.VERBOSE -> Log.v(tag, message, throwable)
            LogLevel.DEBUG -> Log.d(tag, message, throwable)
            LogLevel.INFO -> Log.i(tag, message, throwable)
            LogLevel.WARN -> Log.w(tag, message, throwable)
            LogLevel.ERROR -> Log.e(tag, message, throwable)
        }
        
        // Log to file if enabled
        if (logToFile) {
            writeToFile(logMessage, throwable)
        }
    }
    
    /**
     * Write log to file
     */
    private fun writeToFile(message: String, throwable: Throwable? = null) {
        try {
            logFile?.let { file ->
                FileWriter(file, true).use { writer ->
                    writer.appendLine(message)
                    throwable?.let { 
                        writer.appendLine("Exception: ${it.message}")
                        writer.appendLine("Stack trace: ${it.stackTraceToString()}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write to log file", e)
        }
    }
    
    /**
     * Format details map for logging
     */
    private fun formatDetails(details: Map<String, Any>): String {
        if (details.isEmpty()) return ""
        return details.entries.joinToString(", ", "[", "]") { "${it.key}=${it.value}" }
    }
    
    /**
     * Get current date for log file naming
     */
    private fun getCurrentDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }
    
    /**
     * Clear old log files (keep last 7 days)
     */
    fun clearOldLogs() {
        try {
            logFile?.parentFile?.listFiles()?.forEach { file ->
                if (file.name.startsWith("thati_alert_") && file.name.endsWith(".log")) {
                    val fileDate = file.name.substring(12, 22) // Extract date from filename
                    val calendar = Calendar.getInstance()
                    calendar.add(Calendar.DAY_OF_YEAR, -7)
                    val cutoffDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
                    
                    if (fileDate < cutoffDate) {
                        file.delete()
                        Log.d(TAG, "Deleted old log file: ${file.name}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear old logs", e)
        }
    }
    
    /**
     * Get log file size in MB
     */
    fun getLogFileSize(): Double {
        return try {
            logFile?.length()?.div(1024.0 * 1024.0) ?: 0.0
        } catch (e: Exception) {
            0.0
        }
    }
    
    /**
     * Export logs for debugging
     */
    fun exportLogs(): String? {
        return try {
            logFile?.readText()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export logs", e)
            null
        }
    }
}