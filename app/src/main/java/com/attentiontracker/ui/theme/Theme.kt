package com.attentiontracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ─── Neobrutalist Accent Palette ───────────────────────────────────────────
val NeoYellow              = Color(0xFFFFE600) // Primary Accent / CTA / Saturation
val NeoMint                = Color(0xFF2DE28D) // Secondary / Active Break / Milestone
val NeoMintContainer       = Color(0xFF53FCA4) // Mint Container / Safe state
val NeoPink                = Color(0xFFFF5C8D) // Hot Cyber Pink / Peak strain / Alert
val NeoCyan                = Color(0xFF38DBFF) // Electric Cyan / Telemetry / Distance target
val NeoOrange              = Color(0xFFFF6B4A) // Sunset Orange / Warning
val NeoLavender            = Color(0xFFE8D5FF) // Soft Lavender / Secondary card background
val NeoBlack               = Color(0xFF000000) // Primary Ink / Borders / Shadows
val NeoWhite               = Color(0xFFFFFFFF) // Surface Card / High contrast
val NeoPaper               = Color(0xFFFFFDF9) // Warm Canvas Backdrop
val NeoInk                 = Color(0xFF1B1B1B) // Primary Typography
val NeoSurface             = Color(0xFFF9F9F9) // Surface
val NeoCard                = Color(0xFFFFFFFF) // Neutral Card Surface
val NeoSurfaceMid          = Color(0xFFEEEEEE) // Surface Container
val NeoSurfaceHigh         = Color(0xFFE8E8E8) // Surface Container High
val NeoOutline             = Color(0xFF7C775F) // Outline
val NeoTertiary            = Color(0xFFB31F56) // Deep Cyber Pink
val NeoTertiaryContainer   = Color(0xFFFFDCE1) // Soft Pink Container
val NeoSecondaryDark       = Color(0xFF006D3F) // Deep Green
val NeoSecondaryFixedDim   = Color(0xFF2CE28D) // Mint Dim
val NeoPrimaryGold         = Color(0xFF6A5F00) // Deep Gold / Surface Tint

// Legacy color aliases for backward compatibility
val OnSurface        = NeoInk
val SubText          = Color(0xFF7C775F)
val DarkNavy         = NeoBlack
val MidNavy          = NeoPaper
val AccentCyan       = NeoCyan
val SurfaceCard      = NeoCard

// ─── Material3 Color Scheme ─────────────────────────────────────────────────
val NeoColorScheme = lightColorScheme(
    background           = NeoPaper,
    onBackground         = NeoInk,
    primary              = NeoPrimaryGold,
    onPrimary            = NeoWhite,
    primaryContainer     = NeoYellow,
    onPrimaryContainer   = Color(0xFF726600),
    secondary            = NeoSecondaryDark,
    onSecondary          = NeoWhite,
    secondaryContainer   = NeoMintContainer,
    onSecondaryContainer = Color(0xFF007242),
    tertiary             = NeoTertiary,
    onTertiary           = NeoWhite,
    tertiaryContainer    = NeoTertiaryContainer,
    onTertiaryContainer  = Color(0xFFBD285D),
    surface              = NeoSurface,
    onSurface            = NeoInk,
    surfaceVariant       = Color(0xFFE2E2E2),
    onSurfaceVariant     = Color(0xFF4B4731),
    outline              = NeoOutline,
    outlineVariant       = Color(0xFFCDC7AA),
    error                = Color(0xFFBA1A1A),
    onError              = NeoWhite,
    errorContainer       = Color(0xFFFFDAD6),
    onErrorContainer     = Color(0xFF93000A),
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
