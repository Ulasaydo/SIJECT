package com.signlanguage.translator.data.model

import com.google.gson.annotations.SerializedName

data class TranslationResponse(
    @SerializedName("sentence")
    val sentence: String,
    @SerializedName("confidence")
    val confidence: Float = 0f,
    @SerializedName("alternatives")
    val alternatives: List<String> = emptyList()
)
