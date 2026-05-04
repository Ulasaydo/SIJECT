package com.signlanguage.translator.domain.services

import com.signlanguage.translator.data.model.PredictionCandidate
import com.signlanguage.translator.data.model.PredictionResult
import com.signlanguage.translator.utils.Constants
import com.signlanguage.translator.utils.LogUtils

/**
 * Converts raw model probabilities into ranked candidates and accepted predictions.
 */
object PredictionProcessor {
    fun processPrediction(
        output: FloatArray,
        labels: List<String>,
        confidenceThreshold: Float = Constants.CONFIDENCE_THRESHOLD,
        inferenceTimeMillis: Long = 0L,
        debugMode: Boolean = false
    ): PredictionResult {
        if (output.isEmpty() || labels.isEmpty()) {
            return PredictionResult(label = "Model Not Loaded", confidence = 0f, accepted = false)
        }

        val candidates = output.withIndex()
            .sortedByDescending { it.value }
            .take(3)
            .map { indexedValue ->
                PredictionCandidate(
                    label = labels.getOrElse(indexedValue.index) { "class_${indexedValue.index}" },
                    confidence = indexedValue.value.coerceIn(0f, 1f)
                )
            }
        val predictedIndex = output.indices.maxByOrNull { output[it] } ?: -1
        val confidence = output.getOrElse(predictedIndex) { 0f }.coerceIn(0f, 1f)
        val label = labels.getOrElse(predictedIndex) { "Index Out of Range" }

        if (debugMode) {
            LogUtils.d(
                "Prediction",
                candidates.joinToString(" | ") { "${it.label}: ${(it.confidence * 100).toInt()}%" }
            )
        }

        return PredictionResult(
            predictedIndex = predictedIndex,
            label = if (confidence >= confidenceThreshold) label else "Uncertain",
            confidence = confidence,
            accepted = confidence >= confidenceThreshold && predictedIndex in labels.indices,
            candidates = candidates,
            allProbabilities = output.toList(),
            inferenceTimeMillis = inferenceTimeMillis
        )
    }
}
