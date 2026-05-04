package com.signlanguage.translator.domain.services

import com.signlanguage.translator.data.model.TranslationRequest
import com.signlanguage.translator.data.model.TranslationResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * Retrofit contract for the backend translation service used by the mobile pipeline.
 */
interface APIService {
    @POST("/api/translate")
    suspend fun translateWords(@Body request: TranslationRequest): Response<TranslationResponse>

    @GET("/api/health")
    suspend fun checkHealth(): Response<String>
}
