package com.example.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.mediapipe.HandLandmarkerHelper
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Composable
fun CameraView(
    modifier: Modifier = Modifier,
    isFrontCamera: Boolean = true,
    hasCameraPermission: Boolean = true,
    handLandmarkerHelper: HandLandmarkerHelper?,
    onFpsUpdated: (Int) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }

    // Rolling FPS tracker
    val fpsTracker = remember {
        object {
            var frameCount = 0
            var lastFpsTime = System.currentTimeMillis()

            fun recordFrame(onFps: (Int) -> Unit) {
                frameCount++
                val now = System.currentTimeMillis()
                if (now - lastFpsTime >= 1000L) {
                    val fps = frameCount
                    frameCount = 0
                    lastFpsTime = now
                    onFps(fps)
                }
            }
        }
    }

    val previewView = remember {
        PreviewView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    DisposableEffect(lifecycleOwner) {
        onDispose {
            try {
                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                if (cameraProviderFuture.isDone) {
                    cameraProviderFuture.get().unbindAll()
                }
            } catch (e: Throwable) {
                Log.e("CameraView", "Error unbinding camera on dispose", e)
            }
            try {
                cameraExecutor.shutdown()
            } catch (e: Throwable) {
                Log.e("CameraView", "Error shutting down camera executor", e)
            }
        }
    }

    LaunchedEffect(isFrontCamera, handLandmarkerHelper, hasCameraPermission) {
        val permissionGranted = hasCameraPermission && ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (!permissionGranted) {
            Log.w("CameraView", "Camera permission not yet granted; deferring camera binding")
            return@LaunchedEffect
        }

        try {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()

                    val preferredFacing = if (isFrontCamera) {
                        CameraSelector.LENS_FACING_FRONT
                    } else {
                        CameraSelector.LENS_FACING_BACK
                    }

                    val cameraSelector = try {
                        val preferred = CameraSelector.Builder().requireLensFacing(preferredFacing).build()
                        if (cameraProvider.hasCamera(preferred)) {
                            preferred
                        } else if (cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                            CameraSelector.DEFAULT_FRONT_CAMERA
                        } else if (cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
                            CameraSelector.DEFAULT_BACK_CAMERA
                        } else {
                            CameraSelector.DEFAULT_BACK_CAMERA
                        }
                    } catch (e: Throwable) {
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

                    val preview = Preview.Builder()
                        .setResolutionSelector(resolutionSelector)
                        .build()
                        .also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                        .setResolutionSelector(resolutionSelector)
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    var isProcessingFrame = false
                    var lastFrameTimestamp = 0L

                    imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy: ImageProxy ->
                        val currentTime = android.os.SystemClock.uptimeMillis()
                        // Throttle to max 25 FPS to prevent camera buffer saturation & CPU lockup
                        if (isProcessingFrame || (currentTime - lastFrameTimestamp < 40L)) {
                            imageProxy.close()
                            return@setAnalyzer
                        }
                        isProcessingFrame = true
                        lastFrameTimestamp = currentTime
                        try {
                            processImageProxy(imageProxy, isFrontCamera, handLandmarkerHelper)
                            fpsTracker.recordFrame(onFpsUpdated)
                        } catch (e: Throwable) {
                            Log.e("CameraView", "Error in image analyzer", e)
                        } finally {
                            isProcessingFrame = false
                        }
                    }

                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                    Log.d("CameraView", "Camera bound successfully")
                } catch (e: Throwable) {
                    Log.e("CameraView", "Camera binding failed", e)
                }
            }, ContextCompat.getMainExecutor(context))
        } catch (e: Throwable) {
            Log.e("CameraView", "Error initializing ProcessCameraProvider", e)
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier
    )
}

/**
 * Converts ImageProxy to properly oriented Bitmap and passes to HandLandmarkerHelper
 */
private fun processImageProxy(
    imageProxy: ImageProxy,
    isFrontCamera: Boolean,
    handLandmarkerHelper: HandLandmarkerHelper?
) {
    try {
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        val bitmap = imageProxy.toBitmap()

        val matrix = Matrix().apply {
            postRotate(rotationDegrees.toFloat())
            if (isFrontCamera) {
                // Flip horizontally so the preview acts as a natural mirror
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

        handLandmarkerHelper?.detectLiveStream(orientedBitmap, isFrontCamera)
    } catch (e: Throwable) {
        Log.e("CameraView", "Error analyzing image frame", e)
    } finally {
        imageProxy.close()
    }
}
