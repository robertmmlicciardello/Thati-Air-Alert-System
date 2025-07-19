package com.thati.airalert.services

import android.content.Context
import android.net.wifi.p2p.*
import android.util.Log
import kotlinx.coroutines.*
import java.io.*
import java.net.*

/**
 * Wi-Fi Direct Manager
 * Wi-Fi Direct ကို အသုံးပြုပြီး alert messages များ ပို့ဆောင်ရန်
 */
class WifiDirectManager(
    private val context: Context,
    private val onMessageReceived: (String) -> Unit
) {
    
    private var wifiP2pManager: WifiP2pManager? = null
    private var channel: WifiP2pManager.Channel? = null
    private var serverSocket: ServerSocket? = null
    private var isDiscovering = false
    private var isGroupOwner = false
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    companion object {
        private const val TAG = "WifiDirectManager"
        private const val SERVER_PORT = 8888
        private const val SERVICE_INSTANCE = "ThatiAlert"
        private const val SERVICE_TYPE = "_thati._tcp"
    }
    
    /**
     * Wi-Fi Direct ကို initialize လုပ်ခြင်း
     */
    fun initialize(): Boolean {
        return try {
            wifiP2pManager = context.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
            channel = wifiP2pManager?.initialize(context, context.mainLooper, null)
            
            if (wifiP2pManager != null && channel != null) {
                Log.d(TAG, "Wi-Fi Direct initialized successfully")
                true
            } else {
                Log.e(TAG, "Failed to initialize Wi-Fi Direct")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Wi-Fi Direct", e)
            false
        }
    }
    
    /**
     * Device discovery ကို စတင်ခြင်း
     */
    fun startDiscovery() {
        if (isDiscovering) return
        
        wifiP2pManager?.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                isDiscovering = true
                Log.d(TAG, "Peer discovery started")
            }
            
            override fun onFailure(reason: Int) {
                Log.e(TAG, "Peer discovery failed: $reason")
            }
        })
    }
    
    /**
     * Device discovery ကို ရပ်ခြင်း
     */
    fun stopDiscovery() {
        if (!isDiscovering) return
        
        wifiP2pManager?.stopPeerDiscovery(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                isDiscovering = false
                Log.d(TAG, "Peer discovery stopped")
            }
            
            override fun onFailure(reason: Int) {
                Log.e(TAG, "Failed to stop peer discovery: $reason")
            }
        })
    }
    
    /**
     * Group ဖန်တီးခြင်း (Admin mode အတွက်)
     */
    fun createGroup() {
        wifiP2pManager?.createGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d(TAG, "Group created successfully")
                isGroupOwner = true
                startServer()
            }
            
            override fun onFailure(reason: Int) {
                Log.e(TAG, "Failed to create group: $reason")
            }
        })
    }
    
    /**
     * Server socket ကို စတင်ခြင်း (Group owner အတွက်)
     */
    private fun startServer() {
        scope.launch {
            try {
                serverSocket = ServerSocket(SERVER_PORT)
                Log.d(TAG, "Server started on port $SERVER_PORT")
                
                while (serverSocket?.isClosed == false) {
                    try {
                        val clientSocket = serverSocket?.accept()
                        clientSocket?.let { socket ->
                            handleClientConnection(socket)
                        }
                    } catch (e: SocketException) {
                        if (!serverSocket?.isClosed!!) {
                            Log.e(TAG, "Server socket error", e)
                        }
                        break
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error starting server", e)
            }
        }
    }
    
    /**
     * Client connection ကို handle လုပ်ခြင်း
     */
    private fun handleClientConnection(socket: Socket) {
        scope.launch {
            try {
                val inputStream = socket.getInputStream()
                val reader = BufferedReader(InputStreamReader(inputStream))
                
                val message = reader.readLine()
                if (message != null) {
                    Log.d(TAG, "Received message: $message")
                    onMessageReceived(message)
                }
                
                reader.close()
                socket.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error handling client connection", e)
            }
        }
    }
    
    /**
     * Message ပို့ခြင်း
     */
    fun sendMessage(message: String, targetAddress: String) {
        scope.launch {
            try {
                val socket = Socket()
                socket.connect(InetSocketAddress(targetAddress, SERVER_PORT), 5000)
                
                val outputStream = socket.getOutputStream()
                val writer = PrintWriter(outputStream, true)
                
                writer.println(message)
                writer.flush()
                
                writer.close()
                socket.close()
                
                Log.d(TAG, "Message sent successfully to $targetAddress")
            } catch (e: Exception) {
                Log.e(TAG, "Error sending message to $targetAddress", e)
            }
        }
    }
    
    /**
     * Broadcast message ပို့ခြင်း (Group owner မှ အားလုံးကို)
     */
    fun broadcastMessage(message: String) {
        if (!isGroupOwner) {
            Log.w(TAG, "Cannot broadcast - not group owner")
            return
        }
        
        // Group members list ကို ရယူပြီး message ပို့ပါ
        wifiP2pManager?.requestGroupInfo(channel) { group ->
            group?.clientList?.forEach { device ->
                // Client devices များကို message ပို့ပါ
                // Note: Real implementation မှာ client IP addresses များ ရယူရမည်
                Log.d(TAG, "Broadcasting to device: ${device.deviceName}")
            }
        }
    }
    
    /**
     * Connection info ကို handle လုပ်ခြင်း
     */
    fun handleConnectionInfo(info: WifiP2pInfo) {
        if (info.groupFormed) {
            if (info.isGroupOwner) {
                Log.d(TAG, "Device is group owner")
                isGroupOwner = true
                startServer()
            } else {
                Log.d(TAG, "Device is group client")
                isGroupOwner = false
                // Group owner IP: info.groupOwnerAddress
            }
        }
    }
    
    /**
     * Peers list ကို handle လုပ်ခြင်း
     */
    fun handlePeersChanged(peers: WifiP2pDeviceList) {
        Log.d(TAG, "Peers changed: ${peers.deviceList.size} devices found")
        
        // Available peers များကို connect လုပ်ရန် ကြိုးစားပါ
        peers.deviceList.forEach { device ->
            Log.d(TAG, "Found peer: ${device.deviceName} (${device.deviceAddress})")
        }
    }
    
    /**
     * Specific device နှင့် connect လုပ်ခြင်း
     */
    fun connectToDevice(device: WifiP2pDevice) {
        val config = WifiP2pConfig().apply {
            deviceAddress = device.deviceAddress
        }
        
        wifiP2pManager?.connect(channel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d(TAG, "Connection initiated to ${device.deviceName}")
            }
            
            override fun onFailure(reason: Int) {
                Log.e(TAG, "Failed to connect to ${device.deviceName}: $reason")
            }
        })
    }
    
    /**
     * Group ကို ဖျက်ခြင်း
     */
    fun removeGroup() {
        wifiP2pManager?.removeGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d(TAG, "Group removed successfully")
                isGroupOwner = false
            }
            
            override fun onFailure(reason: Int) {
                Log.e(TAG, "Failed to remove group: $reason")
            }
        })
    }
    
    /**
     * Resources များကို သန့်ရှင်းခြင်း
     */
    fun cleanup() {
        scope.cancel()
        stopDiscovery()
        
        try {
            serverSocket?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing server socket", e)
        }
        
        removeGroup()
    }
}