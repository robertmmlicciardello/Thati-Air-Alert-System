package com.thati.airalert

import android.content.Intent
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thati.airalert.ui.theme.ThatiAirAlertTheme
import com.thati.airalert.models.AlertMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import com.thati.airalert.utils.AlertBroadcastManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collectLatest

/**
 * Working User Activity - Mesh Network Ready
 */
class WorkingUserActivity : ComponentActivity() {
    
    private val alertReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AlertBroadcastManager.ACTION_NEW_ALERT) {
                // Alert received via broadcast
                Toast.makeText(this@WorkingUserActivity, "🚨 New Alert Received!", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Register broadcast receiver
        LocalBroadcastManager.getInstance(this).registerReceiver(
            alertReceiver,
            IntentFilter(AlertBroadcastManager.ACTION_NEW_ALERT)
        )
        
        setContent {
            ThatiAirAlertTheme {
                WorkingUserScreen(
                    onBackToMain = { 
                        startActivity(Intent(this, SimpleMainActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(alertReceiver)
    }
}

data class NetworkDevice(
    val id: String,
    val name: String,
    val type: String,
    val isOnline: Boolean,
    val signalStrength: Int,
    val lastSeen: Long
)

data class MeshAlert(
    val id: String,
    val message: String,
    val type: String,
    val priority: String,
    val timestamp: Long,
    val source: String,
    val isOffline: Boolean
)

@Composable
fun WorkingUserScreen(onBackToMain: () -> Unit) {
    var isListening by remember { mutableStateOf(false) }
    var meshDevices by remember { mutableStateOf(generateSampleDevices()) }
    var receivedAlerts by remember { mutableStateOf(generateSampleMeshAlerts()) }
    
    // Listen for real-time alerts
    LaunchedEffect(Unit) {
        AlertBroadcastManager.alertFlow.collectLatest { alertMessage ->
            val meshAlert = MeshAlert(
                id = alertMessage.id,
                message = alertMessage.message,
                type = alertMessage.type,
                priority = alertMessage.priority,
                timestamp = alertMessage.timestamp,
                source = "Regional Admin",
                isOffline = false
            )
            receivedAlerts = listOf(meshAlert) + receivedAlerts
        }
    }
    var networkHealth by remember { mutableStateOf(85) }
    var isOfflineMode by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    // Auto-update mesh network status
    LaunchedEffect(isListening) {
        while (isListening) {
            delay(5000)
            meshDevices = generateSampleDevices()
            networkHealth = (70..95).random()
            
            // Simulate receiving mesh network alerts (only if offline mode)
            if (isOfflineMode && (1..15).random() > 13) {
                AlertBroadcastManager.simulateIncomingAlert(context)
            }
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A202C),
                        Color(0xFF2D3748)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            UserHeader(
                isListening = isListening,
                networkHealth = networkHealth,
                isOfflineMode = isOfflineMode,
                onToggleListening = { 
                    isListening = !isListening
                    Toast.makeText(
                        context, 
                        if (isListening) "🔊 Alert စနစ် စတင်ပြီ" else "⏸️ Alert စနစ် ရပ်ပြီ",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                onToggleOfflineMode = { 
                    isOfflineMode = !isOfflineMode
                    Toast.makeText(
                        context, 
                        if (isOfflineMode) "📱 Offline Mode" else "🌐 Online Mode",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                onBackToMain = onBackToMain
            )
            
            // Content Tabs
            var selectedTab by remember { mutableStateOf(0) }
            
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFF2D3748),
                contentColor = Color.White
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("🚨 Alerts") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("🌐 Network") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("⚙️ Settings") }
                )
            }
            
            // Tab Content
            when (selectedTab) {
                0 -> AlertsTab(receivedAlerts, isOfflineMode)
                1 -> NetworkTab(meshDevices, networkHealth, isOfflineMode)
                2 -> SettingsTab(isListening, isOfflineMode)
            }
        }
    }
}

@Composable
fun UserHeader(
    isListening: Boolean,
    networkHealth: Int,
    isOfflineMode: Boolean,
    onToggleListening: () -> Unit,
    onToggleOfflineMode: () -> Unit,
    onBackToMain: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF4A5568)),
        shape = RoundedCornerShape(0.dp)
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
                        text = "👤 User Mode",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = if (isListening) "🔊 Alert များ နားထောင်နေသည်" else "⏸️ Alert စနစ် ရပ်နေသည်",
                        fontSize = 12.sp,
                        color = if (isListening) Color(0xFF68D391) else Color(0xFFFC8181)
                    )
                }
                
                Row {
                    IconButton(onClick = onBackToMain) {
                        Icon(Icons.Default.Home, contentDescription = "Home", tint = Color.White)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Status Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusCard(
                    title = "Network",
                    value = "${networkHealth}%",
                    color = getHealthColor(networkHealth),
                    modifier = Modifier.weight(1f)
                )
                
                StatusCard(
                    title = "Mode",
                    value = if (isOfflineMode) "Offline" else "Online",
                    color = if (isOfflineMode) Color(0xFFED8936) else Color(0xFF38A169),
                    modifier = Modifier.weight(1f)
                )
                
                StatusCard(
                    title = "Status",
                    value = if (isListening) "Active" else "Inactive",
                    color = if (isListening) Color(0xFF38A169) else Color(0xFFE53E3E),
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Control Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onToggleListening,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isListening) Color(0xFFE53E3E) else Color(0xFF38A169)
                    )
                ) {
                    Text(if (isListening) "⏸️ ရပ်ရန်" else "▶️ စတင်ရန်")
                }
                
                OutlinedButton(
                    onClick = onToggleOfflineMode,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    )
                ) {
                    Text(if (isOfflineMode) "🌐 Online" else "📱 Offline")
                }
            }
        }
    }
}

@Composable
fun StatusCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.2f)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = title,
                fontSize = 10.sp,
                color = Color.White
            )
        }
    }
}

@Composable
fun AlertsTab(alerts: List<MeshAlert>, isOfflineMode: Boolean) {
    val context = LocalContext.current
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🚨 လက်ခံရရှိသော Alert များ",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${alerts.size} alerts",
                        fontSize = 12.sp,
                        color = Color(0xFFCBD5E0)
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // Test Alert Button
                    Button(
                        onClick = {
                            AlertBroadcastManager.simulateIncomingAlert(context)
                            Toast.makeText(context, "Test alert sent!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF3182CE)
                        ),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Test Alert", fontSize = 10.sp)
                    }
                }
            }
        }
        
        if (alerts.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2D3748))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "📭",
                            fontSize = 48.sp
                        )
                        Text(
                            text = "Alert မရှိသေးပါ",
                            fontSize = 16.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Alert များ လက်ခံရရှိသည့်အခါ ဤနေရာတွင် ပြသမည်",
                            fontSize = 12.sp,
                            color = Color(0xFFCBD5E0),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(alerts) { alert ->
                AlertCard(alert)
            }
        }
    }
}

@Composable
fun AlertCard(alert: MeshAlert) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (alert.priority) {
                "အရေးကြီး" -> Color(0xFF742A2A)
                "မြင့်" -> Color(0xFF9C4221)
                "အလယ်အလတ်" -> Color(0xFF744210)
                else -> Color(0xFF2D3748)
            }
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${alert.type} Alert",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (alert.isOffline) {
                        Text(
                            text = "📱 Offline",
                            fontSize = 10.sp,
                            color = Color(0xFFED8936),
                            modifier = Modifier
                                .background(
                                    Color.White.copy(alpha = 0.2f),
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    
                    Text(
                        text = alert.priority,
                        fontSize = 10.sp,
                        color = Color.White,
                        modifier = Modifier
                            .background(
                                Color.White.copy(alpha = 0.2f),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = alert.message,
                fontSize = 12.sp,
                color = Color(0xFFE2E8F0)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Source: ${alert.source}",
                    fontSize = 10.sp,
                    color = Color(0xFFCBD5E0)
                )
                
                Text(
                    text = formatMeshTime(alert.timestamp),
                    fontSize = 10.sp,
                    color = Color(0xFFCBD5E0)
                )
            }
        }
    }
}

@Composable
fun NetworkTab(devices: List<NetworkDevice>, networkHealth: Int, isOfflineMode: Boolean) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "🌐 Mesh Network Status",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        
        item {
            NetworkHealthCard(networkHealth, isOfflineMode)
        }
        
        item {
            Text(
                text = "📱 Connected Devices (${devices.count { it.isOnline }}/${devices.size})",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        
        items(devices) { device ->
            DeviceCard(device)
        }
    }
}

@Composable
fun NetworkHealthCard(networkHealth: Int, isOfflineMode: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2D3748)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Network Health",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Text(
                    text = "${networkHealth}%",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = getHealthColor(networkHealth)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LinearProgressIndicator(
                progress = networkHealth / 100f,
                modifier = Modifier.fillMaxWidth(),
                color = getHealthColor(networkHealth),
                trackColor = Color(0xFF4A5568)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = if (isOfflineMode) "📱 Offline Mode - Mesh Network Only" else "🌐 Online Mode - Full Connectivity",
                fontSize = 12.sp,
                color = Color(0xFFCBD5E0)
            )
        }
    }
}

@Composable
fun DeviceCard(device: NetworkDevice) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (device.isOnline) Color(0xFF1A365D) else Color(0xFF4A5568)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = device.name,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = device.type,
                    fontSize = 10.sp,
                    color = Color(0xFFCBD5E0)
                )
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (device.isOnline) {
                    Text(
                        text = "${device.signalStrength}%",
                        fontSize = 10.sp,
                        color = getHealthColor(device.signalStrength)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            if (device.isOnline) Color.Green else Color.Red,
                            CircleShape
                        )
                )
            }
        }
    }
}

@Composable
fun SettingsTab(isListening: Boolean, isOfflineMode: Boolean) {
    var alertSoundEnabled by remember { mutableStateOf(true) }
    var vibrationEnabled by remember { mutableStateOf(true) }
    var autoStartEnabled by remember { mutableStateOf(false) }
    var meshNetworkEnabled by remember { mutableStateOf(true) }
    var notificationEnabled by remember { mutableStateOf(true) }
    var batteryOptimizationEnabled by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "⚙️ Settings",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        
        item {
            SettingCard(
                title = "Alert Sound",
                description = "Enable/disable alert sounds",
                isEnabled = alertSoundEnabled,
                onToggle = { 
                    alertSoundEnabled = it
                    Toast.makeText(context, "Alert sound ${if (it) "enabled" else "disabled"}", Toast.LENGTH_SHORT).show()
                }
            )
        }
        
        item {
            SettingCard(
                title = "Vibration",
                description = "Enable/disable vibration alerts",
                isEnabled = vibrationEnabled,
                onToggle = { 
                    vibrationEnabled = it
                    Toast.makeText(context, "Vibration ${if (it) "enabled" else "disabled"}", Toast.LENGTH_SHORT).show()
                }
            )
        }
        
        item {
            SettingCard(
                title = "Push Notifications",
                description = "Enable/disable push notifications",
                isEnabled = notificationEnabled,
                onToggle = { 
                    notificationEnabled = it
                    Toast.makeText(context, "Notifications ${if (it) "enabled" else "disabled"}", Toast.LENGTH_SHORT).show()
                }
            )
        }
        
        item {
            SettingCard(
                title = "Auto-start",
                description = "Start listening on app launch",
                isEnabled = autoStartEnabled,
                onToggle = { 
                    autoStartEnabled = it
                    Toast.makeText(context, "Auto-start ${if (it) "enabled" else "disabled"}", Toast.LENGTH_SHORT).show()
                }
            )
        }
        
        item {
            SettingCard(
                title = "Mesh Network",
                description = "Enable mesh networking",
                isEnabled = meshNetworkEnabled,
                onToggle = { 
                    meshNetworkEnabled = it
                    Toast.makeText(context, "Mesh network ${if (it) "enabled" else "disabled"}", Toast.LENGTH_SHORT).show()
                }
            )
        }
        
        item {
            SettingCard(
                title = "Battery Optimization",
                description = "Optimize battery usage",
                isEnabled = batteryOptimizationEnabled,
                onToggle = { 
                    batteryOptimizationEnabled = it
                    Toast.makeText(context, "Battery optimization ${if (it) "enabled" else "disabled"}", Toast.LENGTH_SHORT).show()
                }
            )
        }
        
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2D3748))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "📱 Device Information",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Text(
                        text = "Device ID: ${android.os.Build.MODEL}",
                        fontSize = 12.sp,
                        color = Color(0xFFCBD5E0)
                    )
                    
                    Text(
                        text = "Android Version: ${android.os.Build.VERSION.RELEASE}",
                        fontSize = 12.sp,
                        color = Color(0xFFCBD5E0)
                    )
                    
                    Text(
                        text = "App Version: 1.0.0-debug",
                        fontSize = 12.sp,
                        color = Color(0xFFCBD5E0)
                    )
                }
            }
        }
        
        item {
            Button(
                onClick = {
                    Toast.makeText(context, "Settings saved successfully!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF38A169)
                )
            ) {
                Text("💾 Save Settings")
            }
        }
    }
}

@Composable
fun SettingCard(
    title: String,
    description: String,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2D3748)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = Color(0xFFCBD5E0)
                )
            }
            
            Switch(
                checked = isEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF38A169),
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Color(0xFFE53E3E)
                )
            )
        }
    }
}

// Helper functions
fun generateSampleDevices(): List<NetworkDevice> {
    val deviceTypes = listOf("Android Phone", "iPhone", "Tablet", "Router")
    val deviceNames = listOf("Device-001", "Device-002", "Device-003", "Device-004", "Device-005")
    
    return deviceNames.mapIndexed { index, name ->
        NetworkDevice(
            id = "device_$index",
            name = name,
            type = deviceTypes.random(),
            isOnline = (1..10).random() > 2, // 80% online chance
            signalStrength = (40..95).random(),
            lastSeen = System.currentTimeMillis() - (1..3600000).random()
        )
    }
}

fun generateSampleMeshAlerts(): List<MeshAlert> {
    val alertTypes = listOf("လေယာဉ်", "တိုက်ခိုက်မှု", "ရွှေ့ပြောင်းရန်", "ဘေးကင်းပြီ")
    val priorities = listOf("အရေးကြီး", "မြင့်", "အလယ်အလတ်", "နိမ့်")
    val sources = listOf("Mesh Network", "Online Server", "Local Device", "Relay Node")
    
    return (1..5).map { i ->
        MeshAlert(
            id = "alert_$i",
            message = "Sample alert message for testing mesh network functionality",
            type = alertTypes.random(),
            priority = priorities.random(),
            timestamp = System.currentTimeMillis() - (1..86400000).random(),
            source = sources.random(),
            isOffline = (1..10).random() > 7 // 30% offline chance
        )
    }
}

fun getHealthColor(health: Int): Color {
    return when {
        health >= 80 -> Color(0xFF38A169)
        health >= 60 -> Color(0xFFECC94B)
        health >= 40 -> Color(0xFFED8936)
        else -> Color(0xFFE53E3E)
    }
}

fun formatMeshTime(timestamp: Long): String {
    val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    return formatter.format(Date(timestamp))
}