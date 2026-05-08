package com.signlanguage.translator.domain.services

import com.signlanguage.translator.data.model.LandmarkPoint
import kotlin.math.abs

/**
 * Detects whether the hands have moved enough to justify running expensive
 * inference. Compares the latest hand-landmark snapshot against the last
 * [historyWindow] snapshots; if the maximum mean-absolute displacement across
 * any of them is below [displacementThreshold] (in normalized [0,1] coords),
 * the frame is considered a "null gesture" and inference can be skipped.
 *
 * Hand landmarks (LH and RH) are the signal carrier for ASL gestures, so
 * pose displacement is intentionally ignored.
 */
class MotionFilter(
    private val handLandmarkRanges: List<IntRange> = DEFAULT_HAND_RANGES,
    private val displacementThreshold: Float = DEFAULT_THRESHOLD,
    private val historyWindow: Int = DEFAULT_HISTORY
) {
    private val recentSnapshots = ArrayDeque<FloatArray>(historyWindow)

    /**
     * @return `true` if motion is sufficient (or history not yet warm) and the
     *         frame should run through prediction; `false` to skip inference.
     */
    fun shouldProcess(landmarks: List<LandmarkPoint>): Boolean {
        val flat = flattenHands(landmarks)
        if (recentSnapshots.size < historyWindow) {
            recentSnapshots.addLast(flat)
            return true
        }
        val maxDisp = recentSnapshots.maxOf { prev -> meanAbsDiff(prev, flat) }
        recentSnapshots.removeFirst()
        recentSnapshots.addLast(flat)
        return maxDisp >= displacementThreshold
    }

    fun reset() {
        recentSnapshots.clear()
    }

    private fun flattenHands(landmarks: List<LandmarkPoint>): FloatArray {
        val totalPoints = handLandmarkRanges.sumOf { it.count() }
        val out = FloatArray(totalPoints * 2) // x, y only — z is noisy
        var idx = 0
        for (range in handLandmarkRanges) {
            for (i in range) {
                val p = landmarks.getOrNull(i)
                out[idx++] = p?.x ?: 0f
                out[idx++] = p?.y ?: 0f
            }
        }
        return out
    }

    private fun meanAbsDiff(a: FloatArray, b: FloatArray): Float {
        if (a.size != b.size || a.isEmpty()) return 0f
        var sum = 0.0
        for (i in a.indices) sum += abs(a[i] - b[i])
        return (sum / a.size).toFloat()
    }

    companion object {
        private val DEFAULT_HAND_RANGES = listOf(501..521, 522..542)
        private const val DEFAULT_THRESHOLD = 0.005f
        private const val DEFAULT_HISTORY = 5
    }
}
