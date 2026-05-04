package com.signlanguage.translator.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class PerformanceMonitorTest {
    @Test
    fun classifyFrameTime_usesDocumentedThresholds() {
        val monitor = PerformanceMonitor { _, _ -> }

        assertEquals(PerformanceStatus.TARGET, monitor.classifyFrameTime(130L))
        assertEquals(PerformanceStatus.ACCEPTABLE, monitor.classifyFrameTime(150L))
        assertEquals(PerformanceStatus.CRITICAL, monitor.classifyFrameTime(151L))
    }
}
