package com.signlanguage.translator.domain.usecases

import com.signlanguage.translator.data.model.PredictionResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SmoothedPredictionUseCaseTest {
    @Test
    fun acceptsAfterFiveConsecutiveSameLabel() {
        val useCase = SmoothedPredictionUseCase(windowSize = 6, requiredConsecutive = 5)

        repeat(4) {
            val r = useCase(prediction("hello", 0.9f), confidenceThreshold = 0.7f)
            assertFalse("only $it consecutive — must not accept yet", r.accepted)
        }
        val accepted = useCase(prediction("hello", 0.9f), confidenceThreshold = 0.7f)
        assertTrue(accepted.accepted)
        assertEquals("hello", accepted.label)
    }

    @Test
    fun rejectsWhenSingleNoiseFrameBreaksConsecutiveStreak() {
        val useCase = SmoothedPredictionUseCase(windowSize = 6, requiredConsecutive = 5)

        useCase(prediction("hello", 0.9f))
        useCase(prediction("hello", 0.9f))
        useCase(prediction("noise", 0.9f)) // breaks streak
        useCase(prediction("hello", 0.9f))
        val result = useCase(prediction("hello", 0.9f), confidenceThreshold = 0.7f)

        assertFalse(result.accepted) // only 2 consecutive after the break
    }

    @Test
    fun rejectsWhenAverageConfidenceBelowThreshold() {
        val useCase = SmoothedPredictionUseCase(windowSize = 6, requiredConsecutive = 3)

        useCase(prediction("hello", 0.5f))
        useCase(prediction("hello", 0.5f))
        val result = useCase(prediction("hello", 0.5f), confidenceThreshold = 0.7f)

        assertFalse(result.accepted)
    }

    @Test
    fun reportsAverageConsecutiveConfidence() {
        val useCase = SmoothedPredictionUseCase(windowSize = 6, requiredConsecutive = 3)

        useCase(prediction("hello", 0.8f))
        useCase(prediction("hello", 0.9f))
        val r = useCase(prediction("hello", 1.0f), confidenceThreshold = 0.5f)

        assertEquals(0.9f, r.confidence, 1e-3f)
        assertTrue(r.accepted)
    }

    @Test
    fun differentLabelResetsStreak() {
        val useCase = SmoothedPredictionUseCase(windowSize = 6, requiredConsecutive = 3)

        useCase(prediction("a", 0.9f))
        useCase(prediction("a", 0.9f))
        val switch = useCase(prediction("b", 0.9f), confidenceThreshold = 0.7f)
        assertFalse(switch.accepted)
        assertEquals("b", switch.label)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsRequiredLargerThanWindow() {
        SmoothedPredictionUseCase(windowSize = 3, requiredConsecutive = 5)
    }

    private fun prediction(label: String, confidence: Float): PredictionResult {
        return PredictionResult(
            label = label,
            confidence = confidence,
            accepted = confidence >= 0.7f
        )
    }
}
