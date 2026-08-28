package com.example.ui.components

import android.os.SystemClock
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.Utils
import com.example.model.GestureConfig
import com.example.model.HandType
import com.example.model.TrackedHand
import kotlinx.coroutines.delay

@Composable
fun HandCalibrationScanner(
    trackedHands: List<TrackedHand>,
    config: GestureConfig,
    onCalibrationComplete: (newConfig: GestureConfig) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var scanProgress by remember { mutableFloatStateOf(0f) }
    var isCalibrated by remember { mutableStateOf(false) }
    var scanStartTime by remember { mutableLongStateOf(0L) }
    var detectedPalmSize by remember { mutableFloatStateOf(0.12f) }
    var detectedHandType by remember { mutableStateOf(HandType.RIGHT) }

    // Pulse animation for holographic target guide
    val infiniteTransition = rememberInfiniteTransition(label = "scanner_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    // Hand tracking assessment
    val targetHand = trackedHands.firstOrNull()

    LaunchedEffect(targetHand, isCalibrated) {
        if (isCalibrated) return@LaunchedEffect

        if (targetHand != null && targetHand.landmarks.size >= 21) {
            val wrist = targetHand.landmarks[0]
            val middleMcp = targetHand.landmarks[9]
            val palmDist = Utils.distance2D(wrist, middleMcp)

            // Check if hand is reasonably centered in target region
            val isCentered = wrist.x in 0.15f..0.85f && wrist.y in 0.20f..0.90f
            val isGoodDistance = palmDist in 0.05f..0.35f

            if (isCentered && isGoodDistance) {
                if (scanStartTime == 0L) {
                    scanStartTime = SystemClock.uptimeMillis()
                }
                val elapsed = SystemClock.uptimeMillis() - scanStartTime
                val targetDuration = 1400f // 1.4 seconds steady hold for full calibration
                scanProgress = (elapsed / targetDuration).coerceIn(0f, 1f)

                detectedPalmSize = palmDist
                detectedHandType = targetHand.handType

                if (scanProgress >= 1.0f) {
                    isCalibrated = true
                }
            } else {
                scanStartTime = 0L
                scanProgress = 0f
            }
        } else {
            scanStartTime = 0L
            scanProgress = 0f
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xEE070E1A))
    ) {
        // Holographic Target Guide Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cWidth = size.width
            val cHeight = size.height
            val centerX = cWidth / 2f
            val centerY = cHeight * 0.45f

            val guideRadius = cWidth * 0.36f

            // Outer calibration guide ring
            drawCircle(
                color = Color(0xFF00E5FF).copy(alpha = pulseAlpha * 0.4f),
                radius = guideRadius,
                center = Offset(centerX, centerY),
                style = Stroke(width = 3f)
            )

            // Outer dashed scanning ring
            drawCircle(
                color = Color(0xFF00E5FF).copy(alpha = 0.2f),
                radius = guideRadius + 24f,
                center = Offset(centerX, centerY),
                style = Stroke(width = 1.5f)
            )

            // Scanning progress arc
            if (scanProgress > 0f) {
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(Color(0xFF00E5FF), Color(0xFF00FF88), Color(0xFF00E5FF))
                    ),
                    startAngle = -90f,
                    sweepAngle = scanProgress * 360f,
                    useCenter = false,
                    topLeft = Offset(centerX - guideRadius, centerY - guideRadius),
                    size = Size(guideRadius * 2f, guideRadius * 2f),
                    style = Stroke(width = 8f, cap = StrokeCap.Round)
                )
            }

            // Target anatomical nodes inside guide (Wrist + 5 Fingertips silhouette)
            val wristTarget = Offset(centerX, centerY + guideRadius * 0.55f)
            val thumbTarget = Offset(centerX - guideRadius * 0.48f, centerY + guideRadius * 0.10f)
            val indexTarget = Offset(centerX - guideRadius * 0.28f, centerY - guideRadius * 0.50f)
            val middleTarget = Offset(centerX, centerY - guideRadius * 0.65f)
            val ringTarget = Offset(centerX + guideRadius * 0.28f, centerY - guideRadius * 0.50f)
            val pinkyTarget = Offset(centerX + guideRadius * 0.48f, centerY - guideRadius * 0.25f)

            val targets = listOf(
                "Wrist" to wristTarget,
                "Thumb" to thumbTarget,
                "Index" to indexTarget,
                "Middle" to middleTarget,
                "Ring" to ringTarget,
                "Pinky" to pinkyTarget
            )

            for ((_, pos) in targets) {
                drawCircle(
                    color = Color(0xFF00E5FF).copy(alpha = 0.35f),
                    radius = 16f,
                    center = pos
                )
                drawCircle(
                    color = Color(0xFF00E5FF).copy(alpha = pulseAlpha),
                    radius = 6f,
                    center = pos
                )
            }

            // Target Guide Lines
            val connections = listOf(
                wristTarget to thumbTarget,
                wristTarget to indexTarget,
                wristTarget to middleTarget,
                wristTarget to ringTarget,
                wristTarget to pinkyTarget
            )

            for ((start, end) in connections) {
                drawLine(
                    color = Color(0xFF00E5FF).copy(alpha = 0.25f),
                    start = start,
                    end = end,
                    strokeWidth = 2f
                )
            }
        }

        // Top Controls Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 28.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Sensors,
                    contentDescription = null,
                    tint = Color(0xFF00E5FF)
                )
                Text(
                    text = "HAND CALIBRATION",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0x66FFFFFF))
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White
                )
            }
        }

        // Bottom Instruction Card / Calibration Status
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(18.dp),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFA0D1B2A)),
            border = androidx.compose.foundation.BorderStroke(
                1.5.dp,
                if (isCalibrated) Color(0xFF00E676) else Color(0x6600E5FF)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (!isCalibrated) {
                    Text(
                        text = if (targetHand != null) "HOLD HAND STEADY" else "ALIGN YOUR HAND",
                        color = Color(0xFF00E5FF),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )

                    Text(
                        text = if (targetHand != null) {
                            "Scanning 21 joint landmarks for ${targetHand.handType} Hand... ${(scanProgress * 100).toInt()}%"
                        } else {
                            "Position your hand open facing the camera inside the holographic circle."
                        },
                        color = Color(0xCCFFFFFF),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )

                    LinearProgressIndicator(
                        progress = { scanProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = Color(0xFF00E5FF),
                        trackColor = Color(0x3300E5FF)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xAAFFFFFF))
                        ) {
                            Text("Skip")
                        }

                        Button(
                            onClick = {
                                // Instant finish with optimal tuning
                                val newCfg = config.copy(
                                    calibratedPalmSize = detectedPalmSize.coerceIn(0.08f, 0.25f),
                                    sensitivity = 6.0f
                                )
                                onCalibrationComplete(newCfg)
                            },
                            modifier = Modifier.weight(1.5f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
                        ) {
                            Text("Use Optimal Defaults", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // Success State
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Success",
                        tint = Color(0xFF00E676),
                        modifier = Modifier.size(48.dp)
                    )

                    Text(
                        text = "HAND CALIBRATION COMPLETE",
                        color = Color(0xFF00E676),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold
                    )

                    Text(
                        text = "Wrist alignment, finger reach & gesture timings successfully optimized for your ${detectedHandType} hand.",
                        color = Color(0xDDFFFFFF),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                isCalibrated = false
                                scanProgress = 0f
                                scanStartTime = 0L
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00E5FF))
                        ) {
                            Icon(imageVector = Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Scan Again")
                        }

                        Button(
                            onClick = {
                                val calibratedSensitivity = when {
                                    detectedPalmSize > 0.18f -> 5.5f // Larger hand -> standard sensitivity
                                    detectedPalmSize < 0.10f -> 7.0f // Smaller hand / farther away -> higher sensitivity
                                    else -> 6.0f
                                }
                                val newCfg = config.copy(
                                    calibratedPalmSize = detectedPalmSize.coerceIn(0.08f, 0.25f),
                                    sensitivity = calibratedSensitivity
                                )
                                onCalibrationComplete(newCfg)
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676))
                        ) {
                            Text("Apply & Start", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
