package com.signlanguage.translator.data.model

data class PredictionResult(
    val predictedIndex: Int = -1,
    val label: String,
    val confidence: Float,
    val accepted: Boolean,
    val candidates: List<PredictionCandidate> = emptyList(),
    val allProbabilities: List<Float> = emptyList(),
    val inferenceTimeMillis: Long = 0L
)
