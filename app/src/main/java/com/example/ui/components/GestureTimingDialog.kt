package com.example.ui.components

import android.os.SystemClock
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.GestureConfig
import kotlinx.coroutines.delay
import kotlin.math.roundToLong

/**
 * High-precision Gesture Timing Controller Dialog.
 * Allows fine-tuning of:
 * - Single Click duration (pinch contact threshold)
 * - Double Click window (max time between 2 clicks)
 * - Click & Hold duration (sustained pinch to drag)
 * - Pose Hold time (fist, peace, rock-on)
 *
 * Includes Interactive Live Test Pad for instant tactile calibration.
 */
@Composable
fun GestureTimingDialog(
    config: GestureConfig,
    isPinchingActive: Boolean = false,
    onConfigChanged: (GestureConfig) -> Unit,
    onDismiss: () -> Unit
) {
    var clickDuration by remember(config.clickDurationMs) { mutableLongStateOf(config.clickDurationMs) }
    var doubleClickWindow by remember(config.doubleClickWindowMs) { mutableLongStateOf(config.doubleClickWindowMs) }
    var clickHoldDuration by remember(config.clickHoldDurationMs) { mutableLongStateOf(config.clickHoldDurationMs) }
    var poseHoldDuration by remember(config.poseHoldDurationMs) { mutableLongStateOf(config.poseHoldDurationMs) }

    // Live test simulation state
    var isTestPressing by remember { mutableStateOf(false) }
    var testPressStartTime by remember { mutableLongStateOf(0L) }
    var lastTestClickTime by remember { mutableLongStateOf(0L) }
    var testStatusText by remember { mutableStateOf("Press or pinch here to test timings") }
    var testStatusColor by remember { mutableStateOf(Color(0xFF00E5FF)) }
    var testProgress by remember { mutableFloatStateOf(0f) }

    // Live test loop
    LaunchedEffect(isTestPressing, isPinchingActive, clickDuration, doubleClickWindow, clickHoldDuration) {
        val active = isTestPressing || isPinchingActive
        if (active) {
            testPressStartTime = SystemClock.uptimeMillis()
            var clickFired = false
            var holdFired = false

            while (isTestPressing || isPinchingActive) {
                val elapsed = SystemClock.uptimeMillis() - testPressStartTime
                testProgress = (elapsed.toFloat() / clickHoldDuration.toFloat()).coerceIn(0f, 1f)

                if (!clickFired && elapsed >= clickDuration) {
                    clickFired = true
                    val isDouble = (SystemClock.uptimeMillis() - lastTestClickTime) < doubleClickWindow
                    if (isDouble) {
                        testStatusText = "⚡ DOUBLE CLICK TRIGGERED! (${elapsed}ms)"
                        testStatusColor = Color(0xFF00E676)
                        lastTestClickTime = 0L
                    } else {
                        testStatusText = "👆 CLICK CONFIRMED (${elapsed}ms)"
                        testStatusColor = Color(0xFF00E5FF)
                        lastTestClickTime = SystemClock.uptimeMillis()
                    }
                } else if (!holdFired && elapsed >= clickHoldDuration) {
                    holdFired = true
                    testStatusText = "✊ CLICK & HOLD / DRAG ACTIVE! (${elapsed}ms)"
                    testStatusColor = Color(0xFFFFD700)
                }
                delay(30)
            }
            testProgress = 0f
        } else {
            delay(1200)
            if (!isTestPressing && !isPinchingActive) {
                testStatusText = "Press or pinch to test your timings"
                testStatusColor = Color(0xAAFFFFFF)
                testProgress = 0f
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0A1322),
            border = BorderStroke(1.5.dp, Color(0xFF00E5FF))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Top Title Bar
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
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF162942)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "GESTURE TIMINGS",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "Set pinch click, double-click & hold speeds",
                                color = Color(0x99FFFFFF),
                                fontSize = 12.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0x33FFFFFF))
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Presets Bar
                Text(
                    text = "QUICK TIMING PRESETS",
                    color = Color(0xFF00E5FF),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TimingPresetChip(
                        label = "Snappy",
                        sub = "450ms hold",
                        isSelected = clickHoldDuration <= 500L,
                        modifier = Modifier.weight(1f)
                    ) {
                        clickDuration = 60L
                        doubleClickWindow = 350L
                        clickHoldDuration = 450L
                        poseHoldDuration = 400L
                        onConfigChanged(
                            config.copy(
                                clickDurationMs = 60L,
                                doubleClickWindowMs = 350L,
                                clickHoldDurationMs = 450L,
                                poseHoldDurationMs = 400L
                            )
                        )
                    }

                    TimingPresetChip(
                        label = "Balanced",
                        sub = "700ms hold",
                        isSelected = clickHoldDuration in 600L..800L,
                        modifier = Modifier.weight(1f)
                    ) {
                        clickDuration = 100L
                        doubleClickWindow = 500L
                        clickHoldDuration = 700L
                        poseHoldDuration = 500L
                        onConfigChanged(
                            config.copy(
                                clickDurationMs = 100L,
                                doubleClickWindowMs = 500L,
                                clickHoldDurationMs = 700L,
                                poseHoldDurationMs = 500L
                            )
                        )
                    }

                    TimingPresetChip(
                        label = "Relaxed",
                        sub = "1200ms hold",
                        isSelected = clickHoldDuration in 1000L..1500L,
                        modifier = Modifier.weight(1f)
                    ) {
                        clickDuration = 180L
                        doubleClickWindow = 900L
                        clickHoldDuration = 1200L
                        poseHoldDuration = 750L
                        onConfigChanged(
                            config.copy(
                                clickDurationMs = 180L,
                                doubleClickWindowMs = 900L,
                                clickHoldDurationMs = 1200L,
                                poseHoldDurationMs = 750L
                            )
                        )
                    }

                    TimingPresetChip(
                        label = "Steady",
                        sub = "2000ms hold",
                        isSelected = clickHoldDuration > 1700L,
                        modifier = Modifier.weight(1f)
                    ) {
                        clickDuration = 250L
                        doubleClickWindow = 1400L
                        clickHoldDuration = 2000L
                        poseHoldDuration = 1000L
                        onConfigChanged(
                            config.copy(
                                clickDurationMs = 250L,
                                doubleClickWindowMs = 1400L,
                                clickHoldDurationMs = 2000L,
                                poseHoldDurationMs = 1000L
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Interactive Live Test Pad
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    isTestPressing = true
                                    tryAwaitRelease()
                                    isTestPressing = false
                                }
                            )
                        },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF132238)),
                    border = BorderStroke(
                        1.5.dp,
                        if (isTestPressing || isPinchingActive) testStatusColor else Color(0x3300E5FF)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "INTERACTIVE TIMING TEST PAD",
                                color = Color(0xBBFFFFFF),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isTestPressing || isPinchingActive) "TESTING..." else "TOUCH OR PINCH",
                                color = testStatusColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = testStatusText,
                            color = testStatusColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        LinearProgressIndicator(
                            progress = { testProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = testStatusColor,
                            trackColor = Color(0x22FFFFFF)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 1. Click Duration Control
                TimingSettingCard(
                    title = "CLICK CONFIRMATION TIME",
                    valueText = "$clickDuration ms",
                    currentValue = clickDuration.toFloat(),
                    valueRange = 40f..1000f,
                    stepAmount = 20L,
                    description = "How long a pinch must be held to register as a single click. Increase this if you experience accidental clicks from brief finger touches.",
                    onValueChange = { newVal ->
                        clickDuration = newVal
                        onConfigChanged(config.copy(clickDurationMs = newVal))
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 2. Double-Click Interval Window Control
                TimingSettingCard(
                    title = "DOUBLE CLICK WINDOW",
                    valueText = "$doubleClickWindow ms",
                    currentValue = doubleClickWindow.toFloat(),
                    valueRange = 150f..2500f,
                    stepAmount = 50L,
                    description = "Maximum time allowed between two pinches to count as a double-click. Increase this if you want more relaxed, comfortable timing between clicks.",
                    onValueChange = { newVal ->
                        doubleClickWindow = newVal
                        onConfigChanged(config.copy(doubleClickWindowMs = newVal))
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 3. Click and Hold (Drag) Duration Control
                TimingSettingCard(
                    title = "CLICK & HOLD / DRAG DURATION",
                    valueText = "$clickHoldDuration ms",
                    currentValue = clickHoldDuration.toFloat(),
                    valueRange = 250f..4000f,
                    stepAmount = 100L,
                    description = "How long a pinch must be continuously held before it turns into Drag / Hold mode. Increase this so normal clicks never trigger drag.",
                    onValueChange = { newVal ->
                        clickHoldDuration = newVal
                        onConfigChanged(config.copy(clickHoldDurationMs = newVal))
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 4. Pose Hold Duration (Fist, Peace, Rock On)
                TimingSettingCard(
                    title = "GESTURE POSE HOLD DURATION",
                    valueText = "$poseHoldDuration ms",
                    currentValue = poseHoldDuration.toFloat(),
                    valueRange = 200f..3000f,
                    stepAmount = 50L,
                    description = "Duration to sustain a fist (play/pause) or peace sign (mute) before triggering the system action.",
                    onValueChange = { newVal ->
                        poseHoldDuration = newVal
                        onConfigChanged(config.copy(poseHoldDurationMs = newVal))
                    }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            clickDuration = 100L
                            doubleClickWindow = 500L
                            clickHoldDuration = 700L
                            poseHoldDuration = 500L
                            onConfigChanged(
                                config.copy(
                                    clickDurationMs = 100L,
                                    doubleClickWindowMs = 500L,
                                    clickHoldDurationMs = 700L,
                                    poseHoldDurationMs = 500L
                                )
                            )
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF8A80)),
                        border = BorderStroke(1.dp, Color(0x66FF8A80)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reset Defaults", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save & Apply", color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
private fun TimingPresetChip(
    label: String,
    sub: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) Color(0xFF00E5FF) else Color(0xFF162438),
        label = "chip_bg"
    )
    val textColor = if (isSelected) Color.Black else Color.White
    val subColor = if (isSelected) Color(0xCC000000) else Color(0x88FFFFFF)

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = bgColor,
        border = BorderStroke(1.dp, if (isSelected) Color(0xFF00E5FF) else Color(0x33FFFFFF)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                color = textColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = sub,
                color = subColor,
                fontSize = 10.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun TimingSettingCard(
    title: String,
    valueText: String,
    currentValue: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    stepAmount: Long,
    description: String,
    onValueChange: (Long) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF142034)),
        border = BorderStroke(1.dp, Color(0x2200E5FF))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Title & Value Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = Color(0xDDFFFFFF),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    color = Color(0xFF1F3554),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFF00E5FF))
                ) {
                    Text(
                        text = valueText,
                        color = Color(0xFF00E5FF),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Slider with Minus / Plus step buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Minus button
                IconButton(
                    onClick = {
                        val updated = (currentValue.toLong() - stepAmount).coerceAtLeast(valueRange.start.toLong())
                        onValueChange(updated)
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1C2C45))
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Decrease",
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Smooth slider
                Slider(
                    value = currentValue,
                    onValueChange = { onValueChange(it.roundToLong()) },
                    valueRange = valueRange,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF00E5FF),
                        activeTrackColor = Color(0xFF00E5FF),
                        inactiveTrackColor = Color(0x33FFFFFF)
                    )
                )

                // Plus button
                IconButton(
                    onClick = {
                        val updated = (currentValue.toLong() + stepAmount).coerceAtMost(valueRange.endInclusive.toLong())
                        onValueChange(updated)
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1C2C45))
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Increase",
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = description,
                color = Color(0x88FFFFFF),
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }
    }
}
