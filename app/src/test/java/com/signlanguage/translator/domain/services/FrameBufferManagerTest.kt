package com.signlanguage.translator.domain.services

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import com.signlanguage.translator.utils.Constants

class FrameBufferManagerTest {
    @Test
    fun defaultFrameSize_matchesModelFeatureContract() {
        assertEquals(1629, Constants.MODEL_FEATURE_SIZE)
        assertEquals(543, Constants.SELECTED_LANDMARK_COUNT)
        assertEquals(250, Constants.MODEL_OUTPUT_CLASSES)

        val manager = FrameBufferManager()
        val frame = FloatArray(Constants.MODEL_FEATURE_SIZE) { it.toFloat() }

        assertFalse(manager.addFrame(frame))
        assertEquals(1, manager.getFrameCount())
    }

    @Test
    fun addFrame_returnsFullOnlyAfterCapacity() {
        val manager = FrameBufferManager(frameSize = 2, maxFrames = 3)

        assertFalse(manager.addFrame(floatArrayOf(1f, 2f)))
        assertFalse(manager.addFrame(floatArrayOf(3f, 4f)))
        assertTrue(manager.addFrame(floatArrayOf(5f, 6f)))
        assertEquals(3, manager.getFrameCount())
    }

    @Test
    fun getBufferOrdered_returnsChronologicalFramesAfterWrap() {
        val manager = FrameBufferManager(frameSize = 2, maxFrames = 3)

        manager.addFrame(floatArrayOf(1f, 2f))
        manager.addFrame(floatArrayOf(3f, 4f))
        manager.addFrame(floatArrayOf(5f, 6f))
        manager.addFrame(floatArrayOf(7f, 8f))

        assertArrayEquals(
            floatArrayOf(3f, 4f, 5f, 6f, 7f, 8f),
            manager.getBufferOrdered(),
            0.0001f
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun addFrame_rejectsWrongFrameSize() {
        val manager = FrameBufferManager(frameSize = 2, maxFrames = 3)

        manager.addFrame(floatArrayOf(1f))
    }
}
