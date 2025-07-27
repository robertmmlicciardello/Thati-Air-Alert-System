package com.thati.airalert

import android.content.Intent
import android.os.Bundle
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
import androidx.lifecycle.lifecycleScope
import com.thati.airalert.ui.theme.ThatiAirAlertTheme
import com.thati.airalert.services.AlertService
import com.thati.airalert.models.AlertMessage
import com.thati.airalert.database.AlertDatabase
import com.thati.airalert.utils.PreferenceManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Enhanced User Activity - Offline Mode Ready
 * User Mode အတွက် main screen with offline capabilities
 */
class UserActivity : ComponentActivity() {
    
    private lateinit var database: AlertDatabase
    private lateinit var preferenceManager: PreferenceManager
    
    // State variables for offline mode
    private var isServiceRunning by mutableStateOf(false)
    private var receivedAlerts by mutableStateOf<List<AlertMessage>>(emptyList())
    private var connectedDevices by mutableStateOf(0)
    private var networkStatus by mutableStateOf("Offline Mode")
    private var batteryLevel by mutableStateOf(85)
    private var meshSignalStrength by mutableStateOf(0)
    private var isOfflineMode by mutableStateOf(true)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize for offline mode
        initializeOfflineMode()
        
        setContent {
            ThatiAirAlertTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    OfflineUserScreen(
                        onBackPressed = { finish() },
                        isServiceRunning = isServiceRunning,
                        receivedAlerts = receivedAlerts,
                        connectedDevices = connectedDevices,
                        networkStatus = networkStatus,
                        batteryLevel = batteryLevel,
                        isOfflineMode = isOfflineMode,
                        onRefreshAlerts = { loadOfflineAlerts() },
                        onClearAlerts = { clearAllAlerts() },
                        onTestAlert = { simulateTestAlert() }
                    )
                }
            }
        }
    }
    
    private fun initializeOfflineMode() {
        try {
            database = AlertDatabase.getDatabase(this)
            preferenceManager = PreferenceManager(this)
            
            // Start offline simulation
            startOfflineSimulation()
            loadOfflineAlerts()
            
        } catch (e: Exception) {
            // Pure offline mode fallback
            isOfflineMode = true
            networkStatus = "Pure Offline Mode"
        }
    }
    
    private fun startOfflineSimulation() {
        lifecycleScope.launch {
            delay(2000)
            isServiceRunning = true
            connectedDevices = 3 // Simulated nearby devices
            meshSignalStrength = 75
            networkStatus = "Mesh Network Active"
            
            // Simulate periodic network updates
            while (true) {
                delay(5000)
                meshSignalStrength = (60..90).random()
                connectedDevices = (2..5).random()
                batteryLevel = (70..95).random()
            }
        }
    }
    
    private fun simulateTestAlert() {
        val testAlert = AlertMessage(
            id = System.currentTimeMillis().toString(),
            message = "ဒီဟာ test alert တစ်ခုပါ။ Mesh network မှတဆင့် လက်ခံရရှိပါသည်။",
            type = "test",
            timestamp = System.currentTimeMillis(),
            sender = "Test Device",
            priority = "normal",
            location = "Yangon"
        )
        receivedAlerts = listOf(testAlert) + receivedAlerts
    }
    
    private fun loadOfflineAlerts() {
        // Load cached alerts or create demo alerts
        val demoAlerts = listOf(
            AlertMessage(
                id = "1",
                message = "လေကြောင်းသတိပေးချက် - ရန်ကုန်တိုင်းဒေသကြီးတွင် လေယာဉ်များ တွေ့ရှိရပါသည်။",
                type = "aircraft",
                timestamp = System.currentTimeMillis() - 3600000,
                sender = "Yangon Station",
                priority = "high",
                location = "Yangon"
            ),
            AlertMessage(
                id = "2", 
                message = "ဘေးကင်းပြီ - လေကြောင်းအခြေအနေ ပုံမှန်ပြန်လည်ဖြစ်လာပါပြီ။",
                type = "all_clear",
                timestamp = System.currentTimeMillis() - 1800000,
                sender = "Central Command",
                priority = "normal",
                location = "Myanmar"
            )
        )
        receivedAlerts = demoAlerts
    }
    
    private fun clearAllAlerts() {
        receivedAlerts = emptyList()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Cleanup if needed
    }
}

/**
 * Offline User Screen UI
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflineUserScreen(
    onBackPressed: () -> Unit,
    isServiceRunning: Boolean,
    receivedAlerts: List<AlertMessage>,
    connectedDevices: Int,
    networkStatus: String,
    batteryLevel: Int,
    isOfflineMode: Boolean,
    onRefreshAlerts: () -> Unit,
    onClearAlerts: () -> Unit,
    onTestAlert: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header with offline indicator
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBackPressed) {
                Text("← ပြန်သွားရန်")
            }
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "👤 User Mode",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF38A169)
                )
                if (isOfflineMode) {
                    Text(
                        text = "📱 Offline Mode",
                        fontSize = 12.sp,
                        color = Color(0xFF718096)
                    )
                }
            }
            
            // Status indicator
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (isServiceRunning) "🟢" else "🔴",
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isServiceRunning) "Active" else "Inactive",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
        
        // Offline Status Card
        if (isOfflineMode) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3CD))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "📡", fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Mesh Network Mode",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF856404)
                        )
                        Text(
                            text = "အင်တာနက်မလိုအပ်ဘဲ အနီးအနားရှိ devices များနှင့် ချိတ်ဆက်ထားသည်",
                            fontSize = 12.sp,
                            color = Color(0xFF856404)
                        )
                    }
                }
            }
        }
        
        // Enhanced Security Dashboard
        EnhancedSecurityDashboard(
            isServiceRunning = isServiceRunning,
            networkStatus = networkStatus,
            connectedDevices = connectedDevices,
            batteryLevel = batteryLevel
        )
        
        // Alert statistics
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF7FAFC))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${receivedAlerts.size}",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2D3748)
                    )
                    Text(
                        text = "လက်ခံရရှိသော Alert",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF7FAFC))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "$connectedDevices",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2D3748)
                    )
                    Text(
                        text = "ချိတ်ဆက်ထားသော Device",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        
        // User Alert Input Section
        UserAlertInputSection()
        
        // Action buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onRefreshAlerts,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3182CE))
            ) {
                Text("🔄 Refresh", fontSize = 14.sp)
            }
            
            Button(
                onClick = onTestAlert,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38A169))
            ) {
                Text("🧪 Test Alert", fontSize = 14.sp)
            }
            
            Button(
                onClick = onClearAlerts,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53E3E))
            ) {
                Text("🗑️ Clear", fontSize = 14.sp)
            }
        }
        
        // Alerts list header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "လက်ခံရရှိသော သတိပေးချက်များ",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            
            if (receivedAlerts.isNotEmpty()) {
                Text(
                    text = "${receivedAlerts.size} items",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
        
        // Alerts list
        if (receivedAlerts.isEmpty()) {
            // Empty state
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF7FAFC))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "📭",
                        fontSize = 48.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "သတိပေးချက် မရှိသေးပါ",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Alert များ လက်ခံရရှိသည့်အခါ ဤနေရာတွင် ပြသမည်ဖြစ်သည်",
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        } else {
            // Alerts list
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(receivedAlerts.sortedByDescending { it.timestamp }) { alert ->
                    AlertCard(alert = alert)
                }
            }
        }
    }
}

/**
 * Individual Alert Card
 */
@Composable
fun AlertCard(alert: AlertMessage) {
    val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    val priorityColor = when (alert.priority) {
        "critical" -> Color(0xFFE53E3E)
        "high" -> Color(0xFFED8936)
        "medium" -> Color(0xFFECC94B)
        else -> Color(0xFF38A169)
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = priorityColor.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with priority and time
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Priority badge
                Surface(
                    color = priorityColor,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = alert.priority.uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
                
                Text(
                    text = dateFormat.format(Date(alert.timestamp)),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            
            // Alert message
            Text(
                text = alert.message,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // Footer with sender and type
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "From: ${alert.sender}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                
                Text(
                    text = "Type: ${alert.type}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}

/**
 * User Alert Input Section - အသုံးပြုသူကိုယ်တိုင် Alert ပို့ရန်
 */
@Composable
fun UserAlertInputSection() {
    var userAlertMessage by remember { mutableStateOf("") }
    var isSendingAlert by remember { mutableStateOf(false) }
    var selectedAlertType by remember { mutableStateOf("လေယာဉ်တွေ့ရှိ") }
    
    val quickAlertTypes = listOf(
        "✈️ လေယာဉ်တွေ့ရှိ",
        "🚁 ရဟတ်ယာဉ်တွေ့ရှိ", 
        "💥 တိုက်ခိုက်မှုကြား",
        "🔥 မီးလောင်မှု",
        "🚨 အရေးပေါ်အခြေအနေ",
        "✅ ဘေးကင်းပြီ"
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FFF4))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "📢 သတိပေးချက် ပို့ရန်",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF38A169),
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            // Quick alert type selection
            Text(
                text = "အမြန်ရွေးချယ်ရန်:",
                fontSize = 12.sp,
                color = Color(0xFF4A5568),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            LazyColumn(
                modifier = Modifier.height(120.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(quickAlertTypes) { alertType ->
                    OutlinedButton(
                        onClick = { 
                            selectedAlertType = alertType
                            userAlertMessage = alertType
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (selectedAlertType == alertType) 
                                Color(0xFF38A169).copy(alpha = 0.1f) else Color.Transparent
                        )
                    ) {
                        Text(
                            text = alertType,
                            fontSize = 14.sp,
                            color = Color(0xFF2D3748)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Custom message input
            OutlinedTextField(
                value = userAlertMessage,
                onValueChange = { userAlertMessage = it },
                label = { Text("စာသားထည့်ရန် (ရွေးချယ်ခွင့်)") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 2,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF38A169),
                    focusedLabelColor = Color(0xFF38A169)
                )
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Send button
            Button(
                onClick = {
                    if (userAlertMessage.isNotBlank()) {
                        isSendingAlert = true
                        // Simulate sending
                        kotlinx.coroutines.GlobalScope.launch {
                            kotlinx.coroutines.delay(1500)
                            isSendingAlert = false
                            userAlertMessage = ""
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = userAlertMessage.isNotBlank() && !isSendingAlert,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38A169))
            ) {
                if (isSendingAlert) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = if (isSendingAlert) "ပို့နေသည်..." else "📤 Alert ပို့ရန်",
                    fontWeight = FontWeight.Bold
                )
            }
            
            Text(
                text = "💡 Mesh network မှတဆင့် အနီးအနားရှိ devices များထံ ပေးပို့မည်",
                fontSize = 11.sp,
                color = Color(0xFF4A5568),
                modifier = Modifier.padding(top = 8.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Enhanced Security Dashboard - လုံခြုံရေး ထိန်းချုပ်မှု Dashboard
 */
@Composable
fun EnhancedSecurityDashboard(
    isServiceRunning: Boolean,
    networkStatus: String,
    connectedDevices: Int,
    batteryLevel: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isServiceRunning) Color(0xFF1A365D) else Color(0xFF742A2A)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "🛡️ လုံခြုံရေး အခြေအနေ",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = if (isServiceRunning) "စနစ် အလုပ်လုပ်နေသည်" else "စနစ် ရပ်နားနေသည်",
                        fontSize = 12.sp,
                        color = Color(0xFFE2E8F0)
                    )
                }
                
                // Status indicator
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(
                            if (isServiceRunning) Color.Green else Color.Red,
                            shape = CircleShape
                        )
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Security metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SecurityMetric(
                    icon = "📡",
                    label = "Network",
                    value = networkStatus,
                    color = getNetworkStatusColor(networkStatus)
                )
                
                SecurityMetric(
                    icon = "👥",
                    label = "Devices",
                    value = connectedDevices.toString(),
                    color = if (connectedDevices > 0) Color.Green else Color.Yellow
                )
                
                SecurityMetric(
                    icon = "🔋",
                    label = "Battery",
                    value = "${batteryLevel}%",
                    color = getBatteryColor(batteryLevel)
                )
                
                SecurityMetric(
                    icon = "🔐",
                    label = "Encryption",
                    value = "Active",
                    color = Color.Green
                )
            }
        }
    }
}

@Composable
fun SecurityMetric(
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
            fontSize = 20.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = Color(0xFFCBD5E0)
        )
    }
}

// Helper functions
fun getNetworkStatusColor(status: String): Color {
    return when (status) {
        "Mesh Network Active" -> Color.Green
        "Connected" -> Color.Green
        "Connecting" -> Color.Yellow
        else -> Color.Red
    }
}

fun getBatteryColor(level: Int): Color {
    return when {
        level > 60 -> Color.Green
        level > 30 -> Color.Yellow
        else -> Color.Red
    }
}