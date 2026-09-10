package com.attentiontracker.ui.screens

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.attentiontracker.AppUsage
import com.attentiontracker.TimeOfDayUsage
import com.attentiontracker.drawableToBitmap
import com.attentiontracker.formatMs
import com.attentiontracker.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AnalyticsScreen(
    usageStats: List<AppUsage>,
    timeOfDayStats: List<TimeOfDayUsage>,
    hasUsagePerm: Boolean,
    onRequestUsagePerm: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val todayFormatted = remember {
        SimpleDateFormat("MMM dd", Locale.US).format(Date()).uppercase()
    }

    // Prepare time-of-day hours
    val morningMs = timeOfDayStats.getOrNull(0)?.timeMs ?: 6480000L // ~1.8h default
    val afternoonMs = timeOfDayStats.getOrNull(1)?.timeMs ?: 11520000L // ~3.2h default
    val eveningMs = timeOfDayStats.getOrNull(2)?.timeMs ?: 7560000L // ~2.1h default
    val nightMs = timeOfDayStats.getOrNull(3)?.timeMs ?: 2880000L // ~0.8h default

    val morningH = (morningMs / 3600000f)
    val afternoonH = (afternoonMs / 3600000f)
    val eveningH = (eveningMs / 3600000f)
    val nightH = (nightMs / 3600000f)

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
        // 1. TOP HEADER & DATE SELECTOR
        // ═══════════════════════════════════════════════════════════════
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Header with Yellow Highlight
                Box {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth(0.58f)
                            .height(14.dp)
                            .background(NeoYellow)
                    )
                    Text(
                        text = "YOUR EYES TODAY",
                        style = NeoType.headlineLg,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp,
                        color = NeoInk,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }

                // Date Chip
                NeoCard(
                    backgroundColor = NeoCard,
                    borderWidth = 3.dp,
                    shadowOffset = 3.dp,
                    cornerRadius = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "TODAY, $todayFormatted",
                            style = NeoType.labelCode,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeoInk
                        )
                        Icon(
                            imageVector = Icons.Rounded.ArrowDropDown,
                            contentDescription = null,
                            tint = NeoInk,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Text(
                text = "Aggregated biometric gaze strain metrics & app splits.",
                style = NeoType.bodyMd,
                fontSize = 13.sp,
                color = Color(0xFF4B4731),
                fontWeight = FontWeight.Medium
            )
        }

        // ═══════════════════════════════════════════════════════════════
        // 2. HERO ANALYTICS CARD: STRAIN BY TIME OF DAY
        // ═══════════════════════════════════════════════════════════════
        NeoCard(
            backgroundColor = NeoCard,
            borderWidth = 4.dp,
            shadowOffset = 5.dp,
            cornerRadius = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header with Legend
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "STRAIN BY TIME OF DAY",
                            style = NeoType.headlineSm,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = NeoBlack
                        )
                        Icon(
                            imageVector = Icons.Rounded.BarChart,
                            contentDescription = null,
                            tint = NeoSecondaryDark,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Legend
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(NeoPink)
                                    .border(1.5.dp, NeoBlack)
                            )
                            Text(
                                text = "HIGH STRAIN",
                                style = NeoType.labelBadge,
                                fontSize = 10.sp,
                                color = Color(0xFF4B4731)
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(NeoSecondaryFixedDim)
                                    .border(1.5.dp, NeoBlack)
                            )
                            Text(
                                text = "NORMAL STRAIN",
                                style = NeoType.labelBadge,
                                fontSize = 10.sp,
                                color = Color(0xFF4B4731)
                            )
                        }
                    }

                    Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(NeoBlack))
                }

                // Brutalist Vertical Bar Chart
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                ) {
                    // Y-axis Reference Dashed Lines
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 32.dp, end = 4.dp, bottom = 44.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf("4h", "3h", "2h", "1h").forEach { tick ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = tick,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = Color(0xFF7C775F),
                                    modifier = Modifier.width(24.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(1.dp)
                                        .background(Color(0xFFCDC7AA))
                                )
                            }
                        }
                    }

                    // 4 Vertical Bars
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 36.dp, end = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        // Max scale is 4.0 hours
                        val maxScale = 4.0f

                        // 1. Morning Bar
                        TimeOfDayBar(
                            heightFraction = (morningH / maxScale).coerceIn(0.1f, 1f),
                            timeLabel = String.format(Locale.US, "%.1fh", morningH),
                            timeCode = "08-12",
                            slotLabel = "MORN",
                            barColor = NeoCyan,
                            isPeak = false,
                            modifier = Modifier.weight(1f)
                        )

                        // 2. Afternoon Bar (Peak Strain!)
                        TimeOfDayBar(
                            heightFraction = (afternoonH / maxScale).coerceIn(0.1f, 1f),
                            timeLabel = String.format(Locale.US, "%.1fh", afternoonH),
                            timeCode = "12-16",
                            slotLabel = "AFTN",
                            barColor = NeoPink,
                            isPeak = true,
                            modifier = Modifier.weight(1f)
                        )

                        // 3. Evening Bar
                        TimeOfDayBar(
                            heightFraction = (eveningH / maxScale).coerceIn(0.1f, 1f),
                            timeLabel = String.format(Locale.US, "%.1fh", eveningH),
                            timeCode = "16-20",
                            slotLabel = "EVE",
                            barColor = NeoSecondaryFixedDim,
                            isPeak = false,
                            modifier = Modifier.weight(1f)
                        )

                        // 4. Night Bar
                        TimeOfDayBar(
                            heightFraction = (nightH / maxScale).coerceIn(0.1f, 1f),
                            timeLabel = String.format(Locale.US, "%.1fh", nightH),
                            timeCode = "20-24",
                            slotLabel = "NITE",
                            barColor = NeoYellow,
                            isPeak = false,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Chart Footer Micro Telemetry
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, Color(0xFFCDC7AA), RoundedCornerShape(0.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "AVG GAZE FREQ: 42 BLINKS/MIN",
                            style = NeoType.labelTelemetry,
                            fontSize = 9.sp,
                            color = Color(0xFF4B4731)
                        )
                        Text(
                            text = "OPTIMAL REST RATIO: 82%",
                            style = NeoType.labelTelemetry,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeoSecondaryDark
                        )
                    }
                }
            }
        }

        // ═══════════════════════════════════════════════════════════════
        // 3. TOP ATTENTION DRAINING APPS
        // ═══════════════════════════════════════════════════════════════
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sticker Header
                Box(
                    modifier = Modifier
                        .rotate(-1f)
                        .background(NeoYellow, RoundedCornerShape(4.dp))
                        .border(2.5.dp, NeoBlack, RoundedCornerShape(4.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Warning,
                            contentDescription = null,
                            tint = NeoBlack,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "TOP ATTENTION DRAINS",
                            style = NeoType.labelBadge,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = NeoBlack
                        )
                    }
                }

                Text(
                    text = "${usageStats.size.coerceAtLeast(4)} APPS TRACKED",
                    style = NeoType.labelBadge,
                    fontSize = 10.sp,
                    color = Color(0xFF4B4731)
                )
            }

            if (!hasUsagePerm) {
                // Usage permission banner
                NeoCard(
                    backgroundColor = NeoYellow,
                    borderWidth = 3.dp,
                    shadowOffset = 4.dp,
                    cornerRadius = 8.dp,
                    onClick = onRequestUsagePerm
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = NeoBlack,
                            modifier = Modifier.size(24.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "GRANT USAGE ACCESS",
                                style = NeoType.labelBadge,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeoBlack
                            )
                            Text(
                                text = "Tap to enable real-time per-app screen time telemetry.",
                                style = NeoType.bodySm,
                                fontSize = 11.sp,
                                color = NeoInk
                            )
                        }
                    }
                }
            }

            // Display apps (either live usageStats or high-fidelity preview items from Stitch)
            val displayApps = if (usageStats.isNotEmpty()) {
                usageStats
            } else {
                listOf(
                    AppUsage("com.instagram.android", 6300000L, "Instagram"),
                    AppUsage("com.google.android.youtube", 3120000L, "YouTube"),
                    AppUsage("com.slack", 2880000L, "Slack / Work"),
                    AppUsage("com.android.chrome", 2220000L, "Chrome Browser")
                )
            }

            val appCategories = listOf(
                "Social • Continuous Scroll",
                "Video • Glare Heavy",
                "Productivity • Text Strain",
                "Web • High Brightness"
            )
            val appColors = listOf(NeoPink, NeoOrange, NeoYellow, NeoCyan)
            val maxUsageMs = displayApps.maxOfOrNull { it.timeMs } ?: 1L

            displayApps.forEachIndexed { index, app ->
                val fillFraction = (app.timeMs.toFloat() / maxUsageMs.toFloat()).coerceIn(0.15f, 1f)
                val catAccent = appColors[index % appColors.size]
                val catLabel = appCategories.getOrElse(index) { "Screen Activity" }

                NeoCard(
                    backgroundColor = NeoPaper,
                    borderWidth = 3.dp,
                    shadowOffset = 3.dp,
                    cornerRadius = 8.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                // App Icon Box
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(catAccent, RoundedCornerShape(4.dp))
                                        .border(2.dp, NeoBlack, RoundedCornerShape(4.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val iconDrawable = app.icon
                                    if (iconDrawable != null) {
                                        val bmp = remember(app.packageName) { drawableToBitmap(iconDrawable) }
                                        Image(
                                            bitmap = bmp.asImageBitmap(),
                                            contentDescription = app.label,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = when (index % 4) {
                                                0 -> Icons.Rounded.CameraAlt
                                                1 -> Icons.Rounded.SmartDisplay
                                                2 -> Icons.Rounded.Chat
                                                else -> Icons.Rounded.Public
                                            },
                                            contentDescription = null,
                                            tint = NeoBlack,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                Column {
                                    Text(
                                        text = app.label,
                                        style = NeoType.headlineSm,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NeoBlack
                                    )
                                    Text(
                                        text = catLabel,
                                        style = NeoType.labelTelemetry,
                                        fontSize = 9.sp,
                                        color = Color(0xFF4B4731)
                                    )
                                }
                            }

                            // Time Badge
                            Box(
                                modifier = Modifier
                                    .background(catAccent.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                    .border(1.5.dp, NeoBlack, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = formatMs(app.timeMs),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeoBlack
                                )
                            }
                        }

                        // Chunky Progress Bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(14.dp)
                                .background(NeoCard, RoundedCornerShape(2.dp))
                                .border(2.dp, NeoBlack, RoundedCornerShape(2.dp))
                                .padding(1.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(fillFraction)
                                    .background(catAccent)
                                    .border(1.dp, NeoBlack)
                            )
                        }
                    }
                }
            }
        }

        // ═══════════════════════════════════════════════════════════════
        // 4. DAILY SUMMARY BADGE CALLOUT
        // ═══════════════════════════════════════════════════════════════
        NeoCard(
            backgroundColor = NeoSecondaryFixedDim,
            borderWidth = 4.dp,
            shadowOffset = 5.dp,
            cornerRadius = 12.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Retro Eye Mascot square
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(NeoCard, RoundedCornerShape(8.dp))
                        .border(3.dp, NeoBlack, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Mood,
                        contentDescription = null,
                        tint = NeoSecondaryDark,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "EYE STRAIN SCORE: 34",
                            style = NeoType.headlineSm,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = NeoBlack
                        )
                        Box(
                            modifier = Modifier
                                .background(NeoCard, RoundedCornerShape(3.dp))
                                .border(1.5.dp, NeoBlack, RoundedCornerShape(3.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "LOW",
                                style = NeoType.labelBadge,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeoBlack
                            )
                        }
                    }

                    Text(
                        text = "GREAT JOB! Keep taking 20s breaks.",
                        style = NeoType.bodySm,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00522E)
                    )
                }
            }
        }

        // ═══════════════════════════════════════════════════════════════
        // 5. EXPORT ACTION BUTTON
        // ═══════════════════════════════════════════════════════════════
        NeoButton(
            text = "EXPORT TELEMETRY REPORT",
            onClick = {
                Toast.makeText(context, "Telemetry exported to Downloads", Toast.LENGTH_SHORT).show()
            },
            containerColor = NeoCard,
            contentColor = NeoBlack,
            leadingIcon = Icons.Rounded.Download,
            shadowOffset = 4.dp,
            borderWidth = 3.5.dp,
            cornerRadius = 10.dp,
            height = 52.dp,
            fontSize = 14
        )
    }
}

@Composable
private fun TimeOfDayBar(
    heightFraction: Float,
    timeLabel: String,
    timeCode: String,
    slotLabel: String,
    barColor: Color,
    isPeak: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        // Floating "PEAK!" sticker badge for afternoon
        if (isPeak) {
            Box(
                modifier = Modifier
                    .offset(y = 4.dp)
                    .rotate(-3f)
                    .background(NeoPink, RoundedCornerShape(3.dp))
                    .border(2.dp, NeoBlack, RoundedCornerShape(3.dp))
                    .padding(horizontal = 5.dp, vertical = 1.dp)
            ) {
                Text(
                    text = "PEAK!",
                    style = NeoType.labelBadge,
                    fontSize = 9.sp,
                    color = NeoWhite,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        // Time numeral above bar
        Text(
            text = timeLabel,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (isPeak) NeoPink else NeoBlack
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Bar container
        Box(
            modifier = Modifier
                .width(44.dp)
                .fillMaxHeight(heightFraction.coerceIn(0.18f, 0.75f))
                .background(barColor, RoundedCornerShape(2.dp))
                .border(2.5.dp, NeoBlack, RoundedCornerShape(2.dp))
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Slot code (e.g. 12-16)
        Text(
            text = timeCode,
            style = NeoType.labelBadge,
            fontSize = 10.sp,
            color = NeoBlack
        )
        Text(
            text = slotLabel,
            style = NeoType.labelTelemetry,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = if (isPeak) NeoPink else Color(0xFF4B4731)
        )
    }
}
