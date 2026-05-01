package com.example.attit.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.attit.model.Subject
import com.example.attit.viewmodel.HomeViewModel
import com.example.attit.utils.ThemeEngine
import com.example.attit.R
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    userEmail: String,
    onSubjectClick: (String) -> Unit,
    sharedTransitionScope: androidx.compose.animation.SharedTransitionScope,
    animatedVisibilityScope: androidx.compose.animation.AnimatedVisibilityScope,
    onProfileClick: () -> Unit
) {
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()
    val savedName by viewModel.savedUsername.collectAsStateWithLifecycle()
    val currentTheme by viewModel.currentTheme.collectAsStateWithLifecycle()

    // OPTIMIZATION: Remember expensive objects
    val themeColors = remember(currentTheme) { ThemeEngine.getColorsFor(currentTheme) }
    val showAddDialog by viewModel.isAddDialogVisible.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.loadUserProfile(userEmail) }

    val greeting = remember {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 5..11 -> "Good Morning,"
            in 12..16 -> "Afternoon,"
            in 17..20 -> "Good Evening,"
            else -> "Late Night,"
        }
    }

    // Fix: Using random() so it changes every time the screen is loaded/app restarted
    val randomSlogan = remember {
        val slogans = listOf(
            "“Attendance first, excuses later.”", 
            "Present today, successful tomorrow.", 
            "“Your future called — it wants you in class.”", 
            "Teacher said: ‘Be present.’ Literally.",
            "Being absent won’t make you brilliant.",
            "Your chair misses you when you’re gone.",
            "If you can read this, you should be in class.",
            "Being late is still better than being absent.",
            "Attendance so easy, yet you fail it."
        )
        slogans.random()
    }

    val daysOfWeek = remember {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        val days = mutableListOf<Pair<String, Int>>()
        (0..6).forEach { i ->
            val dayName = SimpleDateFormat("EE", Locale.getDefault()).format(cal.time)
            val dayNum = cal.get(Calendar.DAY_OF_MONTH)
            days.add(dayName to dayNum)
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
        days
    }
    val todayDayNum = remember { Calendar.getInstance().get(Calendar.DAY_OF_MONTH) }

    // FADE MASK
    val fadeBrush = remember {
        Brush.verticalGradient(
            0f to Color.Transparent,
            0.15f to Color.Black,
            1.0f to Color.Black
        )
    }

    Scaffold(containerColor = Color.Transparent) { padding ->
        Column(modifier = Modifier.fillMaxSize()) {

            // HEADER
            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                Spacer(modifier = Modifier.height(padding.calculateTopPadding() + 4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            buildAnnotatedString {
                                withStyle(style = SpanStyle(fontWeight = FontWeight.Normal, fontFamily = FontFamily.SansSerif, fontSize = 32.sp, color = themeColors.titleColor)) {
                                    append(greeting)
                                }
                                append("\n")
                                withStyle(style = SpanStyle(fontWeight = FontWeight.Normal, fontFamily = FontFamily.SansSerif, color = Color(0xFF6C63FF), fontSize = 32.sp)) {
                                    append(if (savedName.isNotEmpty()) savedName else "Student")
                                }
                            },
                            lineHeight = 36.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = randomSlogan, style = MaterialTheme.typography.bodyMedium, color = themeColors.bodyColor.copy(alpha = 0.8f), fontFamily = FontFamily.SansSerif, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                LazyRow(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    items(daysOfWeek) { (dayName, dayNum) ->
                        val isToday = dayNum == todayDayNum
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 6.dp).width(40.dp)) {
                            Text(text = dayName.uppercase(), style = MaterialTheme.typography.labelSmall, color = if (isToday) Color(0xFF6C63FF) else themeColors.bodyColor.copy(alpha = 0.6f), fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.SansSerif)
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(modifier = Modifier.size(42.dp).clip(CircleShape).background(if (isToday) themeColors.cardBackground.copy(alpha = 0.9f) else themeColors.cardBackground.copy(alpha = 0.2f)).border(width = 1.dp, color = if(isToday) Color(0xFF6C63FF).copy(alpha = 0.3f) else themeColors.cardBackground.copy(alpha = 0.3f), shape = CircleShape), contentAlignment = Alignment.Center) {
                                Text(text = dayNum.toString(), style = MaterialTheme.typography.bodyLarge, color = if (isToday) Color(0xFF6C63FF) else themeColors.titleColor.copy(alpha = 0.7f), fontWeight = if(isToday) FontWeight.Bold else FontWeight.Normal, fontFamily = FontFamily.SansSerif)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // SCROLLING LIST AREA (With Mask)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithContent {
                        drawContent()
                        drawRect(brush = fadeBrush, blendMode = BlendMode.DstIn)
                    }
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    contentPadding = PaddingValues(top = 10.dp, bottom = 200.dp, start = 24.dp, end = 24.dp)
                ) {
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Daily Plan", style = MaterialTheme.typography.bodyLarge.copy(color = themeColors.titleColor.copy(alpha = 0.7f), fontWeight = FontWeight.Medium, fontFamily = FontFamily.SansSerif))
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    if (subjects.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                                val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.empty_state))
                                val progress by animateLottieCompositionAsState(
                                    composition = composition,
                                    iterations = Int.MAX_VALUE
                                )
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    LottieAnimation(
                                        composition = composition,
                                        progress = { progress },
                                        modifier = Modifier.size(200.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                    Text("No subjects yet. Tap + to start.", color = themeColors.bodyColor, fontFamily = FontFamily.SansSerif)
                                }
                            }
                        }
                    } else {
                        items(items = subjects, key = { it.id }) { subject ->
                            LiquidSubjectCard(
                                subject = subject,
                                cardBgColor = themeColors.cardBackground,
                                textColor = themeColors.cardTextColor,
                                subTextColor = themeColors.bodyColor,
                                onClick = { onSubjectClick(subject.id.toString()) },
                                onPresent = { viewModel.markPresent(subject) },
                                onAbsent = { viewModel.markAbsent(subject) },
                                onUndo = { viewModel.undoLastEntry(subject) },
                                isDarkTheme = themeColors.isDark
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var goal by remember { mutableStateOf("75") }

        AlertDialog(
            onDismissRequest = { viewModel.closeAddDialog() },
            title = {
                Text(
                    "New Subject",
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 24.sp,
                    color = themeColors.titleColor
                )
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Subject Name") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF6C63FF),
                            unfocusedBorderColor = themeColors.bodyColor.copy(alpha = 0.3f),
                            focusedLabelColor = Color(0xFF6C63FF),
                            focusedTextColor = themeColors.titleColor,
                            unfocusedTextColor = themeColors.titleColor,
                            cursorColor = Color(0xFF6C63FF)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = goal,
                        onValueChange = { goal = it },
                        label = { Text("Attendance Goal %") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF6C63FF),
                            unfocusedBorderColor = themeColors.bodyColor.copy(alpha = 0.3f),
                            focusedLabelColor = Color(0xFF6C63FF),
                            focusedTextColor = themeColors.titleColor,
                            unfocusedTextColor = themeColors.titleColor,
                            cursorColor = Color(0xFF6C63FF)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotEmpty()) {
                            val goalVal = goal.toIntOrNull() ?: 75
                            viewModel.addSubject(name, goalVal)
                            viewModel.closeAddDialog()
                        }
                    },
                    modifier = Modifier.height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C63FF))
                ) { Text("Add Subject", fontWeight = FontWeight.Bold, fontFamily = FontFamily.SansSerif) }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.closeAddDialog() },
                    modifier = Modifier.height(48.dp)
                ) { Text("Cancel", color = themeColors.bodyColor, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.SansSerif) }
            },
            shape = RoundedCornerShape(32.dp),
            containerColor = themeColors.cardBackground,
            tonalElevation = 8.dp
        )
    }
}

// ---------------------------------------------------------
// OPTIMIZED CARD (Visual Glass Trick Applied)
// ---------------------------------------------------------
@Composable
fun LiquidSubjectCard(
    subject: Subject,
    cardBgColor: Color,
    textColor: Color,
    subTextColor: Color,
    onClick: () -> Unit,
    onPresent: () -> Unit,
    onAbsent: () -> Unit,
    onUndo: () -> Unit,
    isDarkTheme: Boolean
) {
    val progressValue = (subject.percentage / 100f).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progressValue,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
        label = "progress"
    )

    val statusColor = getStatusColor(subject.percentage, subject.goal)
    val isLowAttendance = subject.percentage < 75f && subject.total > 0
    val warningText = if (isLowAttendance) "⚠️ Low Attendance" else "🛡️ Safe Zone"
    val warningColor = if (isLowAttendance) Color(0xFFE53935) else Color(0xFF4CAF50)

    val bgAlpha = if (isDarkTheme) 0.15f else 0.65f
    val tintColor = if (isDarkTheme) cardBgColor else Color(0xFFF2F2F7)

    // 1. RIM LIGHT (Outer Glow)
    val rimLightBrush = remember {
        Brush.verticalGradient(
            colors = listOf(Color.White.copy(alpha = 0.5f), Color.White.copy(alpha = 0.05f))
        )
    }

    // 2. GLASS SURFACE (Semi-Transparent)
    val liquidSurfaceBrush = remember(tintColor, bgAlpha) {
        Brush.linearGradient(
            colors = listOf(tintColor.copy(alpha = bgAlpha + 0.1f), tintColor.copy(alpha = bgAlpha)),
            start = Offset(0f, 0f),
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        )
    }

    // 3. VISUAL TRICK: Glass Highlight (Simulates Refraction without Physics)
    val glassHighlightBrush = remember {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.1f), // Shine start
                Color.Transparent,              // Clear Middle
                Color.White.copy(alpha = 0.05f) // Shine End
            ),
            start = Offset(0f, 0f),
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY) // Diagonal
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .border(width = 1.dp, brush = rimLightBrush, shape = RoundedCornerShape(32.dp))
            .border(width = 0.5.dp, color = statusColor.copy(alpha = 0.3f), shape = RoundedCornerShape(32.dp))
            .clip(RoundedCornerShape(32.dp))
            // Layer the backgrounds: Base + Highlight
            .background(brush = liquidSurfaceBrush)
            .background(brush = glassHighlightBrush)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier.size(52.dp),
                    color = subTextColor.copy(alpha = 0.1f),
                    strokeWidth = 4.dp,
                    trackColor = ProgressIndicatorDefaults.circularTrackColor,
                    strokeCap = ProgressIndicatorDefaults.CircularDeterminateStrokeCap,
                )
                CircularProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.size(52.dp),
                    color = statusColor,
                    strokeWidth = 4.dp,
                    trackColor = ProgressIndicatorDefaults.circularTrackColor,
                    strokeCap = StrokeCap.Round,
                )
                Text(text = "${subject.percentage.toInt()}%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, fontFamily = FontFamily.SansSerif, color = textColor.copy(alpha = 0.9f))
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = subject.name, style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp), maxLines = 1, color = textColor.copy(alpha = 0.95f))
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "${subject.attended} / ${subject.total} Classes", style = MaterialTheme.typography.bodySmall, color = subTextColor.copy(alpha = 0.8f), fontFamily = FontFamily.SansSerif)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = warningText, style = MaterialTheme.typography.labelSmall, color = warningColor, fontWeight = FontWeight.Bold)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassActionButton("P", Color(0xFF388E3C), Color(0xFFE8F5E9), onPresent)
                GlassActionButton("A", Color(0xFFD32F2F), Color(0xFFFFEBEE), onAbsent)
                GlassIconActionButton(Icons.Default.Refresh, subTextColor, subTextColor.copy(alpha = 0.1f), onUndo)
            }
        }
    }
}

// ... Helper Functions ...
@Composable
fun GlassActionButton(text: String, textColor: Color, bgColor: Color, onClick: () -> Unit) { Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(bgColor).clickable { onClick() }, contentAlignment = Alignment.Center) { Text(text, color = textColor, fontWeight = FontWeight.Bold, fontSize = 16.sp, fontFamily = FontFamily.SansSerif) } }
@Composable
fun GlassIconActionButton(icon: ImageVector, tint: Color, bgColor: Color, onClick: () -> Unit) { Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(bgColor).clickable { onClick() }, contentAlignment = Alignment.Center) { Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp)) } }
fun getStatusColor(current: Float, goal: Int): Color { return if (current >= goal) Color(0xFF4CAF50) else if (current >= goal - 10) Color(0xFFFFC107) else Color(0xFFE53935) }