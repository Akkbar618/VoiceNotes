package com.example.voicenotes.util

import android.content.Context
import com.example.voicenotes.R
import com.example.voicenotes.ai.AiProvider
import com.example.voicenotes.ai.MissingApiKeyException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Sealed class для типизированных ошибок приложения.
 * Каждый тип содержит информацию для отображения пользователю.
 */
sealed class AppError {
    
    // Ошибки сети
    data object NoInternet : AppError()
    data object Timeout : AppError()
    
    // Ошибки API
    data class ApiKeyMissing(val provider: AiProvider) : AppError()
    data object ApiUnauthorized : AppError()
    data object ApiRateLimit : AppError()
    data object ApiServerError : AppError()
    data class ApiGeneric(val message: String?) : AppError()
    
    // Ошибки записи
    data object RecordingStart : AppError()
    data object RecordingStop : AppError()
    data object RecordingPermission : AppError()
    
    // Ошибки данных
    data class DeleteFailed(val cause: String?) : AppError()
    data class UpdateFailed(val cause: String?) : AppError()
    
    // Общие ошибки
    data class Unknown(val message: String?) : AppError()
}

/**
 * Централизованный обработчик ошибок.
 * Преобразует исключения в типизированные ошибки и локализованные сообщения.
 */
object ErrorHandler {
    
    /**
     * Преобразует Exception в типизированную AppError.
     */
    fun fromException(e: Throwable): AppError {
        return when (e) {
            is MissingApiKeyException -> AppError.ApiKeyMissing(e.provider)
            is UnknownHostException -> AppError.NoInternet
            is SocketTimeoutException -> AppError.Timeout
            is IOException -> {
                when {
                    e.message?.contains("timeout", ignoreCase = true) == true -> AppError.Timeout
                    else -> AppError.NoInternet
                }
            }
            else -> {
                val message = e.message ?: ""
                when {
                    message.contains("401") -> AppError.ApiUnauthorized
                    message.contains("429") -> AppError.ApiRateLimit
                    message.contains("500") || message.contains("503") -> AppError.ApiServerError
                    else -> AppError.Unknown(e.message)
                }
            }
        }
    }
    
    /**
     * Преобразует AppError в локализованное сообщение для пользователя.
     */
    fun getLocalizedMessage(context: Context, error: AppError): String {
        return when (error) {
            // Сеть
            is AppError.NoInternet -> context.getString(R.string.error_no_internet)
            is AppError.Timeout -> context.getString(R.string.error_timeout)
            
            // API
            is AppError.ApiKeyMissing -> {
                val providerName = when (error.provider) {
                    AiProvider.GEMINI -> context.getString(R.string.settings_provider_gemini)
                    AiProvider.OPENAI -> context.getString(R.string.settings_provider_openai)
                }
                context.getString(R.string.error_api_key_missing, providerName)
            }
            is AppError.ApiUnauthorized -> context.getString(R.string.error_api_unauthorized)
            is AppError.ApiRateLimit -> context.getString(R.string.error_api_rate_limit)
            is AppError.ApiServerError -> context.getString(R.string.error_api_server)
            is AppError.ApiGeneric -> error.message ?: context.getString(R.string.error_unknown)
            
            // Запись
            is AppError.RecordingStart -> context.getString(R.string.error_recording_start)
            is AppError.RecordingStop -> context.getString(R.string.error_recording_stop)
            is AppError.RecordingPermission -> context.getString(R.string.error_recording_permission)
            
            // Данные
            is AppError.DeleteFailed -> context.getString(R.string.error_delete_failed)
            is AppError.UpdateFailed -> context.getString(R.string.error_update_failed)
            
            // Общие
            is AppError.Unknown -> error.message ?: context.getString(R.string.error_unknown)
        }
    }
}

/**
 * Extension для MissingApiKeyException для хранения провайдера.
 */
val MissingApiKeyException.provider: AiProvider
    get() = when {
        message?.contains("GEMINI") == true -> AiProvider.GEMINI
        message?.contains("OPENAI") == true -> AiProvider.OPENAI
        else -> AiProvider.GEMINI
    }
