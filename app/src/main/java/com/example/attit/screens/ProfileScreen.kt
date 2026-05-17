package com.example.attit.screens

import android.content.Intent
import com.example.attit.utils.ApkDownloader
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.attit.GoogleAuthClient
import com.example.attit.utils.AvatarUtils
import com.example.attit.utils.ThemeEngine
import com.example.attit.utils.AppConfig
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    googleAuth: GoogleAuthClient,
    viewModel: com.example.attit.viewmodel.HomeViewModel,
    userEmail: String,
    onLogout: () -> Unit,
    onBack: () -> Unit,
    onAboutClick: () -> Unit,
    globalXTilt: Float,
    globalYTilt: Float
) {
    val context = LocalContext.current
    val ApkDownloader = remember { ApkDownloader(context) }
    val savedName by viewModel.savedUsername.collectAsStateWithLifecycle()
    val currentAvatarId by viewModel.currentAvatar.collectAsStateWithLifecycle()
    val currentTheme by viewModel.currentTheme.collectAsStateWithLifecycle()

    // --- DYNAMIC COLORS ---
    val themeColors = remember(currentTheme) { ThemeEngine.getColorsFor(currentTheme) }
    val isDarkTheme = themeColors.isDark

    var usernameInput by remember { mutableStateOf("") }
    var isEditing by remember { mutableStateOf(false) }
    var showAvatarDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showQrExpanded by remember { mutableStateOf(false) }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // --- NEW: Update Dialog States ---
    var showUpdateDialog by remember { mutableStateOf(false) }
    var newVersionAvailable by remember { mutableStateOf("") }
    var downloadUrl by remember { mutableStateOf("") }
    var isChecking by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadUserProfile(userEmail)
        if (qrBitmap == null) {
            val uid = googleAuth.getSignedInUser() ?: "unknown"
            withContext(Dispatchers.IO) { qrBitmap = generateQRCode(uid) }
        }
    }

    LaunchedEffect(savedName) {
        if (savedName.isNotEmpty()) usernameInput = savedName
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Row(
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LiquidIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        onClick = onBack,
                        isDark = isDarkTheme,
                        tint = themeColors.titleColor
                    )
                }
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.Start
            ) {

                // --- 1. BIG TITLE ---
                item {
                    Text(
                        text = "Profile",
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = themeColors.titleColor,
                        modifier = Modifier.padding(bottom = 24.dp, top = 8.dp)
                    )
                }

                // --- 2. IDENTITY CARD ---
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Avatar
                        Box(modifier = Modifier.size(80.dp)) {
                            val avatarRes = AvatarUtils.getAvatarResId(currentAvatarId)
                            if (avatarRes != null) {
                                Image(
                                    painter = painterResource(id = avatarRes),
                                    contentDescription = "Avatar",
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(CircleShape)
                                        .clickable { showAvatarDialog = true }
                                        .border(2.dp, themeColors.cardBackground, CircleShape)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                        .clickable { showAvatarDialog = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        userEmail.take(1).uppercase(),
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(themeColors.titleColor)
                                    .border(2.dp, themeColors.cardBackground, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Edit, null, tint = themeColors.cardBackground, modifier = Modifier.size(12.dp))
                            }
                        }

                        Spacer(modifier = Modifier.width(20.dp))

                        // Text Info
                        Column {
                            Text(
                                text = if (savedName.isNotEmpty()) savedName else "Student",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = themeColors.titleColor
                            )
                            Text(
                                text = userEmail,
                                style = MaterialTheme.typography.bodyMedium,
                                color = themeColors.bodyColor
                            )
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }

                // --- 3. QUICK ACTIONS ROW ---
                item {
                    Text(
                        "Quick Actions",
                        style = MaterialTheme.typography.labelMedium,
                        color = themeColors.bodyColor.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // BUTTON 1: MY PASS (QR)
                        QuickActionCard(
                            icon = Icons.Default.QrCode,
                            label = "Pass",
                            subLabel = "View QR",
                            isDark = isDarkTheme,
                            themeColor = themeColors.cardBackground,
                            contentColor = themeColors.titleColor,
                            modifier = Modifier.weight(1f),
                            onClick = { showQrExpanded = true }
                        )

                        // BUTTON 2: UPDATE (FIRESTORE LOGIC)
                        QuickActionCard(
                            icon = Icons.Default.SystemUpdate,
                            label = if (isChecking) "Checking..." else "Update",
                            subLabel = "Version ${AppConfig.VERSION}",
                            isDark = isDarkTheme,
                            themeColor = themeColors.cardBackground,
                            contentColor = Color(0xFF4CAF50), // Green
                            modifier = Modifier.weight(1f),
                            onClick = {
                                if (!isChecking) {
                                    isChecking = true
                                    val db = Firebase.firestore
                                    // Fetch 'app_data' from 'config' collection
                                    db.collection("config").document("app_data").get()
                                        .addOnSuccessListener { document ->
                                            isChecking = false
                                            if (document != null && document.exists()) {
                                                val latest = document.getString("latest_version") ?: AppConfig.VERSION
                                                val url = document.getString("download_url") ?: ""

                                                if (latest != AppConfig.VERSION) {
                                                    // Update available!
                                                    newVersionAvailable = latest
                                                    downloadUrl = url
                                                    showUpdateDialog = true
                                                } else {
                                                    // Already latest
                                                    Toast.makeText(context, "You are up to date! 🚀", Toast.LENGTH_SHORT).show()
                                                }
                                            } else {
                                                Toast.makeText(context, "Config not found", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                        .addOnFailureListener {
                                            isChecking = false
                                            Toast.makeText(context, "Network Error", Toast.LENGTH_SHORT).show()
                                        }
                                }
                            }
                        )

                        // BUTTON 3: SHARE (IMPROVED INTENT)
                        QuickActionCard(
                            icon = Icons.Default.Share,
                            label = "Share",
                            subLabel = "Friends",
                            isDark = isDarkTheme,
                            themeColor = themeColors.cardBackground,
                            contentColor = Color(0xFF2196F3), // Blue
                            modifier = Modifier.weight(1f),
                            onClick = {
                                val sendIntent: Intent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, AppConfig.SHARE_TEXT)
                                    type = "text/plain"
                                }
                                val shareIntent = Intent.createChooser(sendIntent, "Share App")
                                context.startActivity(shareIntent)
                            }
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(32.dp)) }

                // --- 4. PREFERENCES ---
                item {
                    SettingsSectionTitle("Preferences", color = themeColors.bodyColor)

                    // Theme Selector
                    LiquidContainer(isDarkTheme, themeColors.cardBackground) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Palette, null, tint = Color(0xFF6C63FF))
                                Spacer(modifier = Modifier.width(16.dp))
                                Text("App Theme", fontWeight = FontWeight.SemiBold, color = themeColors.cardTextColor)
                            }
                            Spacer(modifier = Modifier.height(12.dp))

                            val themes = remember { listOf(GyroThemes.BUBBLES, GyroThemes.LEAVES, GyroThemes.SPACE, GyroThemes.ZEN) }
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(themes) { theme ->
                                    val isSelected = currentTheme == theme
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when(theme) {
                                                    GyroThemes.SPACE -> Color(0xFF0F172A)
                                                    GyroThemes.LEAVES -> Color(0xFFFF9800)
                                                    GyroThemes.ZEN -> Color(0xFF424242)
                                                    else -> Color(0xFFE0E7FF)
                                                }
                                            )
                                            .border(if(isSelected) 2.dp else 0.dp, if(isSelected) themeColors.titleColor else Color.Transparent, CircleShape)
                                            .clickable { viewModel.updateTheme(theme) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if(isSelected) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Username
                    val usernameError by viewModel.usernameTakenError.collectAsStateWithLifecycle()

                    LaunchedEffect(savedName) {
                        if (savedName.isNotEmpty()) {
                            usernameInput = savedName
                            isEditing = false // Auto-close editing on success
                        }
                    }

                    LiquidContainer(isDarkTheme, themeColors.cardBackground) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Person, null, tint = Color(0xFF6C63FF))
                                Spacer(modifier = Modifier.width(16.dp))

                                if (isEditing) {
                                    OutlinedTextField(
                                        value = usernameInput,
                                        onValueChange = { 
                                            usernameInput = it
                                            viewModel.resetUsernameError()
                                        },
                                        placeholder = { Text("Enter new username") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color(0xFF6C63FF),
                                            unfocusedBorderColor = themeColors.bodyColor.copy(alpha = 0.2f),
                                            focusedTextColor = themeColors.cardTextColor,
                                            unfocusedTextColor = themeColors.cardTextColor,
                                            cursorColor = Color(0xFF6C63FF)
                                        )
                                    )
                                } else {
                                    Text("Username", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), color = themeColors.cardTextColor)
                                    Text(if (usernameInput.isNotEmpty()) "@$usernameInput" else "Set Name", color = themeColors.bodyColor, fontSize = 14.sp)
                                }

                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(onClick = {
                                    if (isEditing) {
                                        if (usernameInput.isNotEmpty() && usernameInput != savedName) {
                                            viewModel.saveUsername(userEmail, usernameInput)
                                        } else {
                                            isEditing = false
                                        }
                                    } else {
                                        isEditing = true
                                        viewModel.resetUsernameError()
                                    }
                                }) {
                                    Icon(if (isEditing) Icons.Default.Check else Icons.Default.Edit, null, tint = themeColors.bodyColor)
                                }
                            }

                            AnimatedVisibility(visible = usernameError && isEditing) {
                                Row(
                                    modifier = Modifier
                                        .padding(top = 12.dp)
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFFFEBEE))
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Info, contentDescription = "Error", tint = Color(0xFFD32F2F), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("This username is already taken.", color = Color(0xFFD32F2F), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(32.dp)) }

                item {
                    SettingsSectionTitle("Support", color = themeColors.bodyColor)

                    // About
                    SettingsListItem(
                        icon = Icons.Default.Info,
                        title = "About ${AppConfig.APP_NAME}",
                        subtitle = "Version ${AppConfig.VERSION}",
                        iconColor = Color(0xFF2196F3),
                        textColor = themeColors.cardTextColor,
                        isDarkTheme = isDarkTheme,
                        themeColor = themeColors.cardBackground,
                        onClick = onAboutClick
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Logout
                    SettingsListItem(
                        icon = Icons.Default.ExitToApp,
                        title = "Log Out",
                        iconColor = Color(0xFFE53935),
                        textColor = Color(0xFFE53935),
                        isDarkTheme = isDarkTheme,
                        themeColor = themeColors.cardBackground,
                        onClick = { showLogoutDialog = true }
                    )
                }

                item { Spacer(modifier = Modifier.height(100.dp)) }
            }
        }
    }

    // --- DIALOGS ---

    // 1. UPDATE DIALOG (New)
    if (showUpdateDialog) {
        AlertDialog(
            onDismissRequest = { showUpdateDialog = false },
            title = { Text("Update Available!", fontWeight = FontWeight.Bold) },
            text = { Text("Version $newVersionAvailable is ready.\n\nNew features are waiting for you.") },
            confirmButton = {
                Button(
                    onClick = {
                        // 1. Close the dialog
                        showUpdateDialog = false

                        // 2. Get the target URL
                        val urlToOpen = if (downloadUrl.isNotEmpty()) downloadUrl else AppConfig.DOWNLOAD_URL

                        // 3. Trigger the seamless background download & auto-install!
                        ApkDownloader.downloadAndInstall(urlToOpen)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) { Text("Download") }
            },
            dismissButton = {
                TextButton(onClick = { showUpdateDialog = false }) { Text("Later") }
            },
            containerColor = Color.White
        )
    }

    // 2. QR DIALOG
    if (showQrExpanded) {
        AlertDialog(
            onDismissRequest = { showQrExpanded = false },
            title = { Text("Your Digital ID", textAlign = TextAlign.Center) },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    if (qrBitmap != null) {
                        Image(
                            bitmap = qrBitmap!!.asImageBitmap(),
                            contentDescription = "QR Code",
                            modifier = Modifier.size(220.dp)
                        )
                    } else {
                        CircularProgressIndicator()
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showQrExpanded = false }) { Text("Close") } },
            containerColor = Color.White
        )
    }

    // 3. AVATAR DIALOG
    if (showAvatarDialog) {
        AlertDialog(
            onDismissRequest = { showAvatarDialog = false },
            title = { Text("Choose Avatar") },
            text = {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.height(300.dp)
                ) {
                    items(AvatarUtils.avatars) { (id, resId) ->
                        Image(
                            painter = painterResource(id = resId),
                            contentDescription = null,
                            modifier = Modifier.size(70.dp).clip(CircleShape).clickable { viewModel.updateAvatar(userEmail, id); showAvatarDialog = false }
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showAvatarDialog = false }) { Text("Close") } },
            containerColor = Color.White
        )
    }

    // 4. LOGOUT DIALOG
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Log Out?") },
            text = { Text("Are you sure you want to sign out?") },
            confirmButton = {
                Button(onClick = { showLogoutDialog = false; CoroutineScope(Dispatchers.IO).launch { googleAuth.signOut(); CoroutineScope(Dispatchers.Main).launch { onLogout() } } }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Log Out") }
            },
            dismissButton = { TextButton(onClick = { showLogoutDialog = false }) { Text("Cancel") } },
            containerColor = Color.White
        )
    }
}

// ==========================================
// LIQUID UI HELPERS (PRIVATE)
// ==========================================

@Composable
private fun QuickActionCard(
    icon: ImageVector,
    label: String,
    subLabel: String,
    isDark: Boolean,
    themeColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    // Prevent crash from rapid clicking
    var lastClickTime by remember { mutableStateOf(0L) }

    LiquidContainer(
        isDarkTheme = isDark,
        themeColor = themeColor,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .height(80.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable {
                val now = System.currentTimeMillis()
                if (now - lastClickTime > 500) { // 500ms debounce
                    lastClickTime = now
                    onClick()
                }
            }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(contentColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = contentColor, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(label, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if(isDark) Color.White else Color.Black.copy(0.8f))
            Text(subLabel, fontSize = 10.sp, color = if(isDark) Color.White.copy(0.5f) else Color.Black.copy(0.5f))
        }
    }
}

@Composable
private fun LiquidContainer(
    isDarkTheme: Boolean,
    themeColor: Color,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(20.dp),
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val bgAlpha = if (isDarkTheme) 0.15f else 0.65f
    val tintColor = if (isDarkTheme) themeColor else Color(0xFFF2F2F7)

    val rimLightBrush = remember {
        Brush.verticalGradient(
            colors = listOf(Color.White.copy(alpha = 0.5f), Color.White.copy(alpha = 0.05f))
        )
    }

    val liquidSurfaceBrush = remember(tintColor, bgAlpha) {
        Brush.linearGradient(
            colors = listOf(
                tintColor.copy(alpha = bgAlpha + 0.1f),
                tintColor.copy(alpha = bgAlpha)
            ),
            start = Offset(0f, 0f),
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        )
    }

    Box(
        modifier = modifier
            .border(width = 1.dp, brush = rimLightBrush, shape = shape)
            .clip(shape)
            .background(brush = liquidSurfaceBrush)
    ) {
        content()
    }
}

@Composable
private fun SettingsListItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    iconColor: Color,
    textColor: Color,
    isDarkTheme: Boolean,
    themeColor: Color,
    onClick: () -> Unit
) {
    LiquidContainer(isDarkTheme = isDarkTheme, themeColor = themeColor) {
        Row(
            modifier = Modifier
                .clickable { onClick() }
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = iconColor, modifier = Modifier.size(20.dp))
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, color = textColor, fontFamily = FontFamily.SansSerif)
                if (subtitle != null) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = textColor.copy(alpha = 0.6f))
                }
            }

            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = textColor.copy(alpha = 0.4f))
        }
    }
}

@Composable
private fun SettingsSectionTitle(title: String, color: Color) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        ),
        color = color.copy(alpha = 0.7f),
        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
    )
}
fun generateQRCode(content: String): Bitmap? {
    return try {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, 512, 512)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bmp.setPixel(x, y, if (bitMatrix[x, y]) AndroidColor.BLACK else AndroidColor.WHITE)
            }
        }
        bmp
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}