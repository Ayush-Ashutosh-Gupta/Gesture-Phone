package com.example.model

/**
 * Sealed hierarchy representing all gesture-triggered actions.
 */
sealed class GestureAction {
    data class MouseMove(val x: Float, val y: Float) : GestureAction()
    data class Click(val isDouble: Boolean, val x: Float = 0f, val y: Float = 0f) : GestureAction()
    data class Hold(val isHolding: Boolean, val x: Float = 0f, val y: Float = 0f) : GestureAction()
    data class Swipe(val direction: SwipeDirection, val velocity: Float = 1.0f) : GestureAction()
    data class MediaControl(val action: MediaAction) : GestureAction()
    data class VolumeControl(val levelPercent: Float, val delta: Float) : GestureAction()
    data class BrightnessControl(val levelPercent: Float) : GestureAction()
    data class SystemAction(val type: SystemActionType) : GestureAction()
    data class Custom(val name: String, val payload: String = "") : GestureAction()
    object None : GestureAction()
}

enum class SwipeDirection {
    UP,
    DOWN,
    LEFT,
    RIGHT
}

enum class MediaAction {
    PLAY_PAUSE,
    NEXT_TRACK,
    PREV_TRACK,
    VOLUME_UP,
    VOLUME_DOWN,
    MUTE_TOGGLE
}

enum class SystemActionType {
    BACK,
    HOME,
    SCREENSHOT,
    FLASHLIGHT,
    LOCK_SCREEN,
    OPEN_RECORDER,
    OPEN_SETTINGS,
    ESCAPE_MODE
}

enum class HandType {
    LEFT,
    RIGHT,
    UNKNOWN
}

enum class GestureType(val displayName: String, val defaultDescription: String) {
    CURSOR_POINT("Index Point", "Move virtual cursor with index tip"),
    PINCH_CLICK("Pinch Click", "Thumb + Index tap for click / drag"),
    FIST_HOLD("Fist Hold", "Clench fist for 0.5s to toggle Play/Pause"),
    SWIPE_UP("Swipe Up", "Quick upward hand motion to scroll up"),
    SWIPE_DOWN("Swipe Down", "Quick downward hand motion to scroll down"),
    SWIPE_LEFT("Swipe Left", "Swipe hand left for next media"),
    SWIPE_RIGHT("Swipe Right", "Swipe hand right for previous media"),
    PEACE_SIGN("Peace / Victory", "Two fingers up - custom action"),
    ROCK_ON("Rock On", "Index + Pinky extended - custom action"),
    THUMBS_UP("Thumbs Up", "Volume Up / Confirm"),
    THUMBS_DOWN("Thumbs Down", "Volume Down / Decline"),
    OK_SIGN("OK Hand", "Open Custom App / Shortcut"),
    PALM_OPEN("Open Palm", "Pause / Reset / Mute"),
    VOLUME_SLIDE("Volume Vertical Slide", "Smooth continuous volume control"),
    CUSTOM_RECORDED("Custom Gesture", "User recorded gesture")
}

/**
 * Normalized 3D Point for hand landmark
 */
data class LandmarkPoint(
    val x: Float,
    val y: Float,
    val z: Float = 0f
)

/**
 * Detailed tracked hand state
 */
data class TrackedHand(
    val handType: HandType,
    val confidence: Float,
    val landmarks: List<LandmarkPoint>,
    val activeGesture: GestureType = GestureType.PALM_OPEN,
    val pinchDistance: Float = 1.0f,
    val isPinching: Boolean = false,
    val isFist: Boolean = false,
    val indexTipNormalized: LandmarkPoint = LandmarkPoint(0.5f, 0.5f),
    val thumbTipNormalized: LandmarkPoint = LandmarkPoint(0.5f, 0.5f),
    val wristNormalized: LandmarkPoint = LandmarkPoint(0.5f, 0.5f),
    val extendedFingers: List<Boolean> = List(5) { false } // [Thumb, Index, Middle, Ring, Pinky]
)

/**
 * App-wide Gesture control configuration
 */
data class GestureConfig(
    val cursorModeEnabled: Boolean = true,
    val pinchClickEnabled: Boolean = true,
    val swipeScrollEnabled: Boolean = true,
    val twoHandModeEnabled: Boolean = true,
    val sensitivity: Float = 6.0f, // 1.0 to 10.0
    val deadzone: Float = 0.035f, // 0.01 to 0.15
    val minConfidence: Float = 0.35f,
    val clickDurationMs: Long = 100L, // 40ms to 1000ms - time pinch must be held to register as a click
    val doubleClickWindowMs: Long = 500L, // 150ms to 2500ms - max interval between 2 pinches for double click
    val clickHoldDurationMs: Long = 700L, // 250ms to 4000ms - time pinch must be held to start hold/drag
    val poseHoldDurationMs: Long = 500L, // 200ms to 3000ms - time to sustain fist/pose for system action
    val singleClickDebounceMs: Long = 100L, // legacy alias
    val calibratedPalmSize: Float = 0.12f, // Calibrated anatomical palm reference size
    val voiceFeedbackEnabled: Boolean = true,
    val hapticFeedbackEnabled: Boolean = true,
    val useFrontCamera: Boolean = true,
    val showDebugLandmarks: Boolean = true,
    val showSkeletonLines: Boolean = true,
    val smoothSmoothingFactor: Float = 0.35f, // EMA alpha
    val invertHandedness: Boolean = false, // When true, swaps detected Left and Right hand mappings
    val skeletonSmoothing: Float = 0.55f, // 0.2 to 0.9 - adaptive smoothing for wrist and finger skeleton stability
    val enabledGestures: Map<GestureType, Boolean> = GestureType.values().associateWith { true }
)

