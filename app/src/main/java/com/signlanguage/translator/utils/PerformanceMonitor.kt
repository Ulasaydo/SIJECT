package com.signlanguage.translator.utils

class PerformanceMonitor(
    private val logger: (String, String) -> Unit = LogUtils::d
) {
    private val timings = mutableMapOf<String, Long>()

    fun startTiming(label: String) {
        timings[label] = System.currentTimeMillis()
    }

    fun endTiming(label: String): Long {
        val startedAt = timings.remove(label) ?: return 0L
        val duration = System.currentTimeMillis() - startedAt
        logger("Timing", "$label: ${duration}ms")
        return duration
    }

    fun classifyFrameTime(totalFrameTimeMillis: Long): PerformanceStatus {
        return when {
            totalFrameTimeMillis <= TARGET_TOTAL_FRAME_TIME_MS -> PerformanceStatus.TARGET
            totalFrameTimeMillis <= ACCEPTABLE_TOTAL_FRAME_TIME_MS -> PerformanceStatus.ACCEPTABLE
            else -> PerformanceStatus.CRITICAL
        }
    }

    companion object {
        const val TARGET_FPS = 30
        const val ACCEPTABLE_FPS = 25
        const val CRITICAL_FPS = 20
        const val TARGET_MEDIAPIPE_LATENCY_MS = 40L
        const val ACCEPTABLE_MEDIAPIPE_LATENCY_MS = 50L
        const val TARGET_TFLITE_LATENCY_MS = 80L
        const val ACCEPTABLE_TFLITE_LATENCY_MS = 100L
        const val TARGET_TOTAL_FRAME_TIME_MS = 130L
        const val ACCEPTABLE_TOTAL_FRAME_TIME_MS = 150L
    }
}

enum class PerformanceStatus {
    TARGET,
    ACCEPTABLE,
    CRITICAL
}
