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

    /**
     * Returns the rolling 30-frame window in chronological order.
     *
     * Conv1D model expects a fixed [maxFrames] x [frameSize] block on every invoke.
     * - Warmup (frameCount < maxFrames): zero-pad at the START (pre-pad) so real frames
     *   land at the END of the window. This matches the training convention where short
     *   sequences are left-padded.
     * - Full buffer: chronological order from oldest to newest (circular unroll).
     */
    fun getBufferOrdered(): FloatArray {
        val result = FloatArray(maxFrames * frameSize)
        if (!isFull()) {
            // Pre-pad: leading (maxFrames - frameCount) frames remain zero, real frames at end.
            val destOffset = (maxFrames - frameCount) * frameSize
            System.arraycopy(buffer, 0, result, destOffset, frameCount * frameSize)
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

    companion object {
        /**
         * Uniformly samples [target] frames from [frames] using evenly spaced indices.
         * Used for offline replay / long-gesture paths; the live pipeline keeps the
         * sliding window in [getBufferOrdered]. Returns frames as-is when count == target.
         */
        fun sampleUniform(frames: List<FloatArray>, target: Int): List<FloatArray> {
            if (frames.isEmpty() || target <= 0) return emptyList()
            if (frames.size == target) return frames
            if (frames.size < target) return frames
            val n = frames.size
            return List(target) { i ->
                // floor((i * n) / target) yields evenly spaced indices in [0, n).
                val idx = ((i.toLong() * n) / target).toInt().coerceIn(0, n - 1)
                frames[idx]
            }
        }
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
