package com.example.voicenotes.util

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException

/**
 * Класс для управления воспроизведением аудио.
 * Предоставляет реактивное состояние для UI.
 */
class AudioPlayer(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _playerState = MutableStateFlow(AudioPlayerState())
    val playerState: StateFlow<AudioPlayerState> = _playerState.asStateFlow()

    fun playFile(file: File) {
        if (!file.exists()) {
            _playerState.update { it.copy(error = AppError.Unknown("Audio file not found")) }
            return
        }
        playUri(Uri.fromFile(file))
    }

    fun playUri(uri: Uri) {
        stop() // Остановить текущее воспроизведение

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, uri)
                prepare()
                start()
                setOnCompletionListener {
                    stop()
                }
            }

            _playerState.update {
                it.copy(
                    isPlaying = true,
                    duration = mediaPlayer?.duration ?: 0,
                    currentPosition = 0,
                    error = null
                )
            }

            startProgressTracker()

        } catch (e: IOException) {
            _playerState.update { it.copy(error = AppError.Unknown("Failed to play audio: ${e.message}")) }
        }
    }

    fun pause() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                _playerState.update { state -> state.copy(isPlaying = false) }
                stopProgressTracker()
            }
        }
    }

    fun resume() {
        mediaPlayer?.let {
            if (!it.isPlaying) {
                it.start()
                _playerState.update { state -> state.copy(isPlaying = true) }
                startProgressTracker()
            }
        }
    }

    fun stop() {
        mediaPlayer?.release()
        mediaPlayer = null
        stopProgressTracker()
        _playerState.update {
            it.copy(
                isPlaying = false,
                currentPosition = 0,
                duration = 0, // Сбрасываем длительность при стопе или оставляем, если нужно? Обычно сбрасываем.
                error = null
            )
        }
    }

    fun seekTo(position: Int) {
        mediaPlayer?.seekTo(position)
        _playerState.update { it.copy(currentPosition = position) }
    }

    fun release() {
        stop()
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive && mediaPlayer?.isPlaying == true) {
                val current = mediaPlayer?.currentPosition ?: 0
                _playerState.update { it.copy(currentPosition = current) }
                delay(100) // Обновляем каждые 100 мс
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }
}

data class AudioPlayerState(
    val isPlaying: Boolean = false,
    val currentPosition: Int = 0,
    val duration: Int = 0,
    val error: AppError? = null
)
