package com.example.attit.screens // or com.example.attit.components, check your package name!

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.attit.model.AttendanceLog

@Composable
fun AttendanceCalendar(logs: List<AttendanceLog>) {
    val calendar = java.util.Calendar.getInstance()
    val currentMonth = calendar.get(java.util.Calendar.MONTH)
    val currentYear = calendar.get(java.util.Calendar.YEAR)
    val daysInMonth = calendar.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)

    // Format: "January 2026"
    val monthName = java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.getDefault()).format(calendar.time)

    // OPTIMIZATION: Filter logs ONCE for this month
    val logsThisMonth = remember(logs) {
        logs.filter { log ->
            val c = java.util.Calendar.getInstance()
            c.timeInMillis = log.timestamp
            c.get(java.util.Calendar.MONTH) == currentMonth &&
                    c.get(java.util.Calendar.YEAR) == currentYear
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Text(
                text = monthName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            val days = (1..daysInMonth).toList()
            val chunkedDays = days.chunked(7)

            chunkedDays.forEach { week ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    week.forEach { day ->
                        // 1. Get logs for this specific day
                        val dayLogs = logsThisMonth.filter { log ->
                            val c = java.util.Calendar.getInstance()
                            c.timeInMillis = log.timestamp
                            c.get(java.util.Calendar.DAY_OF_MONTH) == day
                        }

                        // 2. MIXED COLOR LOGIC
                        val hasPresent = dayLogs.any { it.status.trim().equals("Present", ignoreCase = true) }
                        val hasAbsent = dayLogs.any { it.status.trim().equals("Absent", ignoreCase = true) }

                        val dayColor = when {
                            hasPresent && hasAbsent -> Color(0xFFFFC107) // Yellow (Mixed)
                            hasAbsent -> MaterialTheme.colorScheme.error // Red
                            hasPresent -> MaterialTheme.colorScheme.primary // Green
                            else -> Color.Transparent
                        }

                        // 3. Render
                        Box(
                            modifier = Modifier.size(40.dp).padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (dayColor != Color.Transparent) {
                                Canvas(modifier = Modifier.matchParentSize()) { drawCircle(color = dayColor) }
                            }
                            Text(
                                text = day.toString(),
                                color = if (dayColor == Color.Transparent) Color.Black else Color.White,
                                fontWeight = if (dayColor == Color.Transparent) FontWeight.Normal else FontWeight.Bold
                            )
                        }
                    }
                    // Spacer for empty days
                    if (week.size < 7) {
                        repeat(7 - week.size) { Spacer(modifier = Modifier.size(40.dp)) }
                    }
                }
            }
        }
    }
}