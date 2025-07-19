package com.thati.airalert.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.*

/**
 * Network Health Monitoring Service
 * ကွန်ယက် အခြေအနေ ကို စောင့်ကြည့်ပြီး relay device များ၏ ချိတ်ဆက်မှု အခြေအနေ စစ်ဆေးရန်
 */
class NetworkHealthService : Service() {
    
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private lateinit var connectivityManager: ConnectivityManager
    private var networkHealth = "ကောင်း"
    private var connectedDevices = mutableSetOf<String>()
    private var relayDevices = mutableListOf<RelayDevice>()
    
    data class RelayDevice(
        val id: String,
        val name: String,
        val type: String, // Bluetooth, WiFi Direct
        val isConnected: Boolean,
        val signalStrength: Int,
        val lastSeen: Long
    )
    
    companion object {
        private const val TAG = "NetworkHealth"
        const val ACTION_NETWORK_STATUS = "com.thati.airalert.NETWORK_STATUS"
        const val ACTION_RELAY_DEVICES = "com.thati.airalert.RELAY_DEVICES"
        const val EXTRA_NETWORK_HEALTH = "network_health"
        const val EXTRA_CONNECTED_DEVICES = "connected_devices"
        const val EXTRA_RELAY_DEVICES = "relay_devices"
    }
    
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            Log.d(TAG, "ကွန်ယက် ရရှိနိုင်သည်: $network")
            updateNetworkHealth()
        }
        
        override fun onLost(network: Network) {
            super.onLost(network)
            Log.d(TAG, "ကွန်ယက် ပျောက်ဆုံးသည်: $network")
            updateNetworkHealth()
        }
        
        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            super.onCapabilitiesChanged(network, networkCapabilities)
            updateNetworkHealth()
        }
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onCreate() {
        super.onCreate()
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        Log.d(TAG, "Network Health Service စတင်ခဲ့သည်")
        
        startNetworkMonitoring()
        startRelayDeviceMonitoring()
        initializeSampleRelayDevices()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }
    
    private fun startNetworkMonitoring() {
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
        
        // Start periodic health checks
        serviceScope.launch {
            while (true) {
                updateNetworkHealth()
                broadcastNetworkStatus()
                delay(15000) // Check every 15 seconds
            }
        }
    }
    
    private fun startRelayDeviceMonitoring() {
        serviceScope.launch {
            while (true) {
                scanForRelayDevices()
                updateRelayDeviceStatus()
                broadcastRelayDevices()
                delay(10000) // Scan every 10 seconds
            }
        }
    }
    
    private fun updateNetworkHealth() {
        val activeNetwork = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        
        networkHealth = when {
            networkCapabilities == null -> "မကောင်း"
            networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) -> {
                val signalStrength = getSignalStrength(networkCapabilities)
                when {
                    signalStrength > 70 -> "ကောင်း"
                    signalStrength > 40 -> "အလယ်အလတ်"
                    else -> "မကောင်း"
                }
            }
            else -> "မကောင်း"
        }
        
        Log.d(TAG, "ကွန်ယက် အခြေအနေ: $networkHealth")
    }
    
    private fun getSignalStrength(capabilities: NetworkCapabilities): Int {
        // Simulate signal strength calculation
        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> (60..90).random()
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> (40..80).random()
            else -> (20..50).random()
        }
    }
    
    private fun scanForRelayDevices() {
        // Simulate scanning for relay devices
        val currentTime = System.currentTimeMillis()
        
        // Update existing devices
        relayDevices.forEach { device ->
            val timeSinceLastSeen = currentTime - device.lastSeen
            val isStillConnected = timeSinceLastSeen < 30000 // 30 seconds timeout
            
            relayDevices[relayDevices.indexOf(device)] = device.copy(
                isConnected = isStillConnected,
                signalStrength = if (isStillConnected) (30..90).random() else 0,
                lastSeen = if (isStillConnected) currentTime else device.lastSeen
            )
        }
        
        // Occasionally add new devices
        if ((1..10).random() == 1) {
            addRandomRelayDevice()
        }
    }
    
    private fun addRandomRelayDevice() {
        val deviceTypes = listOf("Bluetooth", "WiFi Direct")
        val deviceNames = listOf(
            "ရဲစခန်း-၁", "ရဲစခန်း-၂", "ကျေးရွာ-A", "ကျေးရွာ-B", 
            "စောင့်ကြည့်ရေးစခန်း", "အရေးပေါ်ဌာန"
        )
        
        val newDevice = RelayDevice(
            id = "device_${System.currentTimeMillis()}",
            name = deviceNames.random(),
            type = deviceTypes.random(),
            isConnected = true,
            signalStrength = (50..90).random(),
            lastSeen = System.currentTimeMillis()
        )
        
        if (relayDevices.size < 8) { // Limit to 8 devices
            relayDevices.add(newDevice)
            Log.d(TAG, "Relay device ထပ်ထည့်ခဲ့သည်: ${newDevice.name}")
        }
    }
    
    private fun updateRelayDeviceStatus() {
        connectedDevices.clear()
        relayDevices.filter { it.isConnected }.forEach { device ->
            connectedDevices.add("${device.name} (${device.type})")
        }
        
        Log.d(TAG, "ချိတ်ဆက်ထားသော relay devices: ${connectedDevices.size}")
    }
    
    private fun initializeSampleRelayDevices() {
        relayDevices.addAll(listOf(
            RelayDevice(
                "device_1", "ရဲစခန်း-မြို့တွင်း", "Bluetooth", 
                true, 85, System.currentTimeMillis()
            ),
            RelayDevice(
                "device_2", "ကျေးရွာ-အရှေ့", "WiFi Direct", 
                true, 72, System.currentTimeMillis()
            ),
            RelayDevice(
                "device_3", "စောင့်ကြည့်ရေးစခန်း", "Bluetooth", 
                false, 0, System.currentTimeMillis() - 60000
            )
        ))
    }
    
    private fun broadcastNetworkStatus() {
        val intent = Intent(ACTION_NETWORK_STATUS).apply {
            putExtra(EXTRA_NETWORK_HEALTH, networkHealth)
            putExtra(EXTRA_CONNECTED_DEVICES, connectedDevices.size)
        }
        sendBroadcast(intent)
    }
    
    private fun broadcastRelayDevices() {
        val intent = Intent(ACTION_RELAY_DEVICES).apply {
            putStringArrayListExtra(EXTRA_RELAY_DEVICES, ArrayList(connectedDevices))
        }
        sendBroadcast(intent)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        connectivityManager.unregisterNetworkCallback(networkCallback)
        serviceScope.cancel()
        Log.d(TAG, "Network Health Service ရပ်တန့်ခဲ့သည်")
    }
}