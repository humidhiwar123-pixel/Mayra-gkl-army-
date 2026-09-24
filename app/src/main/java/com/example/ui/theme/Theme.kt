package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val MyraDarkColorScheme = darkColorScheme(
    primary = MyraRedPrimary,
    onPrimary = Color.White,
    primaryContainer = MyraRedDark,
    onPrimaryContainer = Color.White,
    secondary = MyraRedGlow,
    onSecondary = Color.White,
    secondaryContainer = MyraRedSubtle,
    onSecondaryContainer = MyraRedPrimary,
    tertiary = MyraAccentCyan,
    onTertiary = Color.Black,
    background = MyraBlack,
    onBackground = MyraTextPrimary,
    surface = MyraDarkSurface,
    onSurface = MyraTextPrimary,
    surfaceVariant = MyraCardBg,
    onSurfaceVariant = MyraTextSecondary,
    outline = MyraRedBorder,
    outlineVariant = MyraDivider
)

@Composable
fun MyraAiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // MYRA AI features a custom futuristic Black & Red dark aesthetic for immersive experience
    MaterialTheme(
        colorScheme = MyraDarkColorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MyraAiTheme(darkTheme = darkTheme, content = content)
}
