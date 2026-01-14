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
    /**
     * Обрабатывает голосовую заметку: транскрибирует, создаёт саммари и сохраняет в базу.
     * Сохраняет черновик при ошибках сети.
     */
    /**
     * Обрабатывает голосовую заметку: транскрибирует, создаёт саммари и сохраняет в базу.
     * Сохраняет черновик при ошибках сети.
     */
    suspend fun processVoiceNote(audioFile: File) {
        // 1. Создаем начальную запись в БД со статусом PROCESSING
        val tempNote = NoteEntity(
            title = "Processing...",
            rawText = "",
            summary = "",
            audioPath = audioFile.absolutePath,
            status = NoteStatus.PROCESSING
        )
        // Теперь insertNote возвращает ID
        val noteId = noteDao.insertNote(tempNote)
        
        // 2. Запускаем процессинг
        // Обертываем в try-catch, чтобы убедиться, что исключение доходит до VM для отображения,
        // но при этом статус в БД уже обновлен внутри processNoteById.
        processNoteById(noteId, audioFile)
    }
    
    /**
     * Повторить обработку заметки.
     */
    suspend fun retryNote(noteId: Long) {
        val note = noteDao.getNoteById(noteId) ?: return
        val audioFile = File(note.audioPath)
        
        if (!audioFile.exists()) {
             noteDao.updateStatus(noteId, NoteStatus.FAILED)
             throw Exception("Audio file not found")
        }
        
        // Обновляем статус на процессинг
        noteDao.updateStatus(noteId, NoteStatus.PROCESSING)
        
        // Пытаемся обработать
        processNoteById(noteId, audioFile)
    }

    /**
     * Внутренняя логика обработки заметки по ID.
     */
    private suspend fun processNoteById(noteId: Long, audioFile: File) {
        // Получаем свежую копию заметки
        val currentNote = noteDao.getNoteById(noteId) ?: return

        try {
            // 1. Получаем настройки и ключ
            val prefs = userPreferences.getPreferences()
            val apiKey = when (prefs.selectedProvider) {
                AiProvider.GEMINI -> prefs.geminiApiKey
                AiProvider.OPENAI -> prefs.openaiApiKey
            }
            
            if (apiKey.isBlank()) {
                throw MissingApiKeyException(prefs.selectedProvider)
            }
            
            // 2. Читаем и анализируем
            val audioBytes = audioFile.readBytes()
            val audioBase64 = Base64.encodeToString(audioBytes, Base64.NO_WRAP)
            val mimeType = getMimeType(audioFile)
            
            val aiResult = when (prefs.selectedProvider) {
                AiProvider.GEMINI -> geminiService.analyzeAudio(audioBase64, mimeType, apiKey)
                AiProvider.OPENAI -> {
                     val geminiApiKey = prefs.geminiApiKey
                     if (geminiApiKey.isBlank()) throw Exception("OpenAI requires Gemini API key")
                     
                     val transcription = geminiService.analyzeAudio(audioBase64, mimeType, geminiApiKey)
                     openAiService.generateTitleAndSummary(transcription.rawText, apiKey)
                }
            }
            
            // 3. Успех -> обновляем запись полностью
            val updatedNote = currentNote.copy(
                title = aiResult.title,
                rawText = aiResult.rawText,
                summary = aiResult.summary,
                status = NoteStatus.SYNCED
            )
            noteDao.updateNote(updatedNote)
            
        } catch (e: java.io.IOException) {
            // Ошибка сети -> DRAFT
            noteDao.updateStatus(noteId, NoteStatus.DRAFT)
            throw e
        } catch (e: Exception) {
            // Другая ошибка -> FAILED
            noteDao.updateStatus(noteId, NoteStatus.FAILED)
            throw e
        }
    }
    
    private fun getMimeType(file: File): String {
        return when (file.extension.lowercase()) {
            "mp3" -> "audio/mp3"
            "m4a" -> "audio/mp4"
            "wav" -> "audio/wav"
            "aac" -> "audio/aac"
            "ogg" -> "audio/ogg"
            else -> "audio/mpeg"
        }
    }
}

