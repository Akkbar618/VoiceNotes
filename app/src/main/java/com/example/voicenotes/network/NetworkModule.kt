package com.example.voicenotes.network

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Синглтон для создания и настройки сетевых клиентов.
 */
object NetworkModule {

    private const val GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/"
    private const val OPENAI_BASE_URL = "https://api.openai.com/"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    /**
     * Interceptor для логирования запросов и ответов.
     */
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    /**
     * Настроенный OkHttpClient.
     * Увеличенные таймауты для обработки больших аудио файлов.
     */
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    /**
     * Экземпляр GeminiApi для выполнения запросов.
     */
    val geminiApi: GeminiApi by lazy {
        Retrofit.Builder()
            .baseUrl(GEMINI_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(GeminiApi::class.java)
    }

    /**
     * Экземпляр OpenAiApi для выполнения запросов.
     */
    val openAiApi: OpenAiApi by lazy {
        Retrofit.Builder()
            .baseUrl(OPENAI_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(OpenAiApi::class.java)
    }
}
