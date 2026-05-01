package com.example.attit.screens

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.attit.R
import com.example.attit.utils.AppConfig
import kotlinx.coroutines.delay

@Composable
fun LoginScreen(isLoading: Boolean = false, onSignInClick: () -> Unit) {
    // 1. SYSTEM BAR TRANSPARENCY & RESET
    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = (view.context as Activity).window
        // Save original states to restore later if needed (optional, but good practice)

        // A. Make App Edge-to-Edge (Content goes behind bars)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT

        // B. Set Icons to DARK (Since background is White)
        val insetsController = WindowCompat.getInsetsController(window, view)
        insetsController.isAppearanceLightStatusBars = true
        insetsController.isAppearanceLightNavigationBars = true

        onDispose {
            // C. RESET Icons to LIGHT (Crucial for your Dark Home Screen)
            // If we don't do this, Home Screen icons will stay dark and be invisible.
            insetsController.isAppearanceLightStatusBars = false
            insetsController.isAppearanceLightNavigationBars = false
        }
    }

    // 2. Lottie Setup
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.login_animation))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = Int.MAX_VALUE
    )

    // 3. Entrance Animation State (single run, no recomposition loop)
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(250)
        isVisible = true
    }

    // 4. Main Layout
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                // Add padding for the transparent system bars so content isn't covered
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp)) // Reduced top spacing since we have statusBarsPadding

            // --- HERO ANIMATION ---
            Box(
                modifier = Modifier
                    .weight(1f) // Takes up available space
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                LottieAnimation(
                    composition = composition,
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth(0.9f) // 90% width
                        .aspectRatio(1f) // Keep square aspect ratio
                )
            }

            // --- TEXT & BUTTON SECTION ---
            AnimatedVisibility(
                visible = isVisible,
                enter = slideInVertically(initialOffsetY = { 100 }, animationSpec = tween(600)) +
                        fadeIn(animationSpec = tween(600))
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(bottom = 40.dp)
                ) {
                    // Big Modern Title
                    Text(
                        text = "Hello Student",
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-1).sp
                        ),
                        color = Color.Black.copy(alpha = 0.85f)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Subtitle
                    Text(
                        text = "Your attendance, simplified.\nBy the students, for the students.",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 24.sp
                        ),
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(48.dp))

                    // --- BUTTON ---
                    Button(
                        onClick = onSignInClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .shadow(12.dp, RoundedCornerShape(28.dp), spotColor = Color(0xFF6C63FF).copy(alpha = 0.3f)), // Colored Shadow
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1A1A1A) // Almost Black (Premium Feel)
                        ),
                        shape = RoundedCornerShape(28.dp) // Full Pill Shape
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    "Continue with Google",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.SansSerif
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Small Footer
                    Text(
                        text = "Version ${AppConfig.VERSION} • Powered by ${AppConfig.APP_NAME}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.LightGray
                    )
                }
            }
        }
    }
}