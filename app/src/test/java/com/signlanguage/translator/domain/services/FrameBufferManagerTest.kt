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
        assertEquals(144, Constants.MODEL_FEATURE_SIZE)
        assertEquals(48, Constants.SELECTED_LANDMARK_COUNT)
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

    @Test
    fun getBufferOrdered_prePadsWhenNotFull() {
        val manager = FrameBufferManager(frameSize = 2, maxFrames = 3)

        manager.addFrame(floatArrayOf(1f, 2f))
        manager.addFrame(floatArrayOf(3f, 4f))

        // 2 frames in a 3-frame window → leading slot zero-padded, real frames at the end.
        assertArrayEquals(
            floatArrayOf(0f, 0f, 1f, 2f, 3f, 4f),
            manager.getBufferOrdered(),
            0.0001f
        )
    }

    @Test
    fun sampleUniform_picksEvenlySpacedFrames() {
        val frames = (0 until 9).map { i -> floatArrayOf(i.toFloat()) }

        val sampled = FrameBufferManager.sampleUniform(frames, 3)

        assertEquals(3, sampled.size)
        // (i * 9) / 3 = 0, 3, 6
        assertEquals(0f, sampled[0][0], 0f)
        assertEquals(3f, sampled[1][0], 0f)
        assertEquals(6f, sampled[2][0], 0f)
    }

    @Test
    fun sampleUniform_returnsAsIsWhenShorterOrEqual() {
        val frames = listOf(floatArrayOf(1f), floatArrayOf(2f))
        assertEquals(frames, FrameBufferManager.sampleUniform(frames, 5))
        assertEquals(frames, FrameBufferManager.sampleUniform(frames, 2))
    }

    @Test(expected = IllegalArgumentException::class)
    fun addFrame_rejectsWrongFrameSize() {
        val manager = FrameBufferManager(frameSize = 2, maxFrames = 3)

        manager.addFrame(floatArrayOf(1f))
    }
}
