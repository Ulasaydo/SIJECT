package com.signlanguage.translator.domain.services

import com.signlanguage.translator.utils.Constants
import com.signlanguage.translator.utils.LogUtils

/**
 * Maintains the ordered rolling frame window expected by the TFLite sequence model.
 */
class FrameBufferManager(
    private val frameSize: Int = Constants.MODEL_FEATURE_SIZE,
    private val maxFrames: Int = Constants.FRAME_BUFFER_SIZE
) {
    private val buffer = FloatArray(maxFrames * frameSize)
    private var frameCount = 0
    private var currentIndex = 0
    private val frameTimestamps = ArrayDeque<Long>(maxFrames)

    fun addFrame(frame: FloatArray): Boolean {
        require(frame.size == frameSize) {
            "Frame size mismatch: expected $frameSize, got ${frame.size}"
        }

        System.arraycopy(frame, 0, buffer, currentIndex * frameSize, frameSize)
        if (frameTimestamps.size == maxFrames) {
            frameTimestamps.removeFirst()
        }
        frameTimestamps.addLast(System.currentTimeMillis())

        currentIndex = (currentIndex + 1) % maxFrames
        if (frameCount < maxFrames) {
            frameCount += 1
        }

        return isFull()
    }

    fun getBufferOrdered(): FloatArray {
        val result = FloatArray(maxFrames * frameSize)
        if (!isFull()) {
            System.arraycopy(buffer, 0, result, 0, frameCount * frameSize)
            return result
        }

        val oldestIndex = currentIndex
        val tailFrameCount = maxFrames - oldestIndex
        System.arraycopy(
            buffer,
            oldestIndex * frameSize,
            result,
            0,
            tailFrameCount * frameSize
        )
        System.arraycopy(
            buffer,
            0,
            result,
            tailFrameCount * frameSize,
            oldestIndex * frameSize
        )
        return result
    }

    fun calculateFps(): Int {
        if (frameTimestamps.size < 2) return 0
        val elapsed = frameTimestamps.last() - frameTimestamps.first()
        return if (elapsed > 0) (((frameTimestamps.size - 1) * 1000L) / elapsed).toInt() else 0
    }

    fun isFull(): Boolean = frameCount == maxFrames

    fun getFrameCount(): Int = frameCount

    fun reset() {
        buffer.fill(0f)
        frameCount = 0
        currentIndex = 0
        frameTimestamps.clear()
        LogUtils.d("FrameBuffer", "Buffer reset")
    }

    fun clear() {
        reset()
    }
}
