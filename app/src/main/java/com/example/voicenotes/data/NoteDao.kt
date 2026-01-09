package com.example.voicenotes.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * DAO интерфейс для работы с заметками в Room базе данных.
 */
@Dao
interface NoteDao {
    
    /**
     * Добавить новую заметку (suspend - тяжелая операция).
     */
    @Insert
    suspend fun insertNote(note: NoteEntity)
    
    /**
     * Получить все заметки, отсортированные по дате (новые первыми).
     * Возвращает Flow для реактивных обновлений.
     */
    @Query("SELECT * FROM notes ORDER BY timestamp DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>
    
    /**
     * Получить заметку по ID.
     */
    @Query("SELECT * FROM notes WHERE id = :noteId")
    suspend fun getNoteById(noteId: Long): NoteEntity?
    
    /**
     * Удалить заметку по ID.
     */
    @Query("DELETE FROM notes WHERE id = :noteId")
    suspend fun deleteNote(noteId: Long)
}
