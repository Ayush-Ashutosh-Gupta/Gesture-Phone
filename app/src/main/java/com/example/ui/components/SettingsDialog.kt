package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.Utils
import com.example.gesture.GestureMapper
import com.example.model.GestureConfig
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    config: GestureConfig,
    onConfigChanged: (GestureConfig) -> Unit,
    gestureMapper: GestureMapper,
    onDismiss: () -> Unit,
    onStartCalibration: () -> Unit = {},
    onOpenTimingDialog: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showQrDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0B132B),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(imageVector = Icons.Default.Tune, contentDescription = null, tint = Color(0xFF00E5FF))
                    Text(
                        text = "GESTUREPHONE SETTINGS",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 1. Sensitivity Slider
            SettingsSliderCard(
                title = "TRACKING SENSITIVITY",
                valueText = "${config.sensitivity.roundToInt()}/10",
                value = config.sensitivity,
                range = 1f..10f,
                steps = 8,
                onValueChange = { onConfigChanged(config.copy(sensitivity = it)) },
                description = "Higher values increase responsiveness and cursor speed."
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Deadzone Slider
            SettingsSliderCard(
                title = "DEADZONE FILTER",
                valueText = "${(config.deadzone * 100).roundToInt()}%",
                value = config.deadzone,
                range = 0.01f..0.10f,
                steps = 8,
                onValueChange = { onConfigChanged(config.copy(deadzone = it)) },
                description = "Suppresses micro-jitter to prevent accidental clicks."
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Confidence Threshold
            SettingsSliderCard(
                title = "MIN CONFIDENCE THRESHOLD",
                valueText = "${(config.minConfidence * 100).roundToInt()}%",
                value = config.minConfidence,
                range = 0.2f..0.9f,
                steps = 6,
                onValueChange = { onConfigChanged(config.copy(minConfidence = it)) },
                description = "Ignores partial/low-quality hand detections."
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Section Header: Gesture Timings
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "GESTURE TIMINGS & SENSITIVITY",
                    color = Color(0xFF00E5FF),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(start = 4.dp)
                )
                Button(
                    onClick = {
                        onDismiss()
                        onOpenTimingDialog()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3555)),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Timer, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Tune in Studio", color = Color(0xFF00E5FF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 1. Click Duration Slider
            SettingsSliderCard(
                title = "CLICK CONFIRMATION TIME",
                valueText = "${config.clickDurationMs} ms",
                value = config.clickDurationMs.toFloat(),
                range = 40f..1000f,
                steps = 19,
                onValueChange = { onConfigChanged(config.copy(clickDurationMs = it.toLong())) },
                description = "How long a pinch must be held to register as a single click."
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Double Click Interval Window Slider
            SettingsSliderCard(
                title = "DOUBLE CLICK WINDOW",
                valueText = "${config.doubleClickWindowMs} ms",
                value = config.doubleClickWindowMs.toFloat(),
                range = 150f..2500f,
                steps = 23,
                onValueChange = { onConfigChanged(config.copy(doubleClickWindowMs = it.toLong())) },
                description = "Maximum time between two pinches to register as double-click. Increase for relaxed clicking."
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Click and Hold Duration Slider
            SettingsSliderCard(
                title = "CLICK & HOLD / DRAG DURATION",
                valueText = "${config.clickHoldDurationMs} ms",
                value = config.clickHoldDurationMs.toFloat(),
                range = 250f..4000f,
                steps = 24,
                onValueChange = { onConfigChanged(config.copy(clickHoldDurationMs = it.toLong())) },
                description = "How long a pinch must be held before starting drag/hold. Increase to prevent accidental drag."
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 4. Pose Hold Duration Slider (Fist, Peace, Rock On)
            SettingsSliderCard(
                title = "GESTURE POSE HOLD TIME",
                valueText = "${config.poseHoldDurationMs} ms",
                value = config.poseHoldDurationMs.toFloat(),
                range = 200f..3000f,
                steps = 27,
                onValueChange = { onConfigChanged(config.copy(poseHoldDurationMs = it.toLong())) },
                description = "Duration to sustain fist or peace pose before activating system action."
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 5. Skeleton Stabilization Smoothing Slider
            SettingsSliderCard(
                title = "SKELETON STABILIZATION SMOOTHING",
                valueText = "${(config.skeletonSmoothing * 100).toInt()}%",
                value = config.skeletonSmoothing,
                range = 0.2f..0.85f,
                steps = 12,
                onValueChange = { onConfigChanged(config.copy(skeletonSmoothing = it)) },
                description = "Smooths landmark jitter so the skeleton aligns accurately with your wrist and fingers."
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Hand Skeleton Calibration Card
            Button(
                onClick = {
                    onDismiss()
                    onStartCalibration()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(imageVector = Icons.Default.Sensors, contentDescription = null, tint = Color.Black)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Scan & Calibrate Hand Alignment", color = Color.Black, fontWeight = FontWeight.ExtraBold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Switches Group
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1C2541))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    SettingToggleRow(
                        title = "Invert Left / Right Hands",
                        subtitle = "Swap detected hand orientation if camera lens flips hardware perspective",
                        checked = config.invertHandedness,
                        onCheckedChange = { onConfigChanged(config.copy(invertHandedness = it)) }
                    )
                    SettingToggleRow(
                        title = "Front Camera Mirroring",
                        subtitle = "Uses front lens with natural mirrored coordinates",
                        checked = config.useFrontCamera,
                        onCheckedChange = { onConfigChanged(config.copy(useFrontCamera = it)) }
                    )
                    SettingToggleRow(
                        title = "Voice Audio Feedback",
                        subtitle = "Announces gesture events via Text-to-Speech",
                        checked = config.voiceFeedbackEnabled,
                        onCheckedChange = { onConfigChanged(config.copy(voiceFeedbackEnabled = it)) }
                    )
                    SettingToggleRow(
                        title = "Haptic Vibration",
                        subtitle = "Tactile feedback when pinch/click triggers",
                        checked = config.hapticFeedbackEnabled,
                        onCheckedChange = { onConfigChanged(config.copy(hapticFeedbackEnabled = it)) }
                    )
                    SettingToggleRow(
                        title = "Show Hand Skeleton Lines",
                        subtitle = "Render 21 bone connections",
                        checked = config.showSkeletonLines,
                        onCheckedChange = { onConfigChanged(config.copy(showSkeletonLines = it)) }
                    )
                    SettingToggleRow(
                        title = "Show Debug Joint Dots",
                        subtitle = "Render glowing tracking nodes",
                        checked = config.showDebugLandmarks,
                        onCheckedChange = { onConfigChanged(config.copy(showDebugLandmarks = it)) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // QR Share / Export
            Button(
                onClick = { showQrDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3A506B)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(imageVector = Icons.Default.QrCode, contentDescription = null, tint = Color(0xFF00E5FF))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Export Gesture Mappings via QR", color = Color.White, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Battery Optimization note
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0x3300E5FF))
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(imageVector = Icons.Default.BatteryChargingFull, contentDescription = null, tint = Color(0xFF00E5FF))
                    Text(
                        text = "100% On-Device ML. MediaPipe runs locally on GPU/NNAPI with ultra-low thermal impact and no cloud battery drain.",
                        color = Color(0xDDFFFFFF),
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }

    if (showQrDialog) {
        val qrJson = remember { gestureMapper.exportToJson() }
        val qrBitmap = remember { Utils.generateSimpleQrBitmap(qrJson, 320) }

        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showQrDialog = false },
            containerColor = Color(0xFF0D1B2A),
            title = {
                Text("SHARE GESTURE MAPPINGS", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(220.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "QR Code Mapping",
                            modifier = Modifier.size(200.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Scan from another device running GesturePhone to copy your custom sensitivity & mapping profile.",
                        color = Color(0xAAFFFFFF),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showQrDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
                ) {
                    Text("Done", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun SettingsSliderCard(
    title: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C2541))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, color = Color(0xAAFFFFFF), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(text = valueText, color = Color(0xFF00E5FF), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
            }
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = range,
                steps = steps,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF00E5FF),
                    activeTrackColor = Color(0xFF00E5FF),
                    inactiveTrackColor = Color(0x33FFFFFF)
                )
            )
            Text(text = description, color = Color(0x88FFFFFF), fontSize = 11.sp)
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(text = subtitle, color = Color(0x88FFFFFF), fontSize = 11.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF00E5FF),
                checkedTrackColor = Color(0x6600E5FF),
                uncheckedThumbColor = Color(0xFF888888),
                uncheckedTrackColor = Color(0x33FFFFFF)
            )
        )
    }
}
