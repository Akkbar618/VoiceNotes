package com.example.voicenotes.util

import android.content.Context
import android.util.Log
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Менеджер для управления кэшем аудиофайлов.
 * Отвечает за очистку старых файлов и удаление файлов при удалении заметок.
 */
object AudioCacheManager {
    private const val TAG = "AudioCacheManager"
    private const val KEEP_FILES_DAYS = 30L // Хранить файлы 30 дней (пример)

    /**
     * Очищает файлы в директории кэша, которые старше [KEEP_FILES_DAYS] дней.
     * Эту функцию стоит вызывать периодически (например, при старте приложения или в Worker).
     */
    fun cleanOldFiles(context: Context) {
        val cacheDir = context.cacheDir
        val files = cacheDir.listFiles() ?: return
        val currentTime = System.currentTimeMillis()
        val expiryTime = TimeUnit.DAYS.toMillis(KEEP_FILES_DAYS)

        var deletedCount = 0
        files.forEach { file ->
            // Проверяем, что это аудио файл (начинается с "audio_") и он старый
            if (file.name.startsWith("audio_") && file.isFile) {
                val diff = currentTime - file.lastModified()
                if (diff > expiryTime) {
                    if (file.delete()) {
                        deletedCount++
                    }
                }
            }
        }
        if (deletedCount > 0) {
            Log.d(TAG, "Cleaned up $deletedCount old audio files")
        }
    }

    /**
     * Удаляет конкретный аудиофайл по пути.
     */
    fun deleteAudioFile(path: String) {
        try {
            val file = File(path)
            if (file.exists()) {
                if (file.delete()) {
                    Log.d(TAG, "Deleted audio file: $path")
                } else {
                    Log.w(TAG, "Failed to delete audio file: $path")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error removing file: $path", e)
        }
    }
}
