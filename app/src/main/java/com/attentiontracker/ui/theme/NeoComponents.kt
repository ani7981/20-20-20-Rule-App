package com.attentiontracker.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ─── Modifier Extensions ─────────────────────────────────────────────────────

/**
 * Applies a neobrutalist hard-edge border (no blur, no gradient).
 *
 * Usage:
 *   Box(modifier = Modifier.neoBorder())               // 3 dp black border, 8 dp radius
 *   Box(modifier = Modifier.neoBorder(width = 4.dp, radius = 4.dp))
 *
 * For the full offset drop-shadow effect, layer this inside a card composable
 * where a solid [NeoBlack] Box is drawn behind and offset by (shadowX, shadowY).
 */
fun Modifier.neoBorder(
    width: Dp    = 3.dp,
    color: Color = NeoBlack,
    radius: Dp   = 8.dp
): Modifier = this.border(width, color, RoundedCornerShape(radius))

// ─── Badge / Sticker Composables ─────────────────────────────────────────────

/**
 * Decorative sticker/badge pill.
 *
 * Renders an uppercase pill with a thick black border and a slight rotation
 * to replicate an adhesive sticker applied to industrial equipment
 * (per the Optical Kinetic Neo-Brutalist design spec: -3 deg to +4 deg).
 *
 * @param text            Label text (uppercased automatically).
 * @param backgroundColor Fill color — default [NeoYellow] for primary badges.
 * @param textColor       Typography ink color.
 * @param rotationDeg     Clockwise degrees of tilt (negative = counter-clockwise).
 */
@Composable
fun NeoBadge(
    text: String,
    backgroundColor: Color = NeoYellow,
    textColor: Color = NeoBlack,
    rotationDeg: Float = -2f,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .rotate(rotationDeg)
            .background(backgroundColor, RoundedCornerShape(999.dp))
            .border(2.dp, NeoBlack, RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text  = text.uppercase(),
            style = NeoType.labelBadge,
            color = textColor
        )
    }
}

/**
 * Status pill for binary eye-tracking states.
 *
 * Intended for "LOOKING AT SCREEN" / "LOOKING AWAY" live indicators.
 * No rotation applied — system-state indicators must be immediately legible.
 *
 * @param text            Status label text (uppercased automatically).
 * @param backgroundColor Surface fill — e.g. [NeoMint] for active, [NeoPink] for warning.
 * @param textColor       Ink color over the background.
 */
@Composable
fun NeoStatusPill(
    text: String,
    backgroundColor: Color,
    textColor: Color = NeoBlack,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(backgroundColor, RoundedCornerShape(999.dp))
            .border(2.5.dp, NeoBlack, RoundedCornerShape(999.dp))
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            text  = text.uppercase(),
            style = NeoType.labelBadge,
            color = textColor
        )
    }
}
