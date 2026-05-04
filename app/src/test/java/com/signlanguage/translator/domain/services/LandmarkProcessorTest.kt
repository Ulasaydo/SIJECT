package com.signlanguage.translator.domain.services

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LandmarkProcessorTest {
    @Test
    fun parseLandmarkIndices_acceptsExactlyExpectedIndices() {
        val indices = (0 until 543).joinToString(",")
        val parsed = LandmarkProcessor.parseLandmarkIndices("""{"indices":[$indices]}""")

        assertEquals(543, parsed.size)
        assertEquals(0, parsed.first())
        assertEquals(542, parsed.last())
    }

    @Test
    fun parseLandmarkIndices_rejectsEmptyIndices() {
        val error = runCatching {
            LandmarkProcessor.parseLandmarkIndices("""{"indices":[]}""")
        }.exceptionOrNull()

        assertTrue(error is IllegalStateException)
        assertTrue(error?.message.orEmpty().contains("expected 543"))
    }

    @Test
    fun parseLandmarkIndices_rejectsWrongLengthIndices() {
        val indices = (0 until 542).joinToString(",")
        val error = runCatching {
            LandmarkProcessor.parseLandmarkIndices("""{"indices":[$indices]}""")
        }.exceptionOrNull()

        assertTrue(error is IllegalStateException)
        assertTrue(error?.message.orEmpty().contains("542"))
    }

    @Test
    fun parseLandmarkIndices_rejectsOutOfRangeIndices() {
        val indices = (0 until 542).plus(543).joinToString(",")
        val error = runCatching {
            LandmarkProcessor.parseLandmarkIndices("""{"indices":[$indices]}""")
        }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
        assertTrue(error?.message.orEmpty().contains("out-of-range"))
    }
}
