package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GestureType
import com.example.model.TrackedHand
import kotlin.math.hypot
import kotlin.random.Random

data class EscapeOrb(
    val id: Int,
    val x: Float,
    val y: Float,
    val radius: Float = 40f,
    var isPopped: Boolean = false
)

@Composable
fun EscapeModeView(
    trackedHands: List<TrackedHand>,
    onClose: () -> Unit,
    onSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    var stage by remember { mutableStateOf(1) } // Stage 1: Pop 3 Orbs, Stage 2: Peace Charge, Stage 3: Escape Solved!
    val orbs = remember {
        mutableStateListOf(
            EscapeOrb(1, 0.3f, 0.35f),
            EscapeOrb(2, 0.7f, 0.45f),
            EscapeOrb(3, 0.5f, 0.65f)
        )
    }

    var peaceChargeProgress by remember { mutableStateOf(0f) }
    var score by remember { mutableStateOf(0) }

    // Check interaction with tracked hands
    LaunchedEffect(trackedHands, stage) {
        if (trackedHands.isNotEmpty()) {
            val hand = trackedHands.first()
            val tipX = hand.indexTipNormalized.x
            val tipY = hand.indexTipNormalized.y

            if (stage == 1) {
                // Check if index tip or pinch hit any unpopped orb
                for (orb in orbs) {
                    if (!orb.isPopped) {
                        val dist = hypot(orb.x - tipX, orb.y - tipY)
                        if (dist < 0.12f && hand.isPinching) {
                            orb.isPopped = true
                            score += 100
                        }
                    }
                }
                if (orbs.all { it.isPopped }) {
                    stage = 2
                }
            } else if (stage == 2) {
                // Hold Peace sign to charge
                if (hand.activeGesture == GestureType.PEACE_SIGN) {
                    peaceChargeProgress = (peaceChargeProgress + 0.04f).coerceAtMost(1f)
                    if (peaceChargeProgress >= 1f) {
                        stage = 3
                        score += 300
                        onSuccess()
                    }
                } else {
                    peaceChargeProgress = (peaceChargeProgress - 0.02f).coerceAtLeast(0f)
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xCC050B14))
    ) {
        // Floating Top HUD
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(imageVector = Icons.Default.VpnKey, contentDescription = null, tint = Color(0xFFFFD700))
                    Text(
                        text = "GESTURE ESCAPE PUZZLE",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                }
                Text(
                    text = "Score: $score PTS",
                    color = Color(0xFF00E5FF),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0x44FFFFFF))
            ) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White)
            }
        }

        // Stage 1: Floating energy orbs
        if (stage == 1) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasW = size.width
                val canvasH = size.height

                for (orb in orbs) {
                    if (!orb.isPopped) {
                        val center = Offset(orb.x * canvasW, orb.y * canvasH)
                        // Outer pulse ring
                        drawCircle(
                            color = Color(0x6600E5FF),
                            radius = orb.radius * 1.5f,
                            center = center,
                            style = Stroke(width = 3f)
                        )
                        // Core glowing orb
                        drawCircle(
                            brush = Brush.radialGradient(
                                listOf(Color(0xFF80D8FF), Color(0xFF00E5FF), Color(0x0000E5FF)),
                                center = center,
                                radius = orb.radius
                            ),
                            radius = orb.radius,
                            center = center
                        )
                    }
                }
            }

            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 48.dp, start = 24.dp, end = 24.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xE60D1B2A)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.TouchApp, contentDescription = null, tint = Color(0xFF00E5FF))
                    Text(
                        text = "Stage 1: Hover index finger over glowing orbs & PINCH to pop them!",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Stage 2: Hold Peace Sign
        if (stage == 2) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "STAGE 2: OVERLOAD CORE",
                    color = Color(0xFFFF4081),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Hold PEACE / VICTORY SIGN ✌️ in front of camera to charge energy beam!",
                    color = Color.White,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(16.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x44FFFFFF))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .fillMaxWidth(peaceChargeProgress)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFFFF4081), Color(0xFFFFD700))
                                )
                            )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${(peaceChargeProgress * 100).toInt()}% CHARGED",
                    color = Color(0xFFFFD700),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Stage 3: Escape Victory Card
        if (stage == 3) {
            Card(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xF00D1B2A)),
                border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFFFD700))
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = "ESCAPE OVERRIDE COMPLETE!",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "You mastered pure on-device hand control. No touch. No internet. Total mastery.",
                        color = Color(0xCCFFFFFF),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = onClose,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Return to Control Cockpit", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
