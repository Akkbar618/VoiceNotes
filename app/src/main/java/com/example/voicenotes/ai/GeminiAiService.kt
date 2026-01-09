package com.example.voicenotes.ai

import com.example.voicenotes.network.GeminiApi
import com.example.voicenotes.network.model.GeminiContent
import com.example.voicenotes.network.model.GeminiPart
import com.example.voicenotes.network.model.GeminiRequest
import com.example.voicenotes.network.model.InlineData
import org.json.JSONObject
import javax.inject.Inject

/**
 * Реализация AiService для Google Gemini API.
 * Умеет работать с аудио файлами напрямую (multimodal).
 */
class GeminiAiService @Inject constructor(
    private val api: GeminiApi
) {
    /**
     * Анализирует аудио: транскрибирует, генерирует заголовок и саммари.
     */
    suspend fun analyzeAudio(audioBase64: String, mimeType: String, apiKey: String): AiResponse {
        // 1. Транскрибируем аудио
        val rawText = transcribeAudio(audioBase64, mimeType, apiKey)
        
        // 2. Генерируем заголовок и саммари
        val (title, summary) = generateTitleAndSummary(rawText, apiKey)
        
        return AiResponse(
            title = title,
            summary = summary,
            rawText = rawText
        )
    }
    
    /**
     * Транскрибирует аудио в текст.
     */
    private suspend fun transcribeAudio(audioBase64: String, mimeType: String, apiKey: String): String {
        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(
                        GeminiPart(
                            text = """
                                Transcribe this audio to text accurately.
                                Return ONLY the transcription, nothing else.
                                Keep the original language of the audio.
                            """.trimIndent()
                        ),
                        GeminiPart(
                            inlineData = InlineData(
                                mimeType = mimeType,
                                data = audioBase64
                            )
                        )
                    )
                )
            )
        )
        
        val response = api.generateContent(apiKey, request)
        
        return response.candidates?.firstOrNull()
            ?.content?.parts?.firstOrNull()?.text
            ?: throw Exception("Failed to transcribe audio: empty response")
    }
    
    /**
     * Генерирует заголовок и саммари из текста.
     */
    private suspend fun generateTitleAndSummary(text: String, apiKey: String): Pair<String, String> {
        val systemPrompt = """
            Ты — помощник для обработки голосовых заметок.
            Твоя задача — извлечь смысл из текста.
            
            Верни ответ СТРОГО в формате JSON БЕЗ markdown разметки:
            {"title": "Короткий заголовок (максимум 4-5 слов)", "summary": "Краткая выжимка (2-3 предложения, без буллитов и звездочек)"}
            
            ВАЖНО:
            - Не используй жирный шрифт, звездочки ** или спецсимволы
            - Не используй markdown форматирование
            - Не добавляй пояснения — только JSON
            - Язык ответа: тот же, что и входной текст
        """.trimIndent()
        
        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(
                        GeminiPart(text = "$systemPrompt\n\n---\n\nТекст для обработки:\n$text")
                    )
                )
            )
        )
        
        val response = api.generateContent(apiKey, request)
        
        val jsonText = response.candidates?.firstOrNull()
            ?.content?.parts?.firstOrNull()?.text
            ?: throw Exception("Failed to generate summary: empty response")
        
        return parseAiResponse(jsonText)
    }
    
    /**
     * Парсит JSON ответ от AI.
     */
    private fun parseAiResponse(jsonText: String): Pair<String, String> {
        return try {
            val cleanJson = jsonText
                .replace("```json", "")
                .replace("```", "")
                .trim()
            
            val jsonObject = JSONObject(cleanJson)
            Pair(
                jsonObject.optString("title", "Заметка"),
                jsonObject.optString("summary", cleanJson)
            )
        } catch (e: Exception) {
            Pair("Заметка", jsonText.take(200))
        }
    }
}
