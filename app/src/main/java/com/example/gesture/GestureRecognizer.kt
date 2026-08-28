package com.example.gesture

import android.os.SystemClock
import com.example.Utils
import com.example.model.GestureAction
import com.example.model.GestureConfig
import com.example.model.GestureType
import com.example.model.HandType
import com.example.model.LandmarkPoint
import com.example.model.MediaAction
import com.example.model.SwipeDirection
import com.example.model.SystemActionType
import com.example.model.TrackedHand
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

class GestureRecognizer(
    private var config: GestureConfig = GestureConfig()
) {
    // Smoother for cursor (pointer)
    private val cursorSmoother = Utils.SmoothPoint2D(config.smoothSmoothingFactor)

    // Smoother for continuous volume adjustment
    private val volumeSmoother = Utils.SmoothFloat(0.20f)

    // Motion buffers for swipe tracking per hand (TimeMs, PalmCenterX, PalmCenterY)
    private val rightMotionBuffer = ArrayDeque<Triple<Long, Float, Float>>()
    private val leftMotionBuffer = ArrayDeque<Triple<Long, Float, Float>>()
    private var lastRightSwipeTime: Long = 0L
    private var lastLeftSwipeTime: Long = 0L
    private val SWIPE_COOLDOWN_MS = 400L
    private val SWIPE_MIN_DISPLACEMENT = 0.075f
    private val SWIPE_MAX_DURATION_MS = 450L
    private val SWIPE_MIN_VELOCITY = 0.28f // screens per second

    // Hold trackers for special gestures
    private var leftFistStartTime: Long = 0L
    private var isLeftFistTriggered = false
    private var leftRockOnStartTime: Long = 0L
    private var isLeftRockOnTriggered = false
    private var leftPeaceStartTime: Long = 0L
    private var isLeftPeaceTriggered = false
    private var lastThumbsActionTime: Long = 0L

    // Fast, hysteresis-based Pinch tracking
    private var isCurrentlyPinching = false
    private var pinchStartTime: Long = 0L
    private var isClickTriggered = false
    private var isHoldTriggered = false
    private var lastClickTime: Long = 0L
    private var anchoredPinchPoint: Pair<Float, Float>? = null
    private val DOUBLE_CLICK_WINDOW_MS = 320L
    private val HOLD_TRIGGER_DURATION_MS = 320L

    // Continuous volume tracking
    private var currentVolumePercent: Float = 0.5f

    // Smoothing cache for the 21 landmarks per hand type to stabilize wrist and finger joints
    private val smoothedLandmarksMap = mutableMapOf<HandType, List<LandmarkPoint>>()

    fun updateConfig(newConfig: GestureConfig) {
        config = newConfig
        cursorSmoother.setSmoothingFactor(newConfig.smoothSmoothingFactor)
    }

    /**
     * Main entry: processes MediaPipe result and separates Left Hand vs Right Hand
     */
    fun processResult(
        result: HandLandmarkerResult,
        isFrontCamera: Boolean = true
    ): Pair<List<TrackedHand>, List<GestureAction>> {
        val landmarksList = result.landmarks()
        val handednesses = result.handednesses()
        val trackedHands = mutableListOf<TrackedHand>()
        val actions = mutableListOf<GestureAction>()
        val currentTime = SystemClock.uptimeMillis()

        if (landmarksList.isEmpty()) {
            smoothedLandmarksMap.clear()
            leftFistStartTime = 0L
            isLeftFistTriggered = false
            leftRockOnStartTime = 0L
            isLeftRockOnTriggered = false
            leftPeaceStartTime = 0L
            isLeftPeaceTriggered = false
            rightMotionBuffer.clear()
            leftMotionBuffer.clear()
            if (isCurrentlyPinching) {
                isCurrentlyPinching = false
                isClickTriggered = false
                isHoldTriggered = false
                anchoredPinchPoint = null
                actions.add(GestureAction.Hold(isHolding = false))
            }
            return Pair(emptyList(), actions)
        }

        // 1. Extract raw hands from MediaPipe landmarks
        val rawHands = mutableListOf<TrackedHand>()
        for (i in landmarksList.indices) {
            val rawPoints = landmarksList[i].map { normalizedLandmark ->
                // CameraView applies mirror matrix transformation to front camera frame.
                // Coordinates directly map to display coordinates [0..1] x [0..1].
                val x = normalizedLandmark.x().coerceIn(0f, 1f)
                val y = normalizedLandmark.y().coerceIn(0f, 1f)
                LandmarkPoint(x, y, normalizedLandmark.z())
            }

            if (rawPoints.size < 21) continue

            // Determine handedness:
            // MediaPipe Hand Landmarker outputs 'Left' for the user's left hand and 'Right' for the user's right hand.
            var rawDetectedHandType = HandType.UNKNOWN
            var confidence = 0.85f

            if (i < handednesses.size && handednesses[i].isNotEmpty()) {
                val category = handednesses[i][0]
                confidence = category.score()
                val displayName = category.displayName().lowercase()
                val categoryName = category.categoryName().lowercase()

                val isMediaPipeLeft = displayName.contains("left") || categoryName.contains("left")
                val naturalHandType = if (isMediaPipeLeft) HandType.LEFT else HandType.RIGHT
                rawDetectedHandType = if (config.invertHandedness) {
                    if (naturalHandType == HandType.LEFT) HandType.RIGHT else HandType.LEFT
                } else {
                    naturalHandType
                }
            }

            // 21-Point Adaptive Temporal Skeleton Stabilization:
            // Prevents wrist and finger jitter without adding perceptible lag
            val prevSmoothed = smoothedLandmarksMap[rawDetectedHandType]
            val landmarkPoints = if (prevSmoothed != null && prevSmoothed.size == 21) {
                val alpha = config.skeletonSmoothing.coerceIn(0.2f, 0.85f)
                rawPoints.mapIndexed { idx, currentPt ->
                    val prevPt = prevSmoothed[idx]
                    val dist = Utils.distance2D(currentPt, prevPt)
                    // Dynamic response: fast motions track instantly with high alpha; gentle pauses smooth completely
                    val dynamicAlpha = if (dist > 0.05f) 0.90f else alpha
                    LandmarkPoint(
                        x = prevPt.x + dynamicAlpha * (currentPt.x - prevPt.x),
                        y = prevPt.y + dynamicAlpha * (currentPt.y - prevPt.y),
                        z = prevPt.z + dynamicAlpha * (currentPt.z - prevPt.z)
                    )
                }
            } else {
                rawPoints
            }
            smoothedLandmarksMap[rawDetectedHandType] = landmarkPoints

            val wrist = landmarkPoints[0]
            val thumbTip = landmarkPoints[4]
            val thumbIp = landmarkPoints[3]
            val indexMcp = landmarkPoints[5]
            val indexPip = landmarkPoints[6]
            val indexTip = landmarkPoints[8]
            val middleMcp = landmarkPoints[9]
            val middleTip = landmarkPoints[12]
            val ringMcp = landmarkPoints[13]
            val ringTip = landmarkPoints[16]
            val pinkyMcp = landmarkPoints[17]
            val pinkyTip = landmarkPoints[20]

            // Anatomical palm size for scale-invariant distance calculations
            val palmRefSize = max(0.06f, Utils.distance2D(wrist, middleMcp))

            // Finger extension states (scale-invariant)
            val isIndexExtended = Utils.distance2D(indexTip, wrist) > Utils.distance2D(indexPip, wrist) * 1.10f
            val isMiddleExtended = Utils.distance2D(middleTip, wrist) > Utils.distance2D(middleMcp, wrist) * 1.12f
            val isRingExtended = Utils.distance2D(ringTip, wrist) > Utils.distance2D(ringMcp, wrist) * 1.12f
            val isPinkyExtended = Utils.distance2D(pinkyTip, wrist) > Utils.distance2D(pinkyMcp, wrist) * 1.12f
            val isThumbExtended = Utils.distance2D(thumbTip, indexMcp) > Utils.distance2D(thumbIp, indexMcp) * 1.12f

            val extendedFingers = listOf(isThumbExtended, isIndexExtended, isMiddleExtended, isRingExtended, isPinkyExtended)

            // Scale-invariant Pinch measurement with hysteresis
            val pinchDistThumbIndex = Utils.distance2D(thumbTip, indexTip)
            val pinchRatio = pinchDistThumbIndex / palmRefSize

            val sensitivityMul = (config.sensitivity / 5.0f).coerceIn(0.7f, 1.4f)
            val pinchCloseThreshold = 0.36f * sensitivityMul
            val pinchOpenThreshold = 0.48f * sensitivityMul

            val isPinch = if (isCurrentlyPinching) {
                pinchRatio < pinchOpenThreshold
            } else {
                pinchRatio < pinchCloseThreshold
            }

            // Fist detection
            val isFist = !isIndexExtended && !isMiddleExtended && !isRingExtended && !isPinkyExtended

            // Pose classification
            val gestureType: GestureType = when {
                isPinch -> GestureType.PINCH_CLICK
                isFist -> GestureType.FIST_HOLD
                isIndexExtended && isMiddleExtended && !isRingExtended && !isPinkyExtended -> GestureType.PEACE_SIGN
                isIndexExtended && !isMiddleExtended && !isRingExtended && isPinkyExtended -> GestureType.ROCK_ON
                isThumbExtended && !isIndexExtended && !isMiddleExtended && !isRingExtended && !isPinkyExtended -> {
                    if (thumbTip.y < wrist.y - 0.05f) GestureType.THUMBS_UP else GestureType.THUMBS_DOWN
                }
                isPinch && isMiddleExtended && isRingExtended && isPinkyExtended -> GestureType.OK_SIGN
                isIndexExtended && !isMiddleExtended && !isRingExtended && !isPinkyExtended -> GestureType.CURSOR_POINT
                extendedFingers.count { it } >= 4 -> GestureType.PALM_OPEN
                else -> GestureType.CURSOR_POINT
            }

            val hand = TrackedHand(
                handType = rawDetectedHandType,
                confidence = confidence,
                landmarks = landmarkPoints,
                activeGesture = gestureType,
                pinchDistance = pinchDistThumbIndex,
                isPinching = isPinch,
                isFist = isFist,
                indexTipNormalized = indexTip,
                thumbTipNormalized = thumbTip,
                wristNormalized = wrist,
                extendedFingers = extendedFingers
            )
            rawHands.add(hand)
        }

        // 2. Spatial Handedness Stabilizer
        // If 2 hands are detected, in mirror mode screen left is user's LEFT hand, screen right is user's RIGHT hand.
        val stabilizedHands = if (rawHands.size == 2) {
            val handA = rawHands[0]
            val handB = rawHands[1]
            val leftOnScreen = if (handA.wristNormalized.x < handB.wristNormalized.x) handA else handB
            val rightOnScreen = if (handA.wristNormalized.x < handB.wristNormalized.x) handB else handA
            val finalLeft = if (config.invertHandedness) rightOnScreen.copy(handType = HandType.RIGHT) else leftOnScreen.copy(handType = HandType.LEFT)
            val finalRight = if (config.invertHandedness) leftOnScreen.copy(handType = HandType.LEFT) else rightOnScreen.copy(handType = HandType.RIGHT)
            listOf(finalLeft, finalRight)
        } else {
            rawHands
        }

        trackedHands.addAll(stabilizedHands)

        // 3. Dispatch Hand-Specific Actions
        val rightHand = stabilizedHands.find { it.handType == HandType.RIGHT }
            ?: if (stabilizedHands.size == 1) stabilizedHands[0] else null

        val leftHand = stabilizedHands.find { it.handType == HandType.LEFT }
            ?: if (stabilizedHands.size == 1 && stabilizedHands[0] != rightHand) stabilizedHands[0] else null

        // --- RIGHT HAND PIPELINE: Cursor, Click, Hold/Drag, Navigation Swipes ---
        if (rightHand != null) {
            processRightHandPointerAndClicks(rightHand, actions, currentTime)
            processRightHandNavigationSwipes(rightHand, actions, currentTime)
        } else {
            if (isCurrentlyPinching) {
                isCurrentlyPinching = false
                isHoldTriggered = false
                anchoredPinchPoint = null
                actions.add(GestureAction.Hold(isHolding = false))
            }
        }

        // --- LEFT HAND PIPELINE: System Audio Volume, Light/Torch, Media, Mute ---
        if (leftHand != null) {
            processLeftHandSystemControls(leftHand, actions, currentTime)
            processLeftHandMediaSwipes(leftHand, actions, currentTime)
        } else {
            leftFistStartTime = 0L
            isLeftFistTriggered = false
            leftRockOnStartTime = 0L
            isLeftRockOnTriggered = false
            leftPeaceStartTime = 0L
            isLeftPeaceTriggered = false
        }

        return Pair(trackedHands, actions)
    }

    /**
     * Remaps inner camera frame to comfortably reach all edges of the screen
     */
    private fun remapReach(v: Float, margin: Float = 0.08f): Float {
        return ((v - margin) / (1.0f - 2.0f * margin)).coerceIn(0.005f, 0.995f)
    }

    /**
     * RIGHT HAND: Virtual Cursor movement, Pinch Click, and Hold/Drag
     */
    private fun processRightHandPointerAndClicks(
        hand: TrackedHand,
        actions: MutableList<GestureAction>,
        currentTime: Long
    ) {
        if (!config.cursorModeEnabled) return

        // Use stabilized anchor point when pinching so click doesn't jump
        val targetX = if (hand.isPinching && anchoredPinchPoint != null) {
            anchoredPinchPoint!!.first
        } else {
            remapReach(hand.indexTipNormalized.x)
        }

        val targetY = if (hand.isPinching && anchoredPinchPoint != null) {
            anchoredPinchPoint!!.second
        } else {
            remapReach(hand.indexTipNormalized.y)
        }

        val deadzone = config.deadzone
        val smoothed = cursorSmoother.update(targetX, targetY, deadzone)

        actions.add(GestureAction.MouseMove(smoothed.first, smoothed.second))

        if (config.pinchClickEnabled) {
            if (hand.isPinching) {
                if (!isCurrentlyPinching) {
                    // Pinch Down detected
                    isCurrentlyPinching = true
                    pinchStartTime = currentTime
                    isClickTriggered = false
                    isHoldTriggered = false
                    anchoredPinchPoint = Pair(smoothed.first, smoothed.second)
                }

                val pinchDuration = currentTime - pinchStartTime

                // 1. Click / Double-Click: fires when pinch is sustained for at least config.clickDurationMs
                if (!isClickTriggered && !isHoldTriggered && pinchDuration >= config.clickDurationMs) {
                    isClickTriggered = true
                    val isDoubleClick = (currentTime - lastClickTime) < config.doubleClickWindowMs
                    actions.add(GestureAction.Click(isDouble = isDoubleClick, x = smoothed.first, y = smoothed.second))
                    lastClickTime = if (isDoubleClick) 0L else currentTime
                } else if (!isHoldTriggered && pinchDuration >= config.clickHoldDurationMs) {
                    // 2. Sustained Pinch -> Trigger Hold/Drag based on user-configured hold duration
                    isHoldTriggered = true
                    actions.add(GestureAction.Hold(isHolding = true, x = smoothed.first, y = smoothed.second))
                }
            } else {
                // Pinch Released
                if (isCurrentlyPinching) {
                    isCurrentlyPinching = false
                    isClickTriggered = false
                    anchoredPinchPoint = null
                    if (isHoldTriggered) {
                        isHoldTriggered = false
                        actions.add(GestureAction.Hold(isHolding = false, x = smoothed.first, y = smoothed.second))
                    }
                }
            }
        }
    }

    /**
     * Directional Swipes: Navigation and Media controls
     */
    private fun processRightHandNavigationSwipes(
        hand: TrackedHand,
        actions: MutableList<GestureAction>,
        currentTime: Long
    ) {
        if (!config.swipeScrollEnabled) return

        // Use Palm Center (Midpoint of Wrist and Index/Middle knuckles) for robust trajectory
        val wrist = hand.wristNormalized
        val indexMcp = hand.landmarks.getOrNull(5) ?: wrist
        val palmX = (wrist.x + indexMcp.x) / 2f
        val palmY = (wrist.y + indexMcp.y) / 2f

        rightMotionBuffer.addLast(Triple(currentTime, palmX, palmY))

        while (rightMotionBuffer.isNotEmpty() && (currentTime - rightMotionBuffer.first().first) > SWIPE_MAX_DURATION_MS) {
            rightMotionBuffer.removeFirst()
        }

        if (rightMotionBuffer.size >= 3 && (currentTime - lastRightSwipeTime) > SWIPE_COOLDOWN_MS) {
            val start = rightMotionBuffer.first()
            val end = rightMotionBuffer.last()
            val dx = end.second - start.second
            val dy = end.third - start.third
            val dt = (end.first - start.first).coerceAtLeast(1L)
            val velocity = hypot(dx, dy) / (dt / 1000f)

            if ((abs(dx) > SWIPE_MIN_DISPLACEMENT || abs(dy) > SWIPE_MIN_DISPLACEMENT) && velocity > SWIPE_MIN_VELOCITY) {
                val direction = when {
                    abs(dx) > abs(dy) * 1.2f && dx > SWIPE_MIN_DISPLACEMENT -> SwipeDirection.RIGHT
                    abs(dx) > abs(dy) * 1.2f && dx < -SWIPE_MIN_DISPLACEMENT -> SwipeDirection.LEFT
                    abs(dy) > abs(dx) * 1.2f && dy > SWIPE_MIN_DISPLACEMENT -> SwipeDirection.DOWN
                    abs(dy) > abs(dx) * 1.2f && dy < -SWIPE_MIN_DISPLACEMENT -> SwipeDirection.UP
                    else -> null
                }

                if (direction != null) {
                    actions.add(GestureAction.Swipe(direction, velocity))
                    lastRightSwipeTime = currentTime
                    rightMotionBuffer.clear()
                }
            }
        }
    }

    /**
     * LEFT HAND: System controls (Volume +/- with Thumbs, Torch with Rock-on, Play/Pause with Fist, Mute with Peace)
     */
    private fun processLeftHandSystemControls(
        hand: TrackedHand,
        actions: MutableList<GestureAction>,
        currentTime: Long
    ) {
        // 1. Thumbs Up / Down -> Step Volume Up / Down
        if (currentTime - lastThumbsActionTime > 300L) {
            if (hand.activeGesture == GestureType.THUMBS_UP) {
                actions.add(GestureAction.MediaControl(MediaAction.VOLUME_UP))
                lastThumbsActionTime = currentTime
            } else if (hand.activeGesture == GestureType.THUMBS_DOWN) {
                actions.add(GestureAction.MediaControl(MediaAction.VOLUME_DOWN))
                lastThumbsActionTime = currentTime
            }
        }

        // 2. Rock On Gesture -> Toggle Flashlight / Light
        if (hand.activeGesture == GestureType.ROCK_ON) {
            if (leftRockOnStartTime == 0L) {
                leftRockOnStartTime = currentTime
                isLeftRockOnTriggered = false
            } else if (!isLeftRockOnTriggered && (currentTime - leftRockOnStartTime) >= config.poseHoldDurationMs) {
                isLeftRockOnTriggered = true
                actions.add(GestureAction.SystemAction(SystemActionType.FLASHLIGHT))
            }
        } else {
            leftRockOnStartTime = 0L
            isLeftRockOnTriggered = false
        }

        // 3. Fist Hold -> Media Play / Pause Toggle
        if (hand.isFist) {
            if (leftFistStartTime == 0L) {
                leftFistStartTime = currentTime
                isLeftFistTriggered = false
            } else if (!isLeftFistTriggered && (currentTime - leftFistStartTime) >= config.poseHoldDurationMs) {
                isLeftFistTriggered = true
                actions.add(GestureAction.MediaControl(MediaAction.PLAY_PAUSE))
            }
        } else {
            leftFistStartTime = 0L
            isLeftFistTriggered = false
        }

        // 4. Peace Sign -> Mute Toggle
        if (hand.activeGesture == GestureType.PEACE_SIGN) {
            if (leftPeaceStartTime == 0L) {
                leftPeaceStartTime = currentTime
                isLeftPeaceTriggered = false
            } else if (!isLeftPeaceTriggered && (currentTime - leftPeaceStartTime) >= config.poseHoldDurationMs) {
                isLeftPeaceTriggered = true
                actions.add(GestureAction.MediaControl(MediaAction.MUTE_TOGGLE))
            }
        } else {
            leftPeaceStartTime = 0L
            isLeftPeaceTriggered = false
        }

        // 5. Continuous Volume Sliding with Open Palm
        if (hand.activeGesture == GestureType.PALM_OPEN) {
            val rawY = hand.wristNormalized.y.coerceIn(0.12f, 0.88f)
            val targetVolume = 1.0f - ((rawY - 0.12f) / 0.76f)
            val smoothedVolume = volumeSmoother.update(targetVolume)

            val delta = smoothedVolume - currentVolumePercent
            if (abs(delta) > 0.015f) {
                currentVolumePercent = smoothedVolume
                actions.add(GestureAction.VolumeControl(levelPercent = smoothedVolume, delta = delta))
            }
        }
    }

    /**
     * LEFT HAND: Swipes for Next Track / Previous Track
     */
    private fun processLeftHandMediaSwipes(
        hand: TrackedHand,
        actions: MutableList<GestureAction>,
        currentTime: Long
    ) {
        val wrist = hand.wristNormalized
        val indexMcp = hand.landmarks.getOrNull(5) ?: wrist
        val palmX = (wrist.x + indexMcp.x) / 2f
        val palmY = (wrist.y + indexMcp.y) / 2f

        leftMotionBuffer.addLast(Triple(currentTime, palmX, palmY))

        while (leftMotionBuffer.isNotEmpty() && (currentTime - leftMotionBuffer.first().first) > SWIPE_MAX_DURATION_MS) {
            leftMotionBuffer.removeFirst()
        }

        if (leftMotionBuffer.size >= 3 && (currentTime - lastLeftSwipeTime) > SWIPE_COOLDOWN_MS) {
            val start = leftMotionBuffer.first()
            val end = leftMotionBuffer.last()
            val dx = end.second - start.second
            val dt = (end.first - start.first).coerceAtLeast(1L)
            val velocity = abs(dx) / (dt / 1000f)

            if (abs(dx) > SWIPE_MIN_DISPLACEMENT && velocity > SWIPE_MIN_VELOCITY) {
                if (dx > SWIPE_MIN_DISPLACEMENT) {
                    actions.add(GestureAction.MediaControl(MediaAction.NEXT_TRACK))
                } else if (dx < -SWIPE_MIN_DISPLACEMENT) {
                    actions.add(GestureAction.MediaControl(MediaAction.PREV_TRACK))
                }
                lastLeftSwipeTime = currentTime
                leftMotionBuffer.clear()
            }
        }
    }
}
