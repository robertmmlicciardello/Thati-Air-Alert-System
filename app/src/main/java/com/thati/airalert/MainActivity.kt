package com.thati.airalert

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.thati.airalert.ui.theme.ThatiAirAlertTheme
import kotlinx.coroutines.delay

/**
 * Enhanced Main Activity
 * Modern UI with smooth animations and better UX
 */
class MainActivity : ComponentActivity() {
    
    private var hasRequiredPermissions by mutableStateOf(false)
    private var isCheckingPermissions by mutableStateOf(true)
    
    // Permission များ တောင်းရန်အတွက် launcher
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasRequiredPermissions = permissions.values.all { it }
        isCheckingPermissions = false
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            ThatiAirAlertTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LaunchedEffect(Unit) {
                        delay(1000) // Show splash for 1 second
                        checkAndRequestPermissions()
                    }
                    
                    when {
                        isCheckingPermissions -> {
                            SplashScreen()
                        }
                        !hasRequiredPermissions -> {
                            PermissionScreen(
                                onRequestPermissions = { checkAndRequestPermissions() }
                            )
                        }
                        else -> {
                            ModernModeSelectionScreen(
                                onAdminSelected = { startAdminMode() },
                                onUserSelected = { startUserMode() }
                            )
                        }
                    }
                }
            }
        }
    }
    
    /**
     * လိုအပ်သော permission များ စစ်ဆေးပြီး တောင်းခြင်း
     */
    private fun checkAndRequestPermissions() {
        val requiredPermissions = mutableListOf<String>().apply {
            // Location permissions (Wi-Fi Direct အတွက်)
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            
            // Wi-Fi permissions
            add(Manifest.permission.ACCESS_WIFI_STATE)
            add(Manifest.permission.CHANGE_WIFI_STATE)
            add(Manifest.permission.ACCESS_NETWORK_STATE)
            add(Manifest.permission.CHANGE_NETWORK_STATE)
            
            // Bluetooth permissions
            add(Manifest.permission.BLUETOOTH)
            add(Manifest.permission.BLUETOOTH_ADMIN)
            
            // Android 12+ permissions
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_CONNECT)
                add(Manifest.permission.BLUETOOTH_ADVERTISE)
                add(Manifest.permission.BLUETOOTH_SCAN)
                add(Manifest.permission.NEARBY_WIFI_DEVICES)
            }
            
            // Notification permission (Android 13+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
            
            // Audio permissions
            add(Manifest.permission.MODIFY_AUDIO_SETTINGS)
            add(Manifest.permission.WAKE_LOCK)
            add(Manifest.permission.VIBRATE)
        }
        
        // Permission များ ရှိပြီးလား စစ်ပါ
        val missingPermissions = requiredPermissions.filter { permission ->
            ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED
        }
        
        if (missingPermissions.isEmpty()) {
            hasRequiredPermissions = true
        } else {
            // Permission များ တောင်းပါ
            permissionLauncher.launch(missingPermissions.toTypedArray())
        }
    }
    
    /**
     * Admin Mode ကို စတင်ခြင်း
     */
    private fun startAdminMode() {
        val intent = Intent(this, AdminActivity::class.java)
        startActivity(intent)
        finish()
    }
    
    /**
     * User Mode ကို စတင်ခြင်း
     */
    private fun startUserMode() {
        val intent = Intent(this, UserActivity::class.java)
        startActivity(intent)
        finish()
    }
}

/**
 * Modern Splash Screen with animations
 */
@Composable
fun SplashScreen() {
    val infiniteTransition = rememberInfiniteTransition()
    
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1E40AF),
                        Color(0xFF3B82F6),
                        Color(0xFF60A5FA)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🚨",
                fontSize = 80.sp,
                modifier = Modifier
                    .scale(scale)
                    .alpha(alpha)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "သတိ",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = "Thati Air Alert",
                fontSize = 18.sp,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 3.dp,
                modifier = Modifier.size(32.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "စတင်နေသည်...",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * Modern Mode Selection Screen with enhanced UI
 */
@Composable
fun ModernModeSelectionScreen(
    onAdminSelected: () -> Unit,
    onUserSelected: () -> Unit
) {
    var selectedMode by remember { mutableStateOf<String?>(null) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Logo and Title with animation
            AnimatedVisibility(
                visible = true,
                enter = slideInVertically(
                    initialOffsetY = { -100 },
                    animationSpec = tween(800)
                ) + fadeIn(animationSpec = tween(800))
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🚨",
                        fontSize = 72.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    Text(
                        text = "သတိ",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                    
                    Text(
                        text = "Thati Air Alert",
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    Text(
                        text = "လေကြောင်းသတိပေးချက်များအတွက် အင်တာနက်မလိုအပ်သော အက်ပ်",
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 48.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        lineHeight = 24.sp
                    )
                }
            }
            
            // Mode Selection Cards
            AnimatedVisibility(
                visible = true,
                enter = slideInVertically(
                    initialOffsetY = { 100 },
                    animationSpec = tween(800, delayMillis = 200)
                ) + fadeIn(animationSpec = tween(800, delayMillis = 200))
            ) {
                Column {
                    // Admin Mode Card
                    ModeSelectionCard(
                        title = "🚨 Admin Mode",
                        subtitle = "သတိပေးချက်များ ပေးပို့ရန်",
                        description = "အရေးပေါ်သတိပေးချက်များကို အခြားအသုံးပြုသူများထံ ပေးပို့နိုင်သည်",
                        color = Color(0xFFDC2626),
                        isSelected = selectedMode == "admin",
                        onClick = { 
                            selectedMode = "admin"
                            onAdminSelected()
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // User Mode Card
                    ModeSelectionCard(
                        title = "👤 User Mode",
                        subtitle = "သတိပေးချက်များ လက်ခံရန်",
                        description = "အရေးပေါ်သတိပေးချက်များကို လက်ခံပြီး အသံကျယ်ကျယ်ဖြင့် သတိပေးမည်",
                        color = Color(0xFF16A34A),
                        isSelected = selectedMode == "user",
                        onClick = { 
                            selectedMode = "user"
                            onUserSelected()
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Additional Features
            AnimatedVisibility(
                visible = true,
                enter = slideInVertically(
                    initialOffsetY = { 100 },
                    animationSpec = tween(800, delayMillis = 400)
                ) + fadeIn(animationSpec = tween(800, delayMillis = 400))
            ) {
                Column {
                    Text(
                        text = "အပိုဆောင်း လုပ်ဆောင်ချက်များ",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        QuickActionButton(
                            icon = "🗺️",
                            text = "Myanmar Map",
                            onClick = { 
                                LocalContext.current.startActivity(
                                    Intent(LocalContext.current, MapActivity::class.java)
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                        
                        QuickActionButton(
                            icon = "📋",
                            text = "Alert History",
                            onClick = { 
                                LocalContext.current.startActivity(
                                    Intent(LocalContext.current, AlertHistoryActivity::class.java)
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ModeSelectionCard(
    title: String,
    subtitle: String,
    description: String,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 4.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                color.copy(alpha = 0.1f) 
            else 
                MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        border = if (isSelected) 
            BorderStroke(2.dp, color) 
        else null
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = subtitle,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = description,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
fun QuickActionButton(
    icon: String,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = icon,
                fontSize = 20.sp
            )
            Text(
                text = text,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Enhanced Permission Screen with better UX
 */
@Composable
fun PermissionScreen(
    onRequestPermissions: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Permission icon with animation
        val infiniteTransition = rememberInfiniteTransition()
        val scale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000),
                repeatMode = RepeatMode.Reverse
            )
        )
        
        Text(
            text = "🔐",
            fontSize = 80.sp,
            modifier = Modifier
                .scale(scale)
                .padding(bottom = 24.dp)
        )
        
        Text(
            text = "Permission လိုအပ်သည်",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp),
            color = MaterialTheme.colorScheme.primary
        )
        
        Text(
            text = "App ကို အပြည့်အဝ အသုံးပြုနိုင်ရန် အခုလို permission များ လိုအပ်ပါသည်",
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 32.dp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            lineHeight = 24.sp
        )
        
        // Permission list
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                PermissionItem(
                    icon = "📍",
                    title = "Location",
                    description = "Wi-Fi Direct device discovery အတွက်"
                )
                
                PermissionItem(
                    icon = "📶",
                    title = "Bluetooth",
                    description = "BLE mesh networking အတွက်"
                )
                
                PermissionItem(
                    icon = "🔔",
                    title = "Notifications",
                    description = "အရေးပေါ်သတိပေးချက်များ အတွက်"
                )
                
                PermissionItem(
                    icon = "🔊",
                    title = "Audio",
                    description = "Emergency alarm sounds အတွက်"
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onRequestPermissions,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Permission ပေးရန်",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "သင့်ရဲ့ privacy နှင့် security ကို အပြည့်အဝ လုံခြုံစေပါသည်",
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun PermissionItem(
    icon: String,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = icon,
            fontSize = 24.sp,
            modifier = Modifier.padding(end = 16.dp)
        )
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Text(
                text = description,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * Mode Selection Screen
 * Admin Mode သို့မဟုတ် User Mode ရွေးချယ်ရန်
 */
@Composable
fun ModeSelectionScreen(
    onAdminSelected: () -> Unit,
    onUserSelected: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App Title
        Text(
            text = "သတិ",
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
        
        Text(
            text = "Thati Air Alert",
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        
        // Description
        Text(
            text = "လေကြောင်းသတိပေးချက်များအတွက် အင်တာနက်မလိုအပ်သော အက်ပ်",
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 48.dp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )
        
        // Admin Mode Button
        Button(
            onClick = onAdminSelected,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFE53E3E)
            )
        ) {
            Text(
                text = "🚨 Admin Mode",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        
        Text(
            text = "သတိပေးချက်များ ပေးပို့ရန်",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        
        // User Mode Button
        Button(
            onClick = onUserSelected,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF38A169)
            )
        ) {
            Text(
                text = "👤 User Mode",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        
        Text(
            text = "သတိပေးချက်များ လက်ခံရန်",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        
        // Additional Features Section
        Text(
            text = "အပိုဆောင်း လုပ်ဆောင်ချက်များ",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // Additional Features Buttons
        AdditionalFeaturesButtons()
    }
}

@Composable
fun AdditionalFeaturesButtons() {
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // Map Button
    OutlinedButton(
        onClick = { 
            context.startActivity(Intent(context, MapActivity::class.java))
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = "🗺️ Myanmar Map",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
    
    // Alert History Button
    OutlinedButton(
        onClick = { 
            context.startActivity(Intent(context, AlertHistoryActivity::class.java))
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = "📋 Alert History",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Permission Screen
 * Permission များ တောင်းရန်အတွက် screen
 */
@Composable
fun PermissionScreen(
    onRequestPermissions: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "⚠️",
            fontSize = 64.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Text(
            text = "Permission လိုအပ်သည်",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Text(
            text = "App ကို အသုံးပြုရန် အခုလို permission များ လိုအပ်ပါသည်:\n\n" +
                    "• Location (Wi-Fi Direct အတွက်)\n" +
                    "• Bluetooth (BLE အတွက်)\n" +
                    "• Notification (သတိပေးချက်များအတွက်)\n" +
                    "• Audio (Alarm အတွက်)",
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 32.dp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )
        
        Button(
            onClick = onRequestPermissions,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Permission ပေးရန်",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}