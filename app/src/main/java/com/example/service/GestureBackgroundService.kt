package com.example.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.example.MainActivity
import com.example.audio.AudioVolumeManager
import com.example.gesture.GestureRecognizer
import com.example.mediapipe.HandLandmarkerHelper
import com.example.model.GestureAction
import com.example.model.GestureConfig
import com.example.model.MediaAction
import com.example.model.SwipeDirection
import com.example.model.SystemActionType
import com.example.model.TrackedHand
import com.example.system.TorchManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class GestureBackgroundService : Service(), LifecycleOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle = lifecycleRegistry

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var cameraExecutor: ExecutorService? = null

    private lateinit var handLandmarkerHelper: HandLandmarkerHelper
    private lateinit var gestureRecognizer: GestureRecognizer
    private lateinit var overlayManager: FloatingOverlayManager
    private lateinit var audioManager: AudioManager
    private lateinit var audioVolumeManager: AudioVolumeManager
    private lateinit var torchManager: TorchManager

    private var isFrontCamera = true
    private var lastGestureActionTime: Long = 0L

    override fun onCreate() {
        super.onCreate()
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        cameraExecutor = Executors.newSingleThreadExecutor()

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioVolumeManager = AudioVolumeManager(this)
        torchManager = TorchManager(this)
        overlayManager = FloatingOverlayManager(this)
        gestureRecognizer = GestureRecognizer(GestureConfig(sensitivity = 7.0f, smoothSmoothingFactor = 0.4f))

        handLandmarkerHelper = HandLandmarkerHelper(
            context = this,
            minHandDetectionConfidence = 0.5f,
            minHandTrackingConfidence = 0.5f,
            minHandPresenceConfidence = 0.5f,
            maxNumHands = 2,
            currentDelegate = HandLandmarkerHelper.DELEGATE_CPU,
            handLandmarkerListener = object : HandLandmarkerHelper.LandmarkerListener {
                override fun onError(error: String, errorCode: Int) {
                    Log.e(TAG, "MediaPipe error in background: $error")
                }

                override fun onResults(resultBundle: HandLandmarkerHelper.ResultBundle) {
                    val firstResult = resultBundle.results.firstOrNull() ?: return
                    val (hands, actions) = gestureRecognizer.processResult(firstResult, isFrontCamera)
                    _latestHandsFlow.value = hands
                    overlayManager.updateHandTelemetry(hands)

                    // Execute gestures
                    for (action in actions) {
                        handleGestureAction(action, hands)
                    }
                }
            }
        )

        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED

        val action = intent?.action
        if (action == ACTION_STOP_SERVICE) {
            stopSelf()
            return START_NOT_STICKY
        } else if (action == ACTION_TOGGLE_CAMERA) {
            isFrontCamera = !isFrontCamera
            bindCameraUseCases()
        }

        val notification = createNotification()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceCompat.startForeground(
                    this,
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to start foreground service: ${e.message}", e)
        }

        // Show floating overlays if overlay permission is granted
        if (Settings.canDrawOverlays(this)) {
            overlayManager.showOverlays(
                onSwitchCamera = {
                    isFrontCamera = !isFrontCamera
                    bindCameraUseCases()
                },
                onStopService = {
                    stopSelf()
                },
                onToggleMute = {
                    audioVolumeManager.toggleMute()
                }
            )
        }

        bindCameraUseCases()
        _isServiceRunning.value = true
        instance = this

        return START_STICKY
    }

    private fun bindCameraUseCases() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                val preferredFacing = if (isFrontCamera) {
                    CameraSelector.LENS_FACING_FRONT
                } else {
                    CameraSelector.LENS_FACING_BACK
                }

                val cameraSelector = if (cameraProvider.hasCamera(CameraSelector.Builder().requireLensFacing(preferredFacing).build())) {
                    CameraSelector.Builder().requireLensFacing(preferredFacing).build()
                } else if (cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else {
                    CameraSelector.DEFAULT_BACK_CAMERA
                }

                val resolutionSelector = ResolutionSelector.Builder()
                    .setAspectRatioStrategy(
                        AspectRatioStrategy(
                            AspectRatio.RATIO_4_3,
                            AspectRatioStrategy.FALLBACK_RULE_AUTO
                        )
                    )
                    .setResolutionStrategy(
                        ResolutionStrategy(
                            android.util.Size(640, 480),
                            ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                        )
                    )
                    .build()

                val imageAnalysis = ImageAnalysis.Builder()
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                    .setResolutionSelector(resolutionSelector)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalysis.setAnalyzer(cameraExecutor ?: Executors.newSingleThreadExecutor()) { imageProxy ->
                    try {
                        processImageProxy(imageProxy)
                    } finally {
                        imageProxy.close()
                    }
                }

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, imageAnalysis)
                Log.d(TAG, "Background camera bound successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error binding background camera use cases", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun processImageProxy(imageProxy: ImageProxy) {
        try {
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
            val bitmap = imageProxy.toBitmap()

            val matrix = Matrix().apply {
                postRotate(rotationDegrees.toFloat())
                if (isFrontCamera) {
                    postScale(-1f, 1f)
                }
            }

            val orientedBitmap = Bitmap.createBitmap(
                bitmap,
                0,
                0,
                bitmap.width,
                bitmap.height,
                matrix,
                true
            )

            handLandmarkerHelper.detectLiveStream(orientedBitmap, isFrontCamera)
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing background image frame", e)
        }
    }

    private fun handleGestureAction(action: GestureAction, hands: List<TrackedHand>) {
        when (action) {
            is GestureAction.MouseMove -> {
                val isPinching = hands.firstOrNull()?.isPinching == true
                overlayManager.updateCursorPosition(action.x, action.y, isPinching)
            }
            is GestureAction.Click -> {
                val (screenX, screenY) = overlayManager.getScreenCursorCoordinates()
                val success = GestureAccessibilityService.instance?.performClick(screenX, screenY)
                Log.d(TAG, "Dispatched pinch click at ($screenX, $screenY), result: $success")
            }
            is GestureAction.Hold -> {
                if (action.isHolding) {
                    val (screenX, screenY) = overlayManager.getScreenCursorCoordinates()
                    GestureAccessibilityService.instance?.performLongClick(screenX, screenY)
                    Log.d(TAG, "Dispatched sustained pinch hold at ($screenX, $screenY)")
                }
            }
            is GestureAction.SystemAction -> {
                when (action.type) {
                    SystemActionType.FLASHLIGHT -> {
                        val isOn = torchManager.toggleTorch()
                        Log.d(TAG, "Toggled flashlight: $isOn")
                    }
                    SystemActionType.HOME -> {
                        GestureAccessibilityService.instance?.triggerGlobalAction(
                            android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME
                        )
                    }
                    SystemActionType.BACK -> {
                        GestureAccessibilityService.instance?.triggerGlobalAction(
                            android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK
                        )
                    }
                    else -> {}
                }
            }
            is GestureAction.Swipe -> {
                val currentTime = SystemClock.uptimeMillis()
                if (currentTime - lastGestureActionTime > 400L) {
                    lastGestureActionTime = currentTime
                    when (action.direction) {
                        SwipeDirection.LEFT, SwipeDirection.RIGHT -> {
                            // Back gesture
                            GestureAccessibilityService.instance?.triggerGlobalAction(
                                android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK
                            )
                        }
                        SwipeDirection.UP -> {
                            // Home gesture
                            GestureAccessibilityService.instance?.triggerGlobalAction(
                                android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME
                            )
                        }
                        SwipeDirection.DOWN -> {
                            // Open notification panel
                            GestureAccessibilityService.instance?.triggerGlobalAction(
                                android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS
                            )
                        }
                    }
                }
            }
            is GestureAction.MediaControl -> {
                when (action.action) {
                    MediaAction.PLAY_PAUSE -> {
                        // Dispatch play/pause media key event
                        val downEvent = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
                        val upEvent = KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
                        audioManager.dispatchMediaKeyEvent(downEvent)
                        audioManager.dispatchMediaKeyEvent(upEvent)
                    }
                    MediaAction.VOLUME_UP -> {
                        audioManager.adjustStreamVolume(
                            AudioManager.STREAM_MUSIC,
                            AudioManager.ADJUST_RAISE,
                            AudioManager.FLAG_SHOW_UI
                        )
                    }
                    MediaAction.VOLUME_DOWN -> {
                        audioManager.adjustStreamVolume(
                            AudioManager.STREAM_MUSIC,
                            AudioManager.ADJUST_LOWER,
                            AudioManager.FLAG_SHOW_UI
                        )
                    }
                    MediaAction.NEXT_TRACK -> {
                        val downEvent = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_NEXT)
                        val upEvent = KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_NEXT)
                        audioManager.dispatchMediaKeyEvent(downEvent)
                        audioManager.dispatchMediaKeyEvent(upEvent)
                    }
                    MediaAction.PREV_TRACK -> {
                        val downEvent = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PREVIOUS)
                        val upEvent = KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PREVIOUS)
                        audioManager.dispatchMediaKeyEvent(downEvent)
                        audioManager.dispatchMediaKeyEvent(upEvent)
                    }
                    MediaAction.MUTE_TOGGLE -> {
                        audioVolumeManager.toggleMute()
                    }
                }
            }
            is GestureAction.VolumeControl -> {
                audioVolumeManager.setVolumePercent(action.levelPercent)
            }
            else -> {}
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Gesture Control Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Runs background gesture tracking and floating cursor phone control"
                setShowBadge(false)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val appIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, GestureBackgroundService::class.java).apply {
            this.action = ACTION_STOP_SERVICE
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("GesturePhone Active")
            .setContentText("Controlling phone with hand gestures & floating pointer")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop Control", stopPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        overlayManager.hideOverlays()
        handLandmarkerHelper.clearHandLandmarker()
        cameraExecutor?.shutdown()
        serviceScope.cancel()
        _isServiceRunning.value = false
        if (instance == this) {
            instance = null
        }
        Log.d(TAG, "GestureBackgroundService destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "GestureBgService"
        const val CHANNEL_ID = "gesture_bg_channel"
        const val NOTIFICATION_ID = 9921

        const val ACTION_STOP_SERVICE = "com.example.action.STOP_BG_SERVICE"
        const val ACTION_TOGGLE_CAMERA = "com.example.action.TOGGLE_CAMERA"

        var instance: GestureBackgroundService? = null
            private set

        private val _isServiceRunning = MutableStateFlow(false)
        val isServiceRunning = _isServiceRunning.asStateFlow()

        private val _latestHandsFlow = MutableStateFlow<List<TrackedHand>>(emptyList())
        val latestHandsFlow = _latestHandsFlow.asStateFlow()

        fun start(context: Context) {
            val intent = Intent(context, GestureBackgroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, GestureBackgroundService::class.java).apply {
                action = ACTION_STOP_SERVICE
            }
            context.startService(intent)
        }
    }
}
