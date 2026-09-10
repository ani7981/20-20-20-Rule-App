package com.attentiontracker.ui.screens

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
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
import kotlinx.coroutines.delay

@Composable
fun RestModeScreen(
    onCompleteBreak: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var secondsRemaining by remember { mutableStateOf(20) }
    var isTimerActive by remember { mutableStateOf(true) }
    var isChimeOn by remember { mutableStateOf(true) }
    var breakDone by remember { mutableStateOf(false) }

    // Countdown ticker
    LaunchedEffect(isTimerActive, secondsRemaining) {
        if (isTimerActive && secondsRemaining > 0) {
            delay(1000L)
            secondsRemaining--
            if (secondsRemaining == 0) {
                breakDone = true
                triggerHaptic(context)
                onCompleteBreak()
            }
        }
    }

    // Bounce animation for warning icon
    val infiniteTransition = rememberInfiniteTransition(label = "warningBounce")
    val bounceY by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce"
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
        // 1. WARNING BAR STRIP
        // ═══════════════════════════════════════════════════════════════
        NeoCard(
            backgroundColor = NeoYellow,
            borderWidth = 3.5.dp,
            shadowOffset = 4.dp,
            cornerRadius = 12.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Warning,
                        contentDescription = null,
                        tint = NeoInk,
                        modifier = Modifier
                            .size(22.dp)
                            .offset(y = bounceY.dp)
                    )
                    Text(
                        text = "20-MIN LIMIT REACHED",
                        style = NeoType.headlineSm,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = NeoInk,
                        letterSpacing = 0.5.sp
                    )
                }

                // Emergency pill
                Box(
                    modifier = Modifier
                        .background(NeoCard, RoundedCornerShape(999.dp))
                        .border(1.5.dp, NeoBlack, RoundedCornerShape(999.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Flare,
                            contentDescription = null,
                            tint = Color(0xFFBA1A1A),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "RETINA EMERGENCY",
                            style = NeoType.labelBadge,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeoInk
                        )
                    }
                }
            }
        }

        // ═══════════════════════════════════════════════════════════════
        // 2. HERO INSTRUCTION CARD (Slight Neobrutalist Tilt)
        // ═══════════════════════════════════════════════════════════════
        Box(modifier = Modifier.padding(bottom = 2.dp)) {
            NeoCard(
                backgroundColor = NeoCard,
                borderWidth = 4.dp,
                shadowOffset = 6.dp,
                cornerRadius = 14.dp,
                rotationDeg = -0.7f
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Hot Cyber Pink Top Header Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(NeoTertiary)
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "CRITICAL OVERRIDE",
                                style = NeoType.labelCode,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeoWhite,
                                letterSpacing = 1.sp
                            )

                            // Rule sticker tilted -2deg
                            Box(
                                modifier = Modifier
                                    .rotate(-2f)
                                    .background(NeoYellow, RoundedCornerShape(999.dp))
                                    .border(2.dp, NeoBlack, RoundedCornerShape(999.dp))
                                    .padding(horizontal = 10.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "RULE 20·20·20",
                                    style = NeoType.labelBadge,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = NeoBlack
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.fillMaxWidth().height(2.5.dp).background(NeoBlack))

                    // Instruction Core
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = "REST YOUR\nEYES NOW!",
                                style = NeoType.displayHero,
                                fontSize = 34.sp,
                                lineHeight = 36.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-1.5).sp,
                                color = NeoInk
                            )
                            Text(
                                text = "👀",
                                fontSize = 38.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        // Distance Target Graphic & Explanation Box
                        NeoCard(
                            backgroundColor = NeoMintContainer,
                            borderWidth = 3.dp,
                            shadowOffset = 3.dp,
                            cornerRadius = 10.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "OPTICAL DEFLECTION TARGET",
                                        style = NeoType.labelCode,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF00522E)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .background(NeoCard, RoundedCornerShape(999.dp))
                                            .border(1.5.dp, NeoBlack, RoundedCornerShape(999.dp))
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "≥ 20 FT (6M)",
                                            style = NeoType.labelBadge,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = NeoInk
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    NeoDeflectionEyeGraphic(modifier = Modifier.size(54.dp))
                                    Text(
                                        text = "Stop staring at this screen. Fixate on an object across the room, out a window, or down the corridor.",
                                        style = NeoType.bodyMd,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = NeoInk,
                                        lineHeight = 16.sp
                                    )
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(NeoCard, RoundedCornerShape(6.dp))
                                        .border(1.5.dp, NeoBlack, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "MUSCLE STRAIN: PEAK",
                                        style = NeoType.labelTelemetry,
                                        fontSize = 9.sp,
                                        color = NeoInk
                                    )
                                    Text(
                                        text = "RELAX CILIARY SPASM",
                                        style = NeoType.labelBadge,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NeoSecondaryDark
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ═══════════════════════════════════════════════════════════════
        // 3. CHUNKY BRUTALIST COUNTDOWN WIDGET
        // ═══════════════════════════════════════════════════════════════
        NeoCard(
            backgroundColor = NeoSurfaceMid,
            borderWidth = 3.5.dp,
            shadowOffset = 4.dp,
            cornerRadius = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "REST CADENCE",
                        style = NeoType.labelCode,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4B4731)
                    )
                    Box(
                        modifier = Modifier
                            .background(NeoTertiaryContainer, RoundedCornerShape(999.dp))
                            .border(1.5.dp, NeoBlack, RoundedCornerShape(999.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "LIVE COUNTDOWN",
                            style = NeoType.labelBadge,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFBD285D)
                        )
                    }
                }

                // Giant Timer Numeral
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (breakDone) "DONE!" else String.format("%02d", secondsRemaining),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 58.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (breakDone) NeoSecondaryDark else NeoInk,
                        letterSpacing = (-2).sp
                    )
                    if (!breakDone) {
                        Text(
                            text = "SEC",
                            style = NeoType.headlineSm,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeoPrimaryGold,
                            modifier = Modifier.padding(bottom = 10.dp, start = 4.dp)
                        )
                    }
                }

                Text(
                    text = "Blink softly • Breathe deeply • Release jaw tension",
                    style = NeoType.bodySm,
                    fontSize = 11.sp,
                    color = Color(0xFF4B4731)
                )

                // 20-Segmented Brutalist Progress Bar
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(NeoCard, RoundedCornerShape(8.dp))
                            .border(2.5.dp, NeoBlack, RoundedCornerShape(8.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        val totalSegments = 20
                        val filledSegments = (20 - secondsRemaining).coerceIn(0, 20)

                        repeat(totalSegments) { idx ->
                            val isCompleted = idx < filledSegments
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(18.dp)
                                    .background(
                                        if (isCompleted) NeoMint else NeoYellow,
                                        RoundedCornerShape(2.dp)
                                    )
                                    .border(1.dp, NeoBlack, RoundedCornerShape(2.dp))
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "0S (REACHED)",
                            style = NeoType.labelTelemetry,
                            fontSize = 9.sp,
                            color = Color(0xFF7C775F)
                        )
                        val pct = ((20 - secondsRemaining) * 100 / 20)
                        Text(
                            text = "RELAXATION PROGRESS ($pct%)",
                            style = NeoType.labelTelemetry,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeoSecondaryDark
                        )
                        Text(
                            text = "20S (GOAL)",
                            style = NeoType.labelTelemetry,
                            fontSize = 9.sp,
                            color = Color(0xFF7C775F)
                        )
                    }
                }
            }
        }

        // ═══════════════════════════════════════════════════════════════
        // 4. TACTILE CONTROLS & ACTIONS
        // ═══════════════════════════════════════════════════════════════
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Big Primary CTA
            NeoButton(
                text = if (breakDone) "EYES RESET! RETURN" else "COMPLETE BREAK",
                onClick = {
                    triggerHaptic(context)
                    onCompleteBreak()
                    Toast.makeText(context, "Break completed! Timer reset for 20 minutes.", Toast.LENGTH_SHORT).show()
                },
                containerColor = if (breakDone) NeoMintContainer else NeoYellow,
                contentColor = NeoBlack,
                leadingIcon = if (breakDone) Icons.Rounded.Celebration else Icons.Rounded.CheckCircle,
                shadowOffset = 4.dp,
                borderWidth = 3.5.dp,
                cornerRadius = 12.dp,
                height = 54.dp,
                fontSize = 15
            )

            // Secondary Controls (Snooze + Chime)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Snooze Button
                NeoButton(
                    text = "SNOOZE (+120S)",
                    onClick = {
                        secondsRemaining = 20
                        breakDone = false
                        Toast.makeText(context, "Snoozed 2 minutes", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f),
                    containerColor = NeoCard,
                    contentColor = NeoBlack,
                    leadingIcon = Icons.Rounded.Snooze,
                    shadowOffset = 3.dp,
                    borderWidth = 2.5.dp,
                    cornerRadius = 10.dp,
                    height = 46.dp,
                    fontSize = 11
                )

                // Chime Toggle
                NeoButton(
                    text = if (isChimeOn) "CHIME: ON" else "CHIME: OFF",
                    onClick = { isChimeOn = !isChimeOn },
                    modifier = Modifier.weight(1f),
                    containerColor = NeoCard,
                    contentColor = NeoBlack,
                    leadingIcon = if (isChimeOn) Icons.Rounded.VolumeUp else Icons.Rounded.VolumeOff,
                    shadowOffset = 3.dp,
                    borderWidth = 2.5.dp,
                    cornerRadius = 10.dp,
                    height = 46.dp,
                    fontSize = 11
                )
            }
        }

        // ═══════════════════════════════════════════════════════════════
        // 5. TELEMETRY SENSOR BADGE AT BOTTOM
        // ═══════════════════════════════════════════════════════════════
        NeoCard(
            backgroundColor = NeoSurfaceHigh,
            borderWidth = 3.dp,
            shadowOffset = 3.dp,
            cornerRadius = 12.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(NeoSecondaryDark, CircleShape)
                        .border(1.5.dp, NeoBlack, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Sensors,
                        contentDescription = null,
                        tint = NeoWhite,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "VISION SENSOR V3.2",
                            style = NeoType.labelTelemetry,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeoBlack
                        )
                        Box(
                            modifier = Modifier
                                .background(NeoMintContainer, RoundedCornerShape(999.dp))
                                .border(1.dp, NeoBlack, RoundedCornerShape(999.dp))
                                .padding(horizontal = 6.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "ACTIVE LOCK",
                                style = NeoType.labelBadge,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00522E)
                            )
                        }
                    }
                    Text(
                        text = "Deflection verified • Distance est: 6.2m • Pupil dilation normal",
                        style = NeoType.labelTelemetry,
                        fontSize = 9.sp,
                        color = Color(0xFF4B4731)
                    )
                }
            }
        }
    }
}

private fun triggerHaptic(context: Context) {
    try {
        val pattern = longArrayOf(0, 150, 100, 150, 100, 150)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v?.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                v?.vibrate(pattern, -1)
            }
        }
    } catch (_: Exception) {}
}
