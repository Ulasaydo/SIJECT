package com.signlanguage.translator.domain.services

import com.signlanguage.translator.data.model.LandmarkPoint
import com.signlanguage.translator.utils.Constants
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionFilterTest {
    private fun stillFrame(seed: Float): List<LandmarkPoint> {
        return List(Constants.TOTAL_LANDMARK_COUNT) { i ->
            LandmarkPoint(x = seed + i * 1e-6f, y = seed - i * 1e-6f, z = 0f, visibility = 1f)
        }
    }

    private fun movedFrame(seed: Float, delta: Float): List<LandmarkPoint> {
        return List(Constants.TOTAL_LANDMARK_COUNT) { i ->
            LandmarkPoint(x = seed + i * 1e-6f + delta, y = seed - i * 1e-6f, z = 0f, visibility = 1f)
        }
    }

    @Test
    fun warmupFrames_alwaysPass() {
        val filter = MotionFilter(displacementThreshold = 0.005f, historyWindow = 5)
        repeat(5) {
            assertTrue("warmup frame $it should pass", filter.shouldProcess(stillFrame(0.5f)))
        }
    }

    @Test
    fun stillHands_rejectAfterWarmup() {
        val filter = MotionFilter(displacementThreshold = 0.005f, historyWindow = 5)
        repeat(5) { filter.shouldProcess(stillFrame(0.5f)) }
        assertFalse(filter.shouldProcess(stillFrame(0.5f)))
    }

    @Test
    fun movingHands_passAfterWarmup() {
        val filter = MotionFilter(displacementThreshold = 0.005f, historyWindow = 5)
        repeat(5) { filter.shouldProcess(stillFrame(0.5f)) }
        assertTrue(filter.shouldProcess(movedFrame(0.5f, 0.05f)))
    }

    @Test
    fun reset_clearsHistory() {
        val filter = MotionFilter(displacementThreshold = 0.005f, historyWindow = 5)
        repeat(5) { filter.shouldProcess(stillFrame(0.5f)) }
        filter.reset()
        assertTrue("after reset, first frame is warmup again", filter.shouldProcess(stillFrame(0.5f)))
    }
}
