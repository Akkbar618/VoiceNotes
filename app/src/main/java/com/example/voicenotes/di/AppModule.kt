package com.example.voicenotes.di

import android.content.Context
import com.example.voicenotes.ai.GeminiAiService
import com.example.voicenotes.ai.OpenAiService
import com.example.voicenotes.data.AppDatabase
import com.example.voicenotes.data.NoteDao
import com.example.voicenotes.network.GeminiApi
import com.example.voicenotes.network.NetworkModule
import com.example.voicenotes.network.OpenAiApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt модуль для предоставления зависимостей уровня приложения.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * Предоставляет экземпляр базы данных (Singleton).
     */
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getInstance(context)
    }

    /**
     * Предоставляет NoteDao из базы данных.
     */
    @Provides
    @Singleton
    fun provideNoteDao(database: AppDatabase): NoteDao {
        return database.noteDao()
    }

    /**
     * Предоставляет GeminiApi.
     */
    @Provides
    @Singleton
    fun provideGeminiApi(): GeminiApi {
        return NetworkModule.geminiApi
    }

    /**
     * Предоставляет OpenAiApi.
     */
    @Provides
    @Singleton
    fun provideOpenAiApi(): OpenAiApi {
        return NetworkModule.openAiApi
    }

    /**
     * Предоставляет GeminiAiService.
     */
    @Provides
    @Singleton
    fun provideGeminiAiService(geminiApi: GeminiApi): GeminiAiService {
        return GeminiAiService(geminiApi)
    }

    /**
     * Предоставляет OpenAiService.
     */
    @Provides
    @Singleton
    fun provideOpenAiService(openAiApi: OpenAiApi): OpenAiService {
        return OpenAiService(openAiApi)
    }
}
