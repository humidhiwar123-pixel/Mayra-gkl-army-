package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.navigation.Screen
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyraTopBar(
    title: String,
    canNavigateBack: Boolean,
    onNavigateBack: () -> Unit,
    onOpenSecurity: () -> Unit = {},
    onOpenDeveloper: () -> Unit = {}
) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(MyraRedPrimary)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    color = MyraTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        },
        navigationIcon = {
            if (canNavigateBack) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.testTag("nav_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MyraRedPrimary
                    )
                }
            }
        },
        actions = {
            IconButton(
                onClick = onOpenSecurity,
                modifier = Modifier.testTag("nav_security_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "Security Center",
                    tint = MyraTextSecondary
                )
            }
            IconButton(
                onClick = onOpenDeveloper,
                modifier = Modifier.testTag("nav_dev_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Terminal,
                    contentDescription = "Developer Hub",
                    tint = MyraTextSecondary
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MyraBlack
        )
    )
}

@Composable
fun MyraBottomBar(
    currentRoute: String,
    onNavigateToRoute: (String) -> Unit
) {
    val items = listOf(
        Screen.Home to (Icons.Default.Home to Icons.Outlined.Home),
        Screen.Chat to (androidx.compose.material.icons.Icons.AutoMirrored.Filled.Chat to androidx.compose.material.icons.Icons.AutoMirrored.Outlined.Chat),
        Screen.Voice to (Icons.Default.Mic to Icons.Outlined.Mic),
        Screen.AiTools to (Icons.Default.AutoAwesome to Icons.Outlined.AutoAwesome),
        Screen.Settings to (Icons.Default.Settings to Icons.Outlined.Settings)
    )

    NavigationBar(
        containerColor = MyraDarkSurface,
        tonalElevation = 8.dp,
        modifier = Modifier.border(
            width = 1.dp,
            brush = Brush.verticalGradient(
                colors = listOf(MyraRedBorder, Color.Transparent)
            ),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        )
    ) {
        items.forEach { (screen, icons) ->
            val isSelected = currentRoute == screen.route
            NavigationBarItem(
                selected = isSelected,
                onClick = { onNavigateToRoute(screen.route) },
                icon = {
                    Icon(
                        imageVector = if (isSelected) icons.first else icons.second,
                        contentDescription = screen.title,
                        tint = if (isSelected) MyraRedPrimary else MyraTextTertiary
                    )
                },
                label = {
                    Text(
                        text = screen.title.uppercase(),
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal,
                        color = if (isSelected) MyraRedPrimary else MyraTextTertiary
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = MyraRedSubtle
                ),
                modifier = Modifier.testTag("nav_item_${screen.route}")
            )
        }
    }
}
