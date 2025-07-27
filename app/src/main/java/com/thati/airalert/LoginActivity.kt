@file:OptIn(ExperimentalMaterial3Api::class)

package com.thati.airalert

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thati.airalert.ui.theme.ThatiAirAlertTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Login Activity - လုံခြုံရေး အကောင့်ဝင်ရောက်မှု
 */
class LoginActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Check for existing login session
        checkExistingLogin()
        
        setContent {
            ThatiAirAlertTheme {
                LoginScreen(
                    onLoginSuccess = { userType, region, username ->
                        // Save login session
                        saveLoginSession(userType, region, username)
                        
                        when (userType) {
                            UserType.MAIN_ADMIN -> {
                                startActivity(Intent(this@LoginActivity, MainAdminActivity::class.java))
                            }
                            UserType.REGIONAL_ADMIN -> {
                                val intent = Intent(this@LoginActivity, RegionalAdminActivity::class.java)
                                intent.putExtra("region", region)
                                startActivity(intent)
                            }
                            UserType.ONLINE_ADMIN -> {
                                // Online admin uses MainAdminActivity with special flag
                                val intent = Intent(this@LoginActivity, MainAdminActivity::class.java)
                                intent.putExtra("admin_type", "online")
                                startActivity(intent)
                            }
                            UserType.USER -> {
                                startActivity(Intent(this@LoginActivity, SimpleUserActivity::class.java))
                            }
                        }
                        finish()
                    },
                    onGuestAccess = {
                        startActivity(Intent(this@LoginActivity, SimpleUserActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }
    
    private fun checkExistingLogin() {
        val sharedPref = getSharedPreferences("thati_login", MODE_PRIVATE)
        val savedUserType = sharedPref.getString("user_type", null)
        val savedRegion = sharedPref.getString("region", "")
        val loginTime = sharedPref.getLong("login_time", 0)
        
        // Check if login is still valid (24 hours)
        val currentTime = System.currentTimeMillis()
        val twentyFourHours = 24 * 60 * 60 * 1000
        
        if (savedUserType != null && (currentTime - loginTime) < twentyFourHours) {
            // Auto-login based on saved session
            when (savedUserType) {
                "MAIN_ADMIN" -> {
                    startActivity(Intent(this, MainAdminActivity::class.java))
                    finish()
                }
                "REGIONAL_ADMIN" -> {
                    val intent = Intent(this, RegionalAdminActivity::class.java)
                    intent.putExtra("region", savedRegion)
                    startActivity(intent)
                    finish()
                }
                "ONLINE_ADMIN" -> {
                    val intent = Intent(this, MainAdminActivity::class.java)
                    intent.putExtra("admin_type", "online")
                    startActivity(intent)
                    finish()
                }
            }
        }
    }
    
    private fun saveLoginSession(userType: UserType, region: String, username: String) {
        val sharedPref = getSharedPreferences("thati_login", MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("user_type", userType.name)
            putString("region", region)
            putString("username", username)
            putLong("login_time", System.currentTimeMillis())
            apply()
        }
    }
}

enum class UserType {
    MAIN_ADMIN, REGIONAL_ADMIN, ONLINE_ADMIN, USER
}

data class AdminAccount(
    val username: String,
    val password: String,
    val userType: UserType,
    val region: String = "",
    val isActive: Boolean = true,
    val lastLogin: Long = 0L
)

// Enhanced admin accounts database with online admin
val adminAccounts = mutableListOf(
    AdminAccount("main_admin", "thati2024@secure", UserType.MAIN_ADMIN),
    AdminAccount("online_admin", "online@2024", UserType.ONLINE_ADMIN, "Myanmar"),
    AdminAccount("yangon_admin", "yangon@2024", UserType.REGIONAL_ADMIN, "ရန်ကုန်တိုင်းဒေသကြီး"),
    AdminAccount("mandalay_admin", "mandalay@2024", UserType.REGIONAL_ADMIN, "မန္တလေးတိုင်းဒေသကြီး"),
    AdminAccount("sagaing_admin", "sagaing@2024", UserType.REGIONAL_ADMIN, "စစ်ကိုင်းတိုင်းဒေသကြီး"),
    AdminAccount("bago_admin", "bago@2024", UserType.REGIONAL_ADMIN, "ပဲခူးတိုင်းဒေသကြီး")
)

@Composable
fun LoginScreen(
    onLoginSuccess: (UserType, String, String) -> Unit,
    onGuestAccess: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var loginAttempts by remember { mutableStateOf(0) }
    var isLocked by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A202C),
                        Color(0xFF2D3748),
                        Color(0xFF4A5568)
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
            // App Logo and Title
            Card(
                modifier = Modifier.padding(bottom = 32.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0x88000000)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🛡️",
                        fontSize = 48.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "သတိ",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Thati Air Alert System",
                        fontSize = 14.sp,
                        color = Color(0xFFE2E8F0),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "လုံခြုံရေး အကောင့်ဝင်ရောက်မှု",
                        fontSize = 12.sp,
                        color = Color(0xFFCBD5E0),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            
            // Login Form
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF7FAFC)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "🔐 အကောင့်ဝင်ရောက်ရန်",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2D3748),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    // Username Field
                    OutlinedTextField(
                        value = username,
                        onValueChange = { 
                            username = it
                            errorMessage = ""
                        },
                        label = { Text("အသုံးပြုသူအမည်") },
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLocked,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF3182CE),
                            focusedLabelColor = Color(0xFF3182CE)
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Password Field
                    OutlinedTextField(
                        value = password,
                        onValueChange = { 
                            password = it
                            errorMessage = ""
                        },
                        label = { Text("စကားဝှက်") },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null)
                        },
                        trailingIcon = {
                            TextButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Text(
                                    text = if (isPasswordVisible) "🙈" else "👁️",
                                    fontSize = 16.sp
                                )
                            }
                        },
                        visualTransformation = if (isPasswordVisible) 
                            VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLocked,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF3182CE),
                            focusedLabelColor = Color(0xFF3182CE)
                        )
                    )
                    
                    // Error Message
                    if (errorMessage.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMessage,
                            color = Color(0xFFE53E3E),
                            fontSize = 12.sp,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                    
                    // Lock Message
                    if (isLocked) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "⚠️ လုံခြုံရေးအတွက် ၃၀ စက္ကန့် စောင့်ပါ",
                            color = Color(0xFFED8936),
                            fontSize = 12.sp,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Login Button
                    Button(
                        onClick = {
                            if (username.isBlank() || password.isBlank()) {
                                errorMessage = "အသုံးပြုသူအမည် နှင့် စကားဝှက် ထည့်ပါ"
                                return@Button
                            }
                            
                            isLoading = true
                            scope.launch {
                                delay(1500) // Simulate authentication delay
                                
                                val account = adminAccounts.find { 
                                    it.username == username && it.password == password && it.isActive 
                                }
                                
                                if (account != null) {
                                    // Update last login time
                                    val accountIndex = adminAccounts.indexOfFirst { it.username == username }
                                    if (accountIndex != -1) {
                                        adminAccounts[accountIndex] = account.copy(lastLogin = System.currentTimeMillis())
                                    }
                                    onLoginSuccess(account.userType, account.region, account.username)
                                } else {
                                    loginAttempts++
                                    if (loginAttempts >= 3) {
                                        isLocked = true
                                        errorMessage = "လုံခြုံရေးအတွက် ၃၀ စက္ကန့် စောင့်ပါ"
                                        delay(30000)
                                        isLocked = false
                                        loginAttempts = 0
                                    } else {
                                        errorMessage = "အကောင့်အချက်အလက် မမှန်ကန်ပါ (${3-loginAttempts} ကြိမ် ကျန်)"
                                    }
                                }
                                isLoading = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading && !isLocked,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3182CE)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = if (isLoading) "စစ်ဆေးနေသည်..." else "🔓 ဝင်ရောက်ရန်",
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Demo Accounts Info
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F9FF)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = "Demo Accounts:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E40AF),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            Text(
                                text = "Main Admin: main_admin / thati2024@secure\n" +
                                        "Online Admin: online_admin / online@2024\n" +
                                        "Yangon Admin: yangon_admin / yangon@2024\n" +
                                        "Mandalay Admin: mandalay_admin / mandalay@2024",
                                fontSize = 10.sp,
                                color = Color(0xFF1E40AF)
                            )
                        }
                    }
                    
                    // Guest Access Button
                    OutlinedButton(
                        onClick = onGuestAccess,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF4A5568)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "👤 ဧည့်သည်အနေဖြင့် ဝင်ရောက်ရန်",
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Security Notice
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0x44E53E3E)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "🔒 ဤစနစ်သည် လုံခြုံရေး ကာကွယ်မှုဖြင့် ထိန်းချုပ်ထားပါသည်။\nအကောင့်အချက်အလက်များကို လျှို့ဝှက်စွာ သိမ်းဆည်းပါ။",
                    color = Color.White,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}