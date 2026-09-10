package com.attentiontracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.attentiontracker.ui.theme.*

enum class AppTab(val label: String, val icon: ImageVector) {
    TRACKER("Tracker", Icons.Rounded.Visibility),
    ANALYTICS("Analytics", Icons.Rounded.BarChart),
    REST_MODE("Rest Mode", Icons.Rounded.Spa),
    SETTINGS("Settings", Icons.Rounded.Tune)
}

@Composable
fun NeoTopBar(
    currentTab: AppTab,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(NeoSurface)
            .statusBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: Logo + App Titles
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    NeoEyeLogo(modifier = Modifier.size(34.dp))
                    Column {
                        Text(
                            text = "20·20·20 TRACKER",
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp,
                            letterSpacing = (-0.5).sp,
                            color = NeoInk
                        )
                        Text(
                            text = currentTab.label.uppercase(),
                            style = NeoType.labelBadge,
                            fontSize = 10.sp,
                            color = Color(0xFF4B4731)
                        )
                    }
                }

                // Right: Profile Avatar circle
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(NeoPrimaryGold, CircleShape)
                        .border(2.dp, NeoBlack, CircleShape)
                        .clickable(onClick = onProfileClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Person,
                        contentDescription = "Profile",
                        tint = NeoWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // High contrast divider
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(NeoBlack)
            )
        }
    }
}

@Composable
fun NeoBottomBar(
    currentTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(NeoSurface)
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Divider
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp)
                    .background(NeoBlack)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppTab.values().forEach { tab ->
                    val isSelected = tab == currentTab
                    val interactionSource = remember { MutableInteractionSource() }

                    if (isSelected) {
                        // Selected tab in Electric Lemon Yellow with black border and offset shadow
                        Box(modifier = Modifier.padding(bottom = 2.dp, end = 2.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(width = 76.dp, height = 54.dp)
                                    .offset(x = 2.dp, y = 2.dp)
                                    .background(NeoBlack, RoundedCornerShape(8.dp))
                            )
                            Column(
                                modifier = Modifier
                                    .size(width = 76.dp, height = 54.dp)
                                    .background(NeoYellow, RoundedCornerShape(8.dp))
                                    .border(2.5.dp, NeoBlack, RoundedCornerShape(8.dp))
                                    .clickable(
                                        interactionSource = interactionSource,
                                        indication = null,
                                        onClick = { onTabSelected(tab) }
                                    ),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.label,
                                    tint = NeoBlack,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = tab.label.uppercase(),
                                    style = NeoType.labelBadge,
                                    fontSize = 9.sp,
                                    color = NeoBlack
                                )
                            }
                        }
                    } else {
                        // Unselected tab
                        Column(
                            modifier = Modifier
                                .size(width = 76.dp, height = 54.dp)
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = null,
                                    onClick = { onTabSelected(tab) }
                                ),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.label,
                                tint = Color(0xFF4B4731),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = tab.label.uppercase(),
                                style = NeoType.labelBadge,
                                fontSize = 9.sp,
                                color = Color(0xFF4B4731)
                            )
                        }
                    }
                }
            }
        }
    }
}
