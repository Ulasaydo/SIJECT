package com.signlanguage.translator.domain.usecases

import com.signlanguage.translator.data.model.TranslationResult
import com.signlanguage.translator.data.repository.SignLanguageRepository

/**
 * Sends accepted word sequences to the repository for backend translation with fallback support.
 */
class TranslateWordsUseCase(
    private val repository: SignLanguageRepository
) {
    suspend operator fun invoke(words: List<String>): Result<TranslationResult> {
        return repository.translateWords(words)
    }
}
