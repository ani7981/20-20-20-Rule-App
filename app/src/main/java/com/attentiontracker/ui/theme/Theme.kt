package com.attentiontracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ─── Neobrutalist Accent Palette ───────────────────────────────────────────
val NeoYellow        = Color(0xFFFFE600)
val NeoMint          = Color(0xFF2DE28D)
val NeoMintContainer = Color(0xFF53FCA4)
val NeoPink          = Color(0xFFFF5C8D)
val NeoCyan          = Color(0xFF38DBFF)
val NeoOrange        = Color(0xFFFF6B4A)
val NeoLavender      = Color(0xFFE8D5FF)
val NeoBlack         = Color(0xFF000000)
val NeoWhite         = Color(0xFFFFFFFF)
val NeoPaper         = Color(0xFFFFFDF9)
val NeoInk           = Color(0xFF1B1B1B)
val NeoSurface       = Color(0xFFF9F9F9)
val NeoCard          = Color(0xFFFFFFFF)
val NeoSurfaceMid    = Color(0xFFEEEEEE)
val NeoSurfaceHigh   = Color(0xFFE8E8E8)
val NeoOutline       = Color(0xFF7C775F)

// Legacy color aliases for backward compatibility
val OnSurface        = NeoInk
val SubText          = Color(0xFF7C775F)
val DarkNavy         = NeoBlack
val MidNavy          = NeoPaper
val AccentCyan       = NeoCyan
val SurfaceCard      = NeoCard

// ─── Material3 Color Scheme ─────────────────────────────────────────────────
val NeoColorScheme = lightColorScheme(
    // Background / Canvas — warm off-white paper to reduce digital fatigue
    background           = NeoPaper,
    onBackground         = NeoInk,

    // Primary — dark gold, for navigation emphasis
    primary              = Color(0xFF6A5F00),
    onPrimary            = Color(0xFFFFFFFF),
    primaryContainer     = NeoYellow,          // Lemon Yellow — CTAs, countdown viz
    onPrimaryContainer   = Color(0xFF726600),

    // Secondary — deep green, break/completion states
    secondary            = Color(0xFF006D3F),
    onSecondary          = Color(0xFFFFFFFF),
    secondaryContainer   = NeoMintContainer,   // Mint — active break / streak
    onSecondaryContainer = Color(0xFF007242),

    // Tertiary — hot pink, critical strain warnings
    tertiary             = Color(0xFFB31F56),
    onTertiary           = Color(0xFFFFFFFF),
    tertiaryContainer    = Color(0xFFFFDCE1),  // Soft pink
    onTertiaryContainer  = Color(0xFFBD285D),

    // Surface
    surface              = NeoSurface,
    onSurface            = NeoInk,
    surfaceVariant       = Color(0xFFE2E2E2),
    onSurfaceVariant     = Color(0xFF4B4731),

    // Outline
    outline              = NeoOutline,
    outlineVariant       = Color(0xFFCDC7AA),

    // Error
    error                = Color(0xFFBA1A1A),
    onError              = Color(0xFFFFFFFF),
    errorContainer       = Color(0xFFFFDAD6),
    onErrorContainer     = Color(0xFF93000A),

    // Inverse
    inverseSurface       = Color(0xFF303030),
    inverseOnSurface     = Color(0xFFF1F1F1),
    inversePrimary       = Color(0xFFDEC800),
)

// ─── App Theme Composable ───────────────────────────────────────────────────
@Composable
fun AttentionTrackerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NeoColorScheme,
        typography  = NeoTypography,
        content     = content
    )
}
