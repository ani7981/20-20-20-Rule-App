package com.attentiontracker.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.attentiontracker.ui.theme.*

@Composable
fun SettingsScreen(
    threshold: Long,
    userName: String,
    onThresholdChange: (Long) -> Unit,
    onSaveProfile: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    val context = LocalContext.current

    // Threshold in whole minutes for the stepper (clamped 5..60)
    val thresholdMinutes = (threshold / 60L).coerceIn(5L, 60L)
    val presets = listOf(15L, 20L, 25L, 30L)

    var profileName by remember(userName) { mutableStateOf(userName.ifBlank { "Alex Mercer" }) }

    // Toggle states
    var isCameraAttentionOn by remember { mutableStateOf(true) }
    var isLiveShadeOn by remember { mutableStateOf(true) }
    var isHapticOn by remember { mutableStateOf(true) }
    var isExporting by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NeoSurface)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {

        // ═══════════════════════════════════════════════════════════════
        // 1. HEADER SECTION WITH STICKER BADGE
        // ═══════════════════════════════════════════════════════════════
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PREFERENCES & AI SENSORS",
                    style = NeoType.headlineMd,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = NeoInk,
                    letterSpacing = (-0.5).sp,
                    modifier = Modifier.weight(1f)
                )

                // Rotated Sticker Badge
                Box(
                    modifier = Modifier
                        .rotate(3f)
                        .background(NeoTertiaryContainer, RoundedCornerShape(4.dp))
                        .border(2.dp, NeoBlack, RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "v2.4 STABLE",
                        style = NeoType.labelBadge,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFBD285D)
                    )
                }
            }

            Text(
                text = "Tune detection intervals, battery modes, and hardware notifications.",
                style = NeoType.bodyMd,
                fontSize = 13.sp,
                color = Color(0xFF4B4731),
                fontWeight = FontWeight.Medium
            )
        }

        // ═══════════════════════════════════════════════════════════════
        // 2. THRESHOLD STEPPER CARD
        // ═══════════════════════════════════════════════════════════════
        NeoCard(
            backgroundColor = NeoCard,
            borderWidth = 3.5.dp,
            shadowOffset = 4.dp,
            cornerRadius = 14.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header band
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NeoSurfaceMid)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Schedule,
                                contentDescription = null,
                                tint = NeoBlack,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "EYE BREAK INTERVAL",
                                style = NeoType.headlineSm,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = NeoBlack,
                                letterSpacing = 0.5.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .background(NeoSurfaceHigh, RoundedCornerShape(4.dp))
                                .border(1.dp, NeoBlack, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "SYNCED",
                                style = NeoType.labelTelemetry,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeoBlack
                            )
                        }
                    }
                }

                Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(NeoBlack))

                // Stepper Body
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Stepper Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Minus Button
                        NeoCard(
                            backgroundColor = NeoYellow,
                            borderWidth = 3.dp,
                            shadowOffset = 4.dp,
                            cornerRadius = 8.dp,
                            onClick = {
                                val nextVal = (thresholdMinutes - 5L).coerceAtLeast(5L)
                                onThresholdChange(nextVal * 60L)
                            }
                        ) {
                            Box(
                                modifier = Modifier.size(56.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Remove,
                                    contentDescription = "Decrease interval",
                                    tint = NeoBlack,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        // Center Value
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "$thresholdMinutes",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 46.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = NeoBlack,
                                    letterSpacing = (-2).sp
                                )
                                Text(
                                    text = "MIN",
                                    style = NeoType.headlineSm,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeoBlack,
                                    modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
                                )
                            }
                            Text(
                                text = "Recommended 20-20-20 standard",
                                style = NeoType.labelTelemetry,
                                fontSize = 10.sp,
                                color = Color(0xFF4B4731)
                            )
                        }

                        // Plus Button
                        NeoCard(
                            backgroundColor = NeoYellow,
                            borderWidth = 3.dp,
                            shadowOffset = 4.dp,
                            cornerRadius = 8.dp,
                            onClick = {
                                val nextVal = (thresholdMinutes + 5L).coerceAtMost(60L)
                                onThresholdChange(nextVal * 60L)
                            }
                        ) {
                            Box(
                                modifier = Modifier.size(56.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = "Increase interval",
                                    tint = NeoBlack,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }

                    // Quick Preset Pills
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        presets.forEach { mins ->
                            val isSelected = thresholdMinutes == mins
                            val interactionSource = remember { MutableInteractionSource() }

                            Box(modifier = Modifier.weight(1f).padding(bottom = 2.dp, end = 2.dp)) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(38.dp)
                                        .offset(x = 2.dp, y = 2.dp)
                                        .background(NeoBlack, RoundedCornerShape(6.dp))
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(38.dp)
                                        .background(
                                            if (isSelected) NeoMintContainer else NeoCard,
                                            RoundedCornerShape(6.dp)
                                        )
                                        .border(2.dp, NeoBlack, RoundedCornerShape(6.dp))
                                        .clickable(
                                            interactionSource = interactionSource,
                                            indication = null,
                                            onClick = { onThresholdChange(mins * 60L) }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (mins == 20L) "20m (Std)" else "${mins}m",
                                        style = NeoType.labelCode,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NeoBlack
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ═══════════════════════════════════════════════════════════════
        // 3. AI SENSITIVITY & HARDWARE SWITCHES
        // ═══════════════════════════════════════════════════════════════
        NeoCard(
            backgroundColor = NeoSurfaceMid,
            borderWidth = 3.5.dp,
            shadowOffset = 4.dp,
            cornerRadius = 14.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header band
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NeoSurfaceHigh)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Sensors,
                                contentDescription = null,
                                tint = NeoBlack,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "DETECTION & HARDWARE SWITCHES",
                                style = NeoType.headlineSm,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = NeoBlack,
                                letterSpacing = 0.5.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(NeoMint, CircleShape)
                                .border(1.dp, NeoBlack, CircleShape)
                        )
                    }
                }

                Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(NeoBlack))

                // Toggle Rows
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Switch Row 1: Camera Attention
                    NeoSwitchRow(
                        title = "Front Camera Attention",
                        tag = "AI",
                        tagColor = NeoMintContainer,
                        subtitle = "Detects gaze direction at 2 FPS ultra-low power mode",
                        checked = isCameraAttentionOn,
                        onCheckedChange = { isCameraAttentionOn = it },
                        activeColor = NeoMintContainer,
                        icon = Icons.Rounded.Visibility
                    )

                    // Switch Row 2: Notification Shade
                    NeoSwitchRow(
                        title = "Live Sensor Shade",
                        tag = "SYS",
                        tagColor = NeoYellow,
                        subtitle = "Show live drain & countdown in Android notification shade",
                        checked = isLiveShadeOn,
                        onCheckedChange = { isLiveShadeOn = it },
                        activeColor = NeoYellow,
                        icon = Icons.Rounded.Bolt
                    )

                    // Switch Row 3: Haptic & Tone
                    NeoSwitchRow(
                        title = "Vibrate & Audio Cue",
                        tag = "HAPTIC",
                        tagColor = NeoTertiaryContainer,
                        subtitle = "Haptic pulse & tone when 20 minutes elapsed",
                        checked = isHapticOn,
                        onCheckedChange = { isHapticOn = it },
                        activeColor = NeoTertiaryContainer,
                        icon = Icons.Rounded.Vibration
                    )
                }
            }
        }

        // ═══════════════════════════════════════════════════════════════
        // 4. PROFILE & TELEMETRY DATA CARD
        // ═══════════════════════════════════════════════════════════════
        NeoCard(
            backgroundColor = NeoCard,
            borderWidth = 3.5.dp,
            shadowOffset = 4.dp,
            cornerRadius = 14.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header band
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NeoSurfaceMid)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AccountCircle,
                                contentDescription = null,
                                tint = NeoBlack,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "PROFILE & TELEMETRY DATA",
                                style = NeoType.headlineSm,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = NeoBlack,
                                letterSpacing = 0.5.sp
                            )
                        }

                        Text(
                            text = "ID: 9021-OPTIC",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4B4731)
                        )
                    }
                }

                Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(NeoBlack))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Profile Input Field
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "USER PROFILE NAME",
                                style = NeoType.labelBadge,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeoInk
                            )
                            Text(
                                text = "SAVED LOCAL",
                                style = NeoType.labelTelemetry,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeoSecondaryDark
                            )
                        }

                        // Brutalist Input Box
                        Box(modifier = Modifier.fillMaxWidth().padding(bottom = 3.dp, end = 3.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .offset(x = 3.dp, y = 3.dp)
                                    .background(NeoBlack, RoundedCornerShape(8.dp))
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .background(NeoSurfaceMid, RoundedCornerShape(8.dp))
                                    .border(2.5.dp, NeoBlack, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                BasicTextField(
                                    value = profileName,
                                    onValueChange = {
                                        profileName = it
                                        onSaveProfile(it)
                                    },
                                    textStyle = TextStyle(
                                        fontFamily = FontFamily.Default,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NeoInk
                                    ),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = Icons.Rounded.Edit,
                                    contentDescription = "Edit name",
                                    tint = Color(0xFF4B4731),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // Export Logs Button
                    NeoButton(
                        text = if (isExporting) "LOGS EXPORTED (2.4MB)" else "EXPORT GAZE LOGS (.CSV)",
                        onClick = {
                            isExporting = true
                            Toast.makeText(context, "Gaze logs exported: /sdcard/Download/gaze_log.csv", Toast.LENGTH_LONG).show()
                        },
                        containerColor = if (isExporting) NeoMintContainer else NeoTertiary,
                        contentColor = if (isExporting) NeoBlack else NeoWhite,
                        leadingIcon = if (isExporting) Icons.Rounded.CheckCircle else Icons.Rounded.Download,
                        shadowOffset = 4.dp,
                        borderWidth = 3.5.dp,
                        cornerRadius = 10.dp,
                        height = 52.dp,
                        fontSize = 13
                    )
                }
            }
        }
    }
}

@Composable
private fun NeoSwitchRow(
    title: String,
    tag: String,
    tagColor: Color,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    activeColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    NeoCard(
        backgroundColor = NeoCard,
        borderWidth = 2.5.dp,
        shadowOffset = 3.dp,
        cornerRadius = 10.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = title,
                        style = NeoType.headlineSm,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeoBlack
                    )
                    Box(
                        modifier = Modifier
                            .background(tagColor, RoundedCornerShape(4.dp))
                            .border(1.dp, NeoBlack, RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = tag,
                            style = NeoType.labelTelemetry,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeoBlack
                        )
                    }
                }
                Text(
                    text = subtitle,
                    style = NeoType.bodySm,
                    fontSize = 11.sp,
                    color = Color(0xFF4B4731),
                    lineHeight = 15.sp
                )
            }

            NeoSwitch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                activeColor = activeColor,
                inactiveColor = NeoSurfaceHigh,
                icon = icon
            )
        }
    }
}
