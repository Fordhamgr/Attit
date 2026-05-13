package com.example.attit.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
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

    // Fetch subjects just for this student
    LaunchedEffect(userId) {
        try {
            val snapshot = db.collection("students")
                .document(userId)
                .collection("subjects")
                .get()
                .await()

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Student Details", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text(studentEmail, fontSize = 12.sp, color = Color.Gray)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White, titleContentColor = Color.Black),
                modifier = Modifier.shadow(elevation = 4.dp, spotColor = Color.LightGray)
            )
        },
        containerColor = Color(0xFFF7F8FA)
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Color.Black)
            } else if (subjectsList.isEmpty()) {
                Text("This student hasn't added any subjects yet.", color = Color.Gray, modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    item {
                        Text("Subjects Enrolled (${subjectsList.size})", fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.padding(bottom = 12.dp, start = 4.dp))
                    }
                    items(subjectsList) { subject ->
                        AdminSubjectCard(subject)
                        Spacer(modifier = Modifier.height(12.dp))
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
        else -> Color(0xFFF44336) // Red
    }

    Card(
        modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(16.dp), spotColor = Color.LightGray),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = subject.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Black)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Goal: ${subject.goal}%", fontSize = 12.sp, color = Color.Gray)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(50.dp)) {
                    CircularProgressIndicator(progress = { 1f }, modifier = Modifier.fillMaxSize(), color = Color(0xFFEEEEEE), strokeWidth = 4.dp)
                    CircularProgressIndicator(progress = { progressAnim }, modifier = Modifier.fillMaxSize(), color = statusColor, strokeWidth = 4.dp)
                    Text(text = "${percentage.toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = statusColor)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = "${subject.attended} / ${subject.total}", fontSize = 12.sp, color = Color.DarkGray, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}