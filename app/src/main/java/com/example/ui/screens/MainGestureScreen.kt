package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.SystemClock
import android.view.MotionEvent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CameraFront
import androidx.compose.material.icons.filled.CameraRear
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Pinch
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Swipe
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import com.example.ui.components.GestureTimingDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.audio.AudioVolumeManager
import com.example.audio.VoiceFeedbackManager
import com.example.camera.CameraView
import com.example.gesture.GestureMapper
import com.example.gesture.GestureRecognizer
import com.example.gesture.GestureRecorder
import com.example.mediapipe.HandLandmarkerHelper
import com.example.model.GestureAction
import com.example.model.GestureConfig
import com.example.model.GestureType
import com.example.model.HandType
import com.example.model.LandmarkPoint
import com.example.model.MediaAction
import com.example.model.SystemActionType
import com.example.model.TrackedHand
import com.example.service.GestureBackgroundService
import com.example.system.TorchManager
import com.example.ui.components.CustomGestureRecorderDialog
import com.example.ui.components.EscapeModeView
import com.example.ui.components.GestureGuideDialog
import com.example.ui.components.HandCalibrationScanner
import com.example.ui.components.HandDetectionHud
import com.example.ui.components.OverlayView
import com.example.ui.components.SettingsDialog
import com.example.ui.components.VirtualCursor
import com.example.ui.components.VolumeHud
import kotlinx.coroutines.delay

@Composable
fun MainGestureScreen(
    handLandmarkerHelper: HandLandmarkerHelper,
    gestureRecognizer: GestureRecognizer,
    gestureMapper: GestureMapper,
    gestureRecorder: GestureRecorder,
    audioVolumeManager: AudioVolumeManager,
    voiceFeedbackManager: VoiceFeedbackManager,
    modifier: Modifier = Modifier
) {
    var config by remember { mutableStateOf(GestureConfig()) }
    var fps by remember { mutableIntStateOf(60) }
    var latencyMs by remember { mutableLongStateOf(12L) }

    val context = LocalContext.current
    val isBgServiceRunning by GestureBackgroundService.isServiceRunning.collectAsState()
    val torchManager = remember { TorchManager(context) }

    // Permission state & launcher
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasCameraPermission = permissions[Manifest.permission.CAMERA] == true
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO
                )
            )
        }
    }

    // Simulation / Touch Test Pad state (useful for emulator / test without webcam)
    var isSimulationMode by remember { mutableStateOf(false) }

    // Live tracking states
    var trackedHands by remember { mutableStateOf<List<TrackedHand>>(emptyList()) }
    var streamImageWidth by remember { mutableIntStateOf(480) }
    var streamImageHeight by remember { mutableIntStateOf(640) }
    var cursorX by remember { mutableFloatStateOf(0.5f) }
    var cursorY by remember { mutableFloatStateOf(0.5f) }
    var isPinching by remember { mutableStateOf(false) }
    var lastClickTime by remember { mutableLongStateOf(0L) }
    var activeGestureNotification by remember { mutableStateOf<String?>(null) }
    var lastActiveTime by remember { mutableLongStateOf(0L) }
    var showTelemetryHud by remember { mutableStateOf(true) }

    // Volume HUD state
    val currentVolume by audioVolumeManager.currentVolumePercent.collectAsState()
    val isMuted by audioVolumeManager.isMuted.collectAsState()
    var showVolumeHud by remember { mutableStateOf(false) }
    var lastVolumeAdjustTime by remember { mutableLongStateOf(0L) }

    // Dialog & Mode states
    var showGestureGuide by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showTimingSheet by remember { mutableStateOf(false) }
    var showTopToolsMenu by remember { mutableStateOf(false) }
    var showRecorder by remember { mutableStateOf(false) }
    var showEscapeMode by remember { mutableStateOf(false) }
    var showCalibrationScanner by rememberSaveable { mutableStateOf(false) }
    var hasCompletedFirstBootScan by rememberSaveable { mutableStateOf(false) }
    var selectedLandmarkForMapping by remember { mutableStateOf<Pair<LandmarkPoint, Int>?>(null) }

    // Automatic Hand Calibration on first boot
    LaunchedEffect(Unit) {
        if (!hasCompletedFirstBootScan) {
            delay(400)
            showCalibrationScanner = true
        }
    }

    // Voice recognition states
    val isListeningVoice by voiceFeedbackManager.isListening.collectAsState()
    val lastSpokenVoice by voiceFeedbackManager.lastSpokenText.collectAsState()

    // Keep gesture recognizer config in sync
    LaunchedEffect(config) {
        gestureRecognizer.updateConfig(config)
    }

    // Setup landmarker listener
    LaunchedEffect(handLandmarkerHelper, config.useFrontCamera) {
        handLandmarkerHelper.handLandmarkerListener = object : HandLandmarkerHelper.LandmarkerListener {
            override fun onError(error: String, errorCode: Int) {
                // Handled gracefully
            }

            override fun onResults(resultBundle: HandLandmarkerHelper.ResultBundle) {
                latencyMs = resultBundle.inferenceTime
                if (resultBundle.inputImageWidth > 0 && resultBundle.inputImageHeight > 0) {
                    streamImageWidth = resultBundle.inputImageWidth
                    streamImageHeight = resultBundle.inputImageHeight
                }
                val firstResult = resultBundle.results.firstOrNull() ?: return
                val (hands, actions) = gestureRecognizer.processResult(firstResult, config.useFrontCamera)
                trackedHands = hands
                isPinching = hands.any { it.isPinching }

                // Handle actions
                for (action in actions) {
                    when (action) {
                        is GestureAction.MouseMove -> {
                            cursorX = action.x
                            cursorY = action.y
                        }
                        is GestureAction.Click -> {
                            lastClickTime = SystemClock.uptimeMillis()
                            voiceFeedbackManager.triggerHaptic(config.hapticFeedbackEnabled, 1)
                            activeGestureNotification = if (action.isDouble) "Double Click" else "Pinch Click"
                            lastActiveTime = SystemClock.uptimeMillis()
                        }
                        is GestureAction.Hold -> {
                            isPinching = action.isHolding
                        }
                        is GestureAction.VolumeControl -> {
                            audioVolumeManager.setVolumePercent(action.levelPercent)
                            showVolumeHud = true
                            lastVolumeAdjustTime = SystemClock.uptimeMillis()
                            activeGestureNotification = "Volume: ${(action.levelPercent * 100).toInt()}%"
                            lastActiveTime = SystemClock.uptimeMillis()
                        }
                        is GestureAction.Swipe -> {
                            activeGestureNotification = "Swipe ${action.direction.name}"
                            lastActiveTime = SystemClock.uptimeMillis()
                            voiceFeedbackManager.triggerHaptic(config.hapticFeedbackEnabled, 2)
                            voiceFeedbackManager.speak("Swipe ${action.direction.name.lowercase()}", config.voiceFeedbackEnabled)
                        }
                        is GestureAction.MediaControl -> {
                            when (action.action) {
                                MediaAction.PLAY_PAUSE -> {
                                    activeGestureNotification = "Fist: Play / Pause"
                                    voiceFeedbackManager.speak("Play Pause", config.voiceFeedbackEnabled)
                                }
                                MediaAction.VOLUME_UP -> {
                                    audioVolumeManager.stepVolume(0.1f)
                                    showVolumeHud = true
                                    lastVolumeAdjustTime = SystemClock.uptimeMillis()
                                    activeGestureNotification = "Thumbs Up: Volume +"
                                }
                                MediaAction.VOLUME_DOWN -> {
                                    audioVolumeManager.stepVolume(-0.1f)
                                    showVolumeHud = true
                                    lastVolumeAdjustTime = SystemClock.uptimeMillis()
                                    activeGestureNotification = "Thumbs Down: Volume -"
                                }
                                MediaAction.NEXT_TRACK -> {
                                    activeGestureNotification = "Next Track"
                                }
                                MediaAction.PREV_TRACK -> {
                                    activeGestureNotification = "Previous Track"
                                }
                                MediaAction.MUTE_TOGGLE -> {
                                    audioVolumeManager.toggleMute()
                                    showVolumeHud = true
                                    lastVolumeAdjustTime = SystemClock.uptimeMillis()
                                }
                            }
                            lastActiveTime = SystemClock.uptimeMillis()
                        }
                        is GestureAction.Custom -> {
                            activeGestureNotification = "Pose: ${action.name}"
                            lastActiveTime = SystemClock.uptimeMillis()
                        }
                        is GestureAction.SystemAction -> {
                            when (action.type) {
                                SystemActionType.ESCAPE_MODE -> {
                                    showEscapeMode = true
                                }
                                SystemActionType.FLASHLIGHT -> {
                                    val isOn = torchManager.toggleTorch()
                                    activeGestureNotification = if (isOn) "🤘 Flashlight ON" else "🤘 Flashlight OFF"
                                    lastActiveTime = SystemClock.uptimeMillis()
                                    voiceFeedbackManager.triggerHaptic(config.hapticFeedbackEnabled, 1)
                                }
                                else -> {}
                            }
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    // Auto-hide volume HUD and gesture notification pills after 2s
    LaunchedEffect(lastVolumeAdjustTime) {
        if (lastVolumeAdjustTime > 0) {
            delay(1800)
            showVolumeHud = false
        }
    }
    LaunchedEffect(lastActiveTime) {
        if (lastActiveTime > 0) {
            delay(1500)
            activeGestureNotification = null
        }
    }

    var isCameraPipMode by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070D18))
    ) {
        // 1. CameraX Preview Layer
        if (hasCameraPermission) {
            if (!isCameraPipMode) {
                // Full backdrop mode
                CameraView(
                    modifier = Modifier.fillMaxSize(),
                    isFrontCamera = config.useFrontCamera,
                    handLandmarkerHelper = handLandmarkerHelper,
                    onFpsUpdated = { fps = it }
                )
                // Dark tint overlay so UI controls remain clearly legible
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x33000000))
                )
            } else {
                // Background dark gradient canvas when Camera is in PiP
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF070F1E), Color(0xFF040810), Color(0xFF020408))
                            )
                        )
                )
            }
        } else {
            // Standby visual backdrop when waiting for permission
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            listOf(Color(0xFF0D1B2A), Color(0xFF070D18), Color(0xFF03070E))
                        )
                    )
            )
        }

        // 2. Skeleton Hand OverlayView Layer (full screen)
        if (!isCameraPipMode) {
            OverlayView(
                modifier = Modifier.fillMaxSize(),
                trackedHands = trackedHands,
                config = config,
                streamWidth = streamImageWidth,
                streamHeight = streamImageHeight,
                onLandmarkLongPressed = { lm, idx ->
                    selectedLandmarkForMapping = Pair(lm, idx)
                }
            )
        }

        // 2.5 Floating Picture-in-Picture Mini Camera Window (when PiP active)
        if (hasCameraPermission && isCameraPipMode) {
            Card(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 80.dp, end = 16.dp)
                    .size(width = 130.dp, height = 175.dp),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF00E5FF)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    CameraView(
                        modifier = Modifier.fillMaxSize(),
                        isFrontCamera = config.useFrontCamera,
                        handLandmarkerHelper = handLandmarkerHelper,
                        onFpsUpdated = { fps = it }
                    )
                    OverlayView(
                        modifier = Modifier.fillMaxSize(),
                        trackedHands = trackedHands,
                        config = config,
                        streamWidth = streamImageWidth,
                        streamHeight = streamImageHeight
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(4.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xAA000000))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text("LIVE CAM", color = Color(0xFF00E5FF), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 3. Virtual Mouse Cursor Layer
        if (config.cursorModeEnabled && (trackedHands.isNotEmpty() || isSimulationMode)) {
            VirtualCursor(
                cursorX = cursorX,
                cursorY = cursorY,
                isPinching = isPinching,
                lastClickTime = lastClickTime
            )
        }

        // 4. Top Telemetry Cockpit & Uncluttered Navigation Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            // Row 1: Telemetry Pill on Left, 4 Spacious Touch Targets (>=48dp) on Right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Clean Live Telemetry Pill
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xDD0D1B2A),
                    border = BorderStroke(1.dp, Color(0x4400E5FF)),
                    modifier = Modifier.height(44.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(if (hasCameraPermission) Color(0xFF00E676) else Color(0xFFFF5252))
                        )
                        Text(
                            text = "$fps FPS",
                            color = Color(0xFF00E676),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "•",
                            color = Color(0x66FFFFFF),
                            fontSize = 12.sp
                        )
                        Text(
                            text = "${latencyMs}ms",
                            color = Color(0xFF00E5FF),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Right: High-Contrast, Spacious Action Buttons (48dp x 48dp touch targets)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Gesture Timing Studio Button (Highlighted Cyan Border)
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xDD0D1B2A),
                        border = BorderStroke(1.5.dp, Color(0xFF00E5FF)),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .size(44.dp)
                    ) {
                        IconButton(
                            onClick = { showTimingSheet = true },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = "Gesture Timing Settings",
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    // 2. Camera Switch Button (Front / Rear)
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xDD0D1B2A),
                        border = BorderStroke(1.dp, Color(0x44FFFFFF)),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .size(44.dp)
                    ) {
                        IconButton(
                            onClick = {
                                config = config.copy(useFrontCamera = !config.useFrontCamera)
                            },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = if (config.useFrontCamera) Icons.Default.CameraFront else Icons.Default.CameraRear,
                                contentDescription = if (config.useFrontCamera) "Front Camera Active" else "Back Camera Active",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    // 3. Settings Button
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xDD0D1B2A),
                        border = BorderStroke(1.dp, Color(0x44FFFFFF)),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .size(44.dp)
                    ) {
                        IconButton(
                            onClick = { showSettings = true },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    // 4. More Quick Tools Overflow Menu
                    Box {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xDD0D1B2A),
                            border = BorderStroke(1.dp, Color(0x44FFFFFF)),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .size(44.dp)
                        ) {
                            IconButton(
                                onClick = { showTopToolsMenu = true },
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "More Tools",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showTopToolsMenu,
                            onDismissRequest = { showTopToolsMenu = false },
                            modifier = Modifier
                                .background(Color(0xFF0F1A2C))
                                .border(1.dp, Color(0x3300E5FF), RoundedCornerShape(12.dp))
                        ) {
                            DropdownMenuItem(
                                text = { Text("Calibrate Hand Alignment", color = Color.White, fontWeight = FontWeight.SemiBold) },
                                leadingIcon = {
                                    Icon(Icons.Default.Sensors, contentDescription = null, tint = Color(0xFF00E5FF))
                                },
                                onClick = {
                                    showTopToolsMenu = false
                                    showCalibrationScanner = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Gesture Dictionary Guide", color = Color.White, fontWeight = FontWeight.SemiBold) },
                                leadingIcon = {
                                    Icon(Icons.Default.AutoStories, contentDescription = null, tint = Color(0xFF00E5FF))
                                },
                                onClick = {
                                    showTopToolsMenu = false
                                    showGestureGuide = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (isCameraPipMode) "Full Camera Backdrop" else "Mini-Cam PiP View", color = Color.White, fontWeight = FontWeight.SemiBold) },
                                leadingIcon = {
                                    Icon(Icons.Default.Adjust, contentDescription = null, tint = Color(0xFF00E5FF))
                                },
                                onClick = {
                                    showTopToolsMenu = false
                                    isCameraPipMode = !isCameraPipMode
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (showTelemetryHud) "Hide Hand Telemetry HUD" else "Show Hand Telemetry HUD", color = Color.White, fontWeight = FontWeight.SemiBold) },
                                leadingIcon = {
                                    Icon(Icons.Default.PanTool, contentDescription = null, tint = Color(0xFF00E5FF))
                                },
                                onClick = {
                                    showTopToolsMenu = false
                                    showTelemetryHud = !showTelemetryHud
                                }
                            )
                            HorizontalDivider(color = Color(0x22FFFFFF))
                            DropdownMenuItem(
                                text = { Text("Reset Timings to Default", color = Color(0xFFFF8A80), fontWeight = FontWeight.SemiBold) },
                                leadingIcon = {
                                    Icon(Icons.Default.RestartAlt, contentDescription = null, tint = Color(0xFFFF8A80))
                                },
                                onClick = {
                                    showTopToolsMenu = false
                                    val reset = config.copy(
                                        clickDurationMs = 100L,
                                        doubleClickWindowMs = 500L,
                                        clickHoldDurationMs = 700L,
                                        poseHoldDurationMs = 500L
                                    )
                                    config = reset
                                    gestureRecognizer.updateConfig(reset)
                                    voiceFeedbackManager.speak("Timings reset to defaults", config.voiceFeedbackEnabled)
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Row 2: Sub-bar with Live Hand Detection Status & Timing Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xCC07111E),
                    border = BorderStroke(1.dp, if (trackedHands.isNotEmpty()) Color(0x5500E5FF) else Color(0x33FF5252)),
                    modifier = Modifier.height(36.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val handDotColor = if (trackedHands.isNotEmpty()) Color(0xFF00E5FF) else Color(0xFFFF5252)
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(handDotColor)
                        )
                        val handLabel = when {
                            trackedHands.isEmpty() -> "Looking for hands... (Show hand to camera)"
                            trackedHands.size == 1 -> "${trackedHands[0].handType} Hand Active (Pointer + Click)"
                            else -> "2 Hands: Right (Cursor) + Left (Volume)"
                        }
                        Text(
                            text = handLabel,
                            color = if (trackedHands.isNotEmpty()) Color(0xFFE0F7FA) else Color(0xFFFF8A80),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xAA132238),
                    border = BorderStroke(1.dp, Color(0x3300E5FF)),
                    modifier = Modifier
                        .height(36.dp)
                        .clickable { showTimingSheet = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Hold: ${config.clickHoldDurationMs}ms",
                            color = Color(0xCCFFFFFF),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Live Hand Detection & Background Control HUD
            AnimatedVisibility(
                visible = showTelemetryHud,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                HandDetectionHud(
                    trackedHands = trackedHands,
                    isBackgroundControlRunning = isBgServiceRunning,
                    onOpenGuide = { showGestureGuide = true },
                    onSwitchToSim = {
                        isSimulationMode = true
                        voiceFeedbackManager.speak("Air Touch Simulator activated", config.voiceFeedbackEnabled)
                    },
                    onToggleBackgroundControl = { enable ->
                        if (enable) {
                            GestureBackgroundService.start(context)
                            voiceFeedbackManager.speak("Background gesture control activated. Close or minimize app to control your phone.", config.voiceFeedbackEnabled)
                        } else {
                            GestureBackgroundService.stop(context)
                            voiceFeedbackManager.speak("Background gesture control deactivated.", config.voiceFeedbackEnabled)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Floating Active Gesture Pill Notification
            AnimatedVisibility(
                visible = activeGestureNotification != null,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically(),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xE600E5FF))
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = activeGestureNotification ?: "",
                        color = Color.Black,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        // 5. Volume HUD Floating Center
        VolumeHud(
            volumePercent = currentVolume,
            isMuted = isMuted,
            isVisible = showVolumeHud,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 80.dp)
        )

        // 6. Voice Recognition Indicator (when mic active)
        if (isListeningVoice) {
            Card(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xF0FF3366))
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Mic, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                    Column {
                        Text("LISTENING FOR COMMAND...", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Say 'Volume up', 'Click', 'Scroll down', 'Mute'", color = Color(0xDDFFFFFF), fontSize = 12.sp)
                    }
                }
            }
        }

        // 7. Floating Big MICROPHONE Button
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 120.dp, end = 20.dp)
        ) {
            Button(
                onClick = {},
                modifier = Modifier
                    .size(64.dp)
                    .shadow(12.dp, CircleShape)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                voiceFeedbackManager.startVoiceRecognition()
                                tryAwaitRelease()
                                voiceFeedbackManager.stopVoiceRecognition()
                            }
                        )
                    },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isListeningVoice) Color(0xFFFF3366) else Color(0xFF00E5FF)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Hold to Speak Voice Commands",
                    tint = Color.Black,
                    modifier = Modifier.size(30.dp)
                )
            }
        }

        // 7.5 Interactive Touch Simulation Pad (only active when user explicitly enables Touch Sim)
        if (isSimulationMode) {
            Card(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(width = 280.dp, height = 180.dp)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                cursorX = (offset.x / size.width.toFloat()).coerceIn(0.05f, 0.95f)
                                cursorY = (offset.y / size.height.toFloat()).coerceIn(0.05f, 0.95f)
                                trackedHands = listOf(
                                    createSimulatedHand(cursorX, cursorY, isLeft = false, isPinching = isPinching)
                                )
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                cursorX = (cursorX + dragAmount.x / size.width.toFloat()).coerceIn(0.05f, 0.95f)
                                cursorY = (cursorY + dragAmount.y / size.height.toFloat()).coerceIn(0.05f, 0.95f)
                                trackedHands = listOf(
                                    createSimulatedHand(cursorX, cursorY, isLeft = false, isPinching = isPinching)
                                )
                            }
                        )
                    },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xEE0D1B2A)),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF00E5FF)),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("TOUCH SIMULATION PAD", color = Color(0xFF00E5FF), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("Drag in this box to move virtual cursor", color = Color(0xCCFFFFFF), fontSize = 11.sp, textAlign = TextAlign.Center)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                isPinching = !isPinching
                                lastClickTime = SystemClock.uptimeMillis()
                                activeGestureNotification = if (isPinching) "Sim Pinch Hold" else "Sim Pinch Released"
                                lastActiveTime = SystemClock.uptimeMillis()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isPinching) Color(0xFFFF3366) else Color(0xFF00E5FF)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                if (isPinching) "Release Pinch" else "Simulate Pinch",
                                color = Color.Black,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 8. Bottom Control Cockpit Bar
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 12.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xCC07101E)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x4400E5FF))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 10.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ToggleChip(
                        label = "BG Control",
                        icon = Icons.Default.Adjust,
                        isActive = isBgServiceRunning,
                        onClick = {
                            if (!isBgServiceRunning) {
                                GestureBackgroundService.start(context)
                                voiceFeedbackManager.speak("Background gesture control activated. Close or minimize app to control your phone.", config.voiceFeedbackEnabled)
                            } else {
                                GestureBackgroundService.stop(context)
                                voiceFeedbackManager.speak("Background gesture control deactivated.", config.voiceFeedbackEnabled)
                            }
                        }
                    )

                    ToggleChip(
                        label = "Hands HUD",
                        icon = Icons.Default.PanTool,
                        isActive = showTelemetryHud,
                        onClick = {
                            showTelemetryHud = !showTelemetryHud
                        }
                    )

                    ToggleChip(
                        label = "Cursor",
                        icon = Icons.Default.Mouse,
                        isActive = config.cursorModeEnabled,
                        onClick = {
                            config = config.copy(cursorModeEnabled = !config.cursorModeEnabled)
                            gestureRecognizer.updateConfig(config)
                        }
                    )

                    ToggleChip(
                        label = "Pinch Click",
                        icon = Icons.Default.TouchApp,
                        isActive = config.pinchClickEnabled,
                        onClick = {
                            config = config.copy(pinchClickEnabled = !config.pinchClickEnabled)
                            gestureRecognizer.updateConfig(config)
                        }
                    )

                    ToggleChip(
                        label = if (isSimulationMode) "Sim Mode ON" else "Touch Sim",
                        icon = Icons.Default.PlayArrow,
                        isActive = isSimulationMode,
                        onClick = {
                            isSimulationMode = !isSimulationMode
                            if (isSimulationMode && trackedHands.isEmpty()) {
                                trackedHands = listOf(
                                    createSimulatedHand(cursorX, cursorY, isLeft = false)
                                )
                                voiceFeedbackManager.speak("Touch simulation mode enabled. Drag anywhere to move pointer.", config.voiceFeedbackEnabled)
                            }
                        }
                    )

                    ToggleChip(
                        label = "Timings",
                        icon = Icons.Default.Timer,
                        isActive = showTimingSheet,
                        onClick = { showTimingSheet = true }
                    )

                    ToggleChip(
                        label = "Symbols Guide",
                        icon = Icons.Default.AutoStories,
                        isActive = showGestureGuide,
                        onClick = { showGestureGuide = true }
                    )

                    ToggleChip(
                        label = "Swipe Scroll",
                        icon = Icons.Default.Swipe,
                        isActive = config.swipeScrollEnabled,
                        onClick = {
                            config = config.copy(swipeScrollEnabled = !config.swipeScrollEnabled)
                            gestureRecognizer.updateConfig(config)
                        }
                    )

                    ToggleChip(
                        label = "2-Hand Mode",
                        icon = Icons.Default.PanTool,
                        isActive = config.twoHandModeEnabled,
                        onClick = {
                            config = config.copy(twoHandModeEnabled = !config.twoHandModeEnabled)
                            gestureRecognizer.updateConfig(config)
                        }
                    )

                    ToggleChip(
                        label = "Record Pose",
                        icon = Icons.Default.Psychology,
                        isActive = false,
                        onClick = { showRecorder = true }
                    )

                    ToggleChip(
                        label = "Calibrate Hand",
                        icon = Icons.Default.Sensors,
                        isActive = showCalibrationScanner,
                        onClick = { showCalibrationScanner = true }
                    )

                    ToggleChip(
                        label = "Escape Puzzle",
                        icon = Icons.Default.VpnKey,
                        isActive = false,
                        onClick = { showEscapeMode = true }
                    )
                }
            }
        }

        // 8.5 Gesture Guide Dialog
        if (showGestureGuide) {
            GestureGuideDialog(
                activeHands = trackedHands,
                onDismiss = { showGestureGuide = false }
            )
        }

        // 8.8 Hand Calibration Scanner
        if (showCalibrationScanner) {
            HandCalibrationScanner(
                trackedHands = trackedHands,
                config = config,
                onCalibrationComplete = { updatedConfig ->
                    config = updatedConfig
                    gestureRecognizer.updateConfig(updatedConfig)
                    hasCompletedFirstBootScan = true
                    showCalibrationScanner = false
                    voiceFeedbackManager.speak("Hand alignment and gesture timing calibrated successfully.", config.voiceFeedbackEnabled)
                },
                onDismiss = {
                    hasCompletedFirstBootScan = true
                    showCalibrationScanner = false
                }
            )
        }

        // 9. Escape Mode Overlay
        if (showEscapeMode) {
            EscapeModeView(
                trackedHands = trackedHands,
                onClose = { showEscapeMode = false },
                onSuccess = {
                    voiceFeedbackManager.speak("Puzzle solved! Gesture controls master.", config.voiceFeedbackEnabled)
                }
            )
        }

        // 9.5 Gesture Timing Tuning Studio Dialog
        if (showTimingSheet) {
            GestureTimingDialog(
                config = config,
                isPinchingActive = isPinching,
                onConfigChanged = { updatedConfig ->
                    config = updatedConfig
                    gestureRecognizer.updateConfig(updatedConfig)
                },
                onDismiss = { showTimingSheet = false }
            )
        }

        // 10. Settings Bottom Sheet
        if (showSettings) {
            SettingsDialog(
                config = config,
                onConfigChanged = {
                    config = it
                    gestureRecognizer.updateConfig(it)
                },
                gestureMapper = gestureMapper,
                onDismiss = { showSettings = false },
                onStartCalibration = { showCalibrationScanner = true },
                onOpenTimingDialog = { showTimingSheet = true }
            )
        }

        // 11. Custom Gesture Recorder Bottom Sheet
        if (showRecorder) {
            CustomGestureRecorderDialog(
                trackedHands = trackedHands,
                recorder = gestureRecorder,
                onDismiss = { showRecorder = false }
            )
        }

        // 12. Landmark Mapping Dialog (on long-press joint)
        selectedLandmarkForMapping?.let { (landmark, idx) ->
            AlertDialog(
                onDismissRequest = { selectedLandmarkForMapping = null },
                containerColor = Color(0xFF0D1B2A),
                title = {
                    Text("MAP JOINT #$idx", color = Color.White, fontWeight = FontWeight.Bold)
                },
                text = {
                    Text(
                        "Assign a quick trigger action when Joint #$idx (normalized x: ${"%.2f".format(landmark.x)}, y: ${"%.2f".format(landmark.y)}) is activated.",
                        color = Color(0xCCFFFFFF),
                        fontSize = 13.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            gestureMapper.setMapping(GestureType.OK_SIGN, GestureAction.MediaControl(MediaAction.PLAY_PAUSE))
                            selectedLandmarkForMapping = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
                    ) {
                        Text("Map to Play/Pause", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    Button(
                        onClick = { selectedLandmarkForMapping = null },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x44FFFFFF))
                    ) {
                        Text("Cancel", color = Color.White)
                    }
                }
            )
        }

        // 13. Camera Permission Card (Prompt when camera permission is pending/denied)
        if (!hasCameraPermission && !isSimulationMode) {
            Card(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(0.92f)
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xF20D1B2A)),
                border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF00E5FF))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(Color(0x2200E5FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    Text(
                        text = "Camera Permission Required",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "GesturePhone tracks your hands offline in real time. Grant camera permission to track physical gestures or test using the touch simulator.",
                        color = Color(0xCCFFFFFF),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )

                    Button(
                        onClick = {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.CAMERA,
                                    Manifest.permission.RECORD_AUDIO
                                )
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "GRANT CAMERA ACCESS",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Button(
                        onClick = {
                            isSimulationMode = true
                            trackedHands = listOf(
                                createSimulatedHand(cursorX, cursorY, isLeft = false)
                            )
                            voiceFeedbackManager.speak("Touch simulation enabled", config.voiceFeedbackEnabled)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x3300E5FF)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = Color(0xFF00E5FF))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "TRY AIR TOUCH SIMULATOR",
                            color = Color(0xFF00E5FF),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // 14. Floating Quick Simulation Action Bar (When in simulator mode)
        if (isSimulationMode) {
            Card(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 130.dp, start = 16.dp, end = 16.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xD907101E)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Air Simulator:", color = Color(0xFF00E5FF), fontSize = 12.sp, fontWeight = FontWeight.Bold)

                    // Simulate Pinch Click
                    Button(
                        onClick = {
                            isPinching = true
                            lastClickTime = SystemClock.uptimeMillis()
                            activeGestureNotification = "🤏 Sim Pinch Click"
                            lastActiveTime = SystemClock.uptimeMillis()
                            voiceFeedbackManager.triggerHaptic(config.hapticFeedbackEnabled, 1)
                            trackedHands = listOf(createSimulatedHand(cursorX, cursorY, isLeft = false, isPinching = true))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x3300E5FF)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("🤏 Pinch Click", color = Color.White, fontSize = 11.sp)
                    }

                    // Simulate Flashlight
                    Button(
                        onClick = {
                            val isOn = torchManager.toggleTorch()
                            activeGestureNotification = if (isOn) "🤘 Flashlight ON" else "🤘 Flashlight OFF"
                            lastActiveTime = SystemClock.uptimeMillis()
                            voiceFeedbackManager.triggerHaptic(config.hapticFeedbackEnabled, 1)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x3300E5FF)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("🤘 Flashlight", color = Color.White, fontSize = 11.sp)
                    }

                    // Simulate Vol +
                    Button(
                        onClick = {
                            audioVolumeManager.stepVolume(0.08f)
                            showVolumeHud = true
                            lastVolumeAdjustTime = SystemClock.uptimeMillis()
                            activeGestureNotification = "👍 Volume Up"
                            lastActiveTime = SystemClock.uptimeMillis()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x3300E5FF)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("👍 Vol +", color = Color.White, fontSize = 11.sp)
                    }

                    // Simulate Vol -
                    Button(
                        onClick = {
                            audioVolumeManager.stepVolume(-0.08f)
                            showVolumeHud = true
                            lastVolumeAdjustTime = SystemClock.uptimeMillis()
                            activeGestureNotification = "👎 Volume Down"
                            lastActiveTime = SystemClock.uptimeMillis()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x3300E5FF)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("👎 Vol -", color = Color.White, fontSize = 11.sp)
                    }

                    // Simulate Play/Pause
                    Button(
                        onClick = {
                            activeGestureNotification = "✊ Fist: Play / Pause"
                            lastActiveTime = SystemClock.uptimeMillis()
                            voiceFeedbackManager.speak("Toggled Play Pause", config.voiceFeedbackEnabled)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x3300E5FF)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("✊ Play/Pause", color = Color.White, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

/**
 * Creates realistic 21-joint 3D hand skeleton for simulation and testing
 */
private fun createSimulatedHand(
    x: Float,
    y: Float,
    isLeft: Boolean = false,
    gesture: GestureType = GestureType.CURSOR_POINT,
    isPinching: Boolean = false
): TrackedHand {
    val wrist = LandmarkPoint(x, (y + 0.14f).coerceIn(0f, 1f))
    val thumbTip = if (isPinching) LandmarkPoint((x - 0.015f).coerceIn(0f, 1f), (y - 0.035f).coerceIn(0f, 1f)) else LandmarkPoint((x - 0.05f).coerceIn(0f, 1f), y.coerceIn(0f, 1f))
    val indexTip = if (isPinching) LandmarkPoint((x - 0.015f).coerceIn(0f, 1f), (y - 0.035f).coerceIn(0f, 1f)) else LandmarkPoint(x.coerceIn(0f, 1f), (y - 0.06f).coerceIn(0f, 1f))
    val middleTip = LandmarkPoint((x + 0.02f).coerceIn(0f, 1f), (y - 0.055f).coerceIn(0f, 1f))
    val ringTip = LandmarkPoint((x + 0.038f).coerceIn(0f, 1f), (y - 0.045f).coerceIn(0f, 1f))
    val pinkyTip = LandmarkPoint((x + 0.052f).coerceIn(0f, 1f), (y - 0.03f).coerceIn(0f, 1f))

    val allPoints = mutableListOf<LandmarkPoint>()
    allPoints.add(wrist) // 0
    allPoints.add(LandmarkPoint((x - 0.03f).coerceIn(0f, 1f), (y + 0.07f).coerceIn(0f, 1f))) // 1
    allPoints.add(LandmarkPoint((x - 0.045f).coerceIn(0f, 1f), (y + 0.035f).coerceIn(0f, 1f))) // 2
    allPoints.add(LandmarkPoint((x - 0.055f).coerceIn(0f, 1f), (y + 0.01f).coerceIn(0f, 1f))) // 3
    allPoints.add(thumbTip) // 4
    allPoints.add(LandmarkPoint((x - 0.015f).coerceIn(0f, 1f), (y + 0.02f).coerceIn(0f, 1f))) // 5
    allPoints.add(LandmarkPoint((x - 0.008f).coerceIn(0f, 1f), (y - 0.015f).coerceIn(0f, 1f))) // 6
    allPoints.add(LandmarkPoint((x - 0.004f).coerceIn(0f, 1f), (y - 0.038f).coerceIn(0f, 1f))) // 7
    allPoints.add(indexTip) // 8
    allPoints.add(LandmarkPoint((x + 0.008f).coerceIn(0f, 1f), (y + 0.02f).coerceIn(0f, 1f))) // 9
    allPoints.add(LandmarkPoint((x + 0.012f).coerceIn(0f, 1f), (y - 0.015f).coerceIn(0f, 1f))) // 10
    allPoints.add(LandmarkPoint((x + 0.016f).coerceIn(0f, 1f), (y - 0.038f).coerceIn(0f, 1f))) // 11
    allPoints.add(middleTip) // 12
    allPoints.add(LandmarkPoint((x + 0.025f).coerceIn(0f, 1f), (y + 0.025f).coerceIn(0f, 1f))) // 13
    allPoints.add(LandmarkPoint((x + 0.028f).coerceIn(0f, 1f), (y - 0.008f).coerceIn(0f, 1f))) // 14
    allPoints.add(LandmarkPoint((x + 0.032f).coerceIn(0f, 1f), (y - 0.025f).coerceIn(0f, 1f))) // 15
    allPoints.add(ringTip) // 16
    allPoints.add(LandmarkPoint((x + 0.04f).coerceIn(0f, 1f), (y + 0.035f).coerceIn(0f, 1f))) // 17
    allPoints.add(LandmarkPoint((x + 0.044f).coerceIn(0f, 1f), (y + 0.008f).coerceIn(0f, 1f))) // 18
    allPoints.add(LandmarkPoint((x + 0.048f).coerceIn(0f, 1f), (y - 0.01f).coerceIn(0f, 1f))) // 19
    allPoints.add(pinkyTip) // 20

    return TrackedHand(
        handType = if (isLeft) HandType.LEFT else HandType.RIGHT,
        confidence = 0.99f,
        landmarks = allPoints,
        activeGesture = if (isPinching) GestureType.PINCH_CLICK else gesture,
        pinchDistance = if (isPinching) 0.01f else 0.14f,
        isPinching = isPinching,
        isFist = (gesture == GestureType.FIST_HOLD),
        indexTipNormalized = indexTip,
        thumbTipNormalized = thumbTip,
        wristNormalized = wrist,
        extendedFingers = listOf(true, true, true, true, true)
    )
}

@Composable
private fun StatusBadge(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x990D1B2A))
            .border(1.dp, color.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ToggleChip(
    label: String,
    icon: ImageVector,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (isActive) Color(0xFF00E5FF) else Color(0x33FFFFFF))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isActive) Color.Black else Color.White,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                color = if (isActive) Color.Black else Color.White,
                fontSize = 12.sp,
                fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Medium
            )
        }
    }
}
