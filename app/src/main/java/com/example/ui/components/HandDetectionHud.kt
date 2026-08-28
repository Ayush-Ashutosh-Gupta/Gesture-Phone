package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GestureType
import com.example.model.HandType
import com.example.model.TrackedHand
import com.example.service.GestureAccessibilityService

@Composable
fun HandDetectionHud(
    trackedHands: List<TrackedHand>,
    isBackgroundControlRunning: Boolean,
    onToggleBackgroundControl: (Boolean) -> Unit,
    onOpenGuide: () -> Unit = {},
    onSwitchToSim: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Quick Guide Shortcut Banner
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0x990D1B2A))
                .border(1.dp, Color(0x4400E5FF), RoundedCornerShape(12.dp))
                .clickable { onOpenGuide() }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoStories,
                    contentDescription = "Gesture Guide",
                    tint = Color(0xFF00E5FF),
                    modifier = Modifier.size(18.dp)
                )
                Column {
                    Text(
                        text = "GESTURE SYMBOL DICTIONARY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = "Left: Volume, Light & Media • Right: Pointer, Clicks & Nav",
                        fontSize = 9.sp,
                        color = Color(0xFFA0C4E2)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF00E5FF))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "VIEW GUIDE",
                    color = Color.Black,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 1. Live Hand Detection Telemetry Cards
        if (trackedHands.isEmpty()) {
            NoHandsDetectedCard(onSwitchToSim = onSwitchToSim)
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (hand in trackedHands) {
                    HandTelemetryCard(
                        hand = hand,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 2. Background Device Control Master Card
        BackgroundControlToggleCard(
            isServiceRunning = isBackgroundControlRunning,
            onToggle = onToggleBackgroundControl
        )
    }
}

@Composable
private fun HandTelemetryCard(
    hand: TrackedHand,
    modifier: Modifier = Modifier
) {
    val isRight = hand.handType == HandType.RIGHT
    val primaryColor = if (isRight) Color(0xFF00E5FF) else Color(0xFFFF3366)
    val roleTitle = if (isRight) "POINTER & NAV" else "SYSTEM & LIGHT"
    val cardBg = Color(0xEE0A1424)

    // Compute active action description string for this hand
    val actionText = when (hand.activeGesture) {
        GestureType.PINCH_CLICK -> if (isRight) "🤏 Touch Click / Drag" else "🤏 Pinch"
        GestureType.CURSOR_POINT -> if (isRight) "☝️ Smooth Pointer Moving" else "✋ Vertical Volume Slide"
        GestureType.ROCK_ON -> if (!isRight) "🤘 Flashlight Torch (Hold)" else "🤘 Rock On Pose"
        GestureType.THUMBS_UP -> if (!isRight) "👍 Volume Up (+8%)" else "👍 Thumbs Up"
        GestureType.THUMBS_DOWN -> if (!isRight) "👎 Volume Down (-8%)" else "👎 Thumbs Down"
        GestureType.FIST_HOLD -> if (!isRight) "✊ Play / Pause Media" else "✊ Fist Grip"
        GestureType.PEACE_SIGN -> if (!isRight) "✌️ Mute / Unmute Audio" else "✌️ Victory Sign"
        GestureType.PALM_OPEN -> if (!isRight) "✋ Continuous Volume Slide" else "✋ Open Palm"
        else -> hand.activeGesture.displayName
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, primaryColor.copy(alpha = 0.75f))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Header: Hand Label, Role, & Confidence %
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(primaryColor)
                    )
                    Column {
                        Text(
                            text = if (isRight) "RIGHT HAND" else "LEFT HAND",
                            color = primaryColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = roleTitle,
                            color = Color(0xAAFFFFFF),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    text = "${(hand.confidence * 100).toInt()}%",
                    color = Color(0xCCFFFFFF),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            // Active Gesture Pill with live Action Name
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(primaryColor.copy(alpha = 0.18f))
                    .border(1.dp, primaryColor.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Text(
                    text = actionText,
                    color = if (hand.isPinching) Color(0xFFFFD700) else Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Finger Extension Indicators (Thumb, Index, Middle, Ring, Pinky)
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = "FINGER TRACKING",
                    color = Color(0x88FFFFFF),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val labels = listOf("THU", "IDX", "MID", "RNG", "PNK")
                    hand.extendedFingers.forEachIndexed { index, isExtended ->
                        FingerStatusChip(
                            label = labels.getOrElse(index) { "F$index" },
                            isExtended = isExtended,
                            activeColor = primaryColor
                        )
                    }
                }
            }

            // Pinch Distance Meter
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Pinch Proximity", color = Color(0x88FFFFFF), fontSize = 8.sp)
                    Text(
                        text = "%.3f".format(hand.pinchDistance),
                        color = if (hand.isPinching) Color(0xFFFFD700) else Color(0xCCFFFFFF),
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
                LinearProgressIndicator(
                    progress = { (1.0f - (hand.pinchDistance / 0.15f)).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = if (hand.isPinching) Color(0xFFFFD700) else primaryColor,
                    trackColor = Color(0x33FFFFFF)
                )
            }
        }
    }
}

@Composable
private fun FingerStatusChip(
    label: String,
    isExtended: Boolean,
    activeColor: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isExtended) activeColor.copy(alpha = 0.25f) else Color(0x22FFFFFF))
            .border(
                1.dp,
                if (isExtended) activeColor.copy(alpha = 0.7f) else Color(0x22FFFFFF),
                RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 4.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isExtended) Color.White else Color(0x66FFFFFF),
            fontSize = 8.sp,
            fontWeight = if (isExtended) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun NoHandsDetectedCard(
    onSwitchToSim: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xDD0B1626)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x4400E5FF))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0x3300E5FF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PanTool,
                        contentDescription = null,
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "SEARCHING FOR HANDS...",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Raise hand in front of camera or test controls instantly with Simulator.",
                        color = Color(0xAAFFFFFF),
                        fontSize = 10.sp
                    )
                }
            }

            Button(
                onClick = onSwitchToSim,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0x3300E5FF)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color(0xFF00E5FF),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "SWITCH TO AIR TOUCH SIMULATOR",
                    color = Color(0xFF00E5FF),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun BackgroundControlToggleCard(
    isServiceRunning: Boolean,
    onToggle: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val hasOverlayPermission = Settings.canDrawOverlays(context)
    val hasAccessibility = GestureAccessibilityService.isAccessibilitySettingsOn(context)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isServiceRunning) Color(0xEE0B2538) else Color(0xCC081220)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            if (isServiceRunning) Color(0xFF00E5FF) else Color(0x3300E5FF)
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isServiceRunning) Color(0xFF00E5FF) else Color(0x2200E5FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isServiceRunning) Icons.Default.PlayArrow else Icons.Default.Layers,
                            contentDescription = null,
                            tint = if (isServiceRunning) Color.Black else Color(0xFF00E5FF),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "BACKGROUND PHONE CONTROL",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = if (isServiceRunning) "Running in background • Close app to use anywhere!" else "Controls your phone with floating cursor when app is closed",
                            color = if (isServiceRunning) Color(0xFF00E5FF) else Color(0xAAFFFFFF),
                            fontSize = 10.sp
                        )
                    }
                }

                Switch(
                    checked = isServiceRunning,
                    onCheckedChange = { onToggle(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = Color(0xFF00E5FF),
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color(0x33FFFFFF)
                    )
                )
            }

            // Permission status pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PermissionBadge(
                    name = "Overlay",
                    isGranted = hasOverlayPermission,
                    onClick = {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                        context.startActivity(intent)
                    },
                    modifier = Modifier.weight(1f)
                )

                PermissionBadge(
                    name = "Accessibility",
                    isGranted = hasAccessibility,
                    onClick = {
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        context.startActivity(intent)
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            if (!hasOverlayPermission || !hasAccessibility) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x33FFB300))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFFFB300),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Tap badges above to enable Overlay & Accessibility for full device clicks & home navigation.",
                        color = Color(0xFFFFE082),
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionBadge(
    name: String,
    isGranted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isGranted) Color(0x2200E676) else Color(0x22FF5252))
            .border(
                1.dp,
                if (isGranted) Color(0x6600E676) else Color(0x66FF5252),
                RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = if (isGranted) Color(0xFF00E676) else Color(0xFFFF5252),
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = name,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = if (isGranted) "READY" else "GRANT",
                color = if (isGranted) Color(0xFF00E676) else Color(0xFFFF5252),
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}
