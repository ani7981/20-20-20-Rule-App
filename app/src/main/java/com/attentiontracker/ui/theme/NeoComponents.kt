package com.attentiontracker.ui.theme

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─── Hard Drop Shadow Card Container ──────────────────────────────────────────

/**
 * Fundamental Neobrutalist container with hard-edged pure black offset drop shadow.
 * Replicates the tactile arcade switch depression if [onClick] is provided.
 */
@Composable
fun NeoCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = NeoCard,
    borderColor: Color = NeoBlack,
    borderWidth: Dp = 3.dp,
    shadowOffset: Dp = 4.dp,
    cornerRadius: Dp = 12.dp,
    rotationDeg: Float = 0f,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val currentOffset by animateDpAsState(
        targetValue = if (onClick != null && isPressed) 0.dp else shadowOffset,
        animationSpec = tween(durationMillis = 80),
        label = "neoCardOffset"
    )

    val translationOffset by animateDpAsState(
        targetValue = if (onClick != null && isPressed) shadowOffset else 0.dp,
        animationSpec = tween(durationMillis = 80),
        label = "neoCardTranslation"
    )

    Box(
        modifier = modifier
            .padding(bottom = shadowOffset, end = shadowOffset)
            .then(if (rotationDeg != 0f) Modifier.rotate(rotationDeg) else Modifier)
    ) {
        // Drop shadow layer (pure black, no blur)
        if (currentOffset > 0.dp) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .offset(x = shadowOffset, y = shadowOffset)
                    .background(NeoBlack, RoundedCornerShape(cornerRadius))
            )
        }

        // Top surface card layer
        Box(
            modifier = Modifier
                .offset(x = translationOffset, y = translationOffset)
                .fillMaxWidth()
                .background(backgroundColor, RoundedCornerShape(cornerRadius))
                .border(borderWidth, borderColor, RoundedCornerShape(cornerRadius))
                .then(
                    if (onClick != null) {
                        Modifier.clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = onClick
                        )
                    } else Modifier
                ),
            content = content
        )
    }
}

// ─── Tactile Button ───────────────────────────────────────────────────────────

/**
 * Tactile Neobrutalist Action Button with mechanical switch depression.
 */
@Composable
fun NeoButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = NeoYellow,
    contentColor: Color = NeoBlack,
    leadingIcon: ImageVector? = null,
    leadingEmoji: String? = null,
    shadowOffset: Dp = 4.dp,
    borderWidth: Dp = 3.5.dp,
    cornerRadius: Dp = 12.dp,
    height: Dp = 54.dp,
    fontSize: Int = 16
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val currentOffset by animateDpAsState(
        targetValue = if (isPressed) 0.dp else shadowOffset,
        animationSpec = tween(durationMillis = 75),
        label = "neoBtnOffset"
    )
    val translationOffset by animateDpAsState(
        targetValue = if (isPressed) shadowOffset else 0.dp,
        animationSpec = tween(durationMillis = 75),
        label = "neoBtnTranslate"
    )

    Box(
        modifier = modifier
            .padding(bottom = shadowOffset, end = shadowOffset)
            .height(height + shadowOffset)
    ) {
        // Shadow base
        if (currentOffset > 0.dp) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height)
                    .offset(x = shadowOffset, y = shadowOffset)
                    .background(NeoBlack, RoundedCornerShape(cornerRadius))
            )
        }

        // Action surface
        Box(
            modifier = Modifier
                .offset(x = translationOffset, y = translationOffset)
                .fillMaxWidth()
                .height(height)
                .background(containerColor, RoundedCornerShape(cornerRadius))
                .border(borderWidth, NeoBlack, RoundedCornerShape(cornerRadius))
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                if (leadingIcon != null) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                } else if (leadingEmoji != null) {
                    Text(text = leadingEmoji, fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = text.uppercase(),
                    color = contentColor,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = fontSize.sp,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

// ─── Sticker Badge Pill ───────────────────────────────────────────────────────

/**
 * Adhesive sticker pill with rotation and high-contrast border.
 */
@Composable
fun NeoBadge(
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = NeoYellow,
    textColor: Color = NeoBlack,
    rotationDeg: Float = -2f,
    shadowOffset: Dp = 2.dp,
    borderWidth: Dp = 2.dp
) {
    Box(
        modifier = modifier
            .rotate(rotationDeg)
            .padding(bottom = shadowOffset, end = shadowOffset)
    ) {
        if (shadowOffset > 0.dp) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .offset(x = shadowOffset, y = shadowOffset)
                    .background(NeoBlack, RoundedCornerShape(999.dp))
            )
        }
        Box(
            modifier = Modifier
                .background(backgroundColor, RoundedCornerShape(999.dp))
                .border(borderWidth, NeoBlack, RoundedCornerShape(999.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = text.uppercase(),
                style = NeoType.labelBadge,
                color = textColor
            )
        }
    }
}

// ─── Live Status Pill with Pulsing Dot ────────────────────────────────────────

@Composable
fun NeoStatusPill(
    text: String,
    backgroundColor: Color = NeoCard,
    textColor: Color = NeoBlack,
    dotColor: Color = NeoMint,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.padding(bottom = 2.dp, end = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 2.dp, y = 2.dp)
                .background(NeoBlack, RoundedCornerShape(999.dp))
        )
        Row(
            modifier = Modifier
                .background(backgroundColor, RoundedCornerShape(999.dp))
                .border(2.5.dp, NeoBlack, RoundedCornerShape(999.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .background(dotColor, CircleShape)
                    .border(1.dp, NeoBlack, CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text.uppercase(),
                style = NeoType.labelCode,
                fontSize = 12.sp,
                color = textColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ─── Brutalist Pill Switch ────────────────────────────────────────────────────

/**
 * Authentic mechanical switch matching the HTML specifications:
 * 64x36dp pill container, 3px solid black border, 3px offset shadow,
 * circular white slider knob with icon that translates on toggle.
 */
@Composable
fun NeoSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    activeColor: Color = NeoMintContainer,
    inactiveColor: Color = NeoSurfaceMid,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }

    val knobOffset by animateDpAsState(
        targetValue = if (checked) 28.dp else 0.dp,
        animationSpec = tween(durationMillis = 150),
        label = "switchKnob"
    )

    Box(
        modifier = modifier
            .padding(bottom = 3.dp, end = 3.dp)
            .size(width = 64.dp, height = 36.dp)
    ) {
        // Shadow base
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 3.dp, y = 3.dp)
                .background(NeoBlack, RoundedCornerShape(999.dp))
        )

        // Pill track
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (checked) activeColor else inactiveColor,
                    RoundedCornerShape(999.dp)
                )
                .border(2.5.dp, NeoBlack, RoundedCornerShape(999.dp))
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = { onCheckedChange(!checked) }
                )
                .padding(3.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            // Knob
            Box(
                modifier = Modifier
                    .offset(x = knobOffset)
                    .size(24.dp)
                    .background(NeoWhite, CircleShape)
                    .border(2.dp, NeoBlack, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = NeoBlack,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

// ─── Segmented Brutalist Progress Bar ─────────────────────────────────────────

/**
 * Discrete block cells separated by black borders, with diagonal stripes
 * for completed progress as defined in the Optical Kinetic Neo-Brutalist specification.
 */
@Composable
fun NeoSegmentedProgressBar(
    totalSegments: Int,
    filledSegments: Int,
    modifier: Modifier = Modifier,
    fillColor: Color = NeoMint,
    emptyColor: Color = NeoCard,
    height: Dp = 26.dp
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 2.dp, end = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .offset(x = 2.dp, y = 2.dp)
                .background(NeoBlack, RoundedCornerShape(8.dp))
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .background(NeoCard, RoundedCornerShape(8.dp))
                .border(2.5.dp, NeoBlack, RoundedCornerShape(8.dp))
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            repeat(totalSegments) { index ->
                val isFilled = index < filledSegments
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(2.dp))
                        .border(1.5.dp, NeoBlack, RoundedCornerShape(2.dp))
                ) {
                    if (isFilled) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            // Base color fill
                            drawRect(fillColor)
                            // Diagonal stripes hatching
                            drawHatchedStripes(NeoBlack.copy(alpha = 0.18f), stripeWidth = 4.dp.toPx(), gap = 8.dp.toPx())
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(emptyColor)
                        )
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawHatchedStripes(color: Color, stripeWidth: Float, gap: Float) {
    val total = size.width + size.height
    var x = -size.height
    while (x < total) {
        drawLine(
            color = color,
            start = Offset(x, size.height),
            end = Offset(x + size.height, 0f),
            strokeWidth = stripeWidth
        )
        x += gap + stripeWidth
    }
}

// ─── 20-20-20 Eye Tracker Logo ───────────────────────────────────────────────

/**
 * Authentic SVG reproduction of the 20·20·20 Eye Tracker Logo from code.html:
 * Rounded lemon yellow box with thick black stroke, white eye sclera, mint iris,
 * black pupil with highlight dot, and rotated hot pink sticker accent.
 */
@Composable
fun NeoEyeLogo(
    modifier: Modifier = Modifier.size(36.dp)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val scale = w / 100f

        // Yellow rounded badge background with black stroke
        val rectPadding = 6f * scale
        val rectSize = 88f * scale
        val cornerRadius = 16f * scale
        val stroke6 = 5f * scale

        drawRoundRect(
            color = NeoYellow,
            topLeft = Offset(rectPadding, rectPadding),
            size = Size(rectSize, rectSize),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius, cornerRadius),
            style = Fill
        )
        drawRoundRect(
            color = NeoBlack,
            topLeft = Offset(rectPadding, rectPadding),
            size = Size(rectSize, rectSize),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius, cornerRadius),
            style = Stroke(width = stroke6)
        )

        // Sclera (Eye shape): path from (20,50) to (80,50)
        val eyePath = Path().apply {
            moveTo(20f * scale, 50f * scale)
            cubicTo(
                20f * scale, 32f * scale,
                80f * scale, 32f * scale,
                80f * scale, 50f * scale
            )
            cubicTo(
                80f * scale, 68f * scale,
                20f * scale, 68f * scale,
                20f * scale, 50f * scale
            )
            close()
        }
        drawPath(eyePath, color = NeoWhite, style = Fill)
        drawPath(eyePath, color = NeoBlack, style = Stroke(width = 4f * scale))

        // Mint Iris
        drawCircle(
            color = NeoMint,
            radius = 16f * scale,
            center = Offset(50f * scale, 50f * scale)
        )
        drawCircle(
            color = NeoBlack,
            radius = 16f * scale,
            center = Offset(50f * scale, 50f * scale),
            style = Stroke(width = 3.5f * scale)
        )

        // Pupil (black)
        drawCircle(
            color = NeoBlack,
            radius = 8f * scale,
            center = Offset(50f * scale, 50f * scale)
        )

        // White reflection dot
        drawCircle(
            color = NeoWhite,
            radius = 2.8f * scale,
            center = Offset(46f * scale, 46f * scale)
        )

        // Rotated Cyber Pink Sticker on top right
        val stickerW = 22f * scale
        val stickerH = 12f * scale
        val stickerX = 64f * scale
        val stickerY = 14f * scale

        drawContext.canvas.nativeCanvas.save()
        drawContext.canvas.nativeCanvas.rotate(12f, stickerX + stickerW / 2f, stickerY + stickerH / 2f)
        drawRoundRect(
            color = NeoPink,
            topLeft = Offset(stickerX, stickerY),
            size = Size(stickerW, stickerH),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f * scale, 4f * scale)
        )
        drawRoundRect(
            color = NeoBlack,
            topLeft = Offset(stickerX, stickerY),
            size = Size(stickerW, stickerH),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f * scale, 4f * scale),
            style = Stroke(width = 2.5f * scale)
        )
        drawContext.canvas.nativeCanvas.restore()
    }
}

// ─── Optical Deflection Target Eye Graphic ───────────────────────────────────

/**
 * The optical deflection vector eye from break_overlay_rest_state/code.html
 */
@Composable
fun NeoDeflectionEyeGraphic(modifier: Modifier = Modifier.size(56.dp)) {
    Canvas(modifier = modifier) {
        val s = size.width / 48f
        // White box background
        drawRoundRect(
            color = NeoWhite,
            topLeft = Offset(0f, 0f),
            size = Size(size.width, size.height),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f * s, 8f * s)
        )
        drawRoundRect(
            color = NeoBlack,
            topLeft = Offset(0f, 0f),
            size = Size(size.width, size.height),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f * s, 8f * s),
            style = Stroke(width = 2.5f * s)
        )

        // Eye path
        val eyePath = Path().apply {
            moveTo(6f * s, 24f * s)
            cubicTo(12f * s, 14f * s, 36f * s, 14f * s, 42f * s, 24f * s)
            cubicTo(36f * s, 34f * s, 12f * s, 34f * s, 6f * s, 24f * s)
            close()
        }
        drawPath(eyePath, color = NeoYellow, style = Fill)
        drawPath(eyePath, color = NeoInk, style = Stroke(width = 2.5f * s))

        // Center pupil green
        drawCircle(
            color = NeoSecondaryDark,
            radius = 6.5f * s,
            center = Offset(24f * s, 24f * s)
        )
        drawCircle(
            color = NeoInk,
            radius = 6.5f * s,
            center = Offset(24f * s, 24f * s),
            style = Stroke(width = 1.8f * s)
        )
        // Reflection
        drawCircle(
            color = NeoWhite,
            radius = 2.2f * s,
            center = Offset(26f * s, 22f * s)
        )

        // Eyelash rays
        drawLine(
            color = NeoInk,
            start = Offset(36f * s, 10f * s),
            end = Offset(41f * s, 6f * s),
            strokeWidth = 2.2f * s
        )
        drawLine(
            color = NeoInk,
            start = Offset(33f * s, 6f * s),
            end = Offset(35f * s, 10f * s),
            strokeWidth = 2.2f * s
        )
    }
}
