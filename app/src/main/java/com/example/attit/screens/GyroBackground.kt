package com.example.attit.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import kotlin.random.Random

object GyroThemes {
    const val BUBBLES = "Bubbles"
    const val LEAVES = "Autumn"
    const val SPACE = "Space"
    const val ZEN = "Zen"
}

@Composable
fun GyroBackground(
    currentTheme: String,
    xTiltProvider: () -> Float, // <--- OPTIMIZATION: Lambda
    yTiltProvider: () -> Float  // <--- OPTIMIZATION: Lambda
) {
    // 1. BACKGROUND COLORS (Darkened for Contrast)
    // Remembered to avoid calculation on every frame
    val bgColor = remember(currentTheme) {
        when(currentTheme) {
            GyroThemes.SPACE -> Color(0xFF0B1021)
            GyroThemes.LEAVES -> Color(0xFFF5EFE0)
            GyroThemes.ZEN -> Color(0xFF181818)
            else -> Color(0xFFEEF2F6)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(bgColor)) {
        when (currentTheme) {
            GyroThemes.LEAVES -> AestheticAutumnTheme(xTiltProvider, yTiltProvider)
            GyroThemes.SPACE -> OptimizedSpaceTheme(xTiltProvider, yTiltProvider)
            GyroThemes.ZEN -> ZenStaticTheme()
            else -> BubbleTheme(xTiltProvider, yTiltProvider)
        }
    }
}

// ==========================================
// 1. AESTHETIC AUTUMN (Optimized)
// ==========================================
@Composable
fun AestheticAutumnTheme(xTiltProvider: () -> Float, yTiltProvider: () -> Float) {
    val infiniteTransition = rememberInfiniteTransition(label = "leaves")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(15000, easing = LinearEasing)), label = "time"
    )

    val leaves = remember {
        List(20) {
            LeafData(
                id = it,
                xStart = Random.nextFloat(),
                yStart = Random.nextFloat(),
                size = Random.nextFloat() * 40f + 20f,
                color = listOf(
                    Color(0xFFD84315),
                    Color(0xFFFF8F00),
                    Color(0xFFBF360C),
                    Color(0xFF8D6E63)
                ).random().copy(alpha = Random.nextFloat() * 0.4f + 0.4f),
                speed = Random.nextFloat() * 0.5f + 0.5f,
                swayAmount = Random.nextFloat() * 50f + 20f
            )
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // READ TILT INSIDE DRAW SCOPE
        val xTilt = xTiltProvider()
        // val yTilt = yTiltProvider() // Not used in leaves currently, but available

        leaves.forEach { leaf ->
            val progress = (time * leaf.speed + leaf.yStart) % 1f
            val currentY = progress * (h + 100) - 50
            val sway = kotlin.math.sin(time * 2 * Math.PI * leaf.speed) * leaf.swayAmount
            val wind = xTilt * 40f

            val currentX = (leaf.xStart * w) + sway + wind

            rotate(degrees = (progress * 360f * 2) + (xTilt * 20), pivot = Offset(currentX.toFloat(), currentY)) {
                drawOval(
                    color = leaf.color,
                    topLeft = Offset(currentX.toFloat(), currentY),
                    size = Size(leaf.size, leaf.size / 1.8f)
                )
            }
        }
    }
}

data class LeafData(
    val id: Int, val xStart: Float, val yStart: Float,
    val size: Float, val color: Color, val speed: Float, val swayAmount: Float
)

// ==========================================
// 2. OPTIMIZED SPACE (Optimized)
// ==========================================
@Composable
fun OptimizedSpaceTheme(xTiltProvider: () -> Float, yTiltProvider: () -> Float) {
    val starCount = 45
    val starData = remember { List(starCount) { Triple(Math.random().toFloat(), Math.random().toFloat(), Math.random().toFloat()) } }

    // Remember brush to save allocations
    val nebulaBrush = remember {
        Brush.radialGradient(listOf(Color(0xFF6366F1).copy(alpha = 0.08f), Color.Transparent))
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // READ TILT INSIDE DRAW SCOPE
        val xTilt = xTiltProvider()
        val yTilt = yTiltProvider()

        // Darker Nebula for better contrast
        drawCircle(
            brush = nebulaBrush,
            center = Offset(w * 0.2f, h * 0.3f),
            radius = w * 0.6f
        )

        starData.forEach { (rx, ry, depth) ->
            val parallaxX = (xTilt * 30f) * depth
            val parallaxY = (yTilt * 30f) * depth
            val x = (rx * w + parallaxX) % w
            val y = (ry * h + parallaxY) % h
            val finalX = if (x < 0) x + w else x
            val finalY = if (y < 0) y + h else y

            drawCircle(
                color = Color.White.copy(alpha = 0.3f + (depth * 0.5f)),
                radius = 1f + (depth * 2f),
                center = Offset(finalX, finalY)
            )
        }
    }
}

// ==========================================
// 3. BUBBLES (Optimized)
// ==========================================
@Composable
fun BubbleTheme(xTiltProvider: () -> Float, yTiltProvider: () -> Float) {
    // Remember complex brushes
    val bottomOrbBrush = remember {
        Brush.radialGradient(listOf(Color(0xFFFFD54F).copy(alpha = 0.4f), Color.Transparent))
    }
    val topOrbColor = remember { Color(0xFFC5CAE9).copy(alpha = 0.5f) }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // READ TILT INSIDE DRAW SCOPE
        val xTilt = xTiltProvider()
        val yTilt = yTiltProvider()

        // 1. Top Left Orb (Darker Indigo)
        drawCircle(
            color = topOrbColor, // Much richer color
            radius = 180.dp.toPx(),
            center = Offset(w * 0.2f + xTilt * 15f, h * 0.2f + yTilt * 15f)
        )

        // 2. Bottom Right Orb (Darker Amber)
        drawCircle(
            brush = bottomOrbBrush,
            radius = 140.dp.toPx(),
            center = Offset(w * 0.5f + xTilt * 60f, h * 0.5f - yTilt * 60f)
        )
    }
}

// ==========================================
// 4. ZEN THEME (Static)
// ==========================================
@Composable
fun ZenStaticTheme() {
    val zenBrush = remember {
        Brush.verticalGradient(
            colors = listOf(Color(0xFF1E1E1E), Color(0xFF000000))
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(zenBrush)
    )
}