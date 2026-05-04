package com.signlanguage.translator.data.model

data class WordHistory(
    val word: String,
    val confidence: Float,
    val timestampMillis: Long
)
