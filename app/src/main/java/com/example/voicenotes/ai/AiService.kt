package com.example.voicenotes.ai

/**
 * Enum провайдеров AI.
 */
enum class AiProvider {
    GEMINI,
    OPENAI
}

/**
 * Унифицированный ответ от AI сервиса.
 */
data class AiResponse(
    val title: String,
    val summary: String,
    val rawText: String
)

/**
 * Интерфейс AI сервиса (Strategy Pattern).
 * Позволяет абстрагироваться от конкретного провайдера.
 */
interface AiService {
    /**
     * Анализирует аудио файл и возвращает транскрипцию, заголовок и саммари.
     * @param audioBase64 Аудио файл в формате Base64
     * @param mimeType MIME тип аудио (например, "audio/mp4")
     * @return Результат анализа с заголовком, саммари и транскрипцией
     */
    suspend fun analyzeAudio(audioBase64: String, mimeType: String): AiResponse
}

/**
 * Исключение при отсутствии API ключа.
 */
class MissingApiKeyException(provider: AiProvider) : 
    Exception("API key for ${provider.name} is not configured")
