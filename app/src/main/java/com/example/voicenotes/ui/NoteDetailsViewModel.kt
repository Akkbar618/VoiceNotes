package com.example.voicenotes.ui

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicenotes.NoteUi
import com.example.voicenotes.data.NoteEntity
import com.example.voicenotes.data.NoteRepository
import com.example.voicenotes.util.AppError
import com.example.voicenotes.util.AudioPlayer
import com.example.voicenotes.util.DateFormatter
import com.example.voicenotes.util.ErrorHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class NoteDetailsUiState(
    val note: NoteUi? = null,
    val isLoading: Boolean = true,
    val error: AppError? = null,
    val isDeleted: Boolean = false
)

@HiltViewModel
class NoteDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: NoteRepository,
    val audioPlayer: AudioPlayer
) : ViewModel() {

    private val noteId: Long = checkNotNull(savedStateHandle["noteId"])

    private val _uiState = MutableStateFlow(NoteDetailsUiState())
    val uiState: StateFlow<NoteDetailsUiState> = _uiState.asStateFlow()

    init {
        loadNote()
    }

    private fun loadNote() {
        viewModelScope.launch {
            try {
                val note = repository.getNoteById(noteId)
                if (note != null) {
                    _uiState.update { 
                        it.copy(
                            note = note.toUi(), 
                            isLoading = false 
                        ) 
                    }
                    // Подготавливаем плеер (можно авто-загрузку сделать, но лучше по клику)
                } else {
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            error = AppError.Unknown("Note not found")
                        ) 
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = ErrorHandler.fromException(e)
                    ) 
                }
            }
        }
    }

    fun deleteNote() {
        viewModelScope.launch {
            try {
                // Сначала останавливаем плеер и удаляем файл
                audioPlayer.stop()
                val note = repository.getNoteById(noteId)
                note?.let {
                    val file = File(it.audioPath)
                    if (file.exists()) {
                        file.delete()
                    }
                }
                
                repository.deleteNote(noteId)
                _uiState.update { it.copy(isDeleted = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = AppError.DeleteFailed(e.message)) }
            }
        }
    }

    fun updateTitle(newTitle: String) {
        viewModelScope.launch {
            try {
                repository.updateNoteTitle(noteId, newTitle)
                // Обновляем локальное состояние
                _uiState.update { state -> 
                    state.copy(note = state.note?.copy(title = newTitle))
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = AppError.UpdateFailed(e.message)) }
            }
        }
    }
    
    fun playAudio() {
        val note = _uiState.value.note ?: return
        // NoteUi не хранит путь к файлу, нужно получить из Entity или передать в NoteUi
        // Поскольку репозиторий возвращает Entity, мне нужно сохранить путь.
        // Переделаем немного: загрузим Entity снова или добавим путь в NoteUi.
        // Проще запросить путь через репозиторий.
        
        viewModelScope.launch {
            val entity = repository.getNoteById(noteId)
            entity?.let {
                val file = File(it.audioPath)
                audioPlayer.playFile(file)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.release()
    }

    private fun NoteEntity.toUi() = NoteUi(
        id = id,
        title = title,
        rawText = rawText,
        summary = summary,
        formattedDate = DateFormatter.formatTimestamp(timestamp),
        previewText = rawText.take(100).replace("\n", " ") + if (rawText.length > 100) "..." else "",
        status = status
    )
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
