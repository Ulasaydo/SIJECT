package com.signlanguage.translator.domain.services

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PredictionProcessorTest {
    @Test
    fun processPrediction_returnsArgmaxLabelWhenConfidencePassesThreshold() {
        val output = floatArrayOf(0.1f, 0.8f, 0.2f)
        val labels = listOf("hello", "drink", "book")

        val result = PredictionProcessor.processPrediction(output, labels, confidenceThreshold = 0.7f)

        assertEquals(1, result.predictedIndex)
        assertEquals("drink", result.label)
        assertEquals(0.8f, result.confidence, 0.0001f)
        assertTrue(result.accepted)
    }

    @Test
    fun processPrediction_marksUncertainWhenBelowThreshold() {
        val output = floatArrayOf(0.1f, 0.6f, 0.2f)
        val labels = listOf("hello", "drink", "book")

        val result = PredictionProcessor.processPrediction(output, labels, confidenceThreshold = 0.7f)

        assertEquals("Uncertain", result.label)
        assertFalse(result.accepted)
    }

    @Test
    fun processPrediction_returnsTopThreeCandidatesSorted() {
        val output = floatArrayOf(0.2f, 0.6f, 0.4f, 0.1f)
        val labels = listOf("a", "b", "c", "d")

        val result = PredictionProcessor.processPrediction(output, labels, confidenceThreshold = 0.1f)

        assertEquals(listOf("b", "c", "a"), result.candidates.map { it.label })
    }
}
