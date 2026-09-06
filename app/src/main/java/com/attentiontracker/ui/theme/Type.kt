package com.attentiontracker.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ─── Raw type tokens ────────────────────────────────────────────────────────
// Uses system fonts as stand-ins for Space Grotesk / Space Mono:
//   FontFamily.Default   → bold grotesque (Roboto on Android)
//   FontFamily.Monospace → monospaced (Droid Sans Mono / system mono)
object NeoType {
    /** 42 sp bold display — mobile hero heading */
    val displayHero = TextStyle(
        fontFamily    = FontFamily.Default,
        fontSize      = 42.sp,
        fontWeight    = FontWeight.Black,
        letterSpacing = (-1.5).sp
    )

    /** 48 sp monospace timer — 00:20:00 readout */
    val timerDisplay = TextStyle(
        fontFamily    = FontFamily.Monospace,
        fontSize      = 48.sp,
        fontWeight    = FontWeight.Bold,
        letterSpacing = (-2).sp
    )

    /** 32 sp headline */
    val headlineLg = TextStyle(
        fontFamily    = FontFamily.Default,
        fontSize      = 32.sp,
        fontWeight    = FontWeight.Bold,
        letterSpacing = (-0.5).sp
    )

    /** 24 sp headline */
    val headlineMd = TextStyle(
        fontFamily    = FontFamily.Default,
        fontSize      = 24.sp,
        fontWeight    = FontWeight.Bold,
        letterSpacing = (-0.25).sp
    )

    /** 20 sp headline */
    val headlineSm = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize   = 20.sp,
        fontWeight = FontWeight.Bold
    )

    /** 16 sp body — primary prose */
    val bodyLg = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize   = 16.sp,
        fontWeight = FontWeight.Medium
    )

    /** 14 sp body — secondary prose */
    val bodyMd = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize   = 14.sp,
        fontWeight = FontWeight.Medium
    )

    /** 12 sp body — fine print */
    val bodySm = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize   = 12.sp,
        fontWeight = FontWeight.Medium
    )

    /** 14 sp monospace uppercase label — code / telemetry tags */
    val labelCode = TextStyle(
        fontFamily    = FontFamily.Monospace,
        fontSize      = 14.sp,
        fontWeight    = FontWeight.Bold,
        letterSpacing = 1.5.sp
    )

    /** 11 sp extra-bold pill badge label */
    val labelBadge = TextStyle(
        fontFamily    = FontFamily.Default,
        fontSize      = 11.sp,
        fontWeight    = FontWeight.ExtraBold,
        letterSpacing = 1.sp
    )

    /** 10 sp monospace telemetry — frame-rate, sync status */
    val labelTelemetry = TextStyle(
        fontFamily    = FontFamily.Monospace,
        fontSize      = 10.sp,
        fontWeight    = FontWeight.Normal,
        letterSpacing = 2.sp
    )
}

// ─── Material3 Typography mapping ───────────────────────────────────────────
val NeoTypography = Typography(
    displayLarge   = NeoType.displayHero,
    headlineLarge  = NeoType.headlineLg,
    headlineMedium = NeoType.headlineMd,
    headlineSmall  = NeoType.headlineSm,
    bodyLarge      = NeoType.bodyLg,
    bodyMedium     = NeoType.bodyMd,
    bodySmall      = NeoType.bodySm,
    labelLarge     = NeoType.labelBadge,
    labelSmall     = NeoType.labelTelemetry
)
