package com.example.attit.utils

import androidx.compose.ui.graphics.Color
import com.example.attit.screens.GyroThemes

// 1. Define what a "Theme" looks like in your app
data class BeastColors(
    val titleColor: Color,
    val bodyColor: Color,
    val cardBackground: Color,
    val cardTextColor: Color,
    val isDark: Boolean // Helper to know if we are in dark mode
)

// 2. The Magic Mapper (cached per theme to avoid repeated Color allocations)
object ThemeEngine {
    private val cache = mutableMapOf<String, BeastColors>()

    fun getColorsFor(theme: String): BeastColors {
        return cache.getOrPut(theme) {
            when (theme) {
            GyroThemes.SPACE -> {
                // DARK THEMES (Space, Rain) -> Light Text
                BeastColors(
                    titleColor = Color(0xFFF8FAFC), // Almost White
                    bodyColor = Color(0xFF94A3B8),  // Light Gray
                    cardBackground = Color(0xFF1E293B).copy(alpha = 0.8f), // Dark Glass
                    cardTextColor = Color(0xFFE2E8F0),
                    isDark = true
                )
            }
            GyroThemes.LEAVES -> {
                // AUTUMN THEME -> Warm Colors
                BeastColors(
                    titleColor = Color(0xFF3E2723), // Dark Brown
                    bodyColor = Color(0xFF5D4037),
                    cardBackground = Color.White.copy(alpha = 0.9f),
                    cardTextColor = Color.Black,
                    isDark = false
                )
            }
            // NEW: ZEN THEME (Clean Dark Mode or Light Mode, your choice. Let's do a Clean Dark.)
            GyroThemes.ZEN -> BeastColors(
                titleColor = Color(0xFFEEEEEE),
                bodyColor = Color(0xFFBDBDBD),
                cardBackground = Color(0xFF424242),
                cardTextColor = Color.White,
                isDark = true
            )
            else -> {
                // LIGHT THEMES (Bubbles) -> Dark Text
                BeastColors(
                    titleColor = Color.Black,
                    bodyColor = Color.Gray,
                    cardBackground = Color.White,
                    cardTextColor = Color.Black,
                    isDark = false
                )
            }
            }
        }
    }
}