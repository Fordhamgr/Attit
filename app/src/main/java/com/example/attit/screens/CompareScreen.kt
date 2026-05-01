package com.example.attit.screens

import androidx.compose.foundation.Image
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airbnb.lottie.compose.*
import com.example.attit.R
import com.example.attit.screens.GyroBackground
import com.example.attit.utils.ThemeEngine
import com.example.attit.model.Friend
import com.example.attit.model.Subject
import com.example.attit.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompareScreen(
    viewModel: HomeViewModel,
    onBack: () -> Unit,
    globalXTilt: Float,
    globalYTilt: Float
) {
    val friends by viewModel.friends.collectAsStateWithLifecycle()
    val friendSubjects by viewModel.friendSubjects.collectAsStateWithLifecycle()
    val friendStatus by viewModel.friendStatus.collectAsStateWithLifecycle()
    val foundEmail by viewModel.foundFriendEmail.collectAsStateWithLifecycle()

    // --- THEME ENGINE ---
    val currentTheme by viewModel.currentTheme.collectAsStateWithLifecycle()
    val themeColors = remember(currentTheme) { ThemeEngine.getColorsFor(currentTheme) }

    var searchInput by remember { mutableStateOf("") }
    var showSaveDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    DisposableEffect(Unit) { onDispose { viewModel.clearSearch() } }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.Transparent
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // HEADER
                item {
                    Text(
                        text = "Friend List",
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = themeColors.titleColor
                    )
                }

                // SEARCH BAR
                item {
                    GlassSearchBar(
                        value = searchInput,
                        onValueChange = { searchInput = it },
                        onSearch = { viewModel.findFriend(searchInput) },
                        onClear = { searchInput = ""; viewModel.clearSearch() },
                        onScan = {
                            startQRScanner(context) { scannedData ->
                                searchInput = ""
                                viewModel.findFriend(scannedData)
                            }
                        },
                        bgColor = themeColors.cardBackground,
                        textColor = themeColors.cardTextColor
                    )
                }

                // SEARCH RESULT
                if (friendStatus.isNotEmpty()) {
                    item {
                        Text(
                            text = "SEARCH RESULT",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.SansSerif
                            ),
                            color = themeColors.bodyColor.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        GlassSearchResultCard(
                            status = friendStatus,
                            email = foundEmail,
                            subjects = friendSubjects,
                            bgColor = themeColors.cardBackground,
                            textColor = themeColors.cardTextColor,
                            onSave = { showSaveDialog = true }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                // FRIENDS LIST
                if (friends.isEmpty() && friendStatus.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.address_book))

                            // Use Int.MAX_VALUE
                            val progress by animateLottieCompositionAsState(
                                composition = composition,
                                iterations = Int.MAX_VALUE
                            )

                            LottieAnimation(
                                composition = composition,
                                progress = { progress },
                                modifier = Modifier.size(200.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "No friends yet.",
                                color = themeColors.bodyColor,
                                style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold)
                            )
                            Text(
                                "Search above to add your friends!",
                                color = themeColors.bodyColor.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.SansSerif)
                            )
                        }
                    }
                } else {
                    item {
                        Text(
                            text = "your friends",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.SansSerif
                            ),
                            color = themeColors.bodyColor.copy(alpha = 0.7f)
                        )
                    }

                    items(friends) { friend ->
                        GlassFriendItem(
                            friend = friend,
                            bgColor = themeColors.cardBackground,
                            textColor = themeColors.cardTextColor,
                            subTextColor = themeColors.bodyColor,
                            onClick = { viewModel.loadSavedFriend(friend) },
                            onDelete = { viewModel.deleteFriend(friend) }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }

        if (showSaveDialog) {
            NanoAddFriendDialog(
                initialEmail = foundEmail,
                onDismiss = { showSaveDialog = false },
                onConfirm = { nickname, email ->
                    viewModel.saveCurrentFriend(nickname, email)
                    showSaveDialog = false
                    searchInput = ""
                }
            )
        }
    }
}

// --- GLASS COMPONENTS ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassSearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit,
    onScan: () -> Unit,
    bgColor: Color,
    textColor: Color
) {
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current

    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text("Search @username or Email", color = textColor.copy(alpha = 0.5f), fontFamily = FontFamily.SansSerif) },
        singleLine = true,
        shape = RoundedCornerShape(24.dp), // Soft Pill
        textStyle = LocalTextStyle.current.copy(color = textColor, fontFamily = FontFamily.SansSerif),
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { onSearch(); keyboardController?.hide() }),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = bgColor.copy(alpha = 0.85f), // Glassy
            unfocusedContainerColor = bgColor.copy(alpha = 0.6f),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ),
        leadingIcon = { Icon(Icons.Default.Search, "Search", tint = textColor.copy(alpha = 0.7f)) },
        trailingIcon = {
            Row {
                IconButton(onClick = onScan) { Icon(Icons.Default.QrCodeScanner, "Scan", tint = textColor.copy(alpha = 0.7f)) }
                if (value.isNotEmpty()) {
                    IconButton(onClick = onClear) { Icon(Icons.Default.Close, "Clear", tint = textColor.copy(alpha = 0.5f)) }
                }
                IconButton(onClick = onSearch) {
                    Box(
                        modifier = Modifier.size(32.dp).clip(CircleShape).background(Color(0xFF6C63FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Search, "Go", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }
        },
        modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(24.dp), spotColor = Color.Black.copy(0.05f))
    )
}

@Composable
fun GlassFriendItem(
    friend: Friend,
    bgColor: Color,
    textColor: Color,
    subTextColor: Color,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor.copy(alpha = 0.65f)) // Glassy
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val avatarRes = com.example.attit.utils.AvatarUtils.getAvatarResId(friend.avatarId)

        if (avatarRes != null) {
            Image(
                painter = androidx.compose.ui.res.painterResource(id = avatarRes),
                contentDescription = "Avatar",
                modifier = Modifier.size(48.dp).clip(CircleShape).background(Color.White)
            )
        } else {
            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape).background(Color(0xFF6C63FF).copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    friend.nickname.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6C63FF)
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = friend.nickname,
                style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.SansSerif),
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Text(
                text = if (!friend.username.isNullOrEmpty()) "@${friend.username}" else friend.email,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.SansSerif),
                color = subTextColor.copy(alpha = 0.7f)
            )
        }

        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Remove", tint = subTextColor.copy(alpha = 0.3f))
        }
    }
}

@Composable
fun GlassSearchResultCard(
    status: String,
    email: String,
    subjects: List<Subject>,
    bgColor: Color,
    textColor: Color,
    onSave: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = bgColor.copy(alpha = 0.9f)),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth().shadow(8.dp, RoundedCornerShape(24.dp), spotColor = Color.Black.copy(0.05f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Person, null, tint = Color(0xFF6C63FF))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = status, fontWeight = FontWeight.Bold, color = textColor, fontFamily = FontFamily.SansSerif)
                    if (subjects.isNotEmpty()) {
                        Text(
                            text = "${subjects.size} Subjects Found",
                            style = MaterialTheme.typography.labelSmall,
                            color = textColor.copy(alpha = 0.6f),
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                }
            }

            if (subjects.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = textColor.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(12.dp))

                subjects.forEach { subject ->
                    val percentage = if (subject.total > 0) (subject.attended.toFloat() / subject.total) * 100 else 0f
                    val isLow = percentage < subject.goal

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = subject.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = textColor.copy(alpha = 0.9f),
                            fontFamily = FontFamily.SansSerif
                        )

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (isLow) Color(0xFFFFEBEE) else Color(0xFFE8F5E9))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${percentage.toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isLow) Color(0xFFD32F2F) else Color(0xFF388E3C),
                                fontFamily = FontFamily.SansSerif
                            )
                        }
                    }
                }
            }

            if (status.contains("Viewing")) {
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onSave,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C63FF)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text("Add to FriendList", fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ... Keep NanoAddFriendDialog & startQRScanner ...
@Composable
fun NanoAddFriendDialog(
    initialEmail: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var nickname by remember { mutableStateOf("") }
    var finalEmail by remember { mutableStateOf(initialEmail) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to Squad", fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(value = nickname, onValueChange = { nickname = it }, label = { Text("Nickname") }, shape = RoundedCornerShape(16.dp), singleLine = true)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(value = finalEmail, onValueChange = { finalEmail = it }, label = { Text("Email") }, shape = RoundedCornerShape(16.dp), singleLine = true)
            }
        },
        confirmButton = {
            Button(
                onClick = { if (nickname.isNotEmpty()) onConfirm(nickname, finalEmail) },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C63FF))
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp)
    )
}

fun startQRScanner(context: android.content.Context, onResult: (String) -> Unit) {
    val options = com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions.Builder()
        .setBarcodeFormats(com.google.mlkit.vision.barcode.common.Barcode.FORMAT_QR_CODE)
        .build()
    val scanner = com.google.mlkit.vision.codescanner.GmsBarcodeScanning.getClient(context, options)
    scanner.startScan()
        .addOnSuccessListener { barcode -> barcode.rawValue?.let { onResult(it) } }
        .addOnFailureListener { android.widget.Toast.makeText(context, "Scan failed", android.widget.Toast.LENGTH_SHORT).show() }
}