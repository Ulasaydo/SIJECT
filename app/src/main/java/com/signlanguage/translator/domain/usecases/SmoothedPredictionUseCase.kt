package com.signlanguage.translator.domain.usecases

import com.signlanguage.translator.data.model.PredictionResult
import com.signlanguage.translator.utils.Constants

/**
 * Accepts a prediction only after [requiredConsecutive] back-to-back frames return
 * the same label. This is a stability counter — single-frame noise can never flip
 * the accepted state.
 *
 * The reported confidence is the mean confidence across the consecutive same-label
 * tail, so brief jitters do not blow up the average.
 */
class SmoothedPredictionUseCase(
    private val windowSize: Int = Constants.SMOOTHING_WINDOW_SIZE,
    private val requiredConsecutive: Int = Constants.SMOOTHING_REQUIRED_CONSECUTIVE
) {
    init {
        require(requiredConsecutive in 1..windowSize) {
            "requiredConsecutive ($requiredConsecutive) must be in 1..$windowSize"
        }
    }

    private val recentPredictions = ArrayDeque<PredictionResult>(windowSize)

    operator fun invoke(
        prediction: PredictionResult,
        confidenceThreshold: Float = Constants.CONFIDENCE_THRESHOLD
    ): PredictionResult {
        if (recentPredictions.size == windowSize) {
            recentPredictions.removeFirst()
        }
        recentPredictions.addLast(prediction)

        // Walk from the newest backwards, count consecutive same-label predictions.
        val tail = mutableListOf<PredictionResult>()
        for (p in recentPredictions.reversed()) {
            if (p.label == prediction.label) tail += p else break
        }

        val averageConfidence = tail.map { it.confidence }.average().toFloat()
        val accepted = tail.size >= requiredConsecutive &&
            averageConfidence >= confidenceThreshold

        return prediction.copy(
            confidence = averageConfidence,
            accepted = accepted
        )
    }
}
