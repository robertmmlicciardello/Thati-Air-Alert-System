@file:OptIn(ExperimentalMaterial3Api::class)

package com.thati.airalert

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thati.airalert.ui.theme.ThatiAirAlertTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import com.thati.airalert.utils.AlertBroadcastManager

/**
 * Regional Admin Activity - ဒေသဆိုင်ရာ Admin Panel
 */
class RegionalAdminActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val region = intent.getStringExtra("region") ?: "Unknown Region"
        
        setContent {
            ThatiAirAlertTheme {
                RegionalAdminScreen(
                    region = region,
                    onLogout = { 
                        clearLoginSession()
                        startActivity(Intent(this, SimpleMainActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }
    
    private fun clearLoginSession() {
        val sharedPref = getSharedPreferences("thati_login", MODE_PRIVATE)
        sharedPref.edit().clear().apply()
    }
}

data class RegionalUser(
    val id: String,
    val name: String,
    val phone: String,
    val isActive: Boolean,
    val lastSeen: Long,
    val deviceType: String
)

data class RegionalAlert(
    val id: String,
    val message: String,
    val type: String,
    val priority: String,
    val timestamp: Long,
    val sentCount: Int,
    val deliveredCount: Int
)

@Composable
fun RegionalAdminScreen(
    region: String,
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    var users by remember { mutableStateOf(emptyList<RegionalUser>()) }
    var alerts by remember { mutableStateOf(emptyList<RegionalAlert>()) }
    var isRefreshing by remember { mutableStateOf(false) }
    var showAddUserDialog by remember { mutableStateOf(false) }
    var showSendAlertDialog by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
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
            RegionalAdminHeader(
                region = region,
                onRefresh = {
                    isRefreshing = true
                    scope.launch {
                        delay(2000)
                        // Refresh users from database or keep current users
                        // Refresh alerts from database or keep current alerts
                        isRefreshing = false
                        Toast.makeText(context, "Data refreshed", Toast.LENGTH_SHORT).show()
                    }
                },
                onLogout = onLogout,
                isRefreshing = isRefreshing
            )
            
            // Tab Navigation
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFF2D3748),
                contentColor = Color.White
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("🚨 Send Alert") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("👥 Users (${users.size})") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("📋 History") }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("📊 Stats") }
                )
            }
            
            // Content
            when (selectedTab) {
                0 -> SendAlertTab(
                    region = region,
                    userCount = users.count { it.isActive },
                    onSendAlert = { message, type, priority ->
                        // Send alert through broadcast manager
                        AlertBroadcastManager.sendAlert(
                            context = context,
                            message = message,
                            type = type,
                            priority = priority,
                            region = region
                        )
                        
                        val newAlert = RegionalAlert(
                            id = "alert_${System.currentTimeMillis()}",
                            message = message,
                            type = type,
                            priority = priority,
                            timestamp = System.currentTimeMillis(),
                            sentCount = users.count { it.isActive },
                            deliveredCount = users.count { it.isActive } // Simulate delivery
                        )
                        alerts = listOf(newAlert) + alerts
                        Toast.makeText(context, "🚨 Alert sent to ${newAlert.sentCount} users!", Toast.LENGTH_LONG).show()
                    }
                )
                1 -> UsersTab(
                    users = users,
                    onAddUser = { showAddUserDialog = true },
                    onToggleUser = { userId ->
                        users = users.map { user ->
                            if (user.id == userId) {
                                user.copy(isActive = !user.isActive)
                            } else user
                        }
                        Toast.makeText(context, "User status updated", Toast.LENGTH_SHORT).show()
                    },
                    onDeleteUser = { userId ->
                        users = users.filter { it.id != userId }
                        Toast.makeText(context, "User removed", Toast.LENGTH_SHORT).show()
                    }
                )
                2 -> AlertHistoryTab(alerts)
                3 -> RegionalStatsTab(users, alerts, region)
            }
        }
        
        // Add User Dialog
        if (showAddUserDialog) {
            AddUserDialog(
                onDismiss = { showAddUserDialog = false },
                onAddUser = { name, phone ->
                    val newUser = RegionalUser(
                        id = "user_${System.currentTimeMillis()}",
                        name = name,
                        phone = phone,
                        isActive = true,
                        lastSeen = System.currentTimeMillis(),
                        deviceType = "Android"
                    )
                    users = users + newUser
                    showAddUserDialog = false
                    Toast.makeText(context, "User added successfully", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

@Composable
fun RegionalAdminHeader(
    region: String,
    onRefresh: () -> Unit,
    onLogout: () -> Unit,
    isRefreshing: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF4A5568)),
        shape = RoundedCornerShape(0.dp)
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
                    text = "🏛️ Regional Admin",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = region,
                    fontSize = 14.sp,
                    color = Color(0xFFE2E8F0)
                )
            }
            
            Row {
                IconButton(onClick = onRefresh, enabled = !isRefreshing) {
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.White)
                    }
                }
                
                IconButton(onClick = onLogout) {
                    Icon(Icons.Default.ExitToApp, contentDescription = "Logout", tint = Color.White)
                }
            }
        }
    }
}

@Composable
fun SendAlertTab(
    region: String,
    userCount: Int,
    onSendAlert: (String, String, String) -> Unit
) {
    val context = LocalContext.current
    var alertMessage by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("လေယာဉ်") }
    var selectedPriority by remember { mutableStateOf("မြင့်") }
    var isSending by remember { mutableStateOf(false) }
    
    val alertTypes = listOf("လေယာဉ်", "တိုက်ခိုက်မှု", "ရွှေ့ပြောင်းရန်", "ဘေးကင်းပြီ", "စမ်းသပ်ချက်")
    val priorities = listOf("အရေးကြီး", "မြင့်", "အလယ်အလတ်", "နိမ့်")
    
    val scope = rememberCoroutineScope()
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2D3748))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "🚨 Send Alert to $region",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    Text(
                        text = "Active Users: $userCount",
                        fontSize = 14.sp,
                        color = Color(0xFF68D391),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    // Alert Type Selection
                    Text(
                        text = "Alert Type:",
                        fontSize = 14.sp,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .padding(bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(alertTypes) { type ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selectedType == type) 
                                        Color(0xFF3182CE) 
                                    else 
                                        Color(0xFF4A5568)
                                ),
                                onClick = { selectedType = type }
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = type,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                    
                    // Priority Selection
                    Text(
                        text = "Priority:",
                        fontSize = 14.sp,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        priorities.forEach { priority ->
                            val priorityColor = when (priority) {
                                "အရေးကြီး" -> Color(0xFFE53E3E)
                                "မြင့်" -> Color(0xFFED8936)
                                "အလယ်အလတ်" -> Color(0xFFECC94B)
                                else -> Color(0xFF38A169)
                            }
                            
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selectedPriority == priority) 
                                        priorityColor 
                                    else 
                                        Color(0xFF4A5568)
                                ),
                                onClick = { selectedPriority = priority }
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = priority,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                    
                    // Message Input
                    OutlinedTextField(
                        value = alertMessage,
                        onValueChange = { alertMessage = it },
                        label = { Text("Alert Message") },
                        placeholder = { Text("Enter alert message...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF3182CE),
                            focusedLabelColor = Color(0xFF3182CE),
                            unfocusedBorderColor = Color(0xFF4A5568),
                            unfocusedLabelColor = Color(0xFFCBD5E0)
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Send Button
                    Button(
                        onClick = {
                            if (alertMessage.isNotBlank()) {
                                isSending = true
                                scope.launch {
                                    delay(2000)
                                    onSendAlert(alertMessage, selectedType, selectedPriority)
                                    alertMessage = ""
                                    isSending = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSending && alertMessage.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE53E3E)
                        )
                    ) {
                        if (isSending) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sending...")
                        } else {
                            Text("🚨 Send Alert to $userCount Users")
                        }
                    }
                }
            }
        }
        
        // Quick Alert Buttons
        item {
            Text(
                text = "⚡ Quick Alerts",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        
        item {
            val quickAlerts = listOf(
                "လေယာဉ် သတိပေးချက် - မြောက်ဘက်မှ ချဉ်းကပ်လာနေ",
                "အရေးပေါ် - ချက်ချင်း ရွှေ့ပြောင်းပါ",
                "ဘေးကင်းပြီ - ပုံမှန်အခြေအနေ ပြန်လည်ရောက်ရှိ",
                "စမ်းသပ်ချက် - Alert စနစ် စစ်ဆေးမှု"
            )
            
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickAlerts.forEach { message ->
                    OutlinedButton(
                        onClick = {
                            isSending = true
                            scope.launch {
                                delay(1500)
                                // Send quick alert
                                AlertBroadcastManager.sendAlert(
                                    context = context,
                                    message = message,
                                    type = "အမြန်",
                                    priority = "မြင့်",
                                    region = region
                                )
                                onSendAlert(message, "အမြန်", "မြင့်")
                                isSending = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSending,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = message,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UsersTab(
    users: List<RegionalUser>,
    onAddUser: () -> Unit,
    onToggleUser: (String) -> Unit,
    onDeleteUser: (String) -> Unit
) {
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
                    text = "👥 User Management",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Button(
                    onClick = onAddUser,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF38A169)
                    )
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add User")
                }
            }
        }
        
        items(users) { user ->
            UserCard(
                user = user,
                onToggle = { onToggleUser(user.id) },
                onDelete = { onDeleteUser(user.id) }
            )
        }
    }
}

@Composable
fun UserCard(
    user: RegionalUser,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (user.isActive) Color(0xFF2D3748) else Color(0xFF4A5568)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = user.phone,
                    fontSize = 12.sp,
                    color = Color(0xFFCBD5E0)
                )
                Text(
                    text = "${user.deviceType} • Last seen: ${formatRegionalTime(user.lastSeen)}",
                    fontSize = 10.sp,
                    color = Color(0xFF9CA3AF)
                )
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Switch(
                    checked = user.isActive,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF38A169),
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color(0xFFE53E3E)
                    )
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color(0xFFE53E3E)
                    )
                }
            }
        }
    }
}

@Composable
fun AlertHistoryTab(alerts: List<RegionalAlert>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "📋 Alert History",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        
        items(alerts) { alert ->
            RegionalAlertCard(alert)
        }
    }
}

@Composable
fun RegionalAlertCard(alert: RegionalAlert) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2D3748))
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${alert.type} Alert",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = alert.priority,
                    fontSize = 12.sp,
                    color = when (alert.priority) {
                        "အရေးကြီး" -> Color(0xFFE53E3E)
                        "မြင့်" -> Color(0xFFED8936)
                        "အလယ်အလတ်" -> Color(0xFFECC94B)
                        else -> Color(0xFF38A169)
                    }
                )
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
                    text = "Sent: ${alert.sentCount} • Delivered: ${alert.deliveredCount}",
                    fontSize = 10.sp,
                    color = Color(0xFFCBD5E0)
                )
                Text(
                    text = formatRegionalTime(alert.timestamp),
                    fontSize = 10.sp,
                    color = Color(0xFFCBD5E0)
                )
            }
        }
    }
}

@Composable
fun RegionalStatsTab(
    users: List<RegionalUser>,
    alerts: List<RegionalAlert>,
    region: String
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "📊 $region Statistics",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                RegionalStatCard(
                    title = "Total Users",
                    value = users.size.toString(),
                    subtitle = "${users.count { it.isActive }} Active",
                    color = Color(0xFF3182CE),
                    modifier = Modifier.weight(1f)
                )
                
                RegionalStatCard(
                    title = "Alerts Today",
                    value = alerts.count { isRegionalToday(it.timestamp) }.toString(),
                    subtitle = "Total Sent",
                    color = Color(0xFFED8936),
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                RegionalStatCard(
                    title = "Delivery Rate",
                    value = "${if (alerts.isNotEmpty()) (alerts.sumOf { it.deliveredCount } * 100 / alerts.sumOf { it.sentCount }) else 0}%",
                    subtitle = "Success Rate",
                    color = Color(0xFF38A169),
                    modifier = Modifier.weight(1f)
                )
                
                RegionalStatCard(
                    title = "Critical Alerts",
                    value = alerts.count { it.priority == "အရေးကြီး" }.toString(),
                    subtitle = "High Priority",
                    color = Color(0xFFE53E3E),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun RegionalStatCard(
    title: String,
    value: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = title,
                fontSize = 12.sp,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = Color(0xFFCBD5E0),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun AddUserDialog(
    onDismiss: () -> Unit,
    onAddUser: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New User") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && phone.isNotBlank()) {
                        onAddUser(name, phone)
                    }
                },
                enabled = name.isNotBlank() && phone.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Helper functions
fun generateRegionalUsers(): List<RegionalUser> {
    val names = listOf("Aung Aung", "Thida", "Kyaw Kyaw", "Mya Mya", "Zaw Zaw", "Nwe Nwe")
    return names.mapIndexed { index, name ->
        RegionalUser(
            id = "user_$index",
            name = name,
            phone = "+9591234567$index",
            isActive = (1..10).random() > 2,
            lastSeen = System.currentTimeMillis() - (1..3600000).random(),
            deviceType = if ((1..2).random() == 1) "Android" else "iPhone"
        )
    }
}

fun generateRegionalAlerts(): List<RegionalAlert> {
    val messages = listOf(
        "လေယာဉ် သတိပေးချက် - မြောက်ဘက်မှ ချဉ်းကပ်လာနေ",
        "အရေးပေါ် - ချက်ချင်း ရွှေ့ပြောင်းပါ",
        "ဘေးကင်းပြီ - ပုံမှန်အခြေအနေ ပြန်လည်ရောက်ရှိ"
    )
    val types = listOf("လေယာဉ်", "အရေးပေါ်", "ဘေးကင်းပြီ")
    val priorities = listOf("အရေးကြီး", "မြင့်", "အလယ်အလတ်")
    
    return (1..5).map { i ->
        val sentCount = (50..200).random()
        RegionalAlert(
            id = "alert_$i",
            message = messages.random(),
            type = types.random(),
            priority = priorities.random(),
            timestamp = System.currentTimeMillis() - (1..86400000).random(),
            sentCount = sentCount,
            deliveredCount = (sentCount * 0.8).toInt() + (1..10).random()
        )
    }
}

fun formatRegionalTime(timestamp: Long): String {
    val formatter = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())
    return formatter.format(Date(timestamp))
}

fun isRegionalToday(timestamp: Long): Boolean {
    val today = Calendar.getInstance()
    val date = Calendar.getInstance().apply { timeInMillis = timestamp }
    return today.get(Calendar.YEAR) == date.get(Calendar.YEAR) &&
           today.get(Calendar.DAY_OF_YEAR) == date.get(Calendar.DAY_OF_YEAR)
}