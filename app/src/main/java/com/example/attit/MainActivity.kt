package com.example.attit

import android.app.Activity
import android.app.Application
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.SystemClock
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.attit.data.AppDatabase
import com.example.attit.data.SubjectRepository
import com.example.attit.screens.*
import com.example.attit.ui.theme.AttitTheme
import com.example.attit.utils.ThemeEngine
import com.example.attit.viewmodel.HomeViewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import com.example.attit.liquid.LiquidState
import com.example.attit.liquid.rememberLiquidState
import com.example.attit.liquid.liquidLens
import kotlinx.coroutines.Dispatchers

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val googleAuth = GoogleAuthClient(this)
        val database = AppDatabase.getDatabase(this)

        setContent {
            val liquidState = rememberLiquidState()
            val context = LocalContext.current
            val application = context.applicationContext as Application

            // --- SENSORS (throttled ~30fps to reduce recomposition & battery) ---
            val scope = rememberCoroutineScope()
            val smoothX = remember { androidx.compose.animation.core.Animatable(0f) }
            val smoothY = remember { androidx.compose.animation.core.Animatable(0f) }

            DisposableEffect(Unit) {
                val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
                val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
                var lastUpdateMs = 0L
                val throttleMs = 32L // ~30fps for smooth but efficient updates
                val listener = object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent?) {
                        event ?: return
                        val now = SystemClock.elapsedRealtime()
                        if (now - lastUpdateMs < throttleMs) return
                        lastUpdateMs = now
                        val targetX = -event.values[0].coerceIn(-5f, 5f)
                        val targetY = event.values[1].coerceIn(-5f, 5f)
                        scope.launch {
                            launch { smoothX.animateTo(targetX, spring(dampingRatio = 0.75f, stiffness = 200f)) }
                            launch { smoothY.animateTo(targetY, spring(dampingRatio = 0.75f, stiffness = 200f)) }
                        }
                    }
                    override fun onAccuracyChanged(s: Sensor?, a: Int) {}
                }
                sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
                onDispose { sensorManager.unregisterListener(listener) }
            }

            // --- DATA & STATE ---
            val initialUser = googleAuth.getSignedInUser()
            var currentUserId by remember { mutableStateOf(initialUser) }
            var activeSubjectId by remember { mutableStateOf<String?>(null) }

            val repository = remember(currentUserId) {
                SubjectRepository(
                    database.subjectDao(), database.attendanceLogDao(), database.friendDao(),
                    if (currentUserId.isNullOrEmpty()) "temp_user" else currentUserId!!, applicationContext
                )
            }

            val homeViewModel: HomeViewModel = viewModel(
                key = currentUserId, factory = HomeViewModelFactory(repository, application)
            )

            // --- THEME COLORS ---
            val currentTheme by homeViewModel.currentTheme.collectAsStateWithLifecycle()
            val themeColors = remember(currentTheme) { ThemeEngine.getColorsFor(currentTheme) }

            // This is the background color for the VOID (behind the shrinking app)
            val fadeColor = remember(currentTheme) {
                when(currentTheme) {
                    "Space" -> Color(0xFF0B1021)
                    "Autumn" -> Color(0xFFF5EFE0)
                    "Zen" -> Color(0xFF181818)
                    else -> Color(0xFFEEF2F6)
                }
            }

            // Drawer card background (Matches theme so it isn't white in dark mode)
            val drawerBgColor = remember(currentTheme) {
                when(currentTheme) {
                    "Space" -> Color(0xFF0B1021)
                    "Autumn" -> Color(0xFFF5EFE0)
                    "Zen" -> Color(0xFF181818)
                    else -> Color(0xFFFFFFFF)
                }
            }

            val navController = rememberNavController()
            val currentBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = currentBackStackEntry?.destination?.route

            val view = LocalView.current
            if (!view.isInEditMode) {
                if (currentRoute != "login") {
                    SideEffect {
                        val window = (view.context as Activity).window
                        window.statusBarColor = android.graphics.Color.TRANSPARENT
                        window.navigationBarColor = android.graphics.Color.TRANSPARENT
                        val insets = WindowCompat.getInsetsController(window, view)
                        insets.isAppearanceLightStatusBars = !themeColors.isDark
                        insets.isAppearanceLightNavigationBars = !themeColors.isDark
                    }
                }
            }

            // Fail-safe: Close drawer on nav change
            LaunchedEffect(currentRoute) {
                if (activeSubjectId != null) activeSubjectId = null
            }

            // --- 3D ANIMATION LOGIC ---
            val drawerProgress by animateFloatAsState(
                targetValue = if (activeSubjectId != null) 1f else 0f,
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f),
                label = "drawer"
            )

            AttitTheme {
                BackHandler(enabled = activeSubjectId != null) {
                    activeSubjectId = null
                }

                // 1. ROOT BACKGROUND
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(fadeColor) // <--- THIS FILLS THE WHITE VOID
                ) {

                    // 2. LAYER A: THE HOME SCREEN (Scales Down)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                val scale = 1f - (0.08f * drawerProgress)
                                scaleX = scale
                                scaleY = scale
                                // Blend smoothly into the void color
                                alpha = 1f - (0.15f * drawerProgress)
                                translationY = (drawerProgress * 50.dp.toPx())

                                val cornerRadius = (32 * drawerProgress).coerceAtLeast(0f)
                                shape = RoundedCornerShape(cornerRadius.dp)
                                clip = true
                            }
                            .then(if (activeSubjectId != null) Modifier else Modifier)
                    ) {
                        if (currentRoute != "login" && currentRoute?.startsWith("admin") != true) {
                            Box(modifier = Modifier.fillMaxSize().liquidLens(liquidState)) {
                                GyroBackground(currentTheme = currentTheme, xTiltProvider = { smoothX.value }, yTiltProvider = { smoothY.value })
                            }
                        } else {
                            // Fixes the top bar leak! Fills the transparent gap with Dark Red for Admin, White for Login.
                            val bgColor = if (currentRoute?.startsWith("admin") == true) Color(0xFF8B0000) else Color.White
                            Box(modifier = Modifier.fillMaxSize().background(bgColor))
                        }

                        val scope = rememberCoroutineScope()
                        val showBottomBar = remember(currentRoute) { currentRoute != "login" && currentRoute != "admin_dashboard"&&currentRoute?.startsWith("admin_student_details") != true }
                        val currentUserEmail = Firebase.auth.currentUser?.email
                        val isRememberedAdmin = currentUserEmail == "admin@attit.com"

                        val startDestination = if (!initialUser.isNullOrEmpty()) {
                            if (isRememberedAdmin) "admin_dashboard" else "home"
                        } else {
                            "login"
                        }

                        Scaffold(containerColor = Color.Transparent, contentWindowInsets = WindowInsets.systemBars) { innerPadding ->
                            Box(modifier = Modifier.fillMaxSize().padding(top = innerPadding.calculateTopPadding())) {
                                SharedTransitionLayout {
                                    NavHost(
                                        navController = navController,
                                        startDestination = startDestination,
                                        enterTransition = { androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(200, delayMillis = 50)) },
                                        exitTransition = { androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(200)) },
                                        popEnterTransition = { androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(200, delayMillis = 50)) },
                                        popExitTransition = { androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(200)) }
                                    ) {

                                        // UPDATED LOGIN COMPOSABLE
                                        composable("login") {
                                            var isLoggingIn by remember { mutableStateOf(false) }
                                            val launcher = rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) { result ->
                                                if (result.resultCode == RESULT_OK) {
                                                    scope.launch {
                                                        if (googleAuth.signInWithIntent(result.data ?: return@launch)) {
                                                            currentUserId = googleAuth.getSignedInUser()
                                                            navController.navigate("home") { popUpTo("login") { inclusive = true } }
                                                        } else {
                                                            Toast.makeText(applicationContext, "Sign in failed", Toast.LENGTH_SHORT).show()
                                                            isLoggingIn = false
                                                        }
                                                    }
                                                } else {
                                                    isLoggingIn = false
                                                }
                                            }

                                            LoginScreen(
                                                isLoading = isLoggingIn,
                                                onGoogleSignInClick = {
                                                    isLoggingIn = true
                                                    launcher.launch(googleAuth.getSignInIntent())
                                                },
                                                onEmailLoginSuccess = { isAdmin ->
                                                    currentUserId = Firebase.auth.currentUser?.uid
                                                    if (isAdmin) {
                                                        navController.navigate("admin_dashboard") { popUpTo("login") { inclusive = true } }
                                                    } else {
                                                        navController.navigate("home") { popUpTo("login") { inclusive = true } }
                                                    }
                                                }
                                            )
                                        }

                                        composable("admin_dashboard") {
                                            AdminDashboardScreen(
                                                onLogout = {
                                                    scope.launch {
                                                        googleAuth.signOut()
                                                        currentUserId = null
                                                        navController.navigate("login") { popUpTo(0) { inclusive = true } }
                                                    }
                                                },
                                                onStudentClick = { uid, email ->
                                                    // Navigate to details screen, passing the ID and Email securely in the URL
                                                    navController.navigate("admin_student_details/$uid?email=$email")
                                                }
                                            )
                                        }
                                        // NEW ADMIN STUDENT DETAILS ROUTE
                                        composable(
                                            route = "admin_student_details/{userId}?email={email}",
                                            arguments = listOf(
                                                navArgument("userId") { type = NavType.StringType },
                                                navArgument("email") { type = NavType.StringType; defaultValue = "Student" }
                                            )
                                        ) { backStackEntry ->
                                            val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
                                            val email = backStackEntry.arguments?.getString("email") ?: "Student"

                                            AdminStudentDetailsScreen(
                                                userId = userId,
                                                studentEmail = email,
                                                onBack = { navController.popBackStack() }
                                            )
                                        }

                                        composable("home") {
                                            if (currentUserId != null) {
                                                HomeScreen(
                                                    viewModel = homeViewModel,
                                                    userEmail = Firebase.auth.currentUser?.email ?: "",
                                                    onSubjectClick = { id -> activeSubjectId = id },
                                                    sharedTransitionScope = this@SharedTransitionLayout,
                                                    animatedVisibilityScope = this@composable,
                                                    onProfileClick = { navController.navigate("profile") },
                                                )
                                            }
                                        }
                                        composable("compare") { CompareScreen(viewModel = homeViewModel, onBack = { navController.popBackStack() }, globalXTilt = smoothX.value, globalYTilt = smoothY.value) }
                                        composable("profile") {
                                            com.example.attit.screens.ProfileScreen(
                                                googleAuth = googleAuth,
                                                viewModel = homeViewModel,
                                                userEmail = Firebase.auth.currentUser?.email ?: "",
                                                onLogout = {
                                                    scope.launch(Dispatchers.IO) {
                                                        homeViewModel.clearUserData()
                                                        database.clearAllTables()
                                                        applicationContext.getSharedPreferences("attit_prefs", Context.MODE_PRIVATE).edit().clear().apply()
                                                        kotlinx.coroutines.withContext(Dispatchers.Main) {
                                                            currentUserId = null
                                                            navController.navigate("login") { popUpTo(0) { inclusive = true } }
                                                        }
                                                    }
                                                },
                                                onBack = { navController.popBackStack() },
                                                onAboutClick = { navController.navigate("about") },
                                                globalXTilt = smoothX.value,
                                                globalYTilt = smoothY.value
                                            )
                                        }
                                        composable("details/{subjectId}", arguments = listOf(navArgument("subjectId") { type = NavType.StringType })) { backStackEntry -> val id = backStackEntry.arguments?.getString("subjectId"); DetailsScreen(subjectId = id, viewModel = homeViewModel, onBack = { navController.popBackStack() }, sharedTransitionScope = this@SharedTransitionLayout, animatedVisibilityScope = this@composable, globalXTilt = smoothX.value, globalYTilt = smoothY.value) }
                                        composable("about") { AboutScreen(viewModel = homeViewModel, onBack = { navController.popBackStack() }) }
                                    }
                                }
                            }
                        }

                        if (showBottomBar) {
                            Box(modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter)) {
                                Box(modifier = Modifier.fillMaxWidth().height(140.dp).align(Alignment.BottomCenter).background(brush = Brush.verticalGradient(colors = listOf(fadeColor.copy(alpha = 0.0f), fadeColor.copy(alpha = 0.4f), fadeColor.copy(alpha = 0.9f), fadeColor))))
                                AttitBottomBar(navController = navController, viewModel = homeViewModel, liquidState = liquidState, currentTheme = currentTheme, isDarkTheme = themeColors.isDark)
                            }
                        }
                    }

                    // 3. LAYER B: THE DRAWER
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val screenHeightPx = constraints.maxHeight.toFloat()

                        // toPx scope
                        val density = LocalDensity.current
                        val topGapPx = with(density) { 40.dp.toPx() }

                        if (drawerProgress > 0.01f) {
                            // OUTER BOX: Movement Only (Translation) - NO CLIP
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        translationY = screenHeightPx * (1f - drawerProgress) + (topGapPx * drawerProgress)
                                        // DO NOT CLIP HERE. This allows the shadow to overflow naturally.
                                    }
                            ) {
                                // INNER BOX: Visuals Only (Shadow, Shape, Clip)
                                Surface(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .shadow(elevation = 32.dp, shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                                        .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)),
                                    color = drawerBgColor // Ensure Drawer is not white in Dark Mode
                                ) {
                                    if (activeSubjectId != null) {
                                        DetailsScreen(
                                            subjectId = activeSubjectId,
                                            viewModel = homeViewModel,
                                            onBack = { activeSubjectId = null },
                                            sharedTransitionScope = null,
                                            animatedVisibilityScope = null,
                                            globalXTilt = smoothX.value,
                                            globalYTilt = smoothY.value
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

}


class HomeViewModelFactory(private val repository: SubjectRepository, private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T { return if (modelClass.isAssignableFrom(HomeViewModel::class.java)) { HomeViewModel(repository, application) as T } else throw IllegalArgumentException("Unknown ViewModel class") }
}