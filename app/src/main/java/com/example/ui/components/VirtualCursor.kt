package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun VirtualCursor(
    cursorX: Float,
    cursorY: Float,
    isPinching: Boolean,
    lastClickTime: Long,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current

    // Ripple animation on click
    val rippleScale = remember { Animatable(0f) }
    val rippleAlpha = remember { Animatable(0f) }

    LaunchedEffect(lastClickTime) {
        if (lastClickTime > 0) {
            rippleScale.snapTo(0.2f)
            rippleAlpha.snapTo(0.9f)
            rippleScale.animateTo(2.4f, animationSpec = tween(300, easing = LinearEasing))
            rippleAlpha.animateTo(0f, animationSpec = tween(300, easing = LinearEasing))
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "cursor_pulse")
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()
        val cursorColor = if (isPinching) Color(0xFFFFD700) else Color(0xFF00E5FF)

        val targetOffsetX = (cursorX.coerceIn(0f, 1f) * widthPx).roundToInt()
        val targetOffsetY = (cursorY.coerceIn(0f, 1f) * heightPx).roundToInt()

        Box(
            modifier = Modifier
                .offset { IntOffset(targetOffsetX - 24, targetOffsetY - 24) }
                .align(Alignment.TopStart)
        ) {
            // Click ripple
            if (rippleAlpha.value > 0.03f) {
                Canvas(
                    modifier = Modifier
                        .size(80.dp)
                        .align(Alignment.Center)
                ) {
                    drawCircle(
                        color = Color(0xFFFFD700).copy(alpha = rippleAlpha.value),
                        radius = (size.minDimension / 2f) * rippleScale.value,
                        center = Offset(size.width / 2, size.height / 2),
                        style = Stroke(width = 4.5f)
                    )
                }
            }

            // Outer pulse glow
            Canvas(
                modifier = Modifier
                    .size(48.dp)
                    .align(Alignment.Center)
            ) {
                drawCircle(
                    color = cursorColor.copy(alpha = if (isPinching) 0.55f else 0.28f),
                    radius = (size.minDimension / 3.0f) * pulseGlow,
                    center = Offset(size.width / 2, size.height / 2)
                )
            }

            // Arrow cursor icon
            Icon(
                imageVector = Icons.Default.Navigation,
                contentDescription = "Virtual Cursor",
                tint = cursorColor,
                modifier = Modifier
                    .size(34.dp)
                    .rotate(315f)
                    .scale(if (isPinching) 0.80f else 1.0f)
                    .align(Alignment.Center)
            )
        }
    }
}

