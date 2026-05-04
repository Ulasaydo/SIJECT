package com.signlanguage.translator.domain.usecases

import com.signlanguage.translator.data.model.PredictionResult
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SmoothedPredictionUseCaseTest {
    @Test
    fun invoke_acceptsWhenTwoOutOfThreePredictionsMatch() {
        val useCase = SmoothedPredictionUseCase(windowSize = 3)

        useCase(prediction("hello", 0.9f), confidenceThreshold = 0.7f)
        useCase(prediction("bye", 0.8f), confidenceThreshold = 0.7f)
        val result = useCase(prediction("hello", 0.9f), confidenceThreshold = 0.7f)

        assertTrue(result.accepted)
    }

    @Test
    fun invoke_rejectsUntilWindowIsFull() {
        val useCase = SmoothedPredictionUseCase(windowSize = 3)

        useCase(prediction("hello", 0.9f), confidenceThreshold = 0.7f)
        val result = useCase(prediction("hello", 0.9f), confidenceThreshold = 0.7f)

        assertFalse(result.accepted)
    }

    @Test
    fun invoke_rejectsWhenAverageConfidenceIsBelowThreshold() {
        val useCase = SmoothedPredictionUseCase(windowSize = 3)

        useCase(prediction("hello", 0.6f), confidenceThreshold = 0.7f)
        useCase(prediction("bye", 0.8f), confidenceThreshold = 0.7f)
        val result = useCase(prediction("hello", 0.6f), confidenceThreshold = 0.7f)

        assertFalse(result.accepted)
    }

    private fun prediction(label: String, confidence: Float): PredictionResult {
        return PredictionResult(
            label = label,
            confidence = confidence,
            accepted = confidence >= 0.7f
        )
    }
}
