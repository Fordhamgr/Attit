package com.example.attit.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Define the Light Color Scheme using our new palette
private val LightColorScheme = lightColorScheme(
    primary = NanoGreenPrimary,
    onPrimary = NanoWhiteSurface,
    primaryContainer = NanoGreenLight,
    onPrimaryContainer = NanoGreenPrimary,

    background = NanoBeigeBackground,
    onBackground = NanoTextPrimary,

    surface = NanoWhiteSurface,
    onSurface = NanoTextPrimary,
    surfaceVariant = NanoOffWhiteSurface, // For slightly different card backgrounds
    onSurfaceVariant = NanoTextSecondary,

    error = NanoError,
    onError = NanoWhiteSurface
)

// We can skip the dark theme for a moment to focus on nailing the light aesthetic first,
// or just map it to the light theme temporarily.

@Composable
fun AttitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is cool, but we want OUR specific look right now, so turn it off.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // For now, forcing Light Scheme to match the design inspiration
    val colorScheme = LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Set status bar color to match background for a seamless look
            window.statusBarColor = colorScheme.background.toArgb()
            // Ensure dark icons on the light status bar background
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography, // We'll keep default typography for now
        content = content
    )
}