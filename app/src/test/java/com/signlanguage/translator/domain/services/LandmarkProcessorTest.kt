package com.signlanguage.translator.domain.services

import com.signlanguage.translator.utils.Constants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LandmarkProcessorTest {
    private val expected = Constants.SELECTED_LANDMARK_COUNT // 48

    @Test
    fun parseLandmarkIndices_acceptsExactlyExpectedIndices() {
        // Use a contiguous range that fits inside [0, TOTAL_LANDMARK_COUNT) and has size SELECTED.
        val indices = (0 until expected).joinToString(",")
        val parsed = LandmarkProcessor.parseLandmarkIndices("""{"indices":[$indices]}""")

        assertEquals(expected, parsed.size)
        assertEquals(0, parsed.first())
        assertEquals(expected - 1, parsed.last())
    }

    @Test
    fun parseLandmarkIndices_rejectsEmptyIndices() {
        val error = runCatching {
            LandmarkProcessor.parseLandmarkIndices("""{"indices":[]}""")
        }.exceptionOrNull()

        assertTrue(error is IllegalStateException)
        assertTrue(error?.message.orEmpty().contains("expected $expected"))
    }

    @Test
    fun parseLandmarkIndices_rejectsWrongLengthIndices() {
        val indices = (0 until (expected - 1)).joinToString(",")
        val error = runCatching {
            LandmarkProcessor.parseLandmarkIndices("""{"indices":[$indices]}""")
        }.exceptionOrNull()

        assertTrue(error is IllegalStateException)
        assertTrue(error?.message.orEmpty().contains("${expected - 1}"))
    }

    @Test
    fun parseLandmarkIndices_rejectsOutOfRangeIndices() {
        // Build a list of size SELECTED with one out-of-range entry.
        val indices = (0 until (expected - 1)).plus(Constants.TOTAL_LANDMARK_COUNT).joinToString(",")
        val error = runCatching {
            LandmarkProcessor.parseLandmarkIndices("""{"indices":[$indices]}""")
        }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
        assertTrue(error?.message.orEmpty().contains("out-of-range"))
    }
}
