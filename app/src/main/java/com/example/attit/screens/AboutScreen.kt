package com.example.attit.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.attit.utils.AppConfig
import com.example.attit.utils.ThemeEngine
import com.example.attit.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    viewModel: HomeViewModel, // REQUIRED: To access the current theme
    onBack: () -> Unit
) {
    val context = LocalContext.current

    // 1. DYNAMIC THEME ENGINE
    // We fetch the current theme so the Glass Color matches (Blue for Space, Beige for Autumn)
    val currentTheme by viewModel.currentTheme.collectAsStateWithLifecycle()
    val themeColors = remember(currentTheme) { ThemeEngine.getColorsFor(currentTheme) }
    val isDark = themeColors.isDark

    // 2. GLASS STYLING (Auto-Adapts)
    // Dark Mode: Dark glass with white text. Light Mode: White glass with dark text.
    val glassBgColor = if (isDark) Color.Black.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.65f)
    val glassBorderStart = if (isDark) Color.White.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.8f)
    val glassBorderEnd = if (isDark) Color.White.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.2f)

    val textColor = themeColors.titleColor
    val subTextColor = themeColors.bodyColor

    Scaffold(
        containerColor = Color.Transparent, // Allows Gyro Background to show through
        topBar = {
            Row(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isDarkTheme = true
                LiquidIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    onClick = onBack,
                    isDark = isDarkTheme,
                    tint = themeColors.titleColor
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // --- APP TITLE ---
            Text(
                text = AppConfig.APP_NAME,
                style = MaterialTheme.typography.displayMedium.copy(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp,
                    color = textColor
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // --- VERSION PILL ---
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0xFF6C63FF).copy(alpha = 0.15f))
                    .border(1.dp, Color(0xFF6C63FF).copy(alpha = 0.3f), CircleShape)
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "v${AppConfig.VERSION} Stable",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = Color(0xFF6C63FF),
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif
                    )
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // --- THE GLASS CARD ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(32.dp))
                    // Gradient Border (Rim Light)
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(glassBorderStart, glassBorderEnd)
                        ),
                        shape = RoundedCornerShape(32.dp)
                    )
                    // Glass Background
                    .background(glassBgColor)
                    .padding(28.dp)
            ) {
                Column {
                    // Mission Section
                    Text(
                        AppConfig.MISSION_TITLE.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = subTextColor.copy(alpha = 0.6f),
                            letterSpacing = 1.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        AppConfig.MISSION_DESC,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = textColor.copy(alpha = 0.9f),
                            lineHeight = 24.sp,
                            fontFamily = FontFamily.SansSerif
                        )
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider(color = subTextColor.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(24.dp))

                    // Developer Section
                    Text(
                        AppConfig.DEV_TITLE.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = subTextColor.copy(alpha = 0.6f),
                            letterSpacing = 1.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        AppConfig.DEV_DESC,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = textColor.copy(alpha = 0.9f),
                            fontFamily = FontFamily.SansSerif,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // --- BUG REPORT BUTTON (Themed Gradient) ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFFE53935), Color(0xFFD32F2F))
                        )
                    )
                    .clickable { sendBugReport(context) }
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.BugReport, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "Report a Bug",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        fontFamily = FontFamily.SansSerif
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Found a bug? Report it here directly.",
                style = MaterialTheme.typography.bodySmall,
                color = subTextColor.copy(alpha = 0.5f),
                fontFamily = FontFamily.SansSerif
            )
        }
    }
}

// --- EMAIL HELPER ---
fun sendBugReport(context: Context) {
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:")
        putExtra(Intent.EXTRA_EMAIL, arrayOf(AppConfig.CONTACT_EMAIL))
        putExtra(Intent.EXTRA_SUBJECT, AppConfig.BUG_REPORT_SUBJECT)
        putExtra(Intent.EXTRA_TEXT, "Describe the bug here:\n\n\n\nDevice Info: ${Build.MODEL} (Android ${Build.VERSION.RELEASE})")
    }
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        // Handle no email app (User might not have one installed)
    }
}