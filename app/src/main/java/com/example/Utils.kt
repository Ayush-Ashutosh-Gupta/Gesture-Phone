package com.example

import android.graphics.Bitmap
import android.graphics.Color
import com.example.model.LandmarkPoint
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

object Utils {

    /**
     * Exponential Moving Average (EMA) 2D Filter for ultra-smooth cursor movement
     */
    class SmoothPoint2D(private var alpha: Float = 0.35f) {
        var currentX: Float = 0.5f
            private set
        var currentY: Float = 0.5f
            private set
        private var initialized = false

        fun setSmoothingFactor(factor: Float) {
            alpha = factor.coerceIn(0.05f, 0.95f)
        }

        fun update(targetX: Float, targetY: Float, deadzone: Float = 0.005f): Pair<Float, Float> {
            if (!initialized) {
                currentX = targetX
                currentY = targetY
                initialized = true
                return Pair(currentX, currentY)
            }

            val dx = targetX - currentX
            val dy = targetY - currentY
            val dist = hypot(dx, dy)

            // If inside deadzone, minimize jitter
            if (dist > deadzone) {
                // Adaptive smoothing: faster movement gets less smoothing (more responsive), slower gets more smoothing (less jitter)
                val dynamicAlpha = min(0.85f, alpha + (dist * 0.5f))
                currentX += dx * dynamicAlpha
                currentY += dy * dynamicAlpha
            }

            return Pair(currentX, currentY)
        }

        fun reset(x: Float = 0.5f, y: Float = 0.5f) {
            currentX = x
            currentY = y
            initialized = false
        }
    }

    /**
     * Smooth 1D Float interpolator (for volume slider)
     */
    class SmoothFloat(private val alpha: Float = 0.25f) {
        var current: Float = 0.5f
            private set
        private var initialized = false

        fun update(target: Float): Float {
            if (!initialized) {
                current = target
                initialized = true
                return current
            }
            current += (target - current) * alpha
            return current
        }

        fun reset(value: Float = 0.5f) {
            current = value
            initialized = false
        }
    }

    /**
     * Distance between two normalized 3D landmarks
     */
    fun distance3D(p1: LandmarkPoint, p2: LandmarkPoint): Float {
        val dx = p1.x - p2.x
        val dy = p1.y - p2.y
        val dz = p1.z - p2.z
        return sqrt(dx * dx + dy * dy + dz * dz)
    }

    /**
     * Distance between two 2D points
     */
    fun distance2D(p1: LandmarkPoint, p2: LandmarkPoint): Float {
        val dx = p1.x - p2.x
        val dy = p1.y - p2.y
        return hypot(dx, dy)
    }

    /**
     * Angle between 3 points in degrees
     */
    fun angleDegrees(p1: LandmarkPoint, vertex: LandmarkPoint, p2: LandmarkPoint): Float {
        val v1x = p1.x - vertex.x
        val v1y = p1.y - vertex.y
        val v2x = p2.x - vertex.x
        val v2y = p2.y - vertex.y

        val dot = v1x * v2x + v1y * v2y
        val cross = v1x * v2y - v1y * v2x
        val angle = atan2(cross, dot)
        var deg = Math.toDegrees(angle.toDouble()).toFloat()
        if (deg < 0) deg += 360f
        return deg
    }

    /**
     * Simple QR Code pattern generator bitmap for sharing mappings
     */
    fun generateSimpleQrBitmap(content: String, size: Int = 300): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val hash = content.hashCode()
        val matrixSize = 25
        val cellSize = size / matrixSize

        // Render QR-like pattern with corner finder patterns
        for (x in 0 until size) {
            for (y in 0 until size) {
                val gridX = (x / cellSize).coerceIn(0, matrixSize - 1)
                val gridY = (y / cellSize).coerceIn(0, matrixSize - 1)

                val isCornerFinder =
                    (gridX < 7 && gridY < 7) ||
                    (gridX > matrixSize - 8 && gridY < 7) ||
                    (gridX < 7 && gridY > matrixSize - 8)

                var isBlack = false
                if (isCornerFinder) {
                    val localX = if (gridX < 7) gridX else gridX - (matrixSize - 7)
                    val localY = if (gridY < 7) gridY else gridY - (matrixSize - 7)
                    val isOuter = localX == 0 || localX == 6 || localY == 0 || localY == 6
                    val isInner = localX in 2..4 && localY in 2..4
                    isBlack = isOuter || isInner
                } else {
                    // Seeded pseudo-random bit pattern based on payload hash & coordinates
                    val bitVal = ((hash xor (gridX * 73856093) xor (gridY * 19349663)) and 1) == 1
                    val charIndex = (gridX + gridY * matrixSize) % max(1, content.length)
                    val charVal = (content[charIndex].code and 1) == 1
                    isBlack = bitVal xor charVal
                }

                bitmap.setPixel(x, y, if (isBlack) Color.BLACK else Color.WHITE)
            }
        }
        return bitmap
    }
}
