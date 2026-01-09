package com.example.voicenotes.network

import com.example.voicenotes.network.model.OpenAiRequest
import com.example.voicenotes.network.model.OpenAiResponse
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * OpenAI API интерфейс для Retrofit.
 */
interface OpenAiApi {
    
    @POST("v1/chat/completions")
    suspend fun chatCompletion(
        @Header("Authorization") authorization: String,
        @Body request: OpenAiRequest
    ): OpenAiResponse
}
