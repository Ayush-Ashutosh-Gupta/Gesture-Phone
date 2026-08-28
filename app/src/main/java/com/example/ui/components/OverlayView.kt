package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import com.example.model.GestureConfig
import com.example.model.HandType
import com.example.model.LandmarkPoint
import com.example.model.TrackedHand

// 21 MediaPipe Hand Landmark connections
val HAND_CONNECTIONS = listOf(
    // Palm / Wrist
    0 to 1, 1 to 2, 2 to 3, 3 to 4, // Thumb
    0 to 5, 5 to 6, 6 to 7, 7 to 8, // Index
    5 to 9, 9 to 10, 10 to 11, 11 to 12, // Middle
    9 to 13, 13 to 14, 14 to 15, 15 to 16, // Ring
    13 to 17, 17 to 18, 18 to 19, 19 to 20, // Pinky
    0 to 17 // Wrist to Pinky base
)

@Composable
fun OverlayView(
    modifier: Modifier = Modifier,
    trackedHands: List<TrackedHand>,
    config: GestureConfig,
    streamWidth: Int = 480,
    streamHeight: Int = 640,
    onLandmarkLongPressed: (LandmarkPoint, Int) -> Unit = { _, _ -> }
) {
    Canvas(
        modifier = modifier.fillMaxSize()
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        if (canvasWidth <= 0 || canvasHeight <= 0) return@Canvas

        // Camera frame aspect ratio in portrait
        // Matching PreviewView.ScaleType.FILL_CENTER uniform scaling & center-cropping
        val portraitWidth = if (streamWidth > 0 && streamHeight > 0) minOf(streamWidth, streamHeight).toFloat() else 480f
        val portraitHeight = if (streamWidth > 0 && streamHeight > 0) maxOf(streamWidth, streamHeight).toFloat() else 640f
        val cameraAspect = portraitWidth / portraitHeight
        val viewAspect = canvasWidth / canvasHeight

        val renderedStreamWidth: Float
        val renderedStreamHeight: Float
        val offsetX: Float
        val offsetY: Float

        if (viewAspect > cameraAspect) {
            // View is wider than camera stream -> scale to fill width, crop top/bottom
            renderedStreamWidth = canvasWidth
            renderedStreamHeight = canvasWidth / cameraAspect
            offsetX = 0f
            offsetY = (canvasHeight - renderedStreamHeight) / 2f
        } else {
            // View is taller than camera stream -> scale to fill height, crop left/right
            renderedStreamHeight = canvasHeight
            renderedStreamWidth = canvasHeight * cameraAspect
            offsetX = (canvasWidth - renderedStreamWidth) / 2f
            offsetY = 0f
        }

        for (hand in trackedHands) {
            val isRight = hand.handType == HandType.RIGHT
            
            // Primary hand colors: Neon Cyan for Right Hand, Electric Coral for Left Hand
            val lineColor = if (isRight) Color(0xFF00E5FF) else Color(0xFFFF3366)
            val jointColor = if (isRight) Color(0xFF80D8FF) else Color(0xFFFF80AB)
            val tipColor = if (isRight) Color(0xFF00E5FF) else Color(0xFFFF1744)
            val wristColor = if (isRight) Color(0xFF00FFFF) else Color(0xFFFF5252)

            val screenPoints = hand.landmarks.map { lm ->
                Offset(
                    x = lm.x * renderedStreamWidth + offsetX,
                    y = lm.y * renderedStreamHeight + offsetY
                )
            }

            // Draw skeleton lines
            if (config.showSkeletonLines && screenPoints.size >= 21) {
                for ((startIdx, endIdx) in HAND_CONNECTIONS) {
                    if (startIdx < screenPoints.size && endIdx < screenPoints.size) {
                        drawLine(
                            color = lineColor.copy(alpha = 0.85f),
                            start = screenPoints[startIdx],
                            end = screenPoints[endIdx],
                            strokeWidth = 5.5f,
                            cap = StrokeCap.Round
                        )
                    }
                }
            }

            // Draw landmarks dots
            if (config.showDebugLandmarks) {
                // 1. Draw Wrist Anchor (Landmark 0) prominently
                if (screenPoints.isNotEmpty()) {
                    val wristPt = screenPoints[0]
                    drawCircle(
                        color = wristColor.copy(alpha = 0.25f),
                        radius = 20f,
                        center = wristPt
                    )
                    drawCircle(
                        color = wristColor,
                        radius = 12f,
                        center = wristPt,
                        style = Stroke(width = 3.5f)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 4.5f,
                        center = wristPt
                    )
                }

                // 2. Draw Finger Joints and Tips
                for ((idx, point) in screenPoints.withIndex()) {
                    if (idx == 0) continue // Already rendered with prominent wrist anchor
                    val isTip = idx in listOf(4, 8, 12, 16, 20)
                    val radius = if (isTip) 10f else 5.5f
                    val dotColor = if (isTip) tipColor else jointColor

                    // Outer glow
                    drawCircle(
                        color = dotColor.copy(alpha = if (isTip) 0.40f else 0.25f),
                        radius = radius * 2.2f,
                        center = point
                    )

                    // Core dot
                    drawCircle(
                        color = dotColor,
                        radius = radius,
                        center = point
                    )

                    // Fingertip precision targeting ring
                    if (isTip) {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.9f),
                            radius = radius * 0.45f,
                            center = point
                        )
                    }
                }
            }

            // Draw Pinch indicator halo between thumb tip and index tip
            if (screenPoints.size >= 9) {
                val thumbTip = screenPoints[4]
                val indexTip = screenPoints[8]
                val midPoint = Offset((thumbTip.x + indexTip.x) / 2f, (thumbTip.y + indexTip.y) / 2f)

                if (hand.isPinching) {
                    // Pinch active glowing gold target ring
                    drawCircle(
                        color = Color(0xFFFFD700),
                        radius = 28f,
                        center = midPoint,
                        style = Stroke(width = 6f)
                    )
                    drawCircle(
                        color = Color(0x66FFD700),
                        radius = 16f,
                        center = midPoint
                    )
                    drawLine(
                        color = Color(0xFFFFD700),
                        start = thumbTip,
                        end = indexTip,
                        strokeWidth = 7f,
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}
