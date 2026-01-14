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
    /**
     * Добавить новую заметку. Возвращает ID вставленной записи.
     */
    @Insert
    suspend fun insertNote(note: NoteEntity): Long
    
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
    
    /**
     * Обновить заголовок заметки.
     */
    @Query("UPDATE notes SET title = :newTitle WHERE id = :noteId")
    suspend fun updateNoteTitle(noteId: Long, newTitle: String)

    /**
     * Обновить статус заметки.
     */
    @Query("UPDATE notes SET status = :status WHERE id = :noteId")
    suspend fun updateStatus(noteId: Long, status: NoteStatus)

    /**
     * Обновить содержимое заметки (заголовок, текст, саммари, статус).
     */
    @androidx.room.Update
    suspend fun updateNote(note: NoteEntity)
}
