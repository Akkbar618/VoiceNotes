package com.example.voicenotes.network

import com.example.voicenotes.network.model.GeminiRequest
import com.example.voicenotes.network.model.GeminiResponse
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Retrofit интерфейс для Google Gemini API.
 */
interface GeminiApi {

    /**
     * Генерирует контент через Gemini API.
     * Используется для транскрипции аудио и генерации саммари.
     * 
     * @param apiKey API ключ (передаётся как query parameter)
     * @param request Запрос с контентом
     * @return Ответ с сгенерированным текстом
     */
    @POST("v1beta/models/gemini-2.0-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
    
    /**
     * Альтернативный метод с моделью gemini-1.5-pro (более качественная, но медленнее).
     */
    @POST("v1beta/models/gemini-1.5-pro-latest:generateContent")
    suspend fun generateContentPro(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}
