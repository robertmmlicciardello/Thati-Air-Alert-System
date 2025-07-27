package com.thati.airalert.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.*

/**
 * Network Utility Functions
 * Helper functions for network operations and connectivity checks
 */
object NetworkUtils {
    
    /**
     * Check if device has internet connectivity
     */
    fun isInternetAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            networkInfo?.isConnected == true
        }
    }
    
    /**
     * Check if Wi-Fi is enabled
     */
    fun isWifiEnabled(context: Context): Boolean {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        return wifiManager.isWifiEnabled
    }
    
    /**
     * Get device IP address
     */
    fun getDeviceIpAddress(): String? {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (networkInterface in interfaces) {
                val addresses = Collections.list(networkInterface.inetAddresses)
                for (address in addresses) {
                    if (!address.isLoopbackAddress) {
                        val hostAddress = address.hostAddress
                        val isIPv4 = hostAddress?.indexOf(':') ?: -1 < 0
                        if (isIPv4) {
                            return hostAddress
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
    
    /**
     * Get Wi-Fi network name (SSID)
     */
    fun getWifiNetworkName(context: Context): String? {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wifiInfo = wifiManager.connectionInfo
        return wifiInfo?.ssid?.replace("\"", "")
    }
    
    /**
     * Check if device is connected to specific Wi-Fi network
     */
    fun isConnectedToWifi(context: Context, ssid: String): Boolean {
        val currentSsid = getWifiNetworkName(context)
        return currentSsid == ssid
    }
    
    /**
     * Get network type (WiFi, Cellular, etc.)
     */
    fun getNetworkType(context: Context): String {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            when {
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> "WiFi"
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "Cellular"
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true -> "Ethernet"
                else -> "Unknown"
            }
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            networkInfo?.typeName ?: "Unknown"
        }
    }
    
    /**
     * Check if device can reach specific host
     */
    fun canReachHost(host: String, timeout: Int = 3000): Boolean {
        return try {
            val address = InetAddress.getByName(host)
            address.isReachable(timeout)
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get signal strength for current network
     */
    fun getSignalStrength(context: Context): Int {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wifiInfo = wifiManager.connectionInfo
        return WifiManager.calculateSignalLevel(wifiInfo.rssi, 5)
    }
    
    /**
     * Check if device is in airplane mode
     */
    fun isAirplaneModeOn(context: Context): Boolean {
        return android.provider.Settings.Global.getInt(
            context.contentResolver,
            android.provider.Settings.Global.AIRPLANE_MODE_ON,
            0
        ) != 0
    }
    
    /**
     * Generate unique device identifier for mesh network
     */
    fun generateDeviceIdentifier(context: Context): String {
        val androidId = android.provider.Settings.Secure.getString(
            context.contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        )
        return "THATI_${androidId.take(8).uppercase()}"
    }
    
    /**
     * Check if port is available for use
     */
    fun isPortAvailable(port: Int): Boolean {
        return try {
            val socket = java.net.ServerSocket(port)
            socket.close()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get available port for mesh networking
     */
    fun getAvailablePort(startPort: Int = 8000, endPort: Int = 9000): Int {
        for (port in startPort..endPort) {
            if (isPortAvailable(port)) {
                return port
            }
        }
        return -1 // No available port found
    }
    
    /**
     * Validate IP address format
     */
    fun isValidIpAddress(ip: String): Boolean {
        return try {
            val parts = ip.split(".")
            if (parts.size != 4) return false
            parts.all { part ->
                val num = part.toIntOrNull()
                num != null && num in 0..255
            }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Check if device is in Myanmar IP range (approximate)
     */
    fun isMyanmarIpRange(ip: String): Boolean {
        // Myanmar IP ranges (approximate)
        val myanmarRanges = listOf(
            "103.7.", "103.8.", "103.9.", "103.10.",
            "203.81.", "203.82.", "203.83.",
            "202.129.", "202.130.", "202.131."
        )
        return myanmarRanges.any { ip.startsWith(it) }
    }
}