package com.mediaplayer.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt
import kotlin.math.tanh

class AudioVideoMathTest {

    @Test
    fun testVolumeBoostTanhSoftSaturation() {
        // At unity or below, gain is 1.0 (no distortion, 100% bit-perfect)
        val unityGain = calculateGain(1.0f)
        assertEquals(1.0f, unityGain, 0.001f)

        // At 150% boost, gain smoothly curves up using tanh
        val boost150 = calculateGain(1.5f)
        assertTrue(boost150 > 1.0f && boost150 < 2.0f)

        // At 200% boost, gain remains bounded without harsh digital clipping
        val boost200 = calculateGain(2.0f)
        assertTrue(boost200 > boost150)
        assertTrue(boost200 <= 2.0f)
    }

    private fun calculateGain(level: Float): Float {
        return if (level <= 1.0f) {
            1.0f
        } else {
            val boost = (level - 1.0f).toDouble()
            1.0f + tanh(boost).toFloat()
        }
    }

    @Test
    fun testPerceptualBrightnessGammaMath() {
        // Perceptual brightness P in [0, 1] mapped to physical luminance L = P^2
        val pLow = 0.2f
        val lLow = pLow * pLow
        assertEquals(0.04f, lLow, 0.001f)

        // Reversal: P = sqrt(L)
        val recovered = sqrt(lLow)
        assertEquals(pLow, recovered, 0.001f)
    }

    @Test
    fun testAdaptiveScrubbingDynamicSpanMath() {
        // Short 5 minute video (300,000 ms)
        val shortSpan = (300_000L * 0.20f).toLong().coerceIn(60_000L, 1_800_000L)
        assertEquals(60_000L, shortSpan) // clamped to 1 minute

        // 2-hour movie (7,200,000 ms)
        val movieSpan = (7_200_000L * 0.20f).toLong().coerceIn(60_000L, 1_800_000L)
        assertEquals(1_440_000L, movieSpan) // 24 minutes span

        // Precision gearboxes
        val gearFull = getPrecisionGear(20f)
        val gearHalf = getPrecisionGear(70f)
        val gearQuarter = getPrecisionGear(150f)
        val gearFine = getPrecisionGear(250f)

        assertEquals(1.0f, gearFull, 0.001f)
        assertEquals(0.5f, gearHalf, 0.001f)
        assertEquals(0.25f, gearQuarter, 0.001f)
        assertEquals(0.1f, gearFine, 0.001f)
    }

    private fun getPrecisionGear(verticalDelta: Float): Float {
        return when {
            verticalDelta < 45f -> 1.0f
            verticalDelta < 110f -> 0.5f
            verticalDelta < 200f -> 0.25f
            else -> 0.1f
        }
    }

    @Test
    fun testAffineFocalPointZoomGeometry() {
        val screenWidth = 1080f
        val screenHeight = 2400f
        val oldScale = 1.0f
        val newScale = 2.0f
        val scaleRatio = newScale / oldScale

        val currentCentroidX = 800f
        val currentCentroidY = 1600f

        val screenCenterX = screenWidth / 2f
        val screenCenterY = screenHeight / 2f

        val focusOffsetX = currentCentroidX - screenCenterX
        val focusOffsetY = currentCentroidY - screenCenterY

        val initialPanX = 0f
        val initialPanY = 0f
        val panDeltaX = 0f
        val panDeltaY = 0f

        val newPanX = (initialPanX - focusOffsetX) * scaleRatio + focusOffsetX + panDeltaX
        val newPanY = (initialPanY - focusOffsetY) * scaleRatio + focusOffsetY + panDeltaY

        // Max pan bounds
        val maxPanX = (screenWidth * (newScale - 1f) / 2f)
        val maxPanY = (screenHeight * (newScale - 1f) / 2f)

        val clampedPanX = newPanX.coerceIn(-maxPanX, maxPanX)
        val clampedPanY = newPanY.coerceIn(-maxPanY, maxPanY)

        assertTrue(clampedPanX in -maxPanX..maxPanX)
        assertTrue(clampedPanY in -maxPanY..maxPanY)
    }
}
