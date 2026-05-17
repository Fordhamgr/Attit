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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class UserAttendanceData(
    val userId: String,
    val email: String,
    val totalClasses: Int,
    val classesAttended: Int
)

class AdminViewModel : ViewModel() {
    var usersList by mutableStateOf<List<UserAttendanceData>>(emptyList())
    var isLoading by mutableStateOf(true)
    private var dataFetched = false

    fun fetchUsers(db: FirebaseFirestore) {
        if (dataFetched) return

        viewModelScope.launch {
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
                dataFetched = true
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    fun clearData() {
        usersList = emptyList()
        dataFetched = false
        isLoading = true
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    onLogout: () -> Unit,
    onStudentClick: (String, String) -> Unit,
    viewModel: AdminViewModel = viewModel()
) {
    val db = Firebase.firestore
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.fetchUsers(db)
    }

    val filteredUsers = viewModel.usersList.filter { it.email.contains(searchQuery, ignoreCase = true) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF2F4F7))
    ) {
        // --- PREMIUM RED HEADER ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF8B0000), Color(0xFFD32F2F))
                    ),
                    shape = RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp)
                )
        )

        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Admin Portal",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.SansSerif
                        )
                        Text(
                            text = "Dashboard",
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.SansSerif
                        )
                    }

                    IconButton(
                        onClick = {
                            viewModel.clearData()
                            onLogout()
                        },
                        modifier = Modifier.background(Color.White.copy(alpha = 0.2f), CircleShape).size(48.dp)
                    ) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Logout", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Box(
                    modifier = Modifier.background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp)).padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Total Students: ${viewModel.usersList.size}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.SansSerif
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text("Search student email...", color = Color.Gray, fontFamily = FontFamily.SansSerif)
                    },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                    modifier = Modifier.fillMaxWidth().height(56.dp).shadow(12.dp, RoundedCornerShape(28.dp), spotColor = Color(0xFF8B0000)),
                    shape = RoundedCornerShape(28.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        cursorColor = Color(0xFFD32F2F)
                    ),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- SPINNER & LIST SECTION ---
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (viewModel.isLoading) {
                    // FIXED: This full-size wrapper stops the glitch and strictly centers it
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(56.dp),
                            color = Color(0xFFD32F2F),
                            strokeWidth = 5.dp
                        )
                    }
                } else if (filteredUsers.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No students found.",
                            color = Color.Gray,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 40.dp)
                    ) {
                        items(filteredUsers) { user ->
                            AdminUserCard(user, onClick = { onStudentClick(user.userId, user.email) })
                            Spacer(modifier = Modifier.height(16.dp))
                        }
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
        else -> Color(0xFFD32F2F)
    }

    Card(
        modifier = Modifier.fillMaxWidth().shadow(elevation = 8.dp, shape = RoundedCornerShape(24.dp), spotColor = Color(0x1A000000)).clip(RoundedCornerShape(24.dp)).clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(modifier = Modifier.padding(20.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(modifier = Modifier.size(52.dp).background(Color(0xFFFFEBEE), CircleShape), contentAlignment = Alignment.Center) {
                    Text(
                        text = user.email.take(1).uppercase(),
                        color = Color(0xFFD32F2F),
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        fontFamily = FontFamily.SansSerif
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = user.email.substringBefore("@"),
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Color(0xFF111111),
                        fontFamily = FontFamily.SansSerif
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = user.email,
                        fontSize = 13.sp,
                        color = Color(0xFF888888),
                        fontFamily = FontFamily.SansSerif
                    )
                }
            }
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(56.dp)) {
                CircularProgressIndicator(progress = { 1f }, modifier = Modifier.fillMaxSize(), color = Color(0xFFF0F0F0), strokeWidth = 5.dp)
                CircularProgressIndicator(progress = { progressAnim }, modifier = Modifier.fillMaxSize(), color = statusColor, strokeWidth = 5.dp)
                Text(
                    text = "${attendancePercentage.toInt()}%",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = statusColor,
                    fontFamily = FontFamily.SansSerif
                )
            }
        }
    }
}