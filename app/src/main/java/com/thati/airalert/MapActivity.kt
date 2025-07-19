package com.thati.airalert

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
import com.thati.airalert.ui.theme.ThatiAirAlertTheme
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Map Activity - Myanmar ပြည်နယ်များ၏ offline map နှင့် alert များ ပြသရန်
 * Simplified version without external map dependencies
 */
class MapActivity : ComponentActivity() {
    
    private var networkHealth by mutableStateOf("ကောင်း")
    private var batteryImpact by mutableStateOf("နည်း")
    private var alertHistory = mutableStateListOf<AlertHistoryItem>()
    
    data class AlertHistoryItem(
        val time: String,
        val message: String,
        val location: String,
        val type: AlertType
    )
    
    enum class AlertType {
        AIRCRAFT, ATTACK, GENERAL
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Simulate network health monitoring
        startNetworkHealthMonitoring()
        
        // Add sample alert history
        initializeSampleData()
        
        setContent {
            ThatiAirAlertTheme {
                MapScreen()
            }
        }
    }
    
    @Composable
    private fun MapScreen() {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF1A202C))
        ) {
            // Status bar
            StatusBar()
            
            // Simplified map view (text-based representation)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0xFF2D3748))
            ) {
                // Myanmar Map Simulation
                MyanmarMapSimulation()
                
                // Alert overlay
                AlertOverlay()
            }
            
            // Bottom controls
            BottomControls()
        }
    }
    
    @Composable
    private fun MyanmarMapSimulation() {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Background map
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "🗺️ Myanmar Offline Map",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Enhanced Myanmar locations with more detail
                val detailedLocations = listOf(
                    MapLocation("🏙️ ရန်ကုန်မြို့", "Yangon", Color(0xFF4299E1), hasAlert = true, alertType = "✈️"),
                    MapLocation("🏛️ မန္တလေးမြို့", "Mandalay", Color(0xFF48BB78), hasAlert = false),
                    MapLocation("🏢 နေပြည်တော်", "Naypyidaw", Color(0xFFED8936), hasAlert = true, alertType = "🔴"),
                    MapLocation("🌊 မော်လမြိုင်မြို့", "Mawlamyine", Color(0xFF9F7AEA), hasAlert = false),
                    MapLocation("⛰️ စစ်ကိုင်းမြို့", "Sagaing", Color(0xFFECC94B), hasAlert = false),
                    MapLocation("🏔️ တောင်ကြီးမြို့", "Taunggyi", Color(0xFFF56565), hasAlert = false),
                    MapLocation("🌴 ပုသိမ်မြို့", "Pathein", Color(0xFF38B2AC), hasAlert = false),
                    MapLocation("🏞️ မြိတ်မြို့", "Myeik", Color(0xFFD69E2E), hasAlert = false)
                )
                
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(detailedLocations) { location ->
                        LocationCard(location = location)
                    }
                }
            }
            
            // User current location indicator
            UserLocationIndicator(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            )
            
            // Relay devices visualization
            RelayDevicesOverlay(
                modifier = Modifier.align(Alignment.TopEnd)
            )
        }
    }
    
    data class MapLocation(
        val name: String,
        val englishName: String,
        val color: Color,
        val hasAlert: Boolean = false,
        val alertType: String = ""
    )
    
    @Composable
    private fun LocationCard(location: MapLocation) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (location.hasAlert) 
                    location.color.copy(alpha = 0.8f) else Color(0xFF4A5568)
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
                        text = location.name,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = location.englishName,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )
                }
                
                if (location.hasAlert) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = location.alertType,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        // Pulsing animation effect
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    Color.Red,
                                    shape = androidx.compose.foundation.shape.CircleShape
                                )
                        )
                    }
                }
            }
        }
    }
    
    @Composable
    private fun UserLocationIndicator(modifier: Modifier = Modifier) {
        Card(
            modifier = modifier,
            colors = CardDefaults.cardColors(containerColor = Color(0xFF3182CE))
        ) {
            Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(
                            Color.White,
                            shape = androidx.compose.foundation.shape.CircleShape
                        )
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "သင့်တည်နေရာ",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
    
    @Composable
    private fun RelayDevicesOverlay(modifier: Modifier = Modifier) {
        Card(
            modifier = modifier.padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0x88000000))
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = "📡 Relay Devices",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                val relayDevices = listOf(
                    RelayDevice("ရဲစခန်း-၁", true, 85),
                    RelayDevice("ကျေးရွာ-A", true, 72),
                    RelayDevice("စောင့်ကြည့်ရေး", false, 0),
                    RelayDevice("အရေးပေါ်ဌာန", true, 91)
                )
                
                relayDevices.forEach { device ->
                    RelayDeviceItem(device = device)
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
    
    data class RelayDevice(
        val name: String,
        val isConnected: Boolean,
        val signalStrength: Int
    )
    
    @Composable
    private fun RelayDeviceItem(device: RelayDevice) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Signal animation
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        if (device.isConnected) Color.Green else Color.Red,
                        shape = androidx.compose.foundation.shape.CircleShape
                    )
            )
            Spacer(modifier = Modifier.width(6.dp))
            
            Column {
                Text(
                    text = device.name,
                    color = Color.White,
                    fontSize = 11.sp
                )
                if (device.isConnected) {
                    Text(
                        text = "Signal: ${device.signalStrength}%",
                        color = Color.Gray,
                        fontSize = 9.sp
                    )
                }
            }
        }
    }
    
    @Composable
    private fun StatusBar() {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2D3748))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusItem("ကွန်ယက်", networkHealth, getHealthColor(networkHealth))
                StatusItem("ဘက်ထရီ", batteryImpact, getBatteryColor(batteryImpact))
                StatusItem("သတိပေးချက်", "${alertHistory.size}", Color.White)
            }
        }
    }
    
    @Composable
    private fun StatusItem(label: String, value: String, color: Color) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                fontSize = 12.sp,
                color = Color.Gray
            )
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
    
    @Composable
    private fun AlertOverlay() {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopEnd
        ) {
            Card(
                modifier = Modifier.padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0x88000000))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = "🔴 လက်ရှိ သတိပေးချက်များ",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    alertHistory.takeLast(3).forEach { alert ->
                        Text(
                            text = "${alert.time}: ${alert.message}",
                            color = Color.White,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }
    }
    
    @Composable
    private fun BottomControls() {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A202C))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = { simulateAircraftAlert() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFED8936))
                ) {
                    Text("✈️ လေယာဉ်")
                }
                
                Button(
                    onClick = { simulateAttackAlert() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53E3E))
                ) {
                    Text("💥 တိုက်ခိုက်မှု")
                }
                
                Button(
                    onClick = { showAlertHistory() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3182CE))
                ) {
                    Text("📋 မှတ်တမ်း")
                }
            }
        }
    }
    
    private fun simulateAircraftAlert() {
        val newAlert = AlertHistoryItem(
            time = getCurrentTime(),
            message = "လေယာဉ် တွေ့ရှိ - မြောက်ဘက်မှ လာနေ",
            location = "ရန်ကုန်မြို့",
            type = AlertType.AIRCRAFT
        )
        alertHistory.add(0, newAlert)
    }
    
    private fun simulateAttackAlert() {
        val newAlert = AlertHistoryItem(
            time = getCurrentTime(),
            message = "တိုက်ခိုက်မှု သတိပေးချက်",
            location = "မန္တလေးမြို့",
            type = AlertType.ATTACK
        )
        alertHistory.add(0, newAlert)
    }
    
    private fun showAlertHistory() {
        // This would typically open a new screen or dialog
        // For now, we'll just update the overlay
    }
    
    private fun startNetworkHealthMonitoring() {
        // Simulate network health changes
        val healthStates = listOf("ကောင်း", "အလယ်အလတ်", "မကောင်း")
        var currentIndex = 0
        
        // Use a coroutine to simulate network changes
        kotlinx.coroutines.GlobalScope.launch {
            while (true) {
                delay(10000) // Change every 10 seconds
                networkHealth = healthStates[currentIndex % healthStates.size]
                currentIndex++
            }
        }
    }
    
    private fun initializeSampleData() {
        alertHistory.addAll(listOf(
            AlertHistoryItem(
                "14:30",
                "လေယာဉ် သတိပေးချက်",
                "ရန်ကုန်မြို့",
                AlertType.AIRCRAFT
            ),
            AlertHistoryItem(
                "14:15",
                "ဧရိယာ သတိပေးချက်",
                "မန္တလေးမြို့",
                AlertType.GENERAL
            ),
            AlertHistoryItem(
                "14:00",
                "တိုက်ခိုက်မှု သတိပေးချက်",
                "နေပြည်တော်",
                AlertType.ATTACK
            )
        ))
    }
    
    private fun getCurrentTime(): String {
        val formatter = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
        return formatter.format(java.util.Date())
    }
    
    private fun getHealthColor(health: String): Color {
        return when (health) {
            "ကောင်း" -> Color.Green
            "အလယ်အလတ်" -> Color.Yellow
            else -> Color.Red
        }
    }
    
    private fun getBatteryColor(impact: String): Color {
        return when (impact) {
            "နည်း" -> Color.Green
            "အလယ်အလတ်" -> Color.Yellow
            else -> Color.Red
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Resume any background monitoring if needed
    }
    
    override fun onPause() {
        super.onPause()
        // Pause any background monitoring if needed
    }
}