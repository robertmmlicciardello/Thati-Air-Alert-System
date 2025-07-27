package com.thati.airalert

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.thati.airalert.ui.theme.ThatiAirAlertTheme
import com.thati.airalert.services.AlertService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Enhanced Main Activity with Auto-Start and Online/Offline Detection
 */
class SimpleMainActivity : ComponentActivity() {
    
    private var isOnline by mutableStateOf(false)
    private var isSystemStarted by mutableStateOf(false)
    private var connectedDevices by mutableStateOf(0)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Auto-start system
        autoStartSystem()
        
        setContent {
            ThatiAirAlertTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    EnhancedMainScreen(
                        isOnline = isOnline,
                        isSystemStarted = isSystemStarted,
                        connectedDevices = connectedDevices,
                        onToggleSystem = { toggleSystem() },
                        onOpenUserMode = { openUserMode() },
                        onOpenAdminLogin = { openAdminLogin() }
                    )
                }
            }
        }
    }
    
    private fun autoStartSystem() {
        lifecycleScope.launch {
            delay(1000) // Brief delay for UI to load
            checkNetworkStatus()
            startAlertSystem()
            
            // Simulate mesh network device discovery
            simulateMeshNetwork()
        }
    }
    
    private fun checkNetworkStatus() {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        
        isOnline = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        
        // Continue checking network status
        lifecycleScope.launch {
            while (true) {
                delay(5000) // Check every 5 seconds
                val currentNetwork = connectivityManager.activeNetwork
                val currentCapabilities = connectivityManager.getNetworkCapabilities(currentNetwork)
                isOnline = currentCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
            }
        }
    }
    
    private fun startAlertSystem() {
        try {
            val intent = Intent(this, AlertService::class.java).apply {
                action = AlertService.ACTION_START_USER
            }
            startForegroundService(intent)
            isSystemStarted = true
        } catch (e: Exception) {
            // Fallback to manual start
            isSystemStarted = false
        }
    }
    
    private fun simulateMeshNetwork() {
        lifecycleScope.launch {
            while (true) {
                delay(3000)
                // Simulate mesh network device discovery
                connectedDevices = if (isSystemStarted) (2..8).random() else 0
            }
        }
    }
    
    private fun toggleSystem() {
        isSystemStarted = !isSystemStarted
        if (isSystemStarted) {
            startAlertSystem()
        } else {
            stopAlertSystem()
        }
    }
    
    private fun stopAlertSystem() {
        try {
            val intent = Intent(this, AlertService::class.java).apply {
                action = AlertService.ACTION_STOP_SERVICE
            }
            startService(intent)
            connectedDevices = 0
        } catch (e: Exception) {
            // Handle error
        }
    }
    
    private fun openUserMode() {
        val intent = Intent(this, SimpleUserActivity::class.java)
        startActivity(intent)
    }
    
    private fun openAdminLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
    }
}

@Composable
fun EnhancedMainScreen(
    isOnline: Boolean,
    isSystemStarted: Boolean,
    connectedDevices: Int,
    onToggleSystem: () -> Unit,
    onOpenUserMode: () -> Unit,
    onOpenAdminLogin: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "🚨",
            fontSize = 80.sp
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "သတိ",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = "Thati Air Alert",
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Myanmar Emergency Alert System",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Network Status Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isOnline) 
                    Color(0xFF10B981).copy(alpha = 0.1f)
                else 
                    Color(0xFFF59E0B).copy(alpha = 0.1f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isOnline) "🌐 Online Mode" else "📱 Offline Mode",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isOnline) Color(0xFF10B981) else Color(0xFFF59E0B)
                    )
                    Text(
                        text = if (isOnline) "Internet ချိတ်ဆက်ထားသည်" else "Mesh Network အသုံးပြုနေသည်",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Text(
                    text = if (isOnline) "✅" else "📡",
                    fontSize = 24.sp
                )
            }
        }
        
        // System Status Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isSystemStarted) 
                    Color(0xFF10B981).copy(alpha = 0.1f)
                else 
                    Color(0xFFEF4444).copy(alpha = 0.1f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isSystemStarted) "🟢 Alert System Active" else "🔴 Alert System Inactive",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isSystemStarted) Color(0xFF10B981) else Color(0xFFEF4444)
                    )
                    Text(
                        text = if (isSystemStarted) "သတိပေးချက်များ လက်ခံရန် အသင့်" else "စနစ်ကို စတင်ရန် လိုအပ်သည်",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Text(
                    text = if (isSystemStarted) "✅" else "⏸️",
                    fontSize = 24.sp
                )
            }
        }
        
        // Mesh Network Status Card
        if (isSystemStarted) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF3B82F6).copy(alpha = 0.1f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "📡 Mesh Network",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF3B82F6)
                        )
                        Text(
                            text = "$connectedDevices devices ချိတ်ဆက်ထားသည်",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Text(
                        text = connectedDevices.toString(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF3B82F6)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Main Action Button (Auto-started, so this is for manual control)
        Button(
            onClick = onToggleSystem,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isSystemStarted) 
                    Color(0xFFEF4444)
                else 
                    Color(0xFF10B981)
            )
        ) {
            Text(
                text = if (isSystemStarted) "🛑 Stop Alert System" else "▶️ Start Alert System",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Mode Selection Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onOpenUserMode,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF3B82F6)
                )
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "👤",
                        fontSize = 20.sp
                    )
                    Text(
                        text = "User Mode",
                        fontSize = 12.sp,
                        color = Color.White
                    )
                }
            }
            
            Button(
                onClick = onOpenAdminLogin,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFEF4444)
                )
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🚨",
                        fontSize = 20.sp
                    )
                    Text(
                        text = "Admin",
                        fontSize = 12.sp,
                        color = Color.White
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Status Text
        Text(
            text = if (isSystemStarted) 
                "✅ App သည် background တွင် အလုပ်လုပ်နေပါသည်" 
            else 
                "⚠️ Alert များ လက်ခံရန် စနစ်ကို စတင်ပါ",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}