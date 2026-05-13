package com.example.attit.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

data class UserAttendanceData(
    val userId: String,
    val email: String,
    val totalClasses: Int,
    val classesAttended: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    onLogout: () -> Unit,
    onStudentClick: (String, String) -> Unit // <-- NEW: Passes ID and Email to the next screen
) {
    val db = Firebase.firestore
    var usersList by remember { mutableStateOf<List<UserAttendanceData>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            val snapshot = db.collection("users").get().await()
            val fetchedUsers = mutableListOf<UserAttendanceData>()

            for (doc in snapshot.documents) {
                val email = doc.getString("email") ?: continue
                val uid = doc.getString("uid") ?: continue

                if (email.lowercase() == "admin@attit.com") continue

                val subjectsSnapshot = db.collection("students").document(uid).collection("subjects").get().await()

                var totalClasses = 0
                var classesAttended = 0

                for (subjectDoc in subjectsSnapshot.documents) {
                    totalClasses += subjectDoc.getLong("total")?.toInt() ?: 0
                    classesAttended += subjectDoc.getLong("attended")?.toInt() ?: 0
                }

                fetchedUsers.add(UserAttendanceData(uid, email, totalClasses, classesAttended))
            }
            usersList = fetchedUsers
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }

    val filteredUsers = usersList.filter { it.email.contains(searchQuery, ignoreCase = true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Dashboard", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White, titleContentColor = Color.Black),
                actions = { IconButton(onClick = onLogout) { Icon(Icons.Default.ExitToApp, contentDescription = "Logout", tint = Color.Black) } },
                modifier = Modifier.shadow(elevation = 4.dp, spotColor = Color.LightGray)
            )
        },
        containerColor = Color(0xFFF7F8FA)
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp),
                modifier = Modifier.fillMaxWidth().shadow(8.dp, RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp), spotColor = Color.LightGray)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Overview", color = Color.Gray, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Total Students: ${usersList.size}", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(20.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search student email...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(26.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color(0xFFE0E0E0), focusedBorderColor = Color.Black,
                            focusedContainerColor = Color(0xFFF5F5F5), unfocusedContainerColor = Color(0xFFF5F5F5)
                        ),
                        singleLine = true
                    )
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color.Black) }
            } else if (filteredUsers.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No students found.", color = Color.Gray) }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)) {
                    items(filteredUsers) { user ->
                        AdminUserCard(user, onClick = { onStudentClick(user.userId, user.email) }) // <-- CLICK TRIGGER
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun AdminUserCard(user: UserAttendanceData, onClick: () -> Unit) {
    val attendancePercentage = if (user.totalClasses > 0) (user.classesAttended.toFloat() / user.totalClasses.toFloat()) * 100 else 0f
    val progressAnim by animateFloatAsState(targetValue = attendancePercentage / 100f, animationSpec = tween(1000), label = "progress")
    val statusColor = when {
        attendancePercentage >= 75f -> Color(0xFF4CAF50)
        attendancePercentage >= 60f -> Color(0xFFFFC107)
        else -> Color(0xFFF44336)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(20.dp), spotColor = Color.LightGray.copy(alpha = 0.5f))
            .clickable { onClick() }, // <-- ADDED CLICKABLE MODIFIER
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(modifier = Modifier.padding(20.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(Color(0xFF1A1A1A)), contentAlignment = Alignment.Center) {
                    Text(text = user.email.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(text = user.email.substringBefore("@"), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                    Text(text = user.email, fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Attended: ${user.classesAttended} / ${user.totalClasses}", fontSize = 13.sp, color = Color.DarkGray, fontWeight = FontWeight.Medium)
                }
            }
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(56.dp)) {
                CircularProgressIndicator(progress = { 1f }, modifier = Modifier.fillMaxSize(), color = Color(0xFFEEEEEE), strokeWidth = 5.dp)
                CircularProgressIndicator(progress = { progressAnim }, modifier = Modifier.fillMaxSize(), color = statusColor, strokeWidth = 5.dp)
                Text(text = "${attendancePercentage.toInt()}%", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = statusColor)
            }
        }
    }
}