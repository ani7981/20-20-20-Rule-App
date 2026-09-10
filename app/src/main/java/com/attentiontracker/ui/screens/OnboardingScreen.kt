package com.attentiontracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.attentiontracker.ui.theme.*

@Composable
fun OnboardingScreen(
    onContinue: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf("") }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NeoSurface)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Logo
            NeoEyeLogo(modifier = Modifier.size(64.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "20·20·20 PROTOCOL",
                    style = NeoType.headlineLg,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp,
                    color = NeoInk
                )
                Text(
                    text = "OPTICAL KINETIC STRAIN TRACKER",
                    style = NeoType.labelBadge,
                    fontSize = 11.sp,
                    color = Color(0xFF4B4731),
                    letterSpacing = 1.sp
                )
            }

            // Welcome Card
            NeoCard(
                backgroundColor = NeoYellow,
                borderWidth = 4.dp,
                shadowOffset = 6.dp,
                cornerRadius = 16.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "RETINA DEFENSE",
                            style = NeoType.labelCode,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeoBlack
                        )
                        Box(
                            modifier = Modifier
                                .rotate(-2f)
                                .background(NeoMint, RoundedCornerShape(999.dp))
                                .border(1.5.dp, NeoBlack, RoundedCornerShape(999.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "RULE 20·20·20",
                                style = NeoType.labelBadge,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeoBlack
                            )
                        }
                    }

                    Text(
                        text = "Every 20 minutes, fixate on an object 20 feet away for 20 seconds to release ciliary muscle spasms.",
                        style = NeoType.bodyMd,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = NeoInk,
                        lineHeight = 18.sp
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "WHAT SHOULD WE CALL YOU?",
                            style = NeoType.labelBadge,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = NeoBlack
                        )

                        // Input Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .background(NeoCard, RoundedCornerShape(8.dp))
                                .border(2.5.dp, NeoBlack, RoundedCornerShape(8.dp))
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (name.isEmpty()) {
                                Text(
                                    text = "Enter your name (e.g. Alex)",
                                    style = NeoType.bodyMd,
                                    color = Color(0xFF7C775F)
                                )
                            }
                            BasicTextField(
                                value = name,
                                onValueChange = { name = it },
                                textStyle = TextStyle(
                                    fontFamily = FontFamily.Default,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeoInk
                                ),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // Start CTA Button
            NeoButton(
                text = "START PROTOCOL",
                onClick = {
                    val finalName = name.ifBlank { "Alex" }
                    onContinue(finalName)
                },
                containerColor = NeoMintContainer,
                contentColor = NeoBlack,
                leadingIcon = Icons.Rounded.ArrowForward,
                shadowOffset = 4.dp,
                borderWidth = 3.5.dp,
                cornerRadius = 12.dp,
                height = 54.dp,
                fontSize = 16
            )
        }
    }
}
