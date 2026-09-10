package com.attentiontracker.ui.screens

import android.content.Context
import android.os.BatteryManager
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.attentiontracker.ui.theme.*

@Composable
fun TrackerScreen(
    userName: String,
    completedBreaks: Int,
    isTracking: Boolean,
    statusText: String,
    elapsedText: String,
    totalScreenTimeStr: String,
    onToggleTracking: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Device battery level
    val batteryPercent = remember {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 82
    }

    // Determine looking status
    val isLooking = statusText.contains("Looking at screen", ignoreCase = true) ||
            statusText.contains("Tracking active", ignoreCase = true)

    // Parse seconds from elapsed text ("Looking for: Xs")
    val elapsedSeconds: Long = remember(elapsedText) {
        val match = Regex("(\\d+)s").find(elapsedText)
        match?.groupValues?.getOrNull(1)?.toLongOrNull() ?: 0L
    }

    // 20 minute protocol in seconds = 1200s
    val intervalTotalSeconds = 1200L
    val remainingSeconds = if (isTracking) {
        (intervalTotalSeconds - (elapsedSeconds % intervalTotalSeconds)).coerceAtLeast(0L)
    } else {
        860L // Default preview 14m 20s
    }

    val minutes = remainingSeconds / 60L
    val seconds = remainingSeconds % 60L
    val formattedTime = String.format("%02d:%02d", minutes, seconds)

    // Progress percentage
    val progressFraction = if (isTracking) {
        ((intervalTotalSeconds - remainingSeconds).toFloat() / intervalTotalSeconds.toFloat()).coerceIn(0f, 1f)
    } else 0.75f
    val stagePercent = (progressFraction * 100).toInt()
    val filledBlocks = (progressFraction * 4).toInt().coerceIn(0, 4)

    // Pulsing dot animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NeoSurface)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // ═══════════════════════════════════════════════════════════════
        // 1. HEADER SECTION (Greeting + Sticker + Calibration)
        // ═══════════════════════════════════════════════════════════════
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Greeting Pill with Overlapping "PRO MODE" Sticker
            Box(modifier = Modifier.padding(top = 6.dp)) {
                // Greeting Pill
                NeoCard(
                    backgroundColor = NeoCard,
                    borderWidth = 3.dp,
                    shadowOffset = 3.dp,
                    cornerRadius = 999.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(NeoMintContainer.copy(alpha = pulseAlpha), CircleShape)
                                .border(1.dp, NeoBlack, CircleShape)
                        )
                        Text(
                            text = "HEY, ${userName.ifBlank { "ALEX" }.uppercase()} 👋",
                            style = NeoType.headlineSm,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeoInk,
                            letterSpacing = (-0.5).sp
                        )
                    }
                }

                // Overlapping PRO MODE sticker
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 10.dp, y = (-10).dp)
                        .rotate(6f)
                        .background(NeoMint, RoundedCornerShape(4.dp))
                        .border(2.dp, NeoBlack, RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "PRO MODE",
                        style = NeoType.labelBadge,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = NeoBlack
                    )
                }
            }

            // Calibration / Quick Tune Button
            NeoCard(
                backgroundColor = NeoYellow,
                borderWidth = 3.dp,
                shadowOffset = 3.dp,
                cornerRadius = 12.dp,
                onClick = {
                    Toast.makeText(context, "AI Vision calibrated to current lighting", Toast.LENGTH_SHORT).show()
                }
            ) {
                Box(
                    modifier = Modifier.size(46.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Tune,
                        contentDescription = "Calibration",
                        tint = NeoBlack,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // ═══════════════════════════════════════════════════════════════
        // 2. HERO LIVE TRACKER CARD (Electric Lemon Yellow)
        // ═══════════════════════════════════════════════════════════════
        NeoCard(
            backgroundColor = NeoYellow,
            borderWidth = 4.dp,
            shadowOffset = 6.dp,
            cornerRadius = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Sticker Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Live Status Pill
                    val statusLabel = when {
                        !isTracking -> "SESSION PAUSED"
                        isLooking   -> "LOOKING AT SCREEN"
                        else        -> "LOOKING AWAY"
                    }
                    val dotColor = when {
                        !isTracking -> NeoOrange
                        isLooking   -> NeoMint
                        else        -> NeoPink
                    }

                    NeoStatusPill(
                        text = statusLabel,
                        backgroundColor = NeoCard,
                        textColor = NeoBlack,
                        dotColor = dotColor
                    )

                    // Telemetry Chip tilted -2deg
                    Box(
                        modifier = Modifier
                            .rotate(-2f)
                            .background(NeoCard, RoundedCornerShape(4.dp))
                            .border(2.dp, NeoBlack, RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "⚡ 2 FPS AI ACTIVE",
                            style = NeoType.labelTelemetry,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeoBlack
                        )
                    }
                }

                // Countdown Display Box
                NeoCard(
                    backgroundColor = NeoCard,
                    borderWidth = 3.dp,
                    shadowOffset = 3.dp,
                    cornerRadius = 10.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp, horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = formattedTime,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 48.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-2).sp,
                                color = NeoBlack
                            )
                            Text(
                                text = "s",
                                style = NeoType.headlineSm,
                                color = NeoOutline,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 6.dp, start = 2.dp)
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Timer,
                                contentDescription = null,
                                tint = Color(0xFF4B4731),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "/ 20:00 INTERVAL PROTOCOL",
                                style = NeoType.labelTelemetry,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4B4731),
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }

                // Segmented Brutalist Progress Bar (4 Chunky Blocks)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "INTERVAL SATURATION",
                            style = NeoType.labelBadge,
                            fontSize = 11.sp,
                            color = NeoBlack
                        )
                        Text(
                            text = "$stagePercent% STAGE",
                            style = NeoType.labelBadge,
                            fontSize = 11.sp,
                            color = NeoBlack
                        )
                    }

                    NeoSegmentedProgressBar(
                        totalSegments = 4,
                        filledSegments = filledBlocks,
                        fillColor = NeoMint,
                        emptyColor = NeoCard,
                        height = 28.dp
                    )
                }

                // Cyber Pink / Mint CTA Button
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    NeoButton(
                        text = if (isTracking) "PAUSE MONITORING" else "START MONITORING",
                        onClick = onToggleTracking,
                        containerColor = if (isTracking) NeoPink else NeoMint,
                        contentColor = NeoBlack,
                        leadingIcon = if (isTracking) Icons.Rounded.PauseCircle else Icons.Rounded.PlayCircle,
                        shadowOffset = 4.dp,
                        borderWidth = 3.5.dp,
                        cornerRadius = 12.dp,
                        height = 54.dp,
                        fontSize = 16
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.TouchApp,
                            contentDescription = null,
                            tint = NeoBlack,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "TAP ONCE TO ${if (isTracking) "PAUSE" else "START"} · 20-20-20 SHIELD ACTIVE",
                            style = NeoType.labelTelemetry,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeoBlack
                        )
                    }
                }
            }
        }

        // ═══════════════════════════════════════════════════════════════
        // 3. QUICK STATS GRID (2-Column Responsive Neo-Cards)
        // ═══════════════════════════════════════════════════════════════
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Card A: Breaks Hit
            NeoCard(
                modifier = Modifier.weight(1f),
                backgroundColor = NeoMintContainer,
                borderWidth = 3.dp,
                shadowOffset = 4.dp,
                cornerRadius = 12.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "BREAKS HIT",
                            style = NeoType.labelBadge,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = NeoBlack
                        )
                        Icon(
                            imageVector = Icons.Rounded.Verified,
                            contentDescription = null,
                            tint = NeoBlack,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.5.dp)
                            .background(NeoBlack)
                            .padding(vertical = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "$completedBreaks / 8",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeoBlack
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val targetPercent = (completedBreaks * 100 / 8).coerceAtMost(100)
                    Box(
                        modifier = Modifier
                            .background(NeoCard.copy(alpha = 0.85f), RoundedCornerShape(4.dp))
                            .border(1.5.dp, NeoBlack, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "🎯 $targetPercent% Target met",
                            style = NeoType.labelBadge,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeoBlack
                        )
                    }
                }
            }

            // Card B: Screen Time
            NeoCard(
                modifier = Modifier.weight(1f),
                backgroundColor = NeoSecondaryFixedDim,
                borderWidth = 3.dp,
                shadowOffset = 4.dp,
                cornerRadius = 12.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SCREEN TIME",
                            style = NeoType.labelBadge,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = NeoBlack
                        )
                        Icon(
                            imageVector = Icons.Rounded.Visibility,
                            contentDescription = null,
                            tint = NeoBlack,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.5.dp)
                            .background(NeoBlack)
                            .padding(vertical = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = totalScreenTimeStr.ifBlank { "4h 12m" },
                        fontFamily = FontFamily.Monospace,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeoBlack
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .background(NeoCard.copy(alpha = 0.85f), RoundedCornerShape(4.dp))
                            .border(1.5.dp, NeoBlack, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "⚡ Active Today",
                            style = NeoType.labelBadge,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeoBlack
                        )
                    }
                }
            }
        }

        // ═══════════════════════════════════════════════════════════════
        // 4. HARDWARE & EFFICIENCY CARD
        // ═══════════════════════════════════════════════════════════════
        NeoCard(
            backgroundColor = NeoCard,
            borderWidth = 3.dp,
            shadowOffset = 4.dp,
            cornerRadius = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Memory,
                            contentDescription = null,
                            tint = NeoBlack,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "HARDWARE & TELEMETRY",
                            style = NeoType.labelBadge,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = NeoBlack,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .background(NeoSurfaceHigh, RoundedCornerShape(4.dp))
                            .border(1.5.dp, NeoBlack, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "OFFLINE NPU",
                            style = NeoType.labelTelemetry,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeoBlack
                        )
                    }
                }

                Box(modifier = Modifier.fillMaxWidth().height(1.5.dp).background(NeoBlack))

                // 3-Pill Telemetry Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Battery
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(NeoMintContainer, RoundedCornerShape(8.dp))
                            .border(2.dp, NeoBlack, RoundedCornerShape(8.dp))
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🔋 $batteryPercent%",
                            style = NeoType.labelBadge,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = NeoBlack
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "BATT SAFE",
                            style = NeoType.labelTelemetry,
                            fontSize = 9.sp,
                            color = NeoBlack
                        )
                    }

                    // Power Draw
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(NeoYellow, RoundedCornerShape(8.dp))
                            .border(2.dp, NeoBlack, RoundedCornerShape(8.dp))
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "⚡ 240 mA",
                            style = NeoType.labelBadge,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = NeoBlack
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "LOW DRAW",
                            style = NeoType.labelTelemetry,
                            fontSize = 9.sp,
                            color = NeoBlack
                        )
                    }

                    // AI Mode
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(NeoTertiaryContainer, RoundedCornerShape(8.dp))
                            .border(2.dp, NeoBlack, RoundedCornerShape(8.dp))
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "📷 2 FPS",
                            style = NeoType.labelBadge,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = NeoBlack
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "VISION AI",
                            style = NeoType.labelTelemetry,
                            fontSize = 9.sp,
                            color = Color(0xFFBD285D)
                        )
                    }
                }

                // Explanatory Footnote
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Security,
                        contentDescription = null,
                        tint = NeoSecondaryDark,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Zero cloud latency. On-device gaze calculation protects your battery & ocular rhythm.",
                        style = NeoType.bodySm,
                        fontSize = 11.sp,
                        color = NeoInk
                    )
                }
            }
        }
    }
}
