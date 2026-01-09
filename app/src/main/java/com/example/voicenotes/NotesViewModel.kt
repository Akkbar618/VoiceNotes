package com.example.voicenotes

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.voicenotes.data.NoteEntity
import com.example.voicenotes.data.NoteRepository
import com.example.voicenotes.util.DateFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.net.UnknownHostException

/**
 * UI модель заметки для отображения в списке.
 */
data class NoteUi(
    val id: Long,
    val rawText: String,
    val summary: String,
    val formattedDate: String,
    val previewText: String  // Краткое превью для списка
)

/**
 * UI состояние экрана заметок.
 */
data class NotesUiState(
    val isRecording: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * ViewModel для управления записью и обработкой голосовых заметок.
 * 
 * Магия реактивного UI:
 * - Подписываемся на Flow из базы данных
 * - Когда Репозиторий сохранит заметку, база сама "пнет" нас через Flow
 * - UI обновится автоматически без ручного добавления в список
 */
class NotesViewModel(private val repository: NoteRepository) : ViewModel() {

    private var audioRecorder: AudioRecorder? = null

    private val _uiState = MutableStateFlow(NotesUiState())
    val uiState: StateFlow<NotesUiState> = _uiState.asStateFlow()

    /**
     * Список заметок из базы данных, преобразованный в UI модель.
     * Подписка на Flow — UI обновляется автоматически при изменениях в БД.
     */
    val notes: StateFlow<List<NoteUi>> = repository.getAllNotes()
        .map { entities -> entities.map { it.toUi() } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private var currentRecordingFile: File? = null

    /**
     * Получить заметку по ID для экрана деталей.
     */
    suspend fun getNoteById(noteId: Long): NoteUi? {
        return repository.getNoteById(noteId)?.toUi()
    }

    /**
     * Удалить заметку по ID.
     */
    fun deleteNote(noteId: Long) {
        viewModelScope.launch {
            try {
                repository.deleteNote(noteId)
                Log.d(TAG, "Note deleted: $noteId")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete note", e)
                _uiState.update { it.copy(error = "Ошибка удаления: ${e.message}") }
            }
        }
    }

    /**
     * Начинает запись аудио.
     */
    fun startRecording(context: Context, cacheDir: File) {
        val fileName = "audio_${System.currentTimeMillis()}.mp3"
        val outputFile = File(cacheDir, fileName)
        
        try {
            if (audioRecorder == null) {
                audioRecorder = AudioRecorder(context.applicationContext)
            }
            audioRecorder?.start(outputFile)
            currentRecordingFile = outputFile
            _uiState.update { it.copy(isRecording = true, error = null) }
            Log.d(TAG, "Recording started: ${outputFile.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording", e)
            _uiState.update { it.copy(error = "Ошибка записи: ${e.message}") }
        }
    }

    /**
     * Останавливает запись аудио и обрабатывает через Gemini API.
     */
    fun stopRecording() {
        try {
            audioRecorder?.stop()
            _uiState.update { it.copy(isRecording = false) }
            Log.d(TAG, "Recording saved to: ${currentRecordingFile?.absolutePath}")
            
            currentRecordingFile?.let { file ->
                processRecording(file)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop recording", e)
            _uiState.update { it.copy(isRecording = false, error = "Ошибка остановки: ${e.message}") }
        }
    }

    /**
     * Обрабатывает записанный аудио файл через Repository.
     * Обрабатывает ошибки сети gracefully.
     */
    private fun processRecording(audioFile: File) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                Log.d(TAG, "Processing audio file: ${audioFile.absolutePath}")
                repository.processVoiceNote(audioFile)
                Log.d(TAG, "Note processed and saved to database")
                _uiState.update { it.copy(isLoading = false, error = null) }
            } catch (e: UnknownHostException) {
                // Нет сети
                Log.e(TAG, "No network connection", e)
                _uiState.update { 
                    it.copy(isLoading = false, error = "Нет сети. Проверьте подключение к интернету.") 
                }
            } catch (e: Exception) {
                // Другие ошибки
                Log.e(TAG, "Failed to process recording", e)
                val errorMessage = when {
                    e.message?.contains("401") == true -> "Ошибка авторизации API"
                    e.message?.contains("429") == true -> "Слишком много запросов. Подождите."
                    e.message?.contains("500") == true -> "Ошибка сервера Gemini"
                    else -> "Ошибка обработки: ${e.message}"
                }
                _uiState.update { it.copy(isLoading = false, error = errorMessage) }
            }
        }
    }

    /**
     * Очищает ошибку.
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Преобразует Entity из БД в UI модель.
     */
    private fun NoteEntity.toUi() = NoteUi(
        id = id,
        rawText = rawText,
        summary = summary,
        formattedDate = DateFormatter.formatTimestamp(timestamp),
        previewText = rawText.take(100).replace("\n", " ") + if (rawText.length > 100) "..." else ""
    )

    override fun onCleared() {
        super.onCleared()
        if (_uiState.value.isRecording) {
            audioRecorder?.stop()
        }
    }

    companion object {
        private const val TAG = "NotesViewModel"
    }
}

/**
 * Factory для создания ViewModel с зависимостями.
 */
class NotesViewModelFactory(private val repository: NoteRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NotesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NotesViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
