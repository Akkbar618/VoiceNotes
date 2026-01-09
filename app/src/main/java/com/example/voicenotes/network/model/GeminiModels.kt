package com.example.voicenotes.network.model

import kotlinx.serialization.Serializable

/**
 * Запрос к Gemini API.
 */
@Serializable
data class GeminiRequest(
    val contents: List<GeminiContent>
)

/**
 * Контент запроса/ответа (может содержать несколько частей).
 */
@Serializable
data class GeminiContent(
    val parts: List<GeminiPart>,
    val role: String? = null
)

/**
 * Часть контента (текст или inline данные).
 */
@Serializable
data class GeminiPart(
    val text: String? = null,
    val inlineData: InlineData? = null
)

/**
 * Inline данные (например, аудио в base64).
 */
@Serializable
data class InlineData(
    val mimeType: String,
    val data: String  // base64 encoded
)

/**
 * Ответ от Gemini API.
 */
@Serializable
data class GeminiResponse(
    val candidates: List<GeminiCandidate>? = null,
    val error: GeminiError? = null
)

/**
 * Кандидат ответа.
 */
@Serializable
data class GeminiCandidate(
    val content: GeminiContent
)

/**
 * Ошибка от API.
 */
@Serializable
data class GeminiError(
    val code: Int,
    val message: String,
    val status: String
)

/**
 * Вспомогательные функции для создания запросов.
 */
object GeminiRequestBuilder {
    
    /**
     * Создаёт запрос для транскрипции аудио.
     * @param audioBase64 Аудио в формате base64
     * @param mimeType MIME тип аудио (например "audio/mp3")
     */
    fun transcribeAudio(audioBase64: String, mimeType: String = "audio/mp3"): GeminiRequest {
        return GeminiRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(
                        GeminiPart(text = "Transcribe this audio to text. Return only the transcription, nothing else."),
                        GeminiPart(inlineData = InlineData(mimeType = mimeType, data = audioBase64))
                    )
                )
            )
        )
    }
    
    /**
     * Создаёт запрос для генерации саммари текста.
     * @param text Текст для обработки
     * @param prompt Инструкция (по умолчанию - сделать саммари)
     */
    fun generateSummary(text: String, prompt: String = "Summarize this text concisely:"): GeminiRequest {
        return GeminiRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(
                        GeminiPart(text = "$prompt\n\n$text")
                    )
                )
            )
        )
    }
}
