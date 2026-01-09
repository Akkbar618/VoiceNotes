package com.example.voicenotes.ai

import android.util.Base64
import com.example.voicenotes.network.OpenAiApi
import com.example.voicenotes.network.model.OpenAiMessage
import com.example.voicenotes.network.model.OpenAiRequest
import org.json.JSONObject
import javax.inject.Inject

/**
 * Реализация AI сервиса для OpenAI (GPT-4o-mini).
 * 
 * Важно: OpenAI НЕ поддерживает аудио напрямую в chat completions.
 * Поэтому для OpenAI нужно сначала транскрибировать через Whisper,
 * но для упрощения мы будем отправлять уже транскрибированный текст.
 * 
 * В текущей реализации: для аудио используем Gemini (multimodal),
 * а OpenAI — только для текстовой генерации заголовка/саммари.
 */
class OpenAiService @Inject constructor(
    private val api: OpenAiApi
) {
    companion object {
        private const val MODEL = "gpt-4o-mini"
    }
    
    /**
     * Генерирует заголовок и саммари для текста.
     * Используется когда транскрипция уже есть.
     */
    suspend fun generateTitleAndSummary(text: String, apiKey: String): AiResponse {
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
        
        val request = OpenAiRequest(
            model = MODEL,
            messages = listOf(
                OpenAiMessage(role = "system", content = systemPrompt),
                OpenAiMessage(role = "user", content = "Текст для обработки:\n$text")
            )
        )
        
        val response = api.chatCompletion("Bearer $apiKey", request)
        
        // Проверяем ошибку
        response.error?.let {
            throw Exception("OpenAI Error: ${it.message ?: it.code}")
        }
        
        val jsonText = response.choices?.firstOrNull()?.message?.content
            ?: throw Exception("Failed to generate summary: empty response")
        
        val (title, summary) = parseAiResponse(jsonText)
        
        return AiResponse(
            title = title,
            summary = summary,
            rawText = text
        )
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
