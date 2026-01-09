package com.example.voicenotes.data

import android.util.Base64
import com.example.voicenotes.ai.AiProvider
import com.example.voicenotes.ai.AiResponse
import com.example.voicenotes.ai.GeminiAiService
import com.example.voicenotes.ai.MissingApiKeyException
import com.example.voicenotes.ai.OpenAiService
import kotlinx.coroutines.flow.Flow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Репозиторий для работы с голосовыми заметками.
 * Инкапсулирует логику взаимодействия с AI сервисами и Room базой данных.
 * 
 * Поддерживает несколько AI провайдеров (Gemini, OpenAI) с выбором в runtime.
 * 
 * Магия: когда мы сохраняем заметку через DAO, база автоматически "пнет"
 * всех подписчиков getAllNotes() через Flow — UI обновится сам!
 */
@Singleton
class NoteRepository @Inject constructor(
    private val noteDao: NoteDao,
    private val geminiService: GeminiAiService,
    private val openAiService: OpenAiService,
    private val userPreferences: UserPreferencesRepository
) {

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
     * Обновить заголовок заметки.
     */
    suspend fun updateNoteTitle(noteId: Long, newTitle: String) = noteDao.updateNoteTitle(noteId, newTitle)

    /**
     * Обрабатывает голосовую заметку: транскрибирует, создаёт саммари и сохраняет в базу.
     * 
     * Использует выбранного AI провайдера из настроек пользователя.
     * 
     * @param audioFile Аудио файл для обработки
     * @throws MissingApiKeyException Если API ключ не настроен
     * @throws Exception При ошибке API
     */
    suspend fun processVoiceNote(audioFile: File) {
        // 1. Получаем настройки пользователя
        val prefs = userPreferences.getPreferences()
        val apiKey = when (prefs.selectedProvider) {
            AiProvider.GEMINI -> prefs.geminiApiKey
            AiProvider.OPENAI -> prefs.openaiApiKey
        }
        
        // 2. Проверяем наличие ключа
        if (apiKey.isBlank()) {
            throw MissingApiKeyException(prefs.selectedProvider)
        }
        
        // 3. Читаем файл и конвертируем в base64
        val audioBytes = audioFile.readBytes()
        val audioBase64 = Base64.encodeToString(audioBytes, Base64.NO_WRAP)
        
        // 4. Определяем MIME тип по расширению
        val mimeType = when (audioFile.extension.lowercase()) {
            "mp3" -> "audio/mp3"
            "m4a" -> "audio/mp4"
            "wav" -> "audio/wav"
            "aac" -> "audio/aac"
            "ogg" -> "audio/ogg"
            else -> "audio/mpeg"
        }
        
        // 5. Анализируем через выбранный AI сервис
        val aiResult = when (prefs.selectedProvider) {
            AiProvider.GEMINI -> {
                // Gemini поддерживает аудио напрямую (multimodal)
                geminiService.analyzeAudio(audioBase64, mimeType, apiKey)
            }
            AiProvider.OPENAI -> {
                // OpenAI не поддерживает аудио в chat completions
                // Используем Gemini для транскрипции, а OpenAI для саммари
                val geminiApiKey = prefs.geminiApiKey
                if (geminiApiKey.isBlank()) {
                    // Если нет Gemini ключа — используем только OpenAI
                    // (транскрипция будет недоступна, передаём пустой текст)
                    throw Exception("OpenAI requires Gemini API key for audio transcription. Please configure both keys.")
                }
                
                // Транскрибируем через Gemini
                val transcription = geminiService.analyzeAudio(audioBase64, mimeType, geminiApiKey)
                
                // Генерируем заголовок и саммари через OpenAI
                openAiService.generateTitleAndSummary(transcription.rawText, apiKey)
            }
        }
        
        // 6. Собираем Entity и сохраняем в базу
        val note = NoteEntity(
            title = aiResult.title,
            rawText = aiResult.rawText,
            summary = aiResult.summary,
            audioPath = audioFile.absolutePath
        )
        noteDao.insertNote(note)
        
        // Возвращать ничего не нужно — база сама уведомит UI через Flow!
    }
}

