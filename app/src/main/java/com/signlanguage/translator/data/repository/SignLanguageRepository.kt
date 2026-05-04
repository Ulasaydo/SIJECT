package com.signlanguage.translator.data.repository

import com.signlanguage.translator.data.model.PredictionResult
import com.signlanguage.translator.data.model.TranslationResult
import com.signlanguage.translator.data.model.WordHistory

class SignLanguageRepository(
    private val localDataRepository: LocalDataRepository,
    private val remoteDataRepository: RemoteDataRepository
) {
    suspend fun translateWords(words: List<String>): Result<TranslationResult> {
        return remoteDataRepository.translateWords(words)
    }

    suspend fun checkBackendHealth(): Boolean {
        return remoteDataRepository.checkBackendHealth()
    }

    fun persistPrediction(prediction: PredictionResult) {
        localDataRepository.saveLastPrediction(
            WordHistory(
                word = prediction.label,
                confidence = prediction.confidence,
                timestampMillis = System.currentTimeMillis()
            )
        )
    }

    fun getWordHistory(): List<WordHistory> = localDataRepository.getWordHistory()

    fun clearHistory() {
        localDataRepository.clearHistory()
    }
}
