package com.signlanguage.translator.domain.usecases

import com.signlanguage.translator.data.model.PredictionResult
import com.signlanguage.translator.utils.Constants

/**
 * Accepts predictions only after a short majority window to reduce frame-to-frame jitter.
 */
class SmoothedPredictionUseCase(
    private val windowSize: Int = Constants.SMOOTHING_WINDOW_SIZE
) {
    private val recentPredictions = ArrayDeque<PredictionResult>(windowSize)

    operator fun invoke(
        prediction: PredictionResult,
        confidenceThreshold: Float = Constants.CONFIDENCE_THRESHOLD
    ): PredictionResult {
        if (recentPredictions.size == windowSize) {
            recentPredictions.removeFirst()
        }
        recentPredictions.addLast(prediction)

        val sameLabel = recentPredictions.filter { it.label == prediction.label }
        val averageConfidence = sameLabel.map { it.confidence }.average().toFloat()
        val requiredVotes = ((windowSize * 2) + 2) / 3
        return prediction.copy(
            confidence = averageConfidence,
            accepted = recentPredictions.size == windowSize &&
                sameLabel.size >= requiredVotes &&
                averageConfidence >= confidenceThreshold
        )
    }
}
