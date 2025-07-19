package com.thati.airalert.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
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
 * Alarm Sound Player
 * Alert လက်ခံရရှိသည့်အခါ အသံကျယ်ကျယ်ဖြင့် သတိပေး alarm မြည်စေရန်
 */
class AlarmPlayer(private val context: Context) {
    
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var alarmJob: Job? = null
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    
    companion object {
        private const val TAG = "AlarmPlayer"
        private const val ALARM_DURATION_MS = 30000L // 30 seconds
        private val VIBRATION_PATTERN = longArrayOf(0, 1000, 500, 1000, 500, 1000)
    }
    
    init {
        setupVibrator()
    }
    
    /**
     * Vibrator ကို setup လုပ်ခြင်း
     */
    private fun setupVibrator() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }
    
    /**
     * Alert alarm ကို စတင်မြည်စေခြင်း
     * @param message Alert message (log အတွက်သာ)
     */
    fun startAlarm(message: String) {
        Log.d(TAG, "Starting alarm for message: $message")
        
        // ရှိနေပြီးသား alarm ကို ရပ်ပါ
        stopAlarm()
        
        // Audio volume ကို အမြင့်ဆုံး ထားပါ
        setMaxVolume()
        
        // Alarm sound ကို စတင်ပါ
        startAlarmSound()
        
        // Vibration ကို စတင်ပါ
        startVibration()
        
        // သတ်မှတ်ထားသော အချိန်ကြာပြီးနောက် အလိုအလျောက် ရပ်စေပါ
        alarmJob = CoroutineScope(Dispatchers.Main).launch {
            delay(ALARM_DURATION_MS)
            stopAlarm()
        }
    }
    
    /**
     * Audio volume ကို အမြင့်ဆုံး ထားခြင်း
     */
    private fun setMaxVolume() {
        try {
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0)
            
            // Notification volume ကိုလည်း တိုးပါ
            val maxNotificationVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION)
            audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, maxNotificationVolume, 0)
            
            Log.d(TAG, "Volume set to maximum: $maxVolume")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting volume", e)
        }
    }
    
    /**
     * Alarm sound ကို စတင်ခြင်း
     */
    private fun startAlarmSound() {
        try {
            // Default alarm sound ကို ရယူပါ
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            
            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, alarmUri)
                
                // Audio attributes ကို သတ်မှတ်ပါ
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                
                // Loop ဖြင့် မြည်စေပါ
                isLooping = true
                
                // Volume ကို အမြင့်ဆုံး ထားပါ
                setVolume(1.0f, 1.0f)
                
                // Prepare နှင့် start
                prepareAsync()
                setOnPreparedListener { mediaPlayer ->
                    mediaPlayer.start()
                    Log.d(TAG, "Alarm sound started")
                }
                
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                    true
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error starting alarm sound", e)
            // Fallback: system notification sound သုံးပါ
            playFallbackSound()
        }
    }
    
    /**
     * Fallback sound ကို မြည်စေခြင်း (MediaPlayer fail ဖြစ်ရင်)
     */
    private fun playFallbackSound() {
        try {
            val notification = RingtoneManager.getRingtone(
                context, 
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            )
            notification?.play()
            Log.d(TAG, "Fallback notification sound played")
        } catch (e: Exception) {
            Log.e(TAG, "Error playing fallback sound", e)
        }
    }
    
    /**
     * Vibration ကို စတင်ခြင်း
     */
    private fun startVibration() {
        try {
            vibrator?.let { vib ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val vibrationEffect = VibrationEffect.createWaveform(VIBRATION_PATTERN, 0)
                    vib.vibrate(vibrationEffect)
                } else {
                    @Suppress("DEPRECATION")
                    vib.vibrate(VIBRATION_PATTERN, 0)
                }
                Log.d(TAG, "Vibration started")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting vibration", e)
        }
    }
    
    /**
     * Alarm ကို ရပ်ခြင်း
     */
    fun stopAlarm() {
        Log.d(TAG, "Stopping alarm")
        
        // Job ကို cancel လုပ်ပါ
        alarmJob?.cancel()
        alarmJob = null
        
        // MediaPlayer ကို ရပ်ပါ
        try {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.stop()
                }
                player.release()
            }
            mediaPlayer = null
            Log.d(TAG, "MediaPlayer stopped and released")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping MediaPlayer", e)
        }
        
        // Vibration ကို ရပ်ပါ
        try {
            vibrator?.cancel()
            Log.d(TAG, "Vibration stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping vibration", e)
        }
    }
    
    /**
     * Alarm မြည်နေလား စစ်ခြင်း
     */
    fun isPlaying(): Boolean {
        return mediaPlayer?.isPlaying == true
    }
    
    /**
     * Resources များကို သန့်ရှင်းခြင်း
     */
    fun cleanup() {
        stopAlarm()
    }
}