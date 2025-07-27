@file:OptIn(ExperimentalMaterial3Api::class)

package com.thati.airalert

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.thati.airalert.ui.theme.ThatiAirAlertTheme
import android.os.Build
import android.util.Log
import com.thati.airalert.models.AlertMessage
import com.thati.airalert.services.AlertService
import com.thati.airalert.utils.AlertBroadcastManager
import com.thati.airalert.utils.AlertSoundPlayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Simple User Activity - ရိုးရှင်းလွယ်ကူသော User Interface
 * နည်းပညာ မသိတဲ့ ပြည်သူတွေအတွက် အလွယ်တကူ သုံးနိုင်အောင် ဒီဇိုင်းထားသည်
 */
class SimpleUserActivity : ComponentActivity() {
    
    private lateinit var alertSoundPlayer: AlertSoundPlayer
    private var isSystemActive by mutableStateOf(false)
    private var receivedAlerts by mutableStateOf<List<SimpleAlert>>(emptyList())
    private var connectedDevices by mutableStateOf(0)
    private var isOnline by mutableStateOf(false)
    
    private val alertReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AlertBroadcastManager.ACTION_NEW_ALERT) {
                val message = intent.getStringExtra("message") ?: "သတိပေးချက် ရရှိပါသည်"
                val priority = intent.getStringExtra("priority") ?: "medium"
                
                // Play alert sound
                alertSoundPlayer.playEmergencyAlert(priority = priority)
                
                // Add to alerts list
                val newAlert = SimpleAlert(
                    id = System.currentTimeMillis().toString(),
                    message = message,
                    timestamp = System.currentTimeMillis(),
                    isImportant = priority in listOf("high", "critical", "အရေးကြီး", "မြင့်")
                )
                receivedAlerts = listOf(newAlert) + receivedAlerts
                
                // Show toast notification
                Toast.makeText(this@SimpleUserActivity, "🚨 $message", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize alert sound player
        alertSoundPlayer = AlertSoundPlayer(this)
        
        // Register broadcast receiver
        LocalBroadcastManager.getInstance(this).registerReceiver(
            alertReceiver,
            IntentFilter(AlertBroadcastManager.ACTION_NEW_ALERT)
        )
        
        // Auto-start the alert system
        autoStartAlertSystem()
        
        setContent {
            ThatiAirAlertTheme {
                SimpleUserScreen(
                    isSystemActive = isSystemActive,
                    receivedAlerts = receivedAlerts,
                    connectedDevices = connectedDevices,
                    isOnline = isOnline,
                    onToggleSystem = { toggleAlertSystem() },
                    onTestAlert = { testAlert() },
                    onClearAlerts = { clearAllAlerts() },
                    onBackToMain = { 
                        startActivity(Intent(this@SimpleUserActivity, SimpleMainActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }
    
    private fun autoStartAlertSystem() {
        lifecycleScope.launch {
            delay(1000) // Brief delay for UI to load
            
            // Check network status
            checkNetworkStatus()
            
            // Start alert service automatically
            startAlertService()
            
            // Simulate mesh network
            simulateMeshNetwork()
        }
    }
    
    private fun checkNetworkStatus() {
        // Simple network check
        try {
            val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            isOnline = capabilities?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        } catch (e: Exception) {
            isOnline = false
        }
        
        // Continue checking network status
        lifecycleScope.launch {
            while (true) {
                delay(10000) // Check every 10 seconds
                try {
                    val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
                    val network = connectivityManager.activeNetwork
                    val capabilities = connectivityManager.getNetworkCapabilities(network)
                    isOnline = capabilities?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
                } catch (e: Exception) {
                    isOnline = false
                }
            }
        }
    }
    
    private fun startAlertService() {
        try {
            val intent = Intent(this, AlertService::class.java).apply {
                action = AlertService.ACTION_START_USER
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            isSystemActive = true
            Toast.makeText(this, "✅ သတိပေးချက်စနစ် စတင်ပြီးပါပြီ", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("SimpleUserActivity", "Error starting alert service", e)
            isSystemActive = false
            Toast.makeText(this, "⚠️ စနစ်စတင်ရာတွင် ပြဿနာရှိသည်: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun simulateMeshNetwork() {
        lifecycleScope.launch {
            while (true) {
                delay(5000)
                if (isSystemActive) {
                    connectedDevices = (2..8).random()
                } else {
                    connectedDevices = 0
                }
            }
        }
    }
    
    private fun toggleAlertSystem() {
        if (isSystemActive) {
            stopAlertSystem()
        } else {
            startAlertService()
        }
    }
    
    private fun stopAlertSystem() {
        try {
            val intent = Intent(this, AlertService::class.java).apply {
                action = AlertService.ACTION_STOP_SERVICE
            }
            startService(intent)
            isSystemActive = false
            connectedDevices = 0
            Toast.makeText(this, "⏸️ သတိပေးချက်စနစ် ရပ်ပြီးပါပြီ", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("SimpleUserActivity", "Error stopping alert service", e)
            Toast.makeText(this, "⚠️ စနစ်ရပ်ရာတွင် ပြဿနာရှိသည်: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun testAlert() {
        val testMessage = "ဒီဟာ စမ်းသပ်ချက် သတိပေးချက်ပါ။ အသံကြားရင် စနစ် အလုပ်လုပ်နေပါသည်။"
        alertSoundPlayer.testAlert("medium")
        
        val testAlert = SimpleAlert(
            id = "test_${System.currentTimeMillis()}",
            message = testMessage,
            timestamp = System.currentTimeMillis(),
            isImportant = false
        )
        receivedAlerts = listOf(testAlert) + receivedAlerts
        Toast.makeText(this, "🧪 စမ်းသပ်ချက် သတိပေးချက် ပို့ပြီးပါပြီ", Toast.LENGTH_SHORT).show()
    }
    
    private fun clearAllAlerts() {
        receivedAlerts = emptyList()
        Toast.makeText(this, "🗑️ သတိပေးချက်များ ရှင်းလင်းပြီးပါပြီ", Toast.LENGTH_SHORT).show()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(alertReceiver)
        alertSoundPlayer.release()
    }
}

data class SimpleAlert(
    val id: String,
    val message: String,
    val timestamp: Long,
    val isImportant: Boolean
)

@Composable
fun SimpleUserScreen(
    isSystemActive: Boolean,
    receivedAlerts: List<SimpleAlert>,
    connectedDevices: Int,
    isOnline: Boolean,
    onToggleSystem: () -> Unit,
    onTestAlert: () -> Unit,
    onClearAlerts: () -> Unit,
    onBackToMain: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1E3A8A),
                        Color(0xFF3B82F6),
                        Color(0xFF60A5FA)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Simple Header
            SimpleHeader(
                isSystemActive = isSystemActive,
                isOnline = isOnline,
                connectedDevices = connectedDevices,
                onBackToMain = onBackToMain
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Main Status Card
            MainStatusCard(
                isSystemActive = isSystemActive,
                onToggleSystem = onToggleSystem
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Quick Actions
            QuickActionsRow(
                onTestAlert = onTestAlert,
                onClearAlerts = onClearAlerts,
                alertCount = receivedAlerts.size
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Alerts List
            AlertsList(
                alerts = receivedAlerts,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun SimpleHeader(
    isSystemActive: Boolean,
    isOnline: Boolean,
    connectedDevices: Int,
    onBackToMain: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "🚨 သတိ",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E40AF)
                    )
                    Text(
                        text = "လေကြောင်းသတိပေးချက်စနစ်",
                        fontSize = 12.sp,
                        color = Color(0xFF6B7280)
                    )
                }
                
                IconButton(onClick = onBackToMain) {
                    Icon(
                        Icons.Default.Home,
                        contentDescription = "ပင်မစာမျက်နှာ",
                        tint = Color(0xFF1E40AF)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Status Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatusIndicator(
                    icon = if (isSystemActive) "✅" else "❌",
                    label = "စနစ်",
                    value = if (isSystemActive) "အလုပ်လုပ်နေ" else "ရပ်နေ",
                    color = if (isSystemActive) Color(0xFF10B981) else Color(0xFFEF4444)
                )
                
                StatusIndicator(
                    icon = if (isOnline) "🌐" else "📱",
                    label = "ချိတ်ဆက်မှု",
                    value = if (isOnline) "Online" else "Offline",
                    color = if (isOnline) Color(0xFF10B981) else Color(0xFFF59E0B)
                )
                
                StatusIndicator(
                    icon = "👥",
                    label = "အနီးအနား",
                    value = "$connectedDevices ခု",
                    color = Color(0xFF3B82F6)
                )
            }
        }
    }
}

@Composable
fun StatusIndicator(
    icon: String,
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = icon,
            fontSize = 20.sp
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = Color(0xFF6B7280)
        )
    }
}

@Composable
fun MainStatusCard(
    isSystemActive: Boolean,
    onToggleSystem: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSystemActive) 
                Color(0xFF10B981).copy(alpha = 0.1f) 
            else 
                Color(0xFFEF4444).copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Animated Status Icon
            val infiniteTransition = rememberInfiniteTransition()
            val scale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = if (isSystemActive) 1.1f else 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000),
                    repeatMode = RepeatMode.Reverse
                )
            )
            
            Text(
                text = if (isSystemActive) "🟢" else "🔴",
                fontSize = 48.sp,
                modifier = Modifier.scale(scale)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = if (isSystemActive) 
                    "သတိပေးချက်စနစ် အလုပ်လုပ်နေပါသည်" 
                else 
                    "သတိပေးချက်စနစ် ရပ်နေပါသည်",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSystemActive) Color(0xFF10B981) else Color(0xFFEF4444),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = if (isSystemActive) 
                    "သတိပေးချက်များ လက်ခံရန် အသင့်ဖြစ်နေပါသည်" 
                else 
                    "စနစ်ကို စတင်ရန် အောက်ခလုတ်ကို နှိပ်ပါ",
                fontSize = 14.sp,
                color = Color(0xFF6B7280),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Toggle Button
            Button(
                onClick = onToggleSystem,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSystemActive) Color(0xFFEF4444) else Color(0xFF10B981)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (isSystemActive) "⏸️ ရပ်ရန်" else "▶️ စတင်ရန်",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun QuickActionsRow(
    onTestAlert: () -> Unit,
    onClearAlerts: () -> Unit,
    alertCount: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Test Alert Button
        Button(
            onClick = onTestAlert,
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF3B82F6)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "🧪 စမ်းကြည့်ရန်",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
        }
        
        // Clear Alerts Button
        Button(
            onClick = onClearAlerts,
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            enabled = alertCount > 0,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFF59E0B)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "🗑️ ရှင်းလင်းရန် ($alertCount)",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
        }
    }
}

@Composable
fun AlertsList(
    alerts: List<SimpleAlert>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "📋 လက်ခံရရှိသော သတိပေးချက်များ (${alerts.size})",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F2937),
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            if (alerts.isEmpty()) {
                // Empty State
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "📭",
                        fontSize = 48.sp
                    )
                    Text(
                        text = "သတိပေးချက် မရှိသေးပါ",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF6B7280),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "သတိပေးချက်များ ရောက်ရှိသည့်အခါ ဤနေရာတွင် ပြသမည်",
                        fontSize = 12.sp,
                        color = Color(0xFF9CA3AF),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            } else {
                // Alerts List
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(alerts.take(10)) { alert ->
                        SimpleAlertCard(alert = alert)
                    }
                    
                    if (alerts.size > 10) {
                        item {
                            Text(
                                text = "နောက်ထပ် ${alerts.size - 10} ခု ရှိသေးသည်...",
                                fontSize = 12.sp,
                                color = Color(0xFF6B7280),
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SimpleAlertCard(alert: SimpleAlert) {
    val dateFormat = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (alert.isImportant) 
                Color(0xFFEF4444).copy(alpha = 0.1f) 
            else 
                Color(0xFFF3F4F6)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Alert Icon
            Text(
                text = if (alert.isImportant) "🚨" else "📢",
                fontSize = 20.sp,
                modifier = Modifier.padding(end = 12.dp)
            )
            
            // Alert Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = alert.message,
                    fontSize = 14.sp,
                    color = Color(0xFF1F2937),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                
                Text(
                    text = dateFormat.format(Date(alert.timestamp)),
                    fontSize = 12.sp,
                    color = Color(0xFF6B7280)
                )
            }
            
            // Priority Indicator
            if (alert.isImportant) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            Color(0xFFEF4444),
                            shape = CircleShape
                        )
                )
            }
        }
    }
}