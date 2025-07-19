@file:OptIn(ExperimentalMaterial3Api::class)

package com.thati.airalert.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Thati Alert - Custom UI Components
 * Professional, polished components for production app
 */

/**
 * Animated Alert Status Card
 */
@Composable
fun AnimatedAlertStatusCard(
    title: String,
    status: AlertStatus,
    count: Int,
    icon: ImageVector,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (status == AlertStatus.CRITICAL) 1.05f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    Card(
        modifier = modifier
            .scale(scale)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = status.backgroundColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (status == AlertStatus.CRITICAL) 8.dp else 4.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = status.iconColor,
                modifier = Modifier.size(32.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = count.toString(),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = status.textColor
            )
            
            Text(
                text = title,
                fontSize = 12.sp,
                color = status.textColor.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Network Signal Strength Indicator
 */
@Composable
fun NetworkSignalIndicator(
    signalStrength: Int, // 0-100
    networkType: NetworkType,
    isConnected: Boolean,
    modifier: Modifier = Modifier
) {
    val animatedStrength by animateIntAsState(
        targetValue = if (isConnected) signalStrength else 0,
        animationSpec = tween(1000),
        label = "signal"
    )
    
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Signal bars
        repeat(4) { index ->
            val barHeight = (index + 1) * 4.dp
            val isActive = animatedStrength > (index * 25)
            
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(barHeight)
                    .background(
                        color = if (isActive) networkType.color else Color.Gray.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(1.dp)
                    )
            )
            
            if (index < 3) {
                Spacer(modifier = Modifier.width(2.dp))
            }
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // Network type indicator
        Text(
            text = networkType.displayName,
            fontSize = 10.sp,
            color = if (isConnected) networkType.color else Color.Gray,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Battery Level Indicator with Optimization Status
 */
@Composable
fun BatteryLevelIndicator(
    batteryLevel: Int,
    isCharging: Boolean,
    optimizationLevel: OptimizationLevel,
    modifier: Modifier = Modifier
) {
    val batteryColor = when {
        batteryLevel > 60 -> Color(0xFF4CAF50)
        batteryLevel > 30 -> Color(0xFFFF9800)
        batteryLevel > 15 -> Color(0xFFFF5722)
        else -> Color(0xFFD32F2F)
    }
    
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = batteryColor.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Battery icon
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(
                        color = Color.Gray.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(2.dp)
                    )
            ) {
                // Battery fill
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(batteryLevel / 100f)
                        .background(
                            color = batteryColor,
                            shape = RoundedCornerShape(2.dp)
                        )
                )
                
                // Charging indicator
                if (isCharging) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = "Charging",
                        tint = Color.White,
                        modifier = Modifier
                            .size(12.dp)
                            .align(Alignment.Center)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Column {
                Text(
                    text = "${batteryLevel}%",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = batteryColor
                )
                
                Text(
                    text = optimizationLevel.displayName,
                    fontSize = 10.sp,
                    color = optimizationLevel.color
                )
            }
        }
    }
}

/**
 * Pulsing Emergency Button
 */
@Composable
fun EmergencyAlertButton(
    text: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "emergency_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isActive) 1.1f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "emergency_scale"
    )
    
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isActive) 0.7f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "emergency_alpha"
    )
    
    Button(
        onClick = onClick,
        modifier = modifier
            .scale(scale)
            .fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFD32F2F).copy(alpha = alpha)
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 8.dp
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Text(
                text = text,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

/**
 * Connection Status Indicator
 */
@Composable
fun ConnectionStatusIndicator(
    deviceName: String,
    connectionType: ConnectionType,
    isConnected: Boolean,
    signalStrength: Int,
    modifier: Modifier = Modifier
) {
    val connectionColor = if (isConnected) Color(0xFF4CAF50) else Color(0xFF757575)
    
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = connectionColor.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Connection type icon
            Icon(
                imageVector = connectionType.icon,
                contentDescription = connectionType.name,
                tint = connectionColor,
                modifier = Modifier.size(20.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = deviceName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    text = if (isConnected) "Connected" else "Disconnected",
                    fontSize = 12.sp,
                    color = connectionColor
                )
            }
            
            if (isConnected) {
                NetworkSignalIndicator(
                    signalStrength = signalStrength,
                    networkType = NetworkType.BLUETOOTH,
                    isConnected = true
                )
            }
        }
    }
}

/**
 * Alert Type Selector
 */
@Composable
fun AlertTypeSelector(
    alertTypes: List<AlertTypeOption>,
    selectedType: AlertTypeOption?,
    onTypeSelected: (AlertTypeOption) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(alertTypes) { alertType ->
            AlertTypeCard(
                alertType = alertType,
                isSelected = selectedType == alertType,
                onClick = { onTypeSelected(alertType) }
            )
        }
    }
}

@Composable
private fun AlertTypeCard(
    alertType: AlertTypeOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                alertType.color.copy(alpha = 0.2f) 
            else 
                MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) 
            BorderStroke(2.dp, alertType.color) 
        else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = alertType.emoji,
                fontSize = 32.sp
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = alertType.name,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                color = if (isSelected) alertType.color else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * Loading Indicator with Message
 */
@Composable
fun ThatiLoadingIndicator(
    message: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 4.dp
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = message,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Data Classes and Enums
 */
enum class AlertStatus(
    val backgroundColor: Color,
    val iconColor: Color,
    val textColor: Color
) {
    NORMAL(Color(0xFFF0F9FF), Color(0xFF0369A1), Color(0xFF0C4A6E)),
    WARNING(Color(0xFFFEF3C7), Color(0xFFD97706), Color(0xFF92400E)),
    CRITICAL(Color(0xFFFEE2E2), Color(0xFFDC2626), Color(0xFF991B1B))
}

enum class NetworkType(
    val displayName: String,
    val color: Color
) {
    WIFI_DIRECT("WiFi", Color(0xFF2196F3)),
    BLUETOOTH("BLE", Color(0xFF9C27B0)),
    CELLULAR("Cell", Color(0xFF4CAF50))
}

enum class OptimizationLevel(
    val displayName: String,
    val color: Color
) {
    NORMAL("Normal", Color(0xFF4CAF50)),
    OPTIMIZED("Optimized", Color(0xFFFF9800)),
    AGGRESSIVE("Power Save", Color(0xFFFF5722))
}

enum class ConnectionType(
    val icon: ImageVector
) {
    WIFI_DIRECT(Icons.Default.Wifi),
    BLUETOOTH(Icons.Default.Bluetooth),
    CELLULAR(Icons.Default.SignalCellularAlt)
}

data class AlertTypeOption(
    val id: String,
    val name: String,
    val emoji: String,
    val color: Color
)