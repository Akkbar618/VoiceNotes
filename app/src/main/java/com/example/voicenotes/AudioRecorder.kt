package com.example.voicenotes

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

/**
 * Класс для управления записью аудио.
 * Использует MediaRecorder для записи звука с микрофона.
 */
class AudioRecorder(private val context: Context) {

    private var recorder: MediaRecorder? = null

    /**
     * Начинает запись аудио в указанный файл.
     * @param outputFile Файл для сохранения записи
     */
    fun start(outputFile: File) {
        recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(outputFile.absolutePath)
            prepare()
            start()
        }
    }

    /**
     * Останавливает запись и освобождает ресурсы.
     */
    fun stop() {
        recorder?.apply {
            stop()
            reset()
            release()
        }
        recorder = null
    }
}
