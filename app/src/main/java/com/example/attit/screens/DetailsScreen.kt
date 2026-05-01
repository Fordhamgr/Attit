package com.example.attit.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.attit.model.AttendanceLog
import com.example.attit.utils.ThemeEngine
import com.example.attit.viewmodel.HomeViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreen(
    subjectId: String?,
    viewModel: HomeViewModel,
    onBack: () -> Unit,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    globalXTilt: Float,
    globalYTilt: Float
) {
    val context = LocalContext.current
    val id = subjectId?.toIntOrNull() ?: 0
    val logs by viewModel.getLogsForSubject(id).collectAsStateWithLifecycle(initialValue = emptyList())
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()
    val subject = subjects.find { it.id == id }

    // --- THEME ENGINE ---
    val currentTheme by viewModel.currentTheme.collectAsStateWithLifecycle()
    val themeColors = remember(currentTheme) { ThemeEngine.getColorsFor(currentTheme) }
    val isDarkTheme = themeColors.isDark

    // --- BACKGROUND COLOR ---
    // Make sure we explicitly fill with White/Black because transparency causes overlap issues for known themes, or default to themeColors
    val screenBackgroundColor = remember(currentTheme) {
        when {
            currentTheme == "Space" -> Color(0xFF0B1021)
            currentTheme == "Zen" -> Color(0xFF181818)
            isDarkTheme -> Color(0xFF121212)
            else -> Color(0xFFFDFDFD)
        }
    }

    var showSubjectDeleteDialog by remember { mutableStateOf(false) }
    var logToDelete by remember { mutableStateOf<AttendanceLog?>(null) }
    var showHistory by remember { mutableStateOf(false) }

    if (subject == null) {
        Box(modifier = Modifier.fillMaxSize().background(screenBackgroundColor))
        return
    }

    if (showSubjectDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showSubjectDeleteDialog = false },
            title = { Text("Delete Subject?", fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete '${subject.name}'?", fontFamily = FontFamily.SansSerif) },
            confirmButton = {
                Button(
                    onClick = { viewModel.deleteSubject(subject); onBack() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                ) { Text("Delete", fontFamily = FontFamily.SansSerif) }
            },
            dismissButton = { TextButton(onClick = { showSubjectDeleteDialog = false }) { Text("Cancel", fontFamily = FontFamily.SansSerif) } },
            containerColor = Color.White
        )
    }

    if (logToDelete != null) {
        AlertDialog(
            onDismissRequest = { logToDelete = null },
            title = { Text("Delete Entry?", fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold) },
            text = { Text("Remove this attendance record?", fontFamily = FontFamily.SansSerif) },
            confirmButton = {
                Button(
                    onClick = {
                        if (logToDelete != null) {
                            viewModel.deleteLog(logToDelete!!, subject)
                            Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show()
                            logToDelete = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                ) { Text("Delete", fontFamily = FontFamily.SansSerif) }
            },
            dismissButton = { TextButton(onClick = { logToDelete = null }) { Text("Cancel", fontFamily = FontFamily.SansSerif) } },
            containerColor = Color.White
        )
    }

    // --- ROOT CONTAINER ---
    // We apply the solid background HERE to ensure opacity over the Home Screen
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(screenBackgroundColor)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp, start = 24.dp, end = 24.dp, bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    LiquidIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        onClick = onBack,
                        isDark = isDarkTheme,
                        tint = themeColors.titleColor
                    )

                    Text(
                        text = subject.name,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.5).sp
                        ),
                        color = themeColors.titleColor,
                        maxLines = 1,
                        modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                        textAlign = TextAlign.Center
                    )

                    LiquidIconButton(
                        icon = Icons.Default.Delete,
                        onClick = { showSubjectDeleteDialog = true },
                        isDark = isDarkTheme,
                        tint = Color(0xFFE53935)
                    )
                }
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp)
            ) {
                item {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        LiquidCalendarCard(
                            logs = logs,
                            cardBg = themeColors.cardBackground,
                            textColor = themeColors.cardTextColor,
                            isDark = isDarkTheme
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        val classesNeeded = remember(subject.attended, subject.total, subject.goal) {
                            calculateClassesNeeded(subject.attended, subject.total, subject.goal)
                        }
                        val predictionText = if (subject.percentage >= subject.goal) {
                            "You are safe! Keep maintaining this pace. 🌟"
                        } else {
                            "You need to attend the next $classesNeeded classes to hit ${subject.goal}%."
                        }

                        LiquidDetailsContainer(
                            isDark = isDarkTheme,
                            themeColor = themeColors.cardBackground,
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Box(modifier = Modifier.padding(20.dp)) {
                                Text(
                                    text = predictionText,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = FontFamily.SansSerif,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    textAlign = TextAlign.Center,
                                    color = themeColors.bodyColor,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = "Current: ${subject.percentage.toInt()}%",
                            style = MaterialTheme.typography.headlineSmall.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold),
                            color = themeColors.titleColor
                        )
                        Text(
                            text = "Goal: ${subject.goal}%",
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.SansSerif),
                            color = themeColors.bodyColor
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        LiquidDetailsContainer(
                            isDark = isDarkTheme,
                            themeColor = themeColors.cardBackground,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.clickable { showHistory = !showHistory }
                        ) {
                            Box(modifier = Modifier.padding(vertical = 12.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        if (showHistory) "Hide History" else "View History Log",
                                        fontFamily = FontFamily.SansSerif,
                                        fontWeight = FontWeight.SemiBold,
                                        color = themeColors.bodyColor.copy(0.7f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(
                                        if (showHistory) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        "Toggle",
                                        tint = themeColors.bodyColor.copy(0.5f)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                if (showHistory) {
                    if (logs.isEmpty()) {
                        item {
                            Text(
                                "No history yet.",
                                modifier = Modifier.padding(top = 8.dp),
                                color = themeColors.bodyColor,
                                fontFamily = FontFamily.SansSerif
                            )
                        }
                    } else {
                        items(items = logs.sortedByDescending { it.timestamp }, key = { it.timestamp }) { log ->
                            Box(modifier = Modifier.animateItem()) {
                                LiquidHistoryCard(
                                    log = log,
                                    cardBg = themeColors.cardBackground,
                                    textColor = themeColors.cardTextColor,
                                    isDark = isDarkTheme,
                                    onDeleteClick = { logToDelete = log }
                                )
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(100.dp)) }
            }
        }
    }
}

// ... (KEEP ALL HELPERS: LiquidCalendarCard, CalendarMonthPage, CalendarMeta, DotDayCell, LiquidHistoryCard, LiquidDetailsContainer, LiquidIconButton, calculateClassesNeeded - EXACTLY AS THEY WERE) ...
private const val CALENDAR_PAST_MONTHS = 12
private const val CALENDAR_TOTAL_PAGES = CALENDAR_PAST_MONTHS * 2 + 1 // past + current + future

@Composable
fun LiquidCalendarCard(logs: List<AttendanceLog>, cardBg: Color, textColor: Color, isDark: Boolean) {
    val initialPage = CALENDAR_PAST_MONTHS
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { CALENDAR_TOTAL_PAGES })

    LiquidDetailsContainer(
        isDark = isDark,
        themeColor = cardBg,
        shape = RoundedCornerShape(32.dp),
        modifier = Modifier.height(400.dp)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            key = { it }
        ) { page ->
            CalendarMonthPage(monthOffset = page - initialPage, logs = logs, textColor = textColor)
        }
    }
}

@Composable
fun CalendarMonthPage(monthOffset: Int, logs: List<AttendanceLog>, textColor: Color) {
    val calendarData = remember(monthOffset) {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, monthOffset)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val monthName = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val gridOffset = (firstDayOfWeek - Calendar.MONDAY + 7) % 7
        CalendarMeta(monthName, daysInMonth, gridOffset, cal.get(Calendar.MONTH), cal.get(Calendar.YEAR))
    }

    val logMap = remember(logs, calendarData.monthIdx, calendarData.yearIdx) {
        val tempCal = Calendar.getInstance()
        logs.filter { log ->
            tempCal.timeInMillis = log.timestamp
            tempCal.get(Calendar.MONTH) == calendarData.monthIdx && tempCal.get(Calendar.YEAR) == calendarData.yearIdx
        }.groupBy { log ->
            tempCal.timeInMillis = log.timestamp
            tempCal.get(Calendar.DAY_OF_MONTH)
        }.mapValues { (_, dayLogs) ->
            val statuses = dayLogs.map { it.status }.toSet()
            if (statuses.contains("Present") && statuses.contains("Absent")) "Mixed" else if (statuses.contains("Present")) "Present" else "Absent"
        }
    }

    val todayDay = remember {
        val today = Calendar.getInstance()
        if (today.get(Calendar.MONTH) == calendarData.monthIdx && today.get(Calendar.YEAR) == calendarData.yearIdx) today.get(Calendar.DAY_OF_MONTH) else -1
    }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = calendarData.monthName,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Black,
                fontSize = 24.sp
            ),
            color = textColor
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach { day ->
                Text(
                    text = day,
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold),
                    color = textColor.copy(0.6f),
                    modifier = Modifier.width(32.dp),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        var currentDay = 1
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            for (row in 0 until 6) {
                if (currentDay > calendarData.daysInMonth) break
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    for (col in 0 until 7) {
                        val cellIndex = row * 7 + col
                        if (cellIndex < calendarData.gridOffset || currentDay > calendarData.daysInMonth) {
                            Spacer(modifier = Modifier.width(32.dp))
                        } else {
                            DotDayCell(day = currentDay, status = logMap[currentDay], isToday = (currentDay == todayDay), textColor = textColor)
                            currentDay++
                        }
                    }
                }
            }
        }
    }
}

data class CalendarMeta(val monthName: String, val daysInMonth: Int, val gridOffset: Int, val monthIdx: Int, val yearIdx: Int)

@Composable
fun DotDayCell(day: Int, status: String?, isToday: Boolean, textColor: Color) {
    Column(modifier = Modifier.width(32.dp).height(40.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(
            modifier = Modifier.size(30.dp)
                .then(if (isToday) Modifier.border(2.dp, Color(0xFF6C63FF), CircleShape) else Modifier),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = day.toString(),
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold),
                color = if (isToday) Color(0xFF6C63FF) else textColor.copy(0.9f)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        if (status != null) {
            val dotColor = if (status == "Present") Color(0xFF4CAF50) else if (status == "Absent") Color(0xFFE53935) else Color(0xFFFFC107)
            Box(modifier = Modifier.size(6.dp).background(dotColor, CircleShape))
        } else {
            Spacer(modifier = Modifier.size(6.dp))
        }
    }
}

private val historyDateFormat = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())

@Composable
fun LiquidHistoryCard(log: AttendanceLog, cardBg: Color, textColor: Color, isDark: Boolean, onDeleteClick: () -> Unit) {
    val dateString = remember(log.timestamp) { historyDateFormat.format(java.util.Date(log.timestamp)) }
    val isPresent = log.status == "Present"
    val statusColor = if (isPresent) Color(0xFF4CAF50) else Color(0xFFE53935)

    LiquidDetailsContainer(
        isDark = isDark,
        themeColor = cardBg,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Row(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).background(statusColor, CircleShape))
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = dateString, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium, fontFamily = FontFamily.SansSerif), color = textColor.copy(0.75f))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = log.status, color = statusColor, fontWeight = FontWeight.Bold, fontFamily = FontFamily.SansSerif, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.width(12.dp))
                IconButton(onClick = onDeleteClick, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, "Delete", tint = textColor.copy(0.4f), modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun LiquidDetailsContainer(
    isDark: Boolean,
    themeColor: Color,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(24.dp),
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val bgAlpha = if (isDark) 0.15f else 0.65f
    val tintColor = if (isDark) themeColor else Color(0xFFF2F2F7)

    val rimLightBrush = remember {
        Brush.verticalGradient(listOf(Color.White.copy(0.5f), Color.White.copy(0.05f)))
    }
    val surfaceBrush = remember(tintColor, bgAlpha) {
        Brush.linearGradient(
            colors = listOf(tintColor.copy(bgAlpha + 0.1f), tintColor.copy(bgAlpha)),
            start = Offset(0f, 0f), end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, rimLightBrush, shape)
            .clip(shape)
            .background(surfaceBrush)
    ) {
        content()
    }
}

@Composable
fun LiquidIconButton(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit, isDark: Boolean, tint: Color) {
    LiquidDetailsContainer(
        isDark = isDark,
        themeColor = Color.Transparent,
        shape = CircleShape,
        modifier = Modifier.size(48.dp).clickable { onClick() }
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(imageVector = icon, contentDescription = null, tint = tint)
        }
    }
}

fun calculateClassesNeeded(attended: Int, total: Int, goal: Int): Int {
    if (goal >= 100) return 999
    val currentPercent = if (total == 0) 0f else (attended.toFloat() / total) * 100
    if (currentPercent >= goal) return 0
    var needed = 0
    var tAtt = attended
    var tTot = total
    while ((tAtt.toFloat() / tTot * 100) < goal) { tAtt++; tTot++; needed++; if (needed > 100) break }
    return needed
}