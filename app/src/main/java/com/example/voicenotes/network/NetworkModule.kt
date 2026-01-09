package com.example.voicenotes.network

import com.example.voicenotes.BuildConfig
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Синглтон для создания и настройки сетевого клиента Gemini API.
 */
object NetworkModule {

    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    /**
     * API ключ из BuildConfig.
     */
    val apiKey: String = BuildConfig.GEMINI_API_KEY

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = false
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
    val api: GeminiApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(GeminiApi::class.java)
    }
}
