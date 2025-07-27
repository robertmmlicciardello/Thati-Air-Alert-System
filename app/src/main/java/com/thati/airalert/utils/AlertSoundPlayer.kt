package com.thati.airalert.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Alert Sound Player for Emergency Alerts
 * Handles sound, vibration, and visual alerts
 */
class AlertSoundPlayer(private val context: Context) {
    
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var alertJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)
    
    init {
        setupVibrator()
    }
    
    private fun setupVibrator() {
        vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }
    
    /**
     * Play emergency alert with sound and vibration
     */
    fun playEmergencyAlert(
        priority: String = "high",
        duration: Long = 10000, // 10 seconds
        enableSound: Boolean = true,
        enableVibration: Boolean = true
    ) {
        stopAlert() // Stop any existing alert
        
        alertJob = scope.launch {
            try {
                if (enableSound) {
                    startAlertSound(priority)
                }
                
                if (enableVibration) {
                    startAlertVibration(priority)
                }
                
                // Auto-stop after duration
                delay(duration)
                stopAlert()
                
            } catch (e: Exception) {
                Log.e("AlertSoundPlayer", "Error playing alert", e)
                stopAlert()
            }
        }
    }
    
    private fun startAlertSound(priority: String) {
        try {
            val alertUri = getAlertSoundUri(priority)
            
            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, alertUri)
                
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                
                isLooping = true
                setVolume(1.0f, 1.0f)
                
                setOnPreparedListener { player ->
                    player.start()
                    Log.d("AlertSoundPlayer", "Alert sound started")
                }
                
                setOnErrorListener { _, what, extra ->
                    Log.e("AlertSoundPlayer", "MediaPlayer error: what=$what, extra=$extra")
                    true
                }
                
                prepareAsync()
            }
            
        } catch (e: Exception) {
            Log.e("AlertSoundPlayer", "Error starting alert sound", e)
        }
    }
    
    private fun startAlertVibration(priority: String) {
        try {
            vibrator?.let { vib ->
                val pattern = getVibrationPattern(priority)
                
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    val vibrationEffect = VibrationEffect.createWaveform(pattern, 0)
                    vib.vibrate(vibrationEffect)
                } else {
                    @Suppress("DEPRECATION")
                    vib.vibrate(pattern, 0)
                }
                
                Log.d("AlertSoundPlayer", "Alert vibration started")
            }
        } catch (e: Exception) {
            Log.e("AlertSoundPlayer", "Error starting vibration", e)
        }
    }
    
    private fun getAlertSoundUri(priority: String): Uri {
        return when (priority) {
            "critical", "အရေးကြီး" -> {
                // Use alarm sound for critical alerts
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            }
            "high", "မြင့်" -> {
                // Use notification sound for high priority
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            }
            else -> {
                // Use ringtone for medium/low priority
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            }
        }
    }
    
    private fun getVibrationPattern(priority: String): LongArray {
        return when (priority) {
            "critical", "အရေးကြီး" -> {
                // Urgent pattern: short bursts
                longArrayOf(0, 200, 100, 200, 100, 200, 500, 200, 100, 200, 100, 200)
            }
            "high", "မြင့်" -> {
                // High priority pattern: medium bursts
                longArrayOf(0, 300, 200, 300, 200, 300, 800)
            }
            else -> {
                // Normal pattern: gentle vibration
                longArrayOf(0, 500, 300, 500, 300, 500)
            }
        }
    }
    
    /**
     * Stop all alert sounds and vibrations
     */
    fun stopAlert() {
        try {
            alertJob?.cancel()
            
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.stop()
                }
                player.release()
                mediaPlayer = null
                Log.d("AlertSoundPlayer", "Alert sound stopped")
            }
            
            vibrator?.cancel()
            Log.d("AlertSoundPlayer", "Alert vibration stopped")
            
        } catch (e: Exception) {
            Log.e("AlertSoundPlayer", "Error stopping alert", e)
        }
    }
    
    /**
     * Test alert sound
     */
    fun testAlert(priority: String = "medium") {
        playEmergencyAlert(
            priority = priority,
            duration = 3000, // 3 seconds for test
            enableSound = true,
            enableVibration = true
        )
    }
    
    /**
     * Check if alert is currently playing
     */
    fun isPlaying(): Boolean {
        return mediaPlayer?.isPlaying == true
    }
    
    /**
     * Cleanup resources
     */
    fun release() {
        stopAlert()
    }
}