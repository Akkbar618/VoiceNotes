package com.example.voicenotes

import android.app.Application
import com.example.voicenotes.data.AppDatabase
import com.example.voicenotes.data.NoteRepository

/**
 * Application класс — точка входа приложения.
 * Инициализирует базу данных и репозиторий как синглтоны.
 */
class App : Application() {
    
    // Ленивая инициализация базы данных
    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    
    // Ленивая инициализация репозитория
    val repository: NoteRepository by lazy { NoteRepository(database.noteDao()) }
}
