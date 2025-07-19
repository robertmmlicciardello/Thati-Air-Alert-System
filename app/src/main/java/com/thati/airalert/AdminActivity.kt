@file:OptIn(ExperimentalMaterial3Api::class)

package com.thati.airalert

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thati.airalert.models.AlertMessage
import com.thati.airalert.network.ProductionMeshNetworkManager
import com.thati.airalert.network.NetworkStatus
import com.thati.airalert.services.AlertService
import com.thati.airalert.ui.components.*
import com.thati.airalert.ui.theme.ThatiAirAlertTheme
import kotlinx.coroutines.*

/**
 * Enhanced Production-Ready Admin Activity
 * Complete admin interface with mesh network management and real-time monitoring
 */
class AdminActivity : ComponentActivity() {
    
    // Service and Network State
    private var isServiceRunning by mutableStateOf(false)
    private var meshNetworkManager: ProductionMeshNetworkManager? = null
    
    // UI State Management
    private var networkStatus by mutableStateOf(NetworkStatus.INITIALIZING)
    private var connectedDevices by mutableStateOf(0)
    private var signalStrength by mutableStateOf(0)
    private var sentAlerts by mutableStateOf<List<AlertMessage>>(emptyList())
    private var isLoading by mutableStateOf(false)
    private var showNetworkDetails by mutableStateOf(false)
    
    // Coroutine scope for background operations
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize mesh network manager
        initializeMeshNetwork()
        
        // Start Alert Service
        startAlertService()
        
        setContent {
            ThatiAirAlertTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    EnhancedAdminScreen(
                        networkStatus = networkStatus,
                        connectedDevices = connectedDevices,
                        signalStrength = signalStrength,
                        sentAlerts = sentAlerts,
                        isLoading = isLoading,
                        showNetworkDetails = showNetworkDetails,
                        onSendAlert = { message, priority, region -> sendEnhancedAlert(message, priority, region) },
                        onToggleNetworkDetails = { showNetworkDetails = !showNetworkDetails },
                        onBackPressed = { finish() },
                        isServiceRunning = isServiceRunning
                    )
                }
            }
        }
    }
    
    /**
     * Initialize mesh network manager
     */
    private fun initializeMeshNetwork() {
        meshNetworkManager = ProductionMeshNetworkManager(
            context = this,
            onAlertReceived = { alert ->
                // Handle received alerts (for relay functionality)
                scope.launch {
                    // Add to received alerts list or process as needed
                }
            },
            onNetworkStatusChanged = { status ->
                networkStatus = status
                updateNetworkMetrics()
            }
        )
        
        // Initialize network in admin mode
        scope.launch {
            try {
                isLoading = true
                val initialized = meshNetworkManager?.initialize() ?: false
                if (initialized) {
                    meshNetworkManager?.startAsAdmin()
                }
            } catch (e: Exception) {
                Log.e("AdminActivity", "Failed to initialize mesh network", e)
            } finally {
                isLoading = false
            }
        }
    }
    
    /**
     * Update network metrics
     */
    private fun updateNetworkMetrics() {
        scope.launch {
            try {
                val topology = meshNetworkManager?.getNetworkTopology()
                val metrics = meshNetworkManager?.getNetworkMetrics()
                
                connectedDevices = topology?.getDevices()?.count { it.isOnline } ?: 0
                signalStrength = topology?.getDevices()?.maxOfOrNull { it.signalStrength } ?: 0
                
            } catch (e: Exception) {
                Log.e("AdminActivity", "Error updating network metrics", e)
            }
        }
    }
    
    /**
     * Alert Service ကို စတင်ခြင်း
     */
    private fun startAlertService() {
        val intent = Intent(this, AlertService::class.java).apply {
            action = AlertService.ACTION_START_ADMIN
        }
        startForegroundService(intent)
        isServiceRunning = true
    }
    
    /**
     * Enhanced alert sending with priority and region
     */
    private fun sendEnhancedAlert(message: String, priority: String, region: String) {
        if (message.isBlank()) return
        
        scope.launch {
            try {
                isLoading = true
                
                // Create alert message
                val alert = AlertMessage(
                    id = "alert-${System.currentTimeMillis()}",
                    message = message,
                    type = "admin",
                    priority = priority,
                    region = region,
                    latitude = 0.0, // Will be updated with actual location
                    longitude = 0.0,
                    timestamp = System.currentTimeMillis().toString()
                )
                
                // Send via mesh network
                val result = meshNetworkManager?.broadcastAlert(alert)
                
                // Also send via traditional service
                val intent = Intent(this@AdminActivity, AlertService::class.java).apply {
                    action = AlertService.ACTION_SEND_ALERT
                    putExtra(AlertService.EXTRA_ALERT_MESSAGE, message)
                    putExtra("priority", priority)
                    putExtra("region", region)
                }
                startService(intent)
                
                // Update sent alerts list
                sentAlerts = sentAlerts + alert
                
                Log.d("AdminActivity", "Alert sent successfully: ${result?.deliveredCount} devices reached")
                
            } catch (e: Exception) {
                Log.e("AdminActivity", "Error sending alert", e)
            } finally {
                isLoading = false
            }
        }
    }
    
    /**
     * Traditional alert sending (backward compatibility)
     */
    private fun sendAlert(message: String) {
        sendEnhancedAlert(message, "medium", "yangon")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        
        // Cancel coroutine scope
        scope.cancel()
        
        // Stop mesh network
        scope.launch {
            meshNetworkManager?.stop()
        }
        
        // Stop service
        val intent = Intent(this, AlertService::class.java).apply {
            action = AlertService.ACTION_STOP_SERVICE
        }
        startService(intent)
    }
}

/**
 * Admin Screen UI
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    onSendAlert: (String) -> Unit,
    onBackPressed: () -> Unit,
    isServiceRunning: Boolean
) {
    var alertMessage by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    
    // Pre-defined alert messages
    val quickAlerts = listOf(
        "လေယာဉ် မြောက်ဘက်မှ လာနေသည်",
        "လေယာဉ် တောင်ဘက်မှ လာနေသည်",
        "လေယာဉ် အရှေ့ဘက်မှ လာနေသည်",
        "လေယာဉ် အနောက်ဘက်မှ လာနေသည်",
        "အရေးပေါ် - ချက်ချင်း ရွှေ့ပြောင်းပါ",
        "ဘေးကင်းပြီ - ပုံမှန်ပြန်လည်လုပ်ဆောင်နိုင်ပါပြီ"
    )
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
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
                text = "🚨 Admin Mode",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE53E3E)
            )
            
            // Status indicator
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .padding(end = 4.dp)
                ) {
                    if (isServiceRunning) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🟢", fontSize = 8.sp)
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🔴", fontSize = 8.sp)
                        }
                    }
                }
                Text(
                    text = if (isServiceRunning) "Active" else "Inactive",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
        
        // Service status card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isServiceRunning) 
                    Color(0xFFE6FFFA) else Color(0xFFFED7D7)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isServiceRunning) "✅ Service Running" else "❌ Service Stopped",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isServiceRunning) Color(0xFF38A169) else Color(0xFFE53E3E)
                )
                Text(
                    text = if (isServiceRunning) 
                        "Wi-Fi Direct နှင့် Bluetooth ဖြင့် alert များ ပို့ဆောင်ရန် အသင့်ဖြစ်နေပါသည်" 
                        else "Service ကို စတင်ရန် လိုအပ်ပါသည်",
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
        
        // Custom alert input
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "စိတ်ကြိုက် သတိပေးချက်",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                OutlinedTextField(
                    value = alertMessage,
                    onValueChange = { alertMessage = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("သတိပေးချက် စာသား ရိုက်ထည့်ပါ...") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    maxLines = 3
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Button(
                    onClick = {
                        if (alertMessage.isNotBlank()) {
                            isSending = true
                            onSendAlert(alertMessage)
                            alertMessage = ""
                            // Reset sending state after a delay
                            GlobalScope.launch {
                                delay(2000)
                                isSending = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = alertMessage.isNotBlank() && !isSending && isServiceRunning,
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
                    }
                    Text(
                        text = if (isSending) "ပို့နေသည်..." else "📢 Alert ပို့ရန်",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        
        // Quick alert buttons
        Text(
            text = "အမြန် သတိပေးချက်များ",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            textAlign = TextAlign.Start
        )
        
        LazyColumn {
            items(quickAlerts) { alert ->
                Button(
                    onClick = {
                        isSending = true
                        onSendAlert(alert)
                        GlobalScope.launch {
                            delay(2000)
                            isSending = false
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    enabled = !isSending && isServiceRunning,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFED8936)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = alert,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}


/**
 * Enhanced Alert Categories with Visual Icons
 */
data class AlertCategory(
    val name: String,
    val englishName: String,
    val color: Color,
    val icon: String
)

val alertCategories = listOf(
    AlertCategory("🚁 ရဟတ်ယာဉ်", "Helicopter", Color(0xFFED8936), "🚁"),
    AlertCategory("✈️ Jet Fighter", "Jet Fighter", Color(0xFFE53E3E), "✈️"),
    AlertCategory("🛩️ ပြည်သူ့လေယာဉ်", "Civil Aircraft", Color(0xFF3182CE), "🛩️"),
    AlertCategory("💥 တိုက်ခိုက်မှု", "Attack", Color(0xFF9B2C2C), "💥"),
    AlertCategory("🔥 မီးလောင်မှု", "Fire", Color(0xFFD69E2E), "🔥"),
    AlertCategory("🌊 ရေကြီးမှု", "Flood", Color(0xFF2B6CB0), "🌊"),
    AlertCategory("⚡ လျှပ်စစ်ပြတ်", "Power Outage", Color(0xFF553C9A), "⚡"),
    AlertCategory("✅ ဘေးကင်းပြီ", "All Clear", Color(0xFF38A169), "✅")
)

/**
 * Enhanced Admin Alert Input Section
 */
@Composable
fun EnhancedAdminAlertSection(
    onSendAlert: (String, AlertCategory) -> Unit
) {
    var customMessage by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(alertCategories[0]) }
    var isSending by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF5F5))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "📢 Alert အမျိုးအစား ရွေးချယ်ရန်",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE53E3E),
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            // Alert category selection
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
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Custom message input
            OutlinedTextField(
                value = customMessage,
                onValueChange = { customMessage = it },
                label = { Text("အပိုစာသား (ရွေးချယ်ခွင့်)") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = selectedCategory.color,
                    focusedLabelColor = selectedCategory.color
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Send button
            Button(
                onClick = {
                    isSending = true
                    val finalMessage = if (customMessage.isNotBlank()) {
                        "${selectedCategory.name} - $customMessage"
                    } else {
                        selectedCategory.name
                    }
                    onSendAlert(finalMessage, selectedCategory)
                    
                    GlobalScope.launch {
                        delay(1500)
                        isSending = false
                        customMessage = ""
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSending,
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
            
            // Special note for "All Clear"
            if (selectedCategory.englishName == "All Clear") {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "💡 ဤ Alert သည် ယခင် Alert များကို ရှင်းလင်းပေးမည်",
                    fontSize = 11.sp,
                    color = Color(0xFF38A169),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Alert Category Card Component
 */
@Composable
fun AlertCategoryCard(
    category: AlertCategory,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                category.color.copy(alpha = 0.2f) else Color(0xFFF7FAFC)
        ),
        border = if (isSelected) 
            androidx.compose.foundation.BorderStroke(2.dp, category.color) else null,
        onClick = onSelect
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = category.icon,
                    fontSize = 20.sp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = category.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF2D3748)
                    )
                    Text(
                        text = category.englishName,
                        fontSize = 11.sp,
                        color = Color(0xFF718096)
                    )
                }
            }
            
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .background(
                            category.color,
                            shape = androidx.compose.foundation.shape.CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "✓",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}