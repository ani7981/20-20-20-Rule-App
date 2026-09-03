package com.attentiontracker.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val DarkNavy = Color(0xFF0A0E1A)
val MidNavy = Color(0xFF162032)
val AccentCyan = Color(0xFF4FC3F7)
val SurfaceCard = Color(0xFF1A2540)
val OnSurface = Color(0xFFE8EAF6)
val SubText = Color(0xFF8FA3C0)

private val darkColorScheme = darkColorScheme(
    primary = AccentCyan,
    onPrimary = DarkNavy,
    background = DarkNavy,
    surface = SurfaceCard,
    onBackground = OnSurface,
    onSurface = OnSurface,
    secondary = AccentCyan,
    onSecondary = DarkNavy
)

@Composable
fun AttentionTrackerTheme(content: @Composable () -> Unit): Unit {
    MaterialTheme(
        colorScheme = darkColorScheme,
        content = content
    )
}
