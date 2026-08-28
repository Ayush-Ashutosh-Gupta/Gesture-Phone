package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.audio.AudioVolumeManager
import com.example.audio.VoiceFeedbackManager
import com.example.gesture.GestureMapper
import com.example.gesture.GestureRecognizer
import com.example.gesture.GestureRecorder
import com.example.mediapipe.HandLandmarkerHelper
import com.example.ui.screens.MainGestureScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.MyApplicationTheme

enum class AppScreen {
    SPLASH,
    MAIN_GESTURE
}

class MainActivity : ComponentActivity() {

    private lateinit var handLandmarkerHelper: HandLandmarkerHelper
    private lateinit var gestureRecognizer: GestureRecognizer
    private lateinit var gestureMapper: GestureMapper
    private lateinit var gestureRecorder: GestureRecorder
    private lateinit var audioVolumeManager: AudioVolumeManager
    private lateinit var voiceFeedbackManager: VoiceFeedbackManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize core engines
        audioVolumeManager = AudioVolumeManager(this)
        gestureRecognizer = GestureRecognizer()
        gestureMapper = GestureMapper(this)
        gestureRecorder = GestureRecorder(this)

        voiceFeedbackManager = VoiceFeedbackManager(this) { voiceCommand ->
            handleVoiceCommand(voiceCommand)
        }

        handLandmarkerHelper = HandLandmarkerHelper(
            context = this,
            minHandDetectionConfidence = 0.35f,
            minHandTrackingConfidence = 0.35f,
            minHandPresenceConfidence = 0.35f,
            maxNumHands = 2,
            currentDelegate = HandLandmarkerHelper.DELEGATE_CPU
        )

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF070D18)
                ) {
                    MainGestureScreen(
                        handLandmarkerHelper = handLandmarkerHelper,
                        gestureRecognizer = gestureRecognizer,
                        gestureMapper = gestureMapper,
                        gestureRecorder = gestureRecorder,
                        audioVolumeManager = audioVolumeManager,
                        voiceFeedbackManager = voiceFeedbackManager
                    )
                }
            }
        }
    }

    private fun handleVoiceCommand(command: String) {
        val lower = command.lowercase()
        when {
            lower.contains("volume up") || lower.contains("turn up") -> {
                audioVolumeManager.stepVolume(0.12f)
                voiceFeedbackManager.speak("Volume increased")
            }
            lower.contains("volume down") || lower.contains("turn down") -> {
                audioVolumeManager.stepVolume(-0.12f)
                voiceFeedbackManager.speak("Volume decreased")
            }
            lower.contains("mute") -> {
                val isMuted = audioVolumeManager.toggleMute()
                voiceFeedbackManager.speak(if (isMuted) "Muted" else "Unmuted")
            }
            lower.contains("play") || lower.contains("pause") -> {
                voiceFeedbackManager.speak("Toggled playback")
            }
            lower.contains("scroll up") || lower.contains("swipe up") -> {
                voiceFeedbackManager.speak("Scrolling up")
            }
            lower.contains("scroll down") || lower.contains("swipe down") -> {
                voiceFeedbackManager.speak("Scrolling down")
            }
            else -> {
                voiceFeedbackManager.speak("Recognized command $command")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handLandmarkerHelper.clearHandLandmarker()
        voiceFeedbackManager.shutdown()
    }
}
