package com.example.attit.screens

import android.app.Activity
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.attit.R
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.delay

enum class LoginState {
    CHOOSER, STUDENT_EMAIL, ADMIN
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    isLoading: Boolean = false,
    onGoogleSignInClick: () -> Unit,
    onEmailLoginSuccess: (isAdmin: Boolean) -> Unit
) {
    // SYSTEM BAR TRANSPARENCY
    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = (view.context as Activity).window
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        val insetsController = WindowCompat.getInsetsController(window, view)
        insetsController.isAppearanceLightStatusBars = true
        insetsController.isAppearanceLightNavigationBars = true
        onDispose {
            insetsController.isAppearanceLightStatusBars = false
            insetsController.isAppearanceLightNavigationBars = false
        }
    }

    val context = LocalContext.current
    val auth = Firebase.auth

    // UI States
    var currentView by remember { mutableStateOf(LoginState.CHOOSER) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isSignUpMode by remember { mutableStateOf(false) }
    var isEmailLoading by remember { mutableStateOf(false) }

    // Lottie Animation
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.login_animation))
    val progress by animateLottieCompositionAsState(composition = composition, iterations = Int.MAX_VALUE)

    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(250)
        isVisible = true
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // --- HERO ANIMATION (Always visible at the top) ---
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                LottieAnimation(
                    composition = composition,
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(0.85f).aspectRatio(1f)
                )
            }

            // --- DYNAMIC BOTTOM SECTION ---
            AnimatedVisibility(
                visible = isVisible,
                enter = slideInVertically(initialOffsetY = { 100 }, animationSpec = tween(600)) + fadeIn(animationSpec = tween(600))
            ) {
                AnimatedContent(
                    targetState = currentView,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(400)) togetherWith fadeOut(animationSpec = tween(400))
                    },
                    label = "login_transitions"
                ) { targetState ->
                    when (targetState) {

                        // ==========================================
                        // 1. THE CLEAN CHOOSER (Default View)
                        // ==========================================
                        LoginState.CHOOSER -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(bottom = 24.dp)) {
                                Text("Hello Student", style = MaterialTheme.typography.displaySmall.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.ExtraBold, letterSpacing = (-1).sp), color = Color.Black.copy(alpha = 0.85f))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Your attendance, simplified.\nBy the students, for the students.", style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, lineHeight = 24.sp), color = Color.Gray, textAlign = TextAlign.Center)
                                Spacer(modifier = Modifier.height(48.dp))

                                // Button 1: Google
                                Button(
                                    onClick = onGoogleSignInClick,
                                    modifier = Modifier.fillMaxWidth().height(56.dp).shadow(12.dp, RoundedCornerShape(28.dp), spotColor = Color(0xFF6C63FF).copy(alpha = 0.3f)),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1A1A)),
                                    shape = RoundedCornerShape(28.dp)
                                ) {
                                    if (isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                                    else Text("Continue with Google", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Button 2: Email
                                OutlinedButton(
                                    onClick = { currentView = LoginState.STUDENT_EMAIL },
                                    modifier = Modifier.fillMaxWidth().height(56.dp),
                                    shape = RoundedCornerShape(28.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray)
                                ) {
                                    Text("Continue with Email", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                                }

                                Spacer(modifier = Modifier.height(32.dp))

                                // Button 3: Admin Link
                                Text(
                                    text = "Admin Access",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold, color = Color.Gray),
                                    modifier = Modifier.clickable { currentView = LoginState.ADMIN }.padding(8.dp)
                                )
                            }
                        }

                        // ==========================================
                        // 2. STUDENT EMAIL FORM
                        // ==========================================
                        LoginState.STUDENT_EMAIL -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(bottom = 24.dp)) {
                                Text(if (isSignUpMode) "Create Account" else "Welcome Back", style = MaterialTheme.typography.headlineMedium.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.ExtraBold), color = Color.Black)
                                Spacer(modifier = Modifier.height(24.dp))

                                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), singleLine = true)
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), singleLine = true)
                                Spacer(modifier = Modifier.height(24.dp))

                                Button(
                                    onClick = {
                                        if (email.isNotEmpty() && password.isNotEmpty()) {
                                            isEmailLoading = true
                                            if (isSignUpMode) {
                                                auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                                                    isEmailLoading = false
                                                    if (task.isSuccessful) onEmailLoginSuccess(false)
                                                    else Toast.makeText(context, "Sign Up Failed", Toast.LENGTH_SHORT).show()
                                                }
                                            } else {
                                                auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                                                    isEmailLoading = false
                                                    if (task.isSuccessful) onEmailLoginSuccess(false)
                                                    else Toast.makeText(context, "Login Failed", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(56.dp).shadow(8.dp, RoundedCornerShape(28.dp)),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1A1A)), shape = RoundedCornerShape(28.dp)
                                ) {
                                    if (isEmailLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                                    else Text(if (isSignUpMode) "Sign Up" else "Login", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = if (isSignUpMode) "Already have an account? " else "Don't have an account? ", color = Color.Gray, fontSize = 14.sp)
                                    Text(text = if (isSignUpMode) "Login" else "Sign Up", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.clickable { isSignUpMode = !isSignUpMode }.padding(4.dp))
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                TextButton(onClick = { currentView = LoginState.CHOOSER }) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Back to options", color = Color.Gray)
                                }
                            }
                        }

                        // ==========================================
                        // 3. SECURE ADMIN FORM (Red Theme)
                        // ==========================================
                        LoginState.ADMIN -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(bottom = 24.dp)) {
                                Text("Admin Portal", style = MaterialTheme.typography.headlineMedium.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.ExtraBold), color = Color(0xFFD32F2F))
                                Spacer(modifier = Modifier.height(24.dp))

                                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Admin Email") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), singleLine = true)
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), singleLine = true)
                                Spacer(modifier = Modifier.height(24.dp))

                                Button(
                                    onClick = {
                                        if (email.isNotEmpty() && password.isNotEmpty()) {
                                            if (email.lowercase() != "admin@attit.com") {
                                                Toast.makeText(context, "Unauthorized Email", Toast.LENGTH_SHORT).show()
                                                return@Button
                                            }
                                            isEmailLoading = true
                                            auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                                                isEmailLoading = false
                                                if (task.isSuccessful) onEmailLoginSuccess(true)
                                                else Toast.makeText(context, "Admin Login Failed", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(56.dp).shadow(8.dp, RoundedCornerShape(28.dp), spotColor = Color(0xFFD32F2F)),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)), shape = RoundedCornerShape(28.dp)
                                ) {
                                    if (isEmailLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                                    else Text("Secure Login", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                                }

                                Spacer(modifier = Modifier.height(24.dp))
                                TextButton(onClick = { currentView = LoginState.CHOOSER }) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Back to options", color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}