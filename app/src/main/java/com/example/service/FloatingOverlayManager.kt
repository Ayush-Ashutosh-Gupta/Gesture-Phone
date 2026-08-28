package com.example.service

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.model.GestureType
import com.example.model.HandType
import com.example.model.TrackedHand

class FloatingOverlayManager(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private var cursorView: View? = null
    private var hudView: View? = null
    private var isOverlayShowing = false

    private val displayMetrics = DisplayMetrics()
    private var screenWidth = 1080
    private var screenHeight = 2400

    private var currentCursorX = 0f
    private var currentCursorY = 0f

    // HUD views cache
    private var handTypeTextView: TextView? = null
    private var gestureNameTextView: TextView? = null
    private var confidenceTextView: TextView? = null
    private var hudStatusIndicator: View? = null
    private var miniPillContainer: LinearLayout? = null

    init {
        updateScreenDimensions()
    }

    fun updateScreenDimensions() {
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        screenWidth = displayMetrics.widthPixels
        screenHeight = displayMetrics.heightPixels
    }

    @SuppressLint("InflateParams", "ClickableViewAccessibility")
    fun showOverlays(
        onSwitchCamera: () -> Unit,
        onStopService: () -> Unit,
        onToggleMute: () -> Unit
    ) {
        if (isOverlayShowing) return
        updateScreenDimensions()

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        // 1. Create Floating Virtual Cursor View
        val cursorParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (screenWidth / 2)
            y = (screenHeight / 2)
        }

        val cursorRoot = FrameLayout(context).apply {
            // High-tech pointer cursor with neon cyan glow and animated pinch ring
            val cursorPointer = View(context).apply {
                val sizePx = (32 * context.resources.displayMetrics.density).toInt()
                layoutParams = FrameLayout.LayoutParams(sizePx, sizePx).apply {
                    gravity = Gravity.CENTER
                }
                val drawable = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.parseColor("#00E5FF"))
                    setStroke((3 * context.resources.displayMetrics.density).toInt(), Color.parseColor("#FFFFFF"))
                }
                background = drawable
                elevation = 20f
            }
            addView(cursorPointer)
        }

        cursorView = cursorRoot

        try {
            windowManager.addView(cursorView, cursorParams)
        } catch (e: Throwable) {
            e.printStackTrace()
        }

        // 2. Create Floating Draggable Gesture HUD Pill
        val hudParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 24
            y = 140
        }

        val hudContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(24, 16, 24, 16)
            elevation = 25f

            val bg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 36f
                setColor(Color.parseColor("#E607101E"))
                setStroke(2, Color.parseColor("#4400E5FF"))
            }
            background = bg

            // Status LED dot
            val dot = View(context).apply {
                val dotSize = (10 * context.resources.displayMetrics.density).toInt()
                layoutParams = LinearLayout.LayoutParams(dotSize, dotSize).apply {
                    gravity = Gravity.CENTER_VERTICAL
                    rightMargin = 16
                }
                val dotBg = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.parseColor("#00E676"))
                }
                background = dotBg
            }
            hudStatusIndicator = dot
            addView(dot)

            // Text Info Column
            val infoCol = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_VERTICAL

                val handTxt = TextView(context).apply {
                    text = "NO HAND DETECTED"
                    setTextColor(Color.parseColor("#00E5FF"))
                    textSize = 11f
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                }
                handTypeTextView = handTxt
                addView(handTxt)

                val gestureTxt = TextView(context).apply {
                    text = "Tracking active..."
                    setTextColor(Color.WHITE)
                    textSize = 12f
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                }
                gestureNameTextView = gestureTxt
                addView(gestureTxt)
            }
            addView(infoCol)

            // Quick App Open Button
            val openAppBtn = TextView(context).apply {
                text = "OPEN APP"
                setTextColor(Color.parseColor("#00E5FF"))
                textSize = 10f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                setPadding(20, 10, 20, 10)
                val btnBg = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = 20f
                    setColor(Color.parseColor("#2200E5FF"))
                }
                background = btnBg
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = Gravity.CENTER_VERTICAL
                    leftMargin = 20
                }
                layoutParams = lp
                setOnClickListener {
                    val appIntent = Intent(context, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    }
                    context.startActivity(appIntent)
                }
            }
            addView(openAppBtn)

            // Stop Service button
            val stopBtn = TextView(context).apply {
                text = "✕"
                setTextColor(Color.parseColor("#FF5252"))
                textSize = 13f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                setPadding(16, 10, 16, 10)
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = Gravity.CENTER_VERTICAL
                    leftMargin = 8
                }
                layoutParams = lp
                setOnClickListener {
                    onStopService()
                }
            }
            addView(stopBtn)

            // Draggable Touch Listener
            setOnTouchListener(object : View.OnTouchListener {
                private var initialX = 0
                private var initialY = 0
                private var initialTouchX = 0f
                private var initialTouchY = 0f

                override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                    if (event == null) return false
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            initialX = hudParams.x
                            initialY = hudParams.y
                            initialTouchX = event.rawX
                            initialTouchY = event.rawY
                            return true
                        }
                        MotionEvent.ACTION_MOVE -> {
                            hudParams.x = initialX + (event.rawX - initialTouchX).toInt()
                            hudParams.y = initialY + (event.rawY - initialTouchY).toInt()
                            try {
                                windowManager.updateViewLayout(hudView, hudParams)
                            } catch (e: Throwable) {
                                e.printStackTrace()
                            }
                            return true
                        }
                    }
                    return false
                }
            })
        }

        hudView = hudContainer

        try {
            windowManager.addView(hudView, hudParams)
            isOverlayShowing = true
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    fun updateCursorPosition(normalizedX: Float, normalizedY: Float, isPinching: Boolean) {
        val targetX = (normalizedX * screenWidth).toInt().coerceIn(0, screenWidth - 40)
        val targetY = (normalizedY * screenHeight).toInt().coerceIn(0, screenHeight - 40)

        currentCursorX = targetX.toFloat()
        currentCursorY = targetY.toFloat()

        mainHandler.post {
            cursorView?.let { view ->
                val params = view.layoutParams as? WindowManager.LayoutParams ?: return@let
                params.x = targetX
                params.y = targetY

                // Animate pinch visually
                val pointer = (view as? FrameLayout)?.getChildAt(0)
                if (pointer != null) {
                    val scale = if (isPinching) 0.65f else 1.0f
                    pointer.scaleX = scale
                    pointer.scaleY = scale
                    val bg = pointer.background as? GradientDrawable
                    if (isPinching) {
                        bg?.setColor(Color.parseColor("#FFD700")) // Gold when clicking/pinching
                    } else {
                        bg?.setColor(Color.parseColor("#00E5FF")) // Cyan when moving
                    }
                }

                try {
                    windowManager.updateViewLayout(view, params)
                } catch (e: Throwable) {
                    // Ignore layout update race conditions
                }
            }
        }
    }

    fun getScreenCursorCoordinates(): Pair<Float, Float> {
        return Pair(currentCursorX, currentCursorY)
    }

    fun updateHandTelemetry(hands: List<TrackedHand>) {
        mainHandler.post {
            if (hands.isEmpty()) {
                handTypeTextView?.text = "NO HAND IN VIEW"
                handTypeTextView?.setTextColor(Color.parseColor("#FF5252"))
                gestureNameTextView?.text = "Wave hand at camera"
                (hudStatusIndicator?.background as? GradientDrawable)?.setColor(Color.parseColor("#FF5252"))
            } else {
                val primaryHand = hands.first()
                val handLabel = when (primaryHand.handType) {
                    HandType.LEFT -> "LEFT HAND (${(primaryHand.confidence * 100).toInt()}%)"
                    HandType.RIGHT -> "RIGHT HAND (${(primaryHand.confidence * 100).toInt()}%)"
                    HandType.UNKNOWN -> "HAND DETECTED"
                }
                handTypeTextView?.text = handLabel
                handTypeTextView?.setTextColor(Color.parseColor("#00E5FF"))

                val gestureLabel = if (primaryHand.isPinching) {
                    "PINCH CLICK"
                } else {
                    primaryHand.activeGesture.displayName
                }
                gestureNameTextView?.text = gestureLabel
                (hudStatusIndicator?.background as? GradientDrawable)?.setColor(Color.parseColor("#00E676"))
            }
        }
    }

    fun hideOverlays() {
        if (!isOverlayShowing) return
        mainHandler.post {
            try {
                cursorView?.let { windowManager.removeView(it) }
                cursorView = null
                hudView?.let { windowManager.removeView(it) }
                hudView = null
                isOverlayShowing = false
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }
}
