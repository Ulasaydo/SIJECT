package com.signlanguage.translator.ui.viewmodels

import com.signlanguage.translator.data.model.LandmarkPoint
import com.signlanguage.translator.data.model.PredictionCandidate

data class TranslationUiState(
    val recognizedWords: List<PredictionCandidate> = emptyList(),
    val translatedSentence: String = "",
    val isLoading: Boolean = false,
    val apiError: String? = null,
    val backendOnline: Boolean? = null,
    val translationConfidence: Float = 0f,
    val alternatives: List<String> = emptyList(),
    val landmarks: List<LandmarkPoint> = emptyList(),
    val sourceImageWidth: Int = 0,
    val sourceImageHeight: Int = 0,
    val errorMessage: String? = null,
    val fps: Int = 0,
    val landmarkTimeMillis: Long = 0L,
    val inferenceTimeMillis: Long = 0L,
    val debugMode: Boolean = true,
    val showLandmarkOverlay: Boolean = true,
    val showFpsCounter: Boolean = true
)
