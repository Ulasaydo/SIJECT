package com.signlanguage.translator.data.model

import com.google.gson.annotations.SerializedName

data class TranslationRequest(
    @SerializedName("words")
    val words: List<String>,
    @SerializedName("language")
    val language: String = "tr"
)
