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
                    onLogout = { finish() }
                )
            }
        }
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
                    text = { Text("📊 စာရင်းအင်း") }
                )
            }
            
            // Content
            when (selectedTab) {
                0 -> RegionalOverviewTab(regionalStatuses)
                1 -> SystemAlertsTab(systemAlerts)
                2 -> AccountManagementTab()
                3 -> StatisticsTab(regionalStatuses, systemAlerts)
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
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "👥 အကောင့် စီမံခန့်ခွဲမှု",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        
        items(adminAccounts.filter { it.userType == UserType.REGIONAL_ADMIN }) { account ->
            AdminAccountCard(account)
        }
    }
}

@Composable
fun AdminAccountCard(account: AdminAccount) {
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
            Column {
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
                if (account.lastLogin > 0) {
                    Text(
                        text = "Last login: ${formatTime(account.lastLogin)}",
                        fontSize = 10.sp,
                        color = Color(0xFFCBD5E0)
                    )
                }
            }
            
            Switch(
                checked = account.isActive,
                onCheckedChange = { /* TODO: Implement account toggle */ },
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
                    value = alerts.count { isToday(it.timestamp) }.toString(),
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

fun isToday(timestamp: Long): Boolean {
    val today = Calendar.getInstance()
    val date = Calendar.getInstance().apply { timeInMillis = timestamp }
    return today.get(Calendar.YEAR) == date.get(Calendar.YEAR) &&
           today.get(Calendar.DAY_OF_YEAR) == date.get(Calendar.DAY_OF_YEAR)
}