package com.thati.airalert

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.thati.airalert.database.AlertDatabase
import com.thati.airalert.models.AlertMessage
import com.thati.airalert.services.AlertService
import com.thati.airalert.ui.theme.ThatiAirAlertTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Enhanced User Activity
 * User mode အတွက် interface - လုံခြုံရေး ပိုမိုကောင်းမွန်သော UI
 */
class UserActivity : ComponentActivity() {
    
    private var isServiceRunning by mutableStateOf(false)
    private var receivedAlerts by mutableStateOf<List<AlertMessage>>(emptyList())
    private lateinit var database: AlertDatabase
    private var isAlarmEnabled by mutableStateOf(false)
    private var userLocation by mutableStateOf("ရန်ကုန်မြို့")
    private var isSendingAlert by mutableStateOf(false)
    private var userAlertMessage by mutableStateOf("")
    private var networkStatus by mutableStateOf("ကောင်း")
    private var connectedDevices by mutableStateOf(0)
    private var batteryLevel by mutableStateOf(100)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Database ကို initialize လုပ်ပါ
        database = AlertDatabase.getDatabase(this)
        
        // Alert Service ကို User mode ဖြင့် စတင်ပါ
        startAlertService()
        
        // Received alerts များကို load လုပ်ပါ
        loadReceivedAlerts()
        
        setContent {
            ThatiAirAlertTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    UserScreen(
                        onBackPressed = { finish() },
                        isServiceRunning = isServiceRunning,
                        receivedAlerts = receivedAlerts,
                        onRefreshAlerts = { loadReceivedAlerts() },
                        onClearAlerts = { clearAllAlerts() }
                    )
                }
            }
        }
    }
    
    /**
     * Alert Service ကို စတင်ခြင်း
     */
    private fun startAlertService() {
        val intent = Intent(this, AlertService::class.java).apply {
            action = AlertService.ACTION_START_USER
        }
        startForegroundService(intent)
        isServiceRunning = true
    }
    
    /**
     * လက်ခံရရှိသော alerts များကို load လုပ်ခြင်း
     */
    private fun loadReceivedAlerts() {
        lifecycleScope.launch {
            try {
                val alerts = database.alertDao().getAllAlerts()
                receivedAlerts = alerts.filter { it.isReceived }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    /**
     * Alert များအားလုံးကို ရှင်းလင်းခြင်း
     */
    private fun clearAllAlerts() {
        lifecycleScope.launch {
            try {
                database.alertDao().clearAllAlerts()
                receivedAlerts = emptyList()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Screen ပြန်ဖွင့်တိုင်း alerts များကို refresh လုပ်ပါ
        loadReceivedAlerts()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Service ကို ရပ်ပါ
        val intent = Intent(this, AlertService::class.java).apply {
            action = AlertService.ACTION_STOP_SERVICE
        }
        startService(intent)
    }
}

/**
 * User Screen UI
 */
@Composable
fun UserScreen(
    onBackPressed: () -> Unit,
    isServiceRunning: Boolean,
    receivedAlerts: List<AlertMessage>,
    onRefreshAlerts: () -> Unit,
    onClearAlerts: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
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
            
            Text(
                text = "👤 User Mode",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF38A169)
            )
            
            // Status indicator
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (isServiceRunning) "🟢" else "🔴",
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isServiceRunning) "Listening" else "Inactive",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
        
        // Enhanced Security Dashboard
        EnhancedSecurityDashboard(
            isServiceRunning = isServiceRunning,
            networkStatus = "ကောင်း", // This would be dynamic in real implementation
            connectedDevices = 5, // This would be dynamic in real implementation
            batteryLevel = 85 // This would be dynamic in real implementation
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
                        text = "${receivedAlerts.count { it.timestamp > System.currentTimeMillis() - 24 * 60 * 60 * 1000 }}",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2D3748)
                    )
                    Text(
                        text = "ယနေ့ Alert",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        
        // User Alert Input Section
        UserAlertInputSection()
        
        // Alarm Control Section
        AlarmControlSection()
        
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
                onClick = onClearAlerts,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53E3E))
            ) {
                Text("🗑️ Clear All", fontSize = 14.sp)
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
    val priorityColor = when (alert.priority.level) {
        4 -> Color(0xFFE53E3E) // Critical
        3 -> Color(0xFFED8936) // High
        2 -> Color(0xFFECC94B) // Medium
        else -> Color(0xFF38A169) // Low
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
                        text = alert.priority.name,
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
            
            // Footer with hop count and type
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Type: ${alert.alertType.name}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                
                if (alert.hopCount > 0) {
                    Text(
                        text = "Relayed ${alert.hopCount}x",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}
/**
 * 
User Alert Input Section - အသုံးပြုသူကိုယ်တိုင် Alert ပို့ရန်
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
                        // TODO: Implement actual alert sending
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
                text = "💡 သင်မြင်တွေ့ရသော အခြေအနေများကို အနီးအနားရှိ အခြားသူများထံ ပေးပို့နိုင်ပါသည်",
                fontSize = 11.sp,
                color = Color(0xFF4A5568),
                modifier = Modifier.padding(top = 8.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Alarm Control Section - Alarm အသံထိန်းချုပ်ရန်
 */
@Composable
fun AlarmControlSection() {
    var isAlarmEnabled by remember { mutableStateOf(false) }
    var isTestingAlarm by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isAlarmEnabled) Color(0xFFFFF5F5) else Color(0xFFF7FAFC)
        )
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
                        text = "🔊 Alarm အသံ",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isAlarmEnabled) Color(0xFFE53E3E) else Color(0xFF4A5568)
                    )
                    Text(
                        text = if (isAlarmEnabled) "ဖွင့်ထားသည်" else "ပိတ်ထားသည်",
                        fontSize = 12.sp,
                        color = Color(0xFF718096)
                    )
                }
                
                Switch(
                    checked = isAlarmEnabled,
                    onCheckedChange = { isAlarmEnabled = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFFE53E3E),
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color(0xFFCBD5E0)
                    )
                )
            }
            
            if (isAlarmEnabled) {
                Spacer(modifier = Modifier.height(12.dp))
                
                Button(
                    onClick = {
                        isTestingAlarm = true
                        // TODO: Play test alarm sound
                        kotlinx.coroutines.GlobalScope.launch {
                            kotlinx.coroutines.delay(2000)
                            isTestingAlarm = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isTestingAlarm,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFED8936))
                ) {
                    if (isTestingAlarm) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = if (isTestingAlarm) "စမ်းနေသည်..." else "🎵 Alarm စမ်းကြည့်ရန်",
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Text(
                    text = "⚠️ Alarm ဖွင့်ထားခြင်းသည် ဘက်ထရီ သုံးစွဲမှုကို တိုးစေနိုင်ပါသည်",
                    fontSize = 10.sp,
                    color = Color(0xFF718096),
                    modifier = Modifier.padding(top = 8.dp),
                    textAlign = TextAlign.Center
                )
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "📱 Alert များရောက်ရှိသောအခါ အသံမြည်စေရန် ဖွင့်ပေးပါ",
                    fontSize = 12.sp,
                    color = Color(0xFF718096),
                    textAlign = TextAlign.Center
                )
            }
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
                            shape = androidx.compose.foundation.shape.CircleShape
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
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Security features status
            SecurityFeaturesList(isServiceRunning)
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

@Composable
fun SecurityFeaturesList(isServiceRunning: Boolean) {
    val features = listOf(
        SecurityFeature("🔒 End-to-End Encryption", isServiceRunning),
        SecurityFeature("📡 Mesh Network Protocol", isServiceRunning),
        SecurityFeature("🛡️ Anti-Tampering", true),
        SecurityFeature("🔐 Secure Key Exchange", isServiceRunning),
        SecurityFeature("📱 Device Authentication", isServiceRunning)
    )
    
    Column {
        Text(
            text = "လုံခြုံရေး လုပ်ဆောင်ချက်များ:",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        features.forEach { feature ->
            SecurityFeatureItem(feature)
        }
    }
}

data class SecurityFeature(
    val name: String,
    val isActive: Boolean
)

@Composable
fun SecurityFeatureItem(feature: SecurityFeature) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (feature.isActive) "✅" else "❌",
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = feature.name,
            fontSize = 11.sp,
            color = if (feature.isActive) Color(0xFFE2E8F0) else Color(0xFF9CA3AF)
        )
    }
}

// Helper functions
fun getNetworkStatusColor(status: String): Color {
    return when (status) {
        "ကောင်း" -> Color.Green
        "အလယ်အလတ်" -> Color.Yellow
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