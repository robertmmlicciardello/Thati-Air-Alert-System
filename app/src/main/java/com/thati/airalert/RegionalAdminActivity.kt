@file:OptIn(ExperimentalMaterial3Api::class)

package com.thati.airalert

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thati.airalert.ui.theme.ThatiAirAlertTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Regional Admin Activity - ဒေသခံ အက်ဒမင် ထိန်းချုပ်မှု
 */
class RegionalAdminActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val region = intent.getStringExtra("region") ?: "Unknown Region"
        
        setContent {
            ThatiAirAlertTheme {
                RegionalAdminScreen(
                    region = region,
                    onLogout = { finish() }
                )
            }
        }
    }
}

data class LocalAlert(
    val id: String,
    val timestamp: Long,
    val type: AlertCategory,
    val message: String,
    val location: String,
    val isActive: Boolean,
    val userReports: Int
)

data class ConnectedUser(
    val id: String,
    val deviceName: String,
    val lastSeen: Long,
    val signalStrength: Int,
    val isOnline: Boolean
)

@Composable
fun RegionalAdminScreen(
    region: String,
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    var localAlerts by remember { mutableStateOf(generateLocalAlerts()) }
    var connectedUsers by remember { mutableStateOf(generateConnectedUsers()) }
    var isOnlineMode by remember { mutableStateOf(true) }
    var networkStatus by remember { mutableStateOf("ကောင်း") }
    
    val scope = rememberCoroutineScope()
    
    // Simulate network status changes
    LaunchedEffect(Unit) {
        while (true) {
            delay(15000)
            networkStatus = listOf("ကောင်း", "အလယ်အလတ်", "မကောင်း").random()
            isOnlineMode = networkStatus != "မကောင်း"
            connectedUsers = generateConnectedUsers()
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF2D3748),
                        Color(0xFF4A5568)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            RegionalAdminHeader(
                region = region,
                isOnlineMode = isOnlineMode,
                networkStatus = networkStatus,
                connectedUsers = connectedUsers.count { it.isOnline },
                onLogout = onLogout
            )
            
            // Tab Navigation
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFF4A5568),
                contentColor = Color.White
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("📢 Alert ပို့ရန်") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("📋 မှတ်တမ်း") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("👥 Users") }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("⚙️ Settings") }
                )
            }
            
            // Content
            when (selectedTab) {
                0 -> AlertSendingTab(
                    region = region,
                    isOnlineMode = isOnlineMode,
                    onSendAlert = { alert ->
                        localAlerts = localAlerts + alert
                    }
                )
                1 -> AlertHistoryTab(localAlerts)
                2 -> ConnectedUsersTab(connectedUsers)
                3 -> SettingsTab(region, isOnlineMode, networkStatus)
            }
        }
    }
}

@Composable
fun RegionalAdminHeader(
    region: String,
    isOnlineMode: Boolean,
    networkStatus: String,
    connectedUsers: Int,
    onLogout: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A202C)),
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
                        text = "🏛️ $region",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "ဒေသခံ အက်ဒမင် ထိန်းချုပ်မှု",
                        fontSize = 12.sp,
                        color = Color(0xFFE2E8F0)
                    )
                }
                
                IconButton(onClick = onLogout) {
                    Icon(Icons.Default.ExitToApp, contentDescription = "Logout", tint = Color.White)
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Status indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatusIndicator(
                    label = "Mode",
                    value = if (isOnlineMode) "Online" else "Offline",
                    color = if (isOnlineMode) Color.Green else Color.Red
                )
                
                StatusIndicator(
                    label = "Network",
                    value = networkStatus,
                    color = getNetworkColor(networkStatus)
                )
                
                StatusIndicator(
                    label = "Users",
                    value = connectedUsers.toString(),
                    color = Color(0xFF3182CE)
                )
            }
        }
    }
}

@Composable
fun StatusIndicator(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
fun AlertSendingTab(
    region: String,
    isOnlineMode: Boolean,
    onSendAlert: (LocalAlert) -> Unit
) {
    var selectedCategory by remember { mutableStateOf(alertCategories[0]) }
    var customMessage by remember { mutableStateOf("") }
    var selectedLocation by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    
    val locations = listOf(
        "မြို့လယ်", "မြို့ရိုး", "ဈေးကြီး", "ဘူတာရုံ", "လေဆိပ်", 
        "ရဲစခန်း", "ဆေးရုံ", "ကျောင်း", "ရုံးများ", "လူနေရပ်ကွက်"
    )
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Mode indicator
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isOnlineMode) Color(0xFF1A365D) else Color(0xFF742A2A)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isOnlineMode) "🌐" else "📡",
                        fontSize = 20.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isOnlineMode) 
                            "🌐 Online Mode - နိုင်ငံအဆင့်နှင့် ချိတ်ဆက်ထားသည်" 
                            else "📡 Offline Mode - ဒေသတွင်းသာ အလုပ်လုပ်သည်",
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }
            }
        }
        
        item {
            Text(
                text = "📢 Alert ပို့ရန်",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        
        item {
            // Alert category selection
            Text(
                text = "Alert အမျိုးအစား:",
                fontSize = 14.sp,
                color = Color.White,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            LazyColumn(
                modifier = Modifier.height(200.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(alertCategories) { category ->
                    AlertCategoryCard(
                        category = category,
                        isSelected = selectedCategory == category,
                        onSelect = { selectedCategory = category }
                    )
                }
            }
        }
        
        item {
            // Location selection
            Text(
                text = "တည်နေရာ:",
                fontSize = 14.sp,
                color = Color.White,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            LazyColumn(
                modifier = Modifier.height(120.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(locations) { location ->
                    OutlinedButton(
                        onClick = { selectedLocation = location },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (selectedLocation == location) 
                                selectedCategory.color.copy(alpha = 0.2f) else Color.Transparent,
                            contentColor = Color.White
                        )
                    ) {
                        Text(text = location, fontSize = 12.sp)
                    }
                }
            }
        }
        
        item {
            // Custom message
            OutlinedTextField(
                value = customMessage,
                onValueChange = { customMessage = it },
                label = { Text("အပိုစာသား (ရွေးချယ်ခွင့်)", color = Color.White) },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = selectedCategory.color,
                    focusedLabelColor = selectedCategory.color,
                    unfocusedBorderColor = Color.White,
                    unfocusedLabelColor = Color.White,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )
        }
        
        item {
            // Send button
            Button(
                onClick = {
                    if (selectedLocation.isNotBlank()) {
                        isSending = true
                        scope.launch {
                            delay(2000)
                            
                            val alert = LocalAlert(
                                id = "alert_${System.currentTimeMillis()}",
                                timestamp = System.currentTimeMillis(),
                                type = selectedCategory,
                                message = if (customMessage.isNotBlank()) 
                                    "${selectedCategory.name} - $customMessage" 
                                    else selectedCategory.name,
                                location = "$selectedLocation, $region",
                                isActive = true,
                                userReports = 0
                            )
                            
                            onSendAlert(alert)
                            customMessage = ""
                            selectedLocation = ""
                            isSending = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSending && selectedLocation.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = selectedCategory.color)
            ) {
                if (isSending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = if (isSending) "ပို့နေသည်..." else "${selectedCategory.icon} Alert ပို့ရန်",
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        item {
            // Coverage info
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0x44000000))
            ) {
                Text(
                    text = if (isOnlineMode) 
                        "📡 Alert သည် ဒေသတွင်းနှင့် နိုင်ငံအဆင့် နှစ်ခုလုံးသို့ ပေးပို့မည်" 
                        else "📱 Alert သည် ဒေသတွင်း Mesh Network မှတစ်ဆင့်သာ ပေးပို့မည်",
                    color = Color.White,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}

@Composable
fun AlertHistoryTab(alerts: List<LocalAlert>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "📋 Alert မှတ်တမ်း",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        
        items(alerts.sortedByDescending { it.timestamp }) { alert ->
            LocalAlertCard(alert)
        }
    }
}

@Composable
fun LocalAlertCard(alert: LocalAlert) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (alert.isActive) 
                alert.type.color.copy(alpha = 0.2f) else Color(0xFF4A5568)
        )
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
                    text = "${alert.type.icon} ${alert.type.name}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                if (alert.isActive) {
                    Text(
                        text = "🔴 Active",
                        fontSize = 10.sp,
                        color = Color(0xFFE53E3E),
                        modifier = Modifier
                            .background(
                                Color.White.copy(alpha = 0.2f),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
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
                    text = "📍 ${alert.location}",
                    fontSize = 10.sp,
                    color = Color(0xFFCBD5E0)
                )
                
                Text(
                    text = formatTime(alert.timestamp),
                    fontSize = 10.sp,
                    color = Color(0xFFCBD5E0)
                )
            }
            
            if (alert.userReports > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "👥 ${alert.userReports} user reports",
                    fontSize = 10.sp,
                    color = Color(0xFF3182CE)
                )
            }
        }
    }
}

@Composable
fun ConnectedUsersTab(users: List<ConnectedUser>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "👥 ချိတ်ဆက်ထားသော Users",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        
        items(users) { user ->
            ConnectedUserCard(user)
        }
    }
}

@Composable
fun ConnectedUserCard(user: ConnectedUser) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (user.isOnline) Color(0xFF2D3748) else Color(0xFF4A5568)
        )
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
                    text = user.deviceName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
                Text(
                    text = if (user.isOnline) "Online" else "Last seen: ${formatTime(user.lastSeen)}",
                    fontSize = 11.sp,
                    color = Color(0xFFCBD5E0)
                )
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (user.isOnline) {
                    Text(
                        text = "${user.signalStrength}%",
                        fontSize = 12.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(
                            if (user.isOnline) Color.Green else Color.Red,
                            shape = androidx.compose.foundation.shape.CircleShape
                        )
                )
            }
        }
    }
}

@Composable
fun SettingsTab(region: String, isOnlineMode: Boolean, networkStatus: String) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "⚙️ Settings",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2D3748))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "🏛️ ဒေသ အချက်အလက်",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    SettingItem("ဒေသ", region)
                    SettingItem("Mode", if (isOnlineMode) "Online" else "Offline")
                    SettingItem("Network Status", networkStatus)
                    SettingItem("Last Sync", formatTime(System.currentTimeMillis()))
                }
            }
        }
        
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2D3748))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "🔧 System Settings",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Auto Sync", color = Color.White)
                        Switch(
                            checked = isOnlineMode,
                            onCheckedChange = { /* TODO */ }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Sound Alerts", color = Color.White)
                        Switch(
                            checked = true,
                            onCheckedChange = { /* TODO */ }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = Color(0xFFCBD5E0),
            fontSize = 12.sp
        )
        Text(
            text = value,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// Helper functions
fun generateLocalAlerts(): List<LocalAlert> {
    return listOf(
        LocalAlert(
            "1", System.currentTimeMillis() - 1800000, alertCategories[0],
            "လေယာဉ် သတိပေးချက်", "မြို့လယ်", true, 3
        ),
        LocalAlert(
            "2", System.currentTimeMillis() - 3600000, alertCategories[7],
            "ဘေးကင်းပြီ", "မြို့ရိုး", false, 0
        )
    )
}

fun generateConnectedUsers(): List<ConnectedUser> {
    val deviceNames = listOf(
        "User Device 1", "Mobile User 2", "Tablet User 3", 
        "Emergency Device", "Field Unit 1", "Backup Device"
    )
    
    return deviceNames.map { name ->
        ConnectedUser(
            id = name.replace(" ", "_").lowercase(),
            deviceName = name,
            lastSeen = System.currentTimeMillis() - (1..3600000).random(),
            signalStrength = (30..95).random(),
            isOnline = (1..10).random() > 3
        )
    }
}