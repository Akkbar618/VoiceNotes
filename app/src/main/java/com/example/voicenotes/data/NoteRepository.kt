package com.example.voicenotes.data

import android.util.Base64
import com.example.voicenotes.network.NetworkModule
import com.example.voicenotes.network.model.GeminiContent
import com.example.voicenotes.network.model.GeminiPart
import com.example.voicenotes.network.model.GeminiRequest
import com.example.voicenotes.network.model.InlineData
import kotlinx.coroutines.flow.Flow
import java.io.File

/**
 * Репозиторий для работы с голосовыми заметками.
 * Инкапсулирует логику взаимодействия с Gemini API и Room базой данных.
 * 
 * Магия: когда мы сохраняем заметку через DAO, база автоматически "пнет"
 * всех подписчиков getAllNotes() через Flow — UI обновится сам!
 */
class NoteRepository(private val noteDao: NoteDao) {

    private val api = NetworkModule.api
    private val apiKey = NetworkModule.apiKey

    /**
     * Получить все заметки из базы данных.
     * Возвращает Flow — UI подписывается и получает обновления автоматически.
     */
    fun getAllNotes(): Flow<List<NoteEntity>> = noteDao.getAllNotes()

    /**
     * Получить заметку по ID.
     */
    suspend fun getNoteById(noteId: Long): NoteEntity? = noteDao.getNoteById(noteId)

    /**
     * Удалить заметку по ID.
     */
    suspend fun deleteNote(noteId: Long) = noteDao.deleteNote(noteId)

    /**
     * Обрабатывает голосовую заметку: транскрибирует, создаёт саммари и сохраняет в базу.
     * 
     * ВАЖНО: Мы не возвращаем данные в UI вручную!
     * База сама "пнет" ViewModel через Flow после сохранения.
     * 
     * @param audioFile Аудио файл для обработки
     * @throws Exception При ошибке API
     */
    suspend fun processVoiceNote(audioFile: File) {
        // 1. Читаем файл и конвертируем в base64
        val audioBytes = audioFile.readBytes()
        val audioBase64 = Base64.encodeToString(audioBytes, Base64.NO_WRAP)
        
        // 2. Определяем MIME тип по расширению
        val mimeType = when (audioFile.extension.lowercase()) {
            "mp3" -> "audio/mp3"
            "m4a" -> "audio/mp4"
            "wav" -> "audio/wav"
            "aac" -> "audio/aac"
            "ogg" -> "audio/ogg"
            else -> "audio/mpeg"
        }
        
        // 3. Транскрибируем аудио
        val rawText = transcribeAudio(audioBase64, mimeType)
        
        // 4. Генерируем саммари
        val summary = generateSummary(rawText)
        
        // 5. Собираем Entity и сохраняем в базу
        val note = NoteEntity(
            rawText = rawText,
            summary = summary,
            audioPath = audioFile.absolutePath
        )
        noteDao.insertNote(note)
        
        // Возвращать ничего не нужно — база сама уведомит UI через Flow!
    }

    /**
     * Транскрибирует аудио в текст через Gemini API.
     */
    private suspend fun transcribeAudio(audioBase64: String, mimeType: String): String {
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
     * Генерирует саммари текста через Gemini API.
     */
    private suspend fun generateSummary(text: String): String {
        val systemPrompt = """
            Ты — личный ассистент для обработки голосовых заметок.
            
            Твоя задача:
            1. Выдели главные идеи и задачи из текста
            2. Структурируй информацию в виде маркированного списка
            3. Если есть конкретные задачи или дедлайны — выдели их отдельно
            4. Будь краток, но не упускай важное
            
            Язык ответа: тот же, что и входной текст.
            
            Формат ответа:
            📌 **Главные идеи:**
            • ...
            
            ✅ **Задачи:**
            • ...
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
        
        return response.candidates?.firstOrNull()
            ?.content?.parts?.firstOrNull()?.text
            ?: throw Exception("Failed to generate summary: empty response")
    }
}
