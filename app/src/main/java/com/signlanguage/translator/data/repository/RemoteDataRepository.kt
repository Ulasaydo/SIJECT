package com.signlanguage.translator.data.repository

import com.signlanguage.translator.data.model.TranslationRequest
import com.signlanguage.translator.data.model.TranslationResult
import com.signlanguage.translator.domain.services.APIService
import com.signlanguage.translator.utils.LogUtils
import java.net.ConnectException
import java.net.SocketTimeoutException

class RemoteDataRepository(
    private val apiService: APIService
) {
    suspend fun translateWords(words: List<String>): Result<TranslationResult> {
        if (words.isEmpty()) {
            return Result.success(TranslationResult(sentence = ""))
        }

        return try {
            val response = apiService.translateWords(TranslationRequest(words = words, language = "tr"))
            when {
                response.isSuccessful && response.body() != null -> {
                    val body = response.body()!!
                    LogUtils.d("API", "Translation: ${body.sentence}")
                    Result.success(
                        TranslationResult(
                            sentence = body.sentence,
                            confidence = body.confidence,
                            alternatives = body.alternatives
                        )
                    )
                }
                response.code() == 400 -> Result.failure(Exception("Invalid request: ${response.message()}"))
                response.code() >= 500 -> Result.failure(Exception("Server error: ${response.message()}"))
                else -> Result.failure(Exception("Unknown API error: ${response.message()}"))
            }
        } catch (exception: SocketTimeoutException) {
            LogUtils.e("API", "Request timeout", exception)
            Result.failure(Exception("Request timeout"))
        } catch (exception: ConnectException) {
            LogUtils.e("API", "Connection failed", exception)
            Result.failure(Exception("No internet connection"))
        } catch (exception: Exception) {
            LogUtils.e("API", "Translation failed", exception)
            Result.failure(exception)
        }
    }

    suspend fun checkBackendHealth(): Boolean {
        return try {
            apiService.checkHealth().isSuccessful
        } catch (exception: Exception) {
            LogUtils.e("API", "Health check failed", exception)
            false
        }
    }
}
