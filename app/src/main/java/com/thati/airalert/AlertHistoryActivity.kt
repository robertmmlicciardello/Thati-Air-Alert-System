@file:OptIn(ExperimentalMaterial3Api::class)

package com.thati.airalert

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import java.text.SimpleDateFormat
import java.util.*

/**
 * Alert History Activity
 * သတိပေးချက် မှတ်တမ်းများ ကြည့်ရှုရန်
 */
class AlertHistoryActivity : ComponentActivity() {
    
    data class AlertRecord(
        val id: String,
        val timestamp: Long,
        val type: AlertType,
        val message: String,
        val location: String,
        val severity: Severity,
        val source: String,
        val relayCount: Int,
        val isRead: Boolean = false
    )
    
    enum class AlertType(val emoji: String, val label: String, val color: Color) {
        HELICOPTER("🚁", "ရဟတ်ယာဉ်", Color(0xFFED8936)),
        JET_FIGHTER("✈️", "Jet Fighter", Color(0xFFE53E3E)),
        CIVIL_AIRCRAFT("🛩️", "ပြည်သူ့လေယာဉ်", Color(0xFF3182CE)),
        ATTACK("💥", "တိုက်ခိုက်မှု", Color(0xFF9B2C2C)),
        FIRE("🔥", "မီးလောင်မှု", Color(0xFFD69E2E)),
        FLOOD("🌊", "ရေကြီးမှု", Color(0xFF2B6CB0)),
        POWER_OUTAGE("⚡", "လျှပ်စစ်ပြတ်", Color(0xFF553C9A)),
        EVACUATION("🏃", "ရွှေ့ပြောင်းရန်", Color(0xFFE53E3E)),
        ALL_CLEAR("✅", "ဘေးကင်းပြီ", Color(0xFF38A169)),
        GENERAL("⚠️", "ယေဘုယျ", Color(0xFF718096))
    }
    
    enum class Severity(val color: Color, val label: String) {
        LOW(Color(0xFF38A169), "နည်း"),
        MEDIUM(Color(0xFFED8936), "အလယ်အလတ်"),
        HIGH(Color(0xFFE53E3E), "မြင့်"),
        CRITICAL(Color(0xFF9B2C2C), "အရေးကြီး")
    }
    
    private val alertHistory = mutableStateListOf<AlertRecord>()
    private var selectedFilter by mutableStateOf<AlertType?>(null)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize with sample data
        initializeSampleAlerts()
        
        setContent {
            ThatiAirAlertTheme {
                AlertHistoryScreen()
            }
        }
    }
    
    @Composable
    private fun AlertHistoryScreen() {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF1A202C))
        ) {
            // Header
            HeaderSection()
            
            // Filter buttons
            FilterSection()
            
            // Statistics
            StatisticsSection()
            
            // Alert list
            AlertListSection()
        }
    }
    
    @Composable
    private fun HeaderSection() {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2D3748))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "📋 သတိပေးချက် မှတ်တမ်း",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "စုစုပေါင်း: ${alertHistory.size} ခု",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
    
    @Composable
    private fun FilterSection() {
        LazyRow(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedFilter == null,
                    onClick = { selectedFilter = null },
                    label = { Text("အားလုံး") }
                )
            }
            
            items(AlertType.values()) { type ->
                FilterChip(
                    selected = selectedFilter == type,
                    onClick = { selectedFilter = if (selectedFilter == type) null else type },
                    label = { Text("${type.emoji} ${type.label}") }
                )
            }
        }
    }
    
    @Composable
    private fun StatisticsSection() {
        val filteredAlerts = getFilteredAlerts()
        val stats = calculateStatistics(filteredAlerts)
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2D3748))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem("ယနေ့", stats.today.toString(), Color.White)
                StatItem("ဒီအပတ်", stats.thisWeek.toString(), Color.Blue)
                StatItem("အရေးကြီး", stats.critical.toString(), Color.Red)
                StatItem("မဖတ်ရသေး", stats.unread.toString(), Color.Yellow)
            }
        }
    }
    
    @Composable
    private fun StatItem(label: String, value: String, color: Color) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
    
    @Composable
    private fun AlertListSection() {
        val filteredAlerts = getFilteredAlerts()
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredAlerts) { alert ->
                AlertItem(alert = alert)
            }
        }
    }
    
    @Composable
    private fun AlertItem(alert: AlertRecord) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (alert.isRead) Color(0xFF2D3748) else Color(0xFF3A4A5C)
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Header row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = alert.type.emoji,
                            fontSize = 20.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = alert.type.label,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    
                    // Severity indicator
                    Card(
                        colors = CardDefaults.cardColors(containerColor = alert.severity.color),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = alert.severity.label,
                            fontSize = 10.sp,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Message
                Text(
                    text = alert.message,
                    fontSize = 14.sp,
                    color = Color.White,
                    lineHeight = 20.sp
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Details row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "📍 ${alert.location}",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = "📡 ${alert.source}",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                    
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = formatTime(alert.timestamp),
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = "🔄 ${alert.relayCount} relay",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
    
    private fun getFilteredAlerts(): List<AlertRecord> {
        return if (selectedFilter == null) {
            alertHistory.sortedByDescending { it.timestamp }
        } else {
            alertHistory.filter { it.type == selectedFilter }.sortedByDescending { it.timestamp }
        }
    }
    
    private fun calculateStatistics(alerts: List<AlertRecord>): Statistics {
        val now = System.currentTimeMillis()
        val oneDayAgo = now - 24 * 60 * 60 * 1000
        val oneWeekAgo = now - 7 * 24 * 60 * 60 * 1000
        
        return Statistics(
            today = alerts.count { it.timestamp > oneDayAgo },
            thisWeek = alerts.count { it.timestamp > oneWeekAgo },
            critical = alerts.count { it.severity == Severity.CRITICAL },
            unread = alerts.count { !it.isRead }
        )
    }
    
    data class Statistics(
        val today: Int,
        val thisWeek: Int,
        val critical: Int,
        val unread: Int
    )
    
    private fun formatTime(timestamp: Long): String {
        val formatter = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())
        return formatter.format(Date(timestamp))
    }
    
    private fun initializeSampleAlerts() {
        val now = System.currentTimeMillis()
        val sampleAlerts = listOf(
            AlertRecord(
                "1", now - 1000 * 60 * 30, AlertType.JET_FIGHTER,
                "Jet Fighter ၃ စင်း မြောက်ဘက်မှ ချဉ်းကပ်လာနေ", "ရန်ကုန်မြို့",
                Severity.HIGH, "ရေဒါစခန်း-၁", 5, false
            ),
            AlertRecord(
                "2", now - 1000 * 60 * 60, AlertType.ATTACK,
                "တိုက်ခိုက်မှု သတိပေးချက် - ချက်ချင်း ရှောင်ကွင်းရန်", "မန္တလေးမြို့",
                Severity.CRITICAL, "စစ်ဌာနချုပ်", 12, true
            ),
            AlertRecord(
                "3", now - 1000 * 60 * 60 * 2, AlertType.EVACUATION,
                "ဧရိယာ ရွှေ့ပြောင်းရန် အမိန့် - အန္တရာယ်ကင်းရှင်းသည့်တိုင်အောင်", "ပဲခူးမြို့",
                Severity.HIGH, "အရေးပေါ်ဌာန", 8, true
            ),
            AlertRecord(
                "4", now - 1000 * 60 * 60 * 4, AlertType.GENERAL,
                "ယေဘုယျ သတိပေးချက် - သတိထားရန်", "နေပြည်တော်",
                Severity.MEDIUM, "ရဲစခန်း-၂", 3, true
            ),
            AlertRecord(
                "5", now - 1000 * 60 * 60 * 6, AlertType.ALL_CLEAR,
                "ဘေးအန္တရာယ် ကင်းရှင်းပြီ - ပုံမှန် လုပ်ငန်းများ ပြန်လည်စတင်နိုင်", "ရန်ကုန်မြို့",
                Severity.LOW, "စစ်ဌာနချုပ်", 15, true
            ),
            AlertRecord(
                "6", now - 1000 * 60 * 60 * 12, AlertType.HELICOPTER,
                "ရဟတ်ယာဉ် ရေဒါတွင် ပျောက်ကွယ်သွား", "ရှမ်းပြည်နယ်",
                Severity.MEDIUM, "ရေဒါစခန်း-၂", 4, true
            ),
            AlertRecord(
                "7", now - 1000 * 60 * 60 * 24, AlertType.ATTACK,
                "ညအချိန် တိုက်ခိုက်မှု ဖြစ်ပွားနိုင်ခြေ", "ကရင်ပြည်နယ်",
                Severity.HIGH, "ကင်းလှည့်တပ်", 6, true
            )
        )
        
        alertHistory.addAll(sampleAlerts)
    }
}