package com.example.mediapipe

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult

class HandLandmarkerHelper(
    context: Context,
    var minHandDetectionConfidence: Float = 0.5f,
    var minHandTrackingConfidence: Float = 0.5f,
    var minHandPresenceConfidence: Float = 0.5f,
    var maxNumHands: Int = 2,
    var currentDelegate: Int = DELEGATE_CPU,
    var handLandmarkerListener: LandmarkerListener? = null
) {
    private val appContext: Context = context.applicationContext
    @Volatile
    private var handLandmarker: HandLandmarker? = null
    @Volatile
    var isReady: Boolean = false
        private set

    init {
        setupHandLandmarker()
    }

    @Synchronized
    fun clearHandLandmarker() {
        isReady = false
        try {
            handLandmarker?.close()
        } catch (e: Throwable) {
            Log.e(TAG, "Error closing HandLandmarker", e)
        }
        handLandmarker = null
    }

    fun isClose(): Boolean {
        return handLandmarker == null
    }

    @Synchronized
    fun setupHandLandmarker() {
        clearHandLandmarker()

        // Set general hand landmarker options
        val baseOptionsBuilder = BaseOptions.builder()

        // Set delegate (GPU or CPU)
        when (currentDelegate) {
            DELEGATE_CPU -> baseOptionsBuilder.setDelegate(Delegate.CPU)
            DELEGATE_GPU -> baseOptionsBuilder.setDelegate(Delegate.GPU)
            else -> baseOptionsBuilder.setDelegate(Delegate.CPU)
        }

        baseOptionsBuilder.setModelAssetPath(MP_HAND_LANDMARKER_TASK)

        try {
            val baseOptions = baseOptionsBuilder.build()
            val optionsBuilder = HandLandmarker.HandLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setMinHandDetectionConfidence(minHandDetectionConfidence)
                .setMinTrackingConfidence(minHandTrackingConfidence)
                .setMinHandPresenceConfidence(minHandPresenceConfidence)
                .setNumHands(maxNumHands)
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setResultListener(this::returnLivestreamResult)
                .setErrorListener(this::returnLivestreamError)

            val options = optionsBuilder.build()
            handLandmarker = HandLandmarker.createFromOptions(appContext, options)
            isReady = true
            Log.d(TAG, "HandLandmarker initialized successfully with delegate: $currentDelegate")
        } catch (e: Throwable) {
            Log.e(TAG, "MediaPipe failed to initialize on delegate ($currentDelegate)", e)
            if (currentDelegate != DELEGATE_CPU) {
                currentDelegate = DELEGATE_CPU
                setupHandLandmarker()
            } else {
                isReady = false
                handLandmarker = null
                handLandmarkerListener?.onError("Hand Landmarker initialization error: ${e.message}")
            }
        }
    }

    private var lastTimestampMs = -1L

    /**
     * Accepts bitmap frame and feeds it into the live stream detector
     */
    fun detectLiveStream(bitmap: Bitmap, isFrontCamera: Boolean) {
        if (!isReady || handLandmarker == null) {
            return
        }
        try {
            var frameTime = SystemClock.uptimeMillis()
            synchronized(this) {
                if (frameTime <= lastTimestampMs) {
                    frameTime = lastTimestampMs + 1
                }
                lastTimestampMs = frameTime
            }
            val mpImage = BitmapImageBuilder(bitmap).build()
            detectAsync(mpImage, frameTime)
        } catch (e: Throwable) {
            Log.e(TAG, "Error building/sending MPImage frame", e)
        }
    }

    fun detectAsync(mpImage: MPImage, frameTime: Long) {
        val landmarker = handLandmarker
        if (!isReady || landmarker == null) {
            return
        }
        try {
            landmarker.detectAsync(mpImage, frameTime)
        } catch (e: Throwable) {
            Log.e(TAG, "Exception during detectAsync: ${e.message}")
        }
    }

    private fun returnLivestreamResult(result: HandLandmarkerResult, input: MPImage) {
        try {
            val finishTimeMs = SystemClock.uptimeMillis()
            val inferenceTime = finishTimeMs - result.timestampMs()
            handLandmarkerListener?.onResults(
                ResultBundle(
                    results = listOf(result),
                    inferenceTime = inferenceTime,
                    inputImageHeight = input.height,
                    inputImageWidth = input.width
                )
            )
        } catch (e: Throwable) {
            Log.e(TAG, "Error handling livestream result in listener", e)
        }
    }

    private fun returnLivestreamError(error: RuntimeException) {
        try {
            Log.e(TAG, "MediaPipe LiveStream error: ${error.message}", error)
            handLandmarkerListener?.onError(error.message ?: "An unknown stream error occurred")
        } catch (e: Throwable) {
            Log.e(TAG, "Error delivering livestream error to listener", e)
        }
    }

    data class ResultBundle(
        val results: List<HandLandmarkerResult>,
        val inferenceTime: Long,
        val inputImageHeight: Int,
        val inputImageWidth: Int
    )

    interface LandmarkerListener {
        fun onError(error: String, errorCode: Int = 0)
        fun onResults(resultBundle: ResultBundle)
    }

    companion object {
        const val TAG = "HandLandmarkerHelper"
        const val MP_HAND_LANDMARKER_TASK = "hand_landmarker.task"
        const val DELEGATE_CPU = 0
        const val DELEGATE_GPU = 1
    }
}
