package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val RoxouDarkColorScheme = darkColorScheme(
    primary = RoxouPrimary,
    onPrimary = Color.White,
    primaryContainer = RoxouSurfaceVariant,
    onPrimaryContainer = RoxouPrimaryLight,
    secondary = RoxouSecondary,
    onSecondary = Color.White,
    tertiary = RoxouOnlineGreen,
    onTertiary = Color.Black,
    background = RoxouBackground,
    onBackground = RoxouOnBackground,
    surface = RoxouSurface,
    onSurface = RoxouOnSurface,
    surfaceVariant = RoxouSurfaceVariant,
    onSurfaceVariant = RoxouOnBackground,
    outline = RoxouDivider
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = RoxouDarkColorScheme,
        typography = Typography,
        content = content
    )
}
