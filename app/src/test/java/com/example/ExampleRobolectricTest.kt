package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.GestureAction
import com.example.model.GestureType
import com.example.model.LandmarkPoint
import com.example.model.MediaAction
import com.example.model.TrackedHand
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context matches GesturePhone`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("GesturePhone", appName)
    }

    @Test
    fun `test distance math and smooth interpolator`() {
        val p1 = LandmarkPoint(0f, 0f, 0f)
        val p2 = LandmarkPoint(3f, 4f, 0f)
        val dist = Utils.distance3D(p1, p2)
        assertEquals(5f, dist, 0.001f)

        val smoother = Utils.SmoothPoint2D(0.5f)
        val initial = smoother.update(0.5f, 0.5f)
        assertEquals(0.5f, initial.first, 0.001f)
    }

    @Test
    fun `test gesture mapper serialization`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val mapper = com.example.gesture.GestureMapper(context)
        mapper.setMapping(GestureType.PINCH_CLICK, GestureAction.Click(isDouble = false))
        val action = mapper.getAction(GestureType.PINCH_CLICK)
        assertTrue(action is GestureAction.Click)
    }

    @Test
    fun `test gesture timing and handedness config`() {
        val config = com.example.model.GestureConfig(
            clickDurationMs = 280L,
            doubleClickWindowMs = 450L,
            clickHoldDurationMs = 600L,
            invertHandedness = true,
            skeletonSmoothing = 0.65f
        )
        assertEquals(280L, config.clickDurationMs)
        assertEquals(450L, config.doubleClickWindowMs)
        assertEquals(600L, config.clickHoldDurationMs)
        assertTrue(config.invertHandedness)
        assertEquals(0.65f, config.skeletonSmoothing, 0.001f)
    }
}
