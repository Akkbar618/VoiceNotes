package com.example.voicenotes.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room база данных для приложения VoiceNotes.
 * Синглтон — одно соединение на всё приложение.
 */
@Database(entities = [NoteEntity::class], version = 3, exportSchema = false)
@androidx.room.TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun noteDao(): NoteDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        // Миграция с версии 1 на 2: добавляем поле title
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE notes ADD COLUMN title TEXT NOT NULL DEFAULT ''")
            }
        }
        
        // Миграция с версии 2 на 3: добавляем поле status
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Добавляем колонку status со значением по умолчанию 'SYNCED'
                database.execSQL("ALTER TABLE notes ADD COLUMN status TEXT NOT NULL DEFAULT 'SYNCED'")
            }
        }
        
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "voicenotes_db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .fallbackToDestructiveMigration() // При критических проблемах — пересоздать БД
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
