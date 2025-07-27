@file:OptIn(ExperimentalMaterial3Api::class)

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import com.thati.airalert.ui.theme.ThatiAirAlertTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Main Admin Activity - နိုင်ငံအဆင့် ထိန်းချုပ်မှု စနစ်
 */
class MainAdminActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            ThatiAirAlertTheme {
                MainAdminScreen(
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

data class RegionalStatus(
    val region: String,
    val adminName: String,
    val isOnline: Boolean,
    val lastActive: Long,
    val alertCount: Int,
    val userCount: Int,
    val networkHealth: String
)

data class SystemAlert(
    val id: String,
    val timestamp: Long,
    val region: String,
    val type: String,
    val message: String,
    val severity: String,
    val isHandled: Boolean
)

@Composable
fun MainAdminScreen(onLogout: () -> Unit) {
    var selectedTab by remember { mutableStateOf(0) }
    var regionalStatuses by remember { mutableStateOf(generateSampleRegionalData()) }
    var systemAlerts by remember { mutableStateOf(generateSampleAlerts()) }
    var isRefreshing by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    
    // Auto-refresh data every 30 seconds
    LaunchedEffect(Unit) {
        while (true) {
            delay(30000)
            regionalStatuses = generateSampleRegionalData()
            systemAlerts = generateSampleAlerts()
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
            MainAdminHeader(
                onRefresh = {
                    isRefreshing = true
                    scope.launch {
                        delay(2000)
                        regionalStatuses = generateSampleRegionalData()
                        systemAlerts = generateSampleAlerts()
                        isRefreshing = false
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
                    text = { Text("🌍 ဒေသများ") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("🚨 Alerts") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("👥 အကောင့်များ") }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("🌐 Online Admin") }
                )
                Tab(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    text = { Text("📊 စာရင်းအင်း") }
                )
            }
            
            // Content
            when (selectedTab) {
                0 -> RegionalOverviewTab(regionalStatuses)
                1 -> SystemAlertsTab(systemAlerts)
                2 -> AccountManagementTab()
                3 -> OnlineAdminTab()
                4 -> StatisticsTab(regionalStatuses, systemAlerts)
            }
        }
    }
}

@Composable
fun MainAdminHeader(
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
                    text = "🛡️ Main Admin Panel",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "နိုင်ငံအဆင့် ထိန်းချုပ်မှု စနစ်",
                    fontSize = 12.sp,
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
fun RegionalOverviewTab(regionalStatuses: List<RegionalStatus>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "🌍 ဒေသအလိုက် အခြေအနေ",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        
        items(regionalStatuses) { status ->
            RegionalStatusCard(status)
        }
    }
}

@Composable
fun RegionalStatusCard(status: RegionalStatus) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (status.isOnline) Color(0xFF1A365D) else Color(0xFF742A2A)
        ),
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
                Column {
                    Text(
                        text = status.region,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Admin: ${status.adminName}",
                        fontSize = 12.sp,
                        color = Color(0xFFE2E8F0)
                    )
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(
                                if (status.isOnline) Color.Green else Color.Red,
                                shape = androidx.compose.foundation.shape.CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (status.isOnline) "Online" else "Offline",
                        fontSize = 12.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatusItem("🚨 Alerts", status.alertCount.toString(), Color(0xFFED8936))
                StatusItem("👥 Users", status.userCount.toString(), Color(0xFF3182CE))
                StatusItem("📡 Network", status.networkHealth, getNetworkColor(status.networkHealth))
            }
            
            if (!status.isOnline) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Last seen: ${formatTime(status.lastActive)}",
                    fontSize = 11.sp,
                    color = Color(0xFFCBD5E0)
                )
            }
        }
    }
}

@Composable
fun StatusItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = Color(0xFFE2E8F0)
        )
    }
}

@Composable
fun SystemAlertsTab(alerts: List<SystemAlert>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "🚨 စနစ် Alerts",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        
        items(alerts) { alert ->
            SystemAlertCard(alert)
        }
    }
}

@Composable
fun SystemAlertCard(alert: SystemAlert) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (alert.severity) {
                "Critical" -> Color(0xFF742A2A)
                "High" -> Color(0xFF9C4221)
                "Medium" -> Color(0xFF744210)
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
                    text = "${alert.type} - ${alert.region}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = alert.severity,
                    fontSize = 11.sp,
                    color = Color(0xFFE2E8F0),
                    modifier = Modifier
                        .background(
                            Color.White.copy(alpha = 0.2f),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatTime(alert.timestamp),
                    fontSize = 10.sp,
                    color = Color(0xFFCBD5E0)
                )
                
                if (!alert.isHandled) {
                    Text(
                        text = "⚠️ လုပ်ဆောင်ရန်",
                        fontSize = 10.sp,
                        color = Color(0xFFED8936),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun AccountManagementTab() {
    var accounts by remember { mutableStateOf(adminAccounts.toMutableList()) }
    var showAddAccountDialog by remember { mutableStateOf(false) }
    var showAddRegionDialog by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "👥 အကောင့် စီမံခန့်ခွဲမှု",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Row {
                    Button(
                        onClick = { showAddRegionDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF3182CE)
                        ),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Region", fontSize = 12.sp)
                    }
                    
                    Button(
                        onClick = { showAddAccountDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF38A169)
                        )
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Admin", fontSize = 12.sp)
                    }
                }
            }
        }
        
        items(accounts.filter { it.userType == UserType.REGIONAL_ADMIN }) { account ->
            AdminAccountCard(
                account = account,
                onToggle = { accountId ->
                    accounts = accounts.map { acc ->
                        if (acc.username == accountId) {
                            acc.copy(isActive = !acc.isActive)
                        } else acc
                    }.toMutableList()
                    Toast.makeText(context, "Account status updated", Toast.LENGTH_SHORT).show()
                },
                onDelete = { accountId ->
                    accounts = accounts.filter { it.username != accountId }.toMutableList()
                    Toast.makeText(context, "Account deleted", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
    
    // Add Account Dialog
    if (showAddAccountDialog) {
        AddAccountDialog(
            onDismiss = { showAddAccountDialog = false },
            onAddAccount = { username, password, region ->
                val newAccount = AdminAccount(
                    username = username,
                    password = password,
                    userType = UserType.REGIONAL_ADMIN,
                    region = region,
                    isActive = true,
                    lastLogin = 0L
                )
                accounts.add(newAccount)
                showAddAccountDialog = false
                Toast.makeText(context, "New admin account created", Toast.LENGTH_SHORT).show()
            }
        )
    }
    
    // Add Region Dialog
    if (showAddRegionDialog) {
        AddRegionDialog(
            onDismiss = { showAddRegionDialog = false },
            onAddRegion = { regionName ->
                val adminUsername = "${regionName.lowercase().replace(" ", "_")}_admin"
                val newAccount = AdminAccount(
                    username = adminUsername,
                    password = "${regionName.lowercase()}@2024",
                    userType = UserType.REGIONAL_ADMIN,
                    region = regionName,
                    isActive = true,
                    lastLogin = 0L
                )
                accounts.add(newAccount)
                showAddRegionDialog = false
                Toast.makeText(context, "New region '$regionName' added with admin account", Toast.LENGTH_LONG).show()
            }
        )
    }
}

@Composable
fun AdminAccountCard(
    account: AdminAccount,
    onToggle: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (account.isActive) Color(0xFF2D3748) else Color(0xFF4A5568)
        ),
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
                    text = account.region,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Username: ${account.username}",
                    fontSize = 12.sp,
                    color = Color(0xFFE2E8F0)
                )
                Text(
                    text = "Password: ${account.password}",
                    fontSize = 10.sp,
                    color = Color(0xFFCBD5E0)
                )
                if (account.lastLogin > 0) {
                    Text(
                        text = "Last login: ${formatTime(account.lastLogin)}",
                        fontSize = 10.sp,
                        color = Color(0xFFCBD5E0)
                    )
                } else {
                    Text(
                        text = "Never logged in",
                        fontSize = 10.sp,
                        color = Color(0xFF9CA3AF)
                    )
                }
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Switch(
                    checked = account.isActive,
                    onCheckedChange = { onToggle(account.username) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF38A169),
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color(0xFFE53E3E)
                    )
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                IconButton(onClick = { onDelete(account.username) }) {
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
fun StatisticsTab(regionalStatuses: List<RegionalStatus>, alerts: List<SystemAlert>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "📊 စနစ် စာရင်းအင်း",
                fontSize = 18.sp,
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
                StatCard(
                    title = "ဒေသများ",
                    value = regionalStatuses.size.toString(),
                    subtitle = "${regionalStatuses.count { it.isOnline }} Online",
                    color = Color(0xFF3182CE),
                    modifier = Modifier.weight(1f)
                )
                
                StatCard(
                    title = "စုစုပေါင်း Users",
                    value = regionalStatuses.sumOf { it.userCount }.toString(),
                    subtitle = "Active Users",
                    color = Color(0xFF38A169),
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "Alerts ယနေ့",
                    value = alerts.count { isMainToday(it.timestamp) }.toString(),
                    subtitle = "${alerts.count { !it.isHandled }} Pending",
                    color = Color(0xFFED8936),
                    modifier = Modifier.weight(1f)
                )
                
                StatCard(
                    title = "Critical Alerts",
                    value = alerts.count { it.severity == "Critical" }.toString(),
                    subtitle = "Needs Attention",
                    color = Color(0xFFE53E3E),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun OnlineAdminTab() {
    var onlineUsers by remember { mutableStateOf(generateOnlineUsers()) }
    var nationalAlerts by remember { mutableStateOf(generateNationalAlerts()) }
    var showSendNationalAlert by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🌐 Online Admin Panel",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Button(
                    onClick = { showSendNationalAlert = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE53E3E)
                    )
                ) {
                    Text("🚨 Send National Alert")
                }
            }
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
                        text = "📊 Online System Status",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        OnlineStatItem("🌐 Online Users", onlineUsers.size.toString(), Color(0xFF38A169))
                        OnlineStatItem("📱 Active Devices", (onlineUsers.size * 1.2).toInt().toString(), Color(0xFF3182CE))
                        OnlineStatItem("🚨 Alerts Today", nationalAlerts.count { isMainToday(it.timestamp) }.toString(), Color(0xFFED8936))
                        OnlineStatItem("📡 Coverage", "98.5%", Color(0xFF10B981))
                    }
                }
            }
        }
        
        item {
            Text(
                text = "👥 Online Users (${onlineUsers.size})",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        
        items(onlineUsers.take(10)) { user ->
            OnlineUserCard(user)
        }
        
        item {
            Text(
                text = "📋 National Alert History",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        
        items(nationalAlerts.take(5)) { alert ->
            NationalAlertCard(alert)
        }
    }
    
    // Send National Alert Dialog
    if (showSendNationalAlert) {
        SendNationalAlertDialog(
            onDismiss = { showSendNationalAlert = false },
            onSendAlert = { message, priority ->
                val newAlert = NationalAlert(
                    id = "national_${System.currentTimeMillis()}",
                    message = message,
                    priority = priority,
                    timestamp = System.currentTimeMillis(),
                    recipientCount = onlineUsers.size,
                    deliveredCount = (onlineUsers.size * 0.95).toInt()
                )
                nationalAlerts = listOf(newAlert) + nationalAlerts
                showSendNationalAlert = false
                Toast.makeText(context, "🚨 National alert sent to ${newAlert.recipientCount} users!", Toast.LENGTH_LONG).show()
            }
        )
    }
}

data class OnlineUser(
    val id: String,
    val name: String,
    val location: String,
    val deviceType: String,
    val isOnline: Boolean,
    val lastSeen: Long
)

data class NationalAlert(
    val id: String,
    val message: String,
    val priority: String,
    val timestamp: Long,
    val recipientCount: Int,
    val deliveredCount: Int
)

@Composable
fun OnlineStatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = Color(0xFFE2E8F0),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun OnlineUserCard(user: OnlineUser) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (user.isOnline) Color(0xFF1A365D) else Color(0xFF4A5568)
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
                    text = "${user.location} • ${user.deviceType}",
                    fontSize = 12.sp,
                    color = Color(0xFFE2E8F0)
                )
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            if (user.isOnline) Color.Green else Color.Red,
                            shape = androidx.compose.foundation.shape.CircleShape
                        )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (user.isOnline) "Online" else "Offline",
                    fontSize = 12.sp,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun NationalAlertCard(alert: NationalAlert) {
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
                    text = "🚨 National Alert",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = alert.priority,
                    fontSize = 12.sp,
                    color = when (alert.priority) {
                        "Critical" -> Color(0xFFE53E3E)
                        "High" -> Color(0xFFED8936)
                        else -> Color(0xFFECC94B)
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
                    text = "Sent: ${alert.recipientCount} • Delivered: ${alert.deliveredCount}",
                    fontSize = 10.sp,
                    color = Color(0xFFCBD5E0)
                )
                Text(
                    text = formatTime(alert.timestamp),
                    fontSize = 10.sp,
                    color = Color(0xFFCBD5E0)
                )
            }
        }
    }
}

@Composable
fun SendNationalAlertDialog(
    onDismiss: () -> Unit,
    onSendAlert: (String, String) -> Unit
) {
    var message by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("High") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("🚨 Send National Alert") },
        text = {
            Column {
                Text(
                    text = "This alert will be sent to all online users nationwide.",
                    fontSize = 12.sp,
                    color = Color(0xFF718096),
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Alert Message") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "Priority Level:",
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Medium", "High", "Critical").forEach { p ->
                        FilterChip(
                            onClick = { priority = p },
                            label = { Text(p) },
                            selected = priority == p
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (message.isNotBlank()) {
                        onSendAlert(message, priority)
                    }
                },
                enabled = message.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE53E3E)
                )
            ) {
                Text("🚨 Send Alert")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

fun generateOnlineUsers(): List<OnlineUser> {
    val locations = listOf("Yangon", "Mandalay", "Naypyidaw", "Bago", "Sagaing", "Rakhine", "Shan", "Kachin")
    val devices = listOf("Android", "iOS", "Web")
    val names = listOf("User", "Admin", "Officer", "Operator", "Manager")
    
    return (1..50).map { i ->
        OnlineUser(
            id = "online_user_$i",
            name = "${names.random()} $i",
            location = locations.random(),
            deviceType = devices.random(),
            isOnline = (1..10).random() > 2, // 80% online
            lastSeen = System.currentTimeMillis() - (1..3600000).random()
        )
    }
}

fun generateNationalAlerts(): List<NationalAlert> {
    return listOf(
        NationalAlert(
            id = "nat_1",
            message = "National emergency alert test - Please ignore this message",
            priority = "Medium",
            timestamp = System.currentTimeMillis() - 3600000,
            recipientCount = 1250,
            deliveredCount = 1187
        ),
        NationalAlert(
            id = "nat_2", 
            message = "Severe weather warning for all regions - Take necessary precautions",
            priority = "High",
            timestamp = System.currentTimeMillis() - 7200000,
            recipientCount = 1340,
            deliveredCount = 1298
        )
    )
}

@Composable
fun StatCard(
    title: String,
    value: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontSize = 24.sp,
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

// Helper functions
fun generateSampleRegionalData(): List<RegionalStatus> {
    val regions = listOf(
        "ရန်ကုန်တိုင်းဒေသကြီး" to "Yangon Admin",
        "မန္တလေးတိုင်းဒေသကြီး" to "Mandalay Admin",
        "စစ်ကိုင်းတိုင်းဒေသကြီး" to "Sagaing Admin",
        "ပဲခူးတိုင်းဒေသကြီး" to "Bago Admin",
        "ရခိုင်ပြည်နယ်" to "Rakhine Admin"
    )
    
    return regions.map { (region, admin) ->
        RegionalStatus(
            region = region,
            adminName = admin,
            isOnline = (1..10).random() > 2, // 80% online chance
            lastActive = System.currentTimeMillis() - (1..3600000).random(),
            alertCount = (0..15).random(),
            userCount = (50..500).random(),
            networkHealth = listOf("ကောင်း", "အလယ်အလတ်", "မကောင်း").random()
        )
    }
}

fun generateSampleAlerts(): List<SystemAlert> {
    val alertTypes = listOf("လေယာဉ်", "တိုက်ခိုက်မှု", "စနစ်ချို့ယွင်းမှု", "ကွန်ယက်ပြဿနာ")
    val regions = listOf("ရန်ကုန်", "မန္တလေး", "စစ်ကိုင်း", "ပဲခူး")
    val severities = listOf("Low", "Medium", "High", "Critical")
    
    return (1..10).map { i ->
        SystemAlert(
            id = "alert_$i",
            timestamp = System.currentTimeMillis() - (1..86400000).random(),
            region = regions.random(),
            type = alertTypes.random(),
            message = "Sample alert message for testing purposes",
            severity = severities.random(),
            isHandled = (1..10).random() > 3
        )
    }
}

fun getNetworkColor(health: String): Color {
    return when (health) {
        "ကောင်း" -> Color.Green
        "အလယ်အလတ်" -> Color.Yellow
        else -> Color.Red
    }
}

fun formatTime(timestamp: Long): String {
    val formatter = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())
    return formatter.format(Date(timestamp))
}

fun isMainToday(timestamp: Long): Boolean {
    val today = Calendar.getInstance()
    val date = Calendar.getInstance().apply { timeInMillis = timestamp }
    return today.get(Calendar.YEAR) == date.get(Calendar.YEAR) &&
           today.get(Calendar.DAY_OF_YEAR) == date.get(Calendar.DAY_OF_YEAR)
}

@Composable
fun AddAccountDialog(
    onDismiss: () -> Unit,
    onAddAccount: (String, String, String) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var region by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Admin Account") },
        text = {
            Column {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = region,
                    onValueChange = { region = it },
                    label = { Text("Region") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (username.isNotBlank() && password.isNotBlank() && region.isNotBlank()) {
                        onAddAccount(username, password, region)
                    }
                },
                enabled = username.isNotBlank() && password.isNotBlank() && region.isNotBlank()
            ) {
                Text("Add Account")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AddRegionDialog(
    onDismiss: () -> Unit,
    onAddRegion: (String) -> Unit
) {
    var regionName by remember { mutableStateOf("") }
    
    val predefinedRegions = listOf(
        "ကချင်ပြည်နယ်",
        "ကယားပြည်နယ်", 
        "ကရင်ပြည်နယ်",
        "ချင်းပြည်နယ်",
        "မွန်ပြည်နယ်",
        "ရှမ်းပြည်နယ်",
        "တနင်္သာရီတိုင်းဒေသကြီး",
        "ဧရာဝတီတိုင်းဒေသကြီး",
        "မကွေးတိုင်းဒေသကြီး"
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Region") },
        text = {
            Column {
                Text(
                    text = "Select a predefined region or enter custom:",
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                LazyColumn(
                    modifier = Modifier.height(200.dp)
                ) {
                    items(predefinedRegions) { region ->
                        TextButton(
                            onClick = { regionName = region },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = region,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Start
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = regionName,
                    onValueChange = { regionName = it },
                    label = { Text("Region Name") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (regionName.isNotBlank()) {
                        onAddRegion(regionName)
                    }
                },
                enabled = regionName.isNotBlank()
            ) {
                Text("Add Region")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}