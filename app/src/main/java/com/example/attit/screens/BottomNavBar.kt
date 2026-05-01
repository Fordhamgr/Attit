package com.example.attit.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.attit.liquid.LiquidState
import com.example.attit.viewmodel.HomeViewModel

@Composable
fun AttitBottomBar(
    navController: NavController,
    viewModel: HomeViewModel,
    liquidState: LiquidState,
    currentTheme: String,
    isDarkTheme: Boolean
) {
    val navBackStackEntry = navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry.value?.destination?.route
    val isHome = currentRoute == "home"

    // Theme-Aware Colors
    val themeBarColor = remember(currentTheme) {
        when (currentTheme) {
            "Space" -> Color(0xFF151B2E)
            "Autumn" -> Color(0xFFFBF6EB)
            "Zen" -> Color(0xFF252525)
            else -> Color.White
        }
    }

    val barColor = themeBarColor.copy(alpha = 0.85f)
    val shadowColor = if (isDarkTheme) Color.Black.copy(0.5f) else Color.Black.copy(0.1f)
    val addButtonColor = if (isDarkTheme) Color(0xFF6C63FF) else Color.White
    val addIconColor = if (isDarkTheme) Color.White else Color.Black

    fun onNavClick(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            // -----------------------------------------------------------
            // FINAL POSITION:
            // 1. padding(bottom = 10.dp) -> Sits just above the gesture line.
            //    If you want it LOWER, change this to 5.dp or 0.dp.
            //    10.dp is usually the sweet spot for "Floating but Low".
            // -----------------------------------------------------------
            .padding(bottom = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .height(72.dp)
                .onGloballyPositioned { coordinates ->
                    liquidState.lensRect = coordinates.boundsInRoot()
                }
                .shadow(elevation = 20.dp, shape = CircleShape, spotColor = shadowColor)
                .background(barColor, CircleShape)
                .clip(CircleShape)
                .padding(horizontal = 10.dp, vertical = 8.dp),

            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
        ) {

            // 1. HOME
            NavPillItem(
                icon = Icons.Default.Home,
                label = "Home",
                isSelected = currentRoute == "home",
                onClick = { onNavClick("home") },
                isDarkTheme = isDarkTheme
            )

            // 2. COMPARE
            NavPillItem(
                icon = Icons.Default.Star,
                label = "Compare",
                isSelected = currentRoute == "compare",
                onClick = { onNavClick("compare") },
                isDarkTheme = isDarkTheme
            )

            // 3. PROFILE
            NavPillItem(
                icon = Icons.Default.Person,
                label = "Profile",
                isSelected = currentRoute == "profile",
                onClick = { onNavClick("profile") },
                isDarkTheme = isDarkTheme,
            )

            // 4. ADD BUTTON
            if (isHome) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(addButtonColor)
                        .clickable { viewModel.openAddDialog() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add",
                        tint = addIconColor,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun NavPillItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    isDarkTheme: Boolean
) {
    val activeBg = remember(isDarkTheme) { if (isDarkTheme) Color(0xFF6C63FF).copy(alpha = 0.3f) else Color(0xFFE0E0FF) }
    val activeContent = remember(isDarkTheme) { if (isDarkTheme) Color(0xFFC5CAE9) else Color(0xFF6C63FF) }
    val inactiveContent = remember(isDarkTheme) { if (isDarkTheme) Color.White.copy(alpha = 0.5f) else Color.Gray.copy(alpha = 0.6f) }

    val backgroundColor by androidx.compose.animation.animateColorAsState(if (isSelected) activeBg else activeBg.copy(alpha = 0f), animationSpec = tween(250))
    val contentColor by androidx.compose.animation.animateColorAsState(if (isSelected) activeContent else inactiveContent, animationSpec = tween(250))

    Box(
        modifier = Modifier
            .height(56.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .padding(horizontal = if (isSelected) 20.dp else 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(26.dp)
            )

            AnimatedVisibility(
                visible = isSelected,
                enter = fadeIn(animationSpec = tween(250)) + expandHorizontally(animationSpec = tween(250)),
                exit = fadeOut(animationSpec = tween(250)) + shrinkHorizontally(animationSpec = tween(250))
            ) {
                Text(
                    text = label,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip
                )
            }
        }
    }
}