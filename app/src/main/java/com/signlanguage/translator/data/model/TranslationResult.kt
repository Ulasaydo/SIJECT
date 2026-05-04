package com.signlanguage.translator.data.model

data class TranslationResult(
    val sentence: String,
    val confidence: Float = 0f,
    val alternatives: List<String> = emptyList(),
    val fromFallback: Boolean = false
)
