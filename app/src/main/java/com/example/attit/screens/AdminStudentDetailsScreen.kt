package com.example.attit.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

data class AdminSubjectData(
    val name: String,
    val attended: Int,
    val total: Int,
    val goal: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminStudentDetailsScreen(
    userId: String,
    studentEmail: String,
    onBack: () -> Unit
) {
    val db = Firebase.firestore
    var subjectsList by remember { mutableStateOf<List<AdminSubjectData>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(userId) {
        try {
            val snapshot = db.collection("students").document(userId).collection("subjects").get().await()
            subjectsList = snapshot.documents.map { doc ->
                AdminSubjectData(
                    name = doc.getString("name") ?: "Unknown Subject",
                    attended = doc.getLong("attended")?.toInt() ?: 0,
                    total = doc.getLong("total")?.toInt() ?: 0,
                    goal = doc.getLong("goal")?.toInt() ?: 75
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }

    // Root Container - completely solid to stop background leaks
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF2F4F7))
    ) {
        // --- CUSTOM RED HEADER (Fixes top edge leak) ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF8B0000), Color(0xFFD32F2F))
                    ),
                    shape = RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp)
                )
        )

        Column(modifier = Modifier.fillMaxSize()) {

            // --- HEADER CONTENT ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding() // Safely pushes below notch
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.2f), CircleShape)
                            .size(44.dp)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = "Student Overview",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.SansSerif
                        )
                        Text(
                            // Formats "name@email.com" to "Name"
                            text = studentEmail.substringBefore("@").replaceFirstChar { it.uppercase() },
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.SansSerif
                        )
                        Text(
                            text = studentEmail,
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 13.sp,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                }
            }

            // --- MAIN CONTENT AREA ---
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (isLoading) {
                    // FIXED: Properly centered full-size box so spinner can't glitch to the side
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(56.dp),
                            color = Color(0xFFD32F2F),
                            strokeWidth = 5.dp
                        )
                    }
                } else if (subjectsList.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No subjects enrolled.",
                            color = Color.Gray,
                            fontSize = 16.sp,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 40.dp)
                    ) {
                        item {
                            Text(
                                text = "Enrolled Subjects (${subjectsList.size})",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF666666),
                                fontSize = 14.sp,
                                fontFamily = FontFamily.SansSerif,
                                modifier = Modifier.padding(bottom = 16.dp, start = 8.dp)
                            )
                        }
                        items(subjectsList) { subject ->
                            AdminSubjectCard(subject)
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminSubjectCard(subject: AdminSubjectData) {
    val percentage = if (subject.total > 0) (subject.attended.toFloat() / subject.total.toFloat()) * 100 else 0f
    val progressAnim by animateFloatAsState(targetValue = percentage / 100f, animationSpec = tween(1000), label = "")

    val statusColor = when {
        percentage >= subject.goal -> Color(0xFF4CAF50) // Green
        percentage >= subject.goal - 10 -> Color(0xFFFFC107) // Yellow
        else -> Color(0xFFD32F2F) // Match Red Theme
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(24.dp), spotColor = Color(0x1A000000))
            .clip(RoundedCornerShape(24.dp)), // Keeps ripple effect inside the rounded corners
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            // Left Side: Icon & Info
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                // Sleek initial box
                Box(
                    modifier = Modifier.size(48.dp).background(Color(0xFFF3F4F6), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = subject.name.take(1).uppercase(),
                        color = Color(0xFF111111),
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        fontFamily = FontFamily.SansSerif
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = subject.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Color(0xFF111111),
                        fontFamily = FontFamily.SansSerif
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Goal: ${subject.goal}%",
                        fontSize = 13.sp,
                        color = Color(0xFF888888),
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Right Side: Progress
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(52.dp)) {
                    CircularProgressIndicator(progress = { 1f }, modifier = Modifier.fillMaxSize(), color = Color(0xFFF0F0F0), strokeWidth = 5.dp)
                    CircularProgressIndicator(progress = { progressAnim }, modifier = Modifier.fillMaxSize(), color = statusColor, strokeWidth = 5.dp)
                    Text(
                        text = "${percentage.toInt()}%",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = statusColor,
                        fontFamily = FontFamily.SansSerif
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${subject.attended} / ${subject.total}",
                    fontSize = 12.sp,
                    color = Color.DarkGray,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif
                )
            }
        }
    }
}