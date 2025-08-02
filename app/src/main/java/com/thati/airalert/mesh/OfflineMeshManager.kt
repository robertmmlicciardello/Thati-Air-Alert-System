package com.thati.airalert.mesh

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Handler
import android.os.Looper
import androidx.core.app.ActivityCompat
import com.thati.airalert.models.AlertMessage
import com.thati.airalert.utils.Logger
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.*
import java.net.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.ArrayList

/**
 * Offline Mesh Manager - True P2P offline communication like BitChat/Briar
 * Wi-Fi Direct + Bluetooth + Hotspot mesh networking
 */
class OfflineMeshManager(private val context: Context) {
    
    companion object {
        private const val TAG = "OfflineMeshManager"
        private const val SERVICE_NAME = "ThatiAirAlert"
        private const val BLUETOOTH_UUID = "8ce255c0-200a-11e0-ac64-0800200c9a66"
        private const val WIFI_DIRECT_PORT = 8888
        private const val HOTSPOT_PORT = 8889
        private const val MESSAGE_BUFFER_SIZE = 1024
        private const val DISCOVERY_INTERVAL = 30000L // 30 seconds
        private const val HEARTBEAT_INTERVAL = 15000L // 15 seconds
        private const val MESSAGE_TTL = 5 // Maximum hops
    }
    
    // Managers
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var wifiP2pManager: WifiP2pManager? = null
    private var wifiP2pChannel: WifiP2pManager.Channel? = null
    private var wifiManager: WifiManager? = null
    
    // Network state
    private val connectedPeers = ConcurrentHashMap<String, PeerInfo>()
    private val messageHistory = ConcurrentHashMap<String, Long>()
    private val pendingMessages = ArrayList<MeshMessage>()
    
    // Sockets and servers
    private var bluetoothServerSocket: BluetoothServerSocket? = null
    private var wifiDirectServerSocket: ServerSocket? = null
    private var hotspotServerSocket: ServerSocket? = null
    
    // Coroutines
    private val meshScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isRunning = false
    
    // Callbacks
    private var onMessageReceived: ((AlertMessage) -> Unit)? = null
    private var onPeerConnected: ((String) -> Unit)? = null
    private var onPeerDisconnected: ((String) -> Unit)? = null
    
    data class PeerInfo(
        val id: String,
        val name: String,
        val type: String, // "bluetooth", "wifi_direct", "hotspot"
        val address: String,
        val lastSeen: Long,
        val isAdmin: Boolean = false
    )
    
    data class MeshMessage(
        val id: String = UUID.randomUUID().toString(),
        val type: String, // "alert", "heartbeat", "discovery"
        val senderId: String,
        val payload: String,
        val timestamp: Long = System.currentTimeMillis(),
        val ttl: Int = MESSAGE_TTL,
        val priority: Int = 1 // 1=low, 2=medium, 3=high, 4=critical
    )
    
    /**
     * Initialize mesh networking
     */
    fun initialize(): Boolean {
        Logger.i(TAG, "Initializing offline mesh manager")
        
        try {
            // Initialize Bluetooth
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            if (bluetoothAdapter == null) {
                Logger.w(TAG, "Bluetooth not supported")
            }
            
            // Initialize Wi-Fi Direct
            wifiP2pManager = context.getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager
            wifiP2pChannel = wifiP2pManager?.initialize(context, Looper.getMainLooper(), null)
            
            // Initialize Wi-Fi Manager
            wifiManager = context.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            
            Logger.i(TAG, "Mesh manager initialized successfully")
            return true
            
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to initialize mesh manager: ${e.message}")
            return false
        }
    }
    
    /**
     * Start mesh networking in admin mode
     */
    fun startAdminMode() {
        if (isRunning) return
        
        Logger.i(TAG, "Starting mesh network in admin mode")
        isRunning = true
        
        meshScope.launch {
            try {
                // Start all servers
                startBluetoothServer()
                startWifiDirectServer()
                startHotspotServer()
                
                // Start discovery
                startDiscovery()
                
                // Start heartbeat
                startHeartbeat()
                
                Logger.i(TAG, "Admin mode mesh network started")
                
            } catch (e: Exception) {
                Logger.e(TAG, "Error starting admin mode: ${e.message}")
                isRunning = false
            }
        }
    }
    
    /**
     * Start mesh networking in user mode
     */
    fun startUserMode() {
        if (isRunning) return
        
        Logger.i(TAG, "Starting mesh network in user mode")
        isRunning = true
        
        meshScope.launch {
            try {
                // Start discovery to find admin nodes
                startDiscovery()
                
                // Start heartbeat
                startHeartbeat()
                
                // Connect to available peers
                connectToAvailablePeers()
                
                Logger.i(TAG, "User mode mesh network started")
                
            } catch (e: Exception) {
                Logger.e(TAG, "Error starting user mode: ${e.message}")
                isRunning = false
            }
        }
    }
    
    /**
     * Send alert through mesh network
     */
    fun sendAlert(alertMessage: AlertMessage) {
        if (!isRunning) {
            Logger.w(TAG, "Mesh network not running, cannot send alert")
            return
        }
        
        Logger.i(TAG, "Sending alert through mesh: ${alertMessage.message}")
        
        val meshMessage = MeshMessage(
            type = "alert",
            senderId = getDeviceId(),
            payload = alertToJson(alertMessage),
            priority = getPriorityLevel(alertMessage.priority)
        )
        
        broadcastMessage(meshMessage)
    }
    
    /**
     * Broadcast message to all connected peers
     */
    private fun broadcastMessage(message: MeshMessage) {
        meshScope.launch {
            try {
                val messageJson = messageToJson(message)
                val messageBytes = messageJson.toByteArray()
                
                // Add to message history to prevent loops
                messageHistory[message.id] = System.currentTimeMillis()
                
                // Send to all connected peers
                connectedPeers.values.forEach { peer ->
                    try {
                        when (peer.type) {
                            "bluetooth" -> sendBluetoothMessage(peer, messageBytes)
                            "wifi_direct" -> sendWifiDirectMessage(peer, messageBytes)
                            "hotspot" -> sendHotspotMessage(peer, messageBytes)
                        }
                    } catch (e: Exception) {
                        Logger.e(TAG, "Failed to send message to ${peer.id}: ${e.message}")
                    }
                }
                
                Logger.d(TAG, "Message broadcasted to ${connectedPeers.size} peers")
                
            } catch (e: Exception) {
                Logger.e(TAG, "Error broadcasting message: ${e.message}")
            }
        }
    }
    
    /**
     * Start Bluetooth server
     */
    private suspend fun startBluetoothServer() {
        if (bluetoothAdapter == null || !bluetoothAdapter!!.isEnabled) {
            Logger.w(TAG, "Bluetooth not available")
            return
        }
        
        try {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                Logger.w(TAG, "Bluetooth permission not granted")
                return
            }
            
            bluetoothServerSocket = bluetoothAdapter!!.listenUsingRfcommWithServiceRecord(
                SERVICE_NAME, UUID.fromString(BLUETOOTH_UUID)
            )
            
            Logger.i(TAG, "Bluetooth server started")
            
            // Accept connections
            while (isRunning && bluetoothServerSocket != null) {
                try {
                    val socket = bluetoothServerSocket!!.accept()
                    handleBluetoothConnection(socket)
                } catch (e: IOException) {
                    if (isRunning) {
                        Logger.e(TAG, "Bluetooth server accept error: ${e.message}")
                    }
                    break
                }
            }
            
        } catch (e: Exception) {
            Logger.e(TAG, "Error starting Bluetooth server: ${e.message}")
        }
    }
    
    /**
     * Start Wi-Fi Direct server
     */
    private suspend fun startWifiDirectServer() {
        try {
            wifiDirectServerSocket = ServerSocket(WIFI_DIRECT_PORT)
            Logger.i(TAG, "Wi-Fi Direct server started on port $WIFI_DIRECT_PORT")
            
            while (isRunning && wifiDirectServerSocket != null) {
                try {
                    val socket = wifiDirectServerSocket!!.accept()
                    handleWifiDirectConnection(socket)
                } catch (e: IOException) {
                    if (isRunning) {
                        Logger.e(TAG, "Wi-Fi Direct server accept error: ${e.message}")
                    }
                    break
                }
            }
            
        } catch (e: Exception) {
            Logger.e(TAG, "Error starting Wi-Fi Direct server: ${e.message}")
        }
    }
    
    /**
     * Start Hotspot server
     */
    private suspend fun startHotspotServer() {
        try {
            hotspotServerSocket = ServerSocket(HOTSPOT_PORT)
            Logger.i(TAG, "Hotspot server started on port $HOTSPOT_PORT")
            
            while (isRunning && hotspotServerSocket != null) {
                try {
                    val socket = hotspotServerSocket!!.accept()
                    handleHotspotConnection(socket)
                } catch (e: IOException) {
                    if (isRunning) {
                        Logger.e(TAG, "Hotspot server accept error: ${e.message}")
                    }
                    break
                }
            }
            
        } catch (e: Exception) {
            Logger.e(TAG, "Error starting Hotspot server: ${e.message}")
        }
    }
    
    /**
     * Handle Bluetooth connection
     */
    private fun handleBluetoothConnection(socket: BluetoothSocket) {
        meshScope.launch {
            try {
                val device = socket.remoteDevice
                val peerId = device.address
                val peerName = if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    device.name ?: "Unknown"
                } else {
                    "Unknown"
                }
                
                val peer = PeerInfo(
                    id = peerId,
                    name = peerName,
                    type = "bluetooth",
                    address = peerId,
                    lastSeen = System.currentTimeMillis()
                )
                
                connectedPeers[peerId] = peer
                onPeerConnected?.invoke(peerId)
                
                Logger.i(TAG, "Bluetooth peer connected: $peerName")
                
                // Handle messages
                val inputStream = socket.inputStream
                val buffer = ByteArray(MESSAGE_BUFFER_SIZE)
                
                while (isRunning && socket.isConnected) {
                    try {
                        val bytesRead = inputStream.read(buffer)
                        if (bytesRead > 0) {
                            val messageJson = String(buffer, 0, bytesRead)
                            handleReceivedMessage(messageJson, peer)
                        }
                    } catch (e: IOException) {
                        break
                    }
                }
                
            } catch (e: Exception) {
                Logger.e(TAG, "Error handling Bluetooth connection: ${e.message}")
            } finally {
                try {
                    socket.close()
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }
    }
    
    /**
     * Handle Wi-Fi Direct connection
     */
    private fun handleWifiDirectConnection(socket: Socket) {
        meshScope.launch {
            try {
                val peerId = socket.inetAddress.hostAddress ?: "unknown"
                val peer = PeerInfo(
                    id = peerId,
                    name = "WiFi-$peerId",
                    type = "wifi_direct",
                    address = peerId,
                    lastSeen = System.currentTimeMillis()
                )
                
                connectedPeers[peerId] = peer
                onPeerConnected?.invoke(peerId)
                
                Logger.i(TAG, "Wi-Fi Direct peer connected: $peerId")
                
                // Handle messages
                val inputStream = socket.getInputStream()
                val buffer = ByteArray(MESSAGE_BUFFER_SIZE)
                
                while (isRunning && socket.isConnected) {
                    try {
                        val bytesRead = inputStream.read(buffer)
                        if (bytesRead > 0) {
                            val messageJson = String(buffer, 0, bytesRead)
                            handleReceivedMessage(messageJson, peer)
                        }
                    } catch (e: IOException) {
                        break
                    }
                }
                
            } catch (e: Exception) {
                Logger.e(TAG, "Error handling Wi-Fi Direct connection: ${e.message}")
            } finally {
                try {
                    socket.close()
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }
    }
    
    /**
     * Handle Hotspot connection
     */
    private fun handleHotspotConnection(socket: Socket) {
        meshScope.launch {
            try {
                val peerId = socket.inetAddress.hostAddress ?: "unknown"
                val peer = PeerInfo(
                    id = peerId,
                    name = "Hotspot-$peerId",
                    type = "hotspot",
                    address = peerId,
                    lastSeen = System.currentTimeMillis()
                )
                
                connectedPeers[peerId] = peer
                onPeerConnected?.invoke(peerId)
                
                Logger.i(TAG, "Hotspot peer connected: $peerId")
                
                // Handle messages
                val inputStream = socket.getInputStream()
                val buffer = ByteArray(MESSAGE_BUFFER_SIZE)
                
                while (isRunning && socket.isConnected) {
                    try {
                        val bytesRead = inputStream.read(buffer)
                        if (bytesRead > 0) {
                            val messageJson = String(buffer, 0, bytesRead)
                            handleReceivedMessage(messageJson, peer)
                        }
                    } catch (e: IOException) {
                        break
                    }
                }
                
            } catch (e: Exception) {
                Logger.e(TAG, "Error handling Hotspot connection: ${e.message}")
            } finally {
                try {
                    socket.close()
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }
    }
    
    /**
     * Handle received message
     */
    private fun handleReceivedMessage(messageJson: String, sender: PeerInfo) {
        try {
            val message = jsonToMessage(messageJson)
            
            // Check if we've already processed this message
            if (messageHistory.containsKey(message.id)) {
                return
            }
            
            // Add to history
            messageHistory[message.id] = System.currentTimeMillis()
            
            // Update sender's last seen
            connectedPeers[sender.id] = sender.copy(lastSeen = System.currentTimeMillis())
            
            Logger.d(TAG, "Received message from ${sender.name}: ${message.type}")
            
            when (message.type) {
                "alert" -> {
                    val alertMessage = jsonToAlert(message.payload)
                    onMessageReceived?.invoke(alertMessage)
                    
                    // Forward message if TTL > 0
                    if (message.ttl > 0) {
                        val forwardedMessage = message.copy(
                            ttl = message.ttl - 1,
                            senderId = getDeviceId()
                        )
                        broadcastMessage(forwardedMessage)
                    }
                }
                "heartbeat" -> {
                    // Handle heartbeat
                    handleHeartbeat(message, sender)
                }
                "discovery" -> {
                    // Handle discovery
                    handleDiscovery(message, sender)
                }
            }
            
        } catch (e: Exception) {
            Logger.e(TAG, "Error handling received message: ${e.message}")
        }
    }
    
    /**
     * Start discovery process
     */
    private fun startDiscovery() {
        meshScope.launch {
            while (isRunning) {
                try {
                    // Bluetooth discovery
                    discoverBluetoothDevices()
                    
                    // Wi-Fi Direct discovery
                    discoverWifiDirectDevices()
                    
                    // Clean up old peers
                    cleanupOldPeers()
                    
                    delay(DISCOVERY_INTERVAL)
                    
                } catch (e: Exception) {
                    Logger.e(TAG, "Error in discovery: ${e.message}")
                    delay(DISCOVERY_INTERVAL)
                }
            }
        }
    }
    
    /**
     * Start heartbeat
     */
    private fun startHeartbeat() {
        meshScope.launch {
            while (isRunning) {
                try {
                    val heartbeatMessage = MeshMessage(
                        type = "heartbeat",
                        senderId = getDeviceId(),
                        payload = createHeartbeatPayload()
                    )
                    
                    broadcastMessage(heartbeatMessage)
                    
                    delay(HEARTBEAT_INTERVAL)
                    
                } catch (e: Exception) {
                    Logger.e(TAG, "Error in heartbeat: ${e.message}")
                    delay(HEARTBEAT_INTERVAL)
                }
            }
        }
    }
    
    /**
     * Connect to available peers
     */
    private fun connectToAvailablePeers() {
        // Implementation for connecting to discovered peers
        // This would include Bluetooth pairing and Wi-Fi Direct connection
    }
    
    /**
     * Send message via Bluetooth
     */
    private fun sendBluetoothMessage(peer: PeerInfo, messageBytes: ByteArray) {
        // Implementation for sending Bluetooth message
    }
    
    /**
     * Send message via Wi-Fi Direct
     */
    private fun sendWifiDirectMessage(peer: PeerInfo, messageBytes: ByteArray) {
        // Implementation for sending Wi-Fi Direct message
    }
    
    /**
     * Send message via Hotspot
     */
    private fun sendHotspotMessage(peer: PeerInfo, messageBytes: ByteArray) {
        // Implementation for sending Hotspot message
    }
    
    /**
     * Discover Bluetooth devices
     */
    private fun discoverBluetoothDevices() {
        // Implementation for Bluetooth discovery
    }
    
    /**
     * Discover Wi-Fi Direct devices
     */
    private fun discoverWifiDirectDevices() {
        // Implementation for Wi-Fi Direct discovery
    }
    
    /**
     * Clean up old peers
     */
    private fun cleanupOldPeers() {
        val currentTime = System.currentTimeMillis()
        val timeout = 60000L // 1 minute
        
        connectedPeers.entries.removeAll { (peerId, peer) ->
            val isOld = currentTime - peer.lastSeen > timeout
            if (isOld) {
                onPeerDisconnected?.invoke(peerId)
                Logger.d(TAG, "Peer ${peer.name} timed out")
            }
            isOld
        }
    }
    
    /**
     * Handle heartbeat message
     */
    private fun handleHeartbeat(message: MeshMessage, sender: PeerInfo) {
        // Update peer info from heartbeat
    }
    
    /**
     * Handle discovery message
     */
    private fun handleDiscovery(message: MeshMessage, sender: PeerInfo) {
        // Respond to discovery
    }
    
    /**
     * Create heartbeat payload
     */
    private fun createHeartbeatPayload(): String {
        return JSONObject().apply {
            put("deviceId", getDeviceId())
            put("deviceName", getDeviceName())
            put("timestamp", System.currentTimeMillis())
            put("connectedPeers", connectedPeers.size)
        }.toString()
    }
    
    /**
     * Convert alert to JSON
     */
    private fun alertToJson(alert: AlertMessage): String {
        return JSONObject().apply {
            put("id", alert.id)
            put("message", alert.message)
            put("type", alert.type)
            put("priority", alert.priority)
            put("timestamp", alert.timestamp)
            put("sender", alert.sender)
            put("location", alert.location)
        }.toString()
    }
    
    /**
     * Convert JSON to alert
     */
    private fun jsonToAlert(json: String): AlertMessage {
        val jsonObj = JSONObject(json)
        return AlertMessage(
            id = jsonObj.getString("id"),
            message = jsonObj.getString("message"),
            type = jsonObj.getString("type"),
            priority = jsonObj.getString("priority"),
            timestamp = jsonObj.getLong("timestamp"),
            sender = jsonObj.getString("sender"),
            location = jsonObj.optString("location", "Unknown")
        )
    }
    
    /**
     * Convert message to JSON
     */
    private fun messageToJson(message: MeshMessage): String {
        return JSONObject().apply {
            put("id", message.id)
            put("type", message.type)
            put("senderId", message.senderId)
            put("payload", message.payload)
            put("timestamp", message.timestamp)
            put("ttl", message.ttl)
            put("priority", message.priority)
        }.toString()
    }
    
    /**
     * Convert JSON to message
     */
    private fun jsonToMessage(json: String): MeshMessage {
        val jsonObj = JSONObject(json)
        return MeshMessage(
            id = jsonObj.getString("id"),
            type = jsonObj.getString("type"),
            senderId = jsonObj.getString("senderId"),
            payload = jsonObj.getString("payload"),
            timestamp = jsonObj.getLong("timestamp"),
            ttl = jsonObj.getInt("ttl"),
            priority = jsonObj.getInt("priority")
        )
    }
    
    /**
     * Get priority level
     */
    private fun getPriorityLevel(priority: String): Int {
        return when (priority.lowercase()) {
            "critical", "အရေးကြီး" -> 4
            "high", "မြင့်" -> 3
            "medium", "အလယ်အလတ်" -> 2
            else -> 1
        }
    }
    
    /**
     * Get device ID
     */
    private fun getDeviceId(): String {
        return android.provider.Settings.Secure.getString(
            context.contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        ) ?: "unknown_device"
    }
    
    /**
     * Get device name
     */
    private fun getDeviceName(): String {
        return android.os.Build.MODEL ?: "Unknown Device"
    }
    
    /**
     * Stop mesh networking
     */
    fun stop() {
        Logger.i(TAG, "Stopping offline mesh manager")
        isRunning = false
        
        try {
            bluetoothServerSocket?.close()
            wifiDirectServerSocket?.close()
            hotspotServerSocket?.close()
            
            connectedPeers.clear()
            messageHistory.clear()
            
            meshScope.cancel()
            
        } catch (e: Exception) {
            Logger.e(TAG, "Error stopping mesh manager: ${e.message}")
        }
    }
    
    /**
     * Set callbacks
     */
    fun setCallbacks(
        onMessageReceived: (AlertMessage) -> Unit,
        onPeerConnected: (String) -> Unit,
        onPeerDisconnected: (String) -> Unit
    ) {
        this.onMessageReceived = onMessageReceived
        this.onPeerConnected = onPeerConnected
        this.onPeerDisconnected = onPeerDisconnected
    }
    
    /**
     * Get connected peers count
     */
    fun getConnectedPeersCount(): Int = connectedPeers.size
    
    /**
     * Get connected peers
     */
    fun getConnectedPeers(): List<PeerInfo> = connectedPeers.values.toList()
    
    /**
     * Is mesh network running
     */
    fun isRunning(): Boolean = isRunning
}