package com.example.voicenotes.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity для хранения голосовых заметок в Room базе данных.
 */
@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val rawText: String,           // Транскрипция (сырой текст)
    val summary: String,           // Саммари от AI
    val audioPath: String,         // Путь к аудио файлу
    val timestamp: Long = System.currentTimeMillis()  // Для сортировки по дате
)
