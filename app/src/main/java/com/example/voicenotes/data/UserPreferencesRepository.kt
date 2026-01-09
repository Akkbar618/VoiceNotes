package com.example.voicenotes.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.voicenotes.ai.AiProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Extension для DataStore.
 */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

/**
 * Данные пользовательских настроек.
 */
data class UserPreferences(
    val geminiApiKey: String = "",
    val openaiApiKey: String = "",
    val selectedProvider: AiProvider = AiProvider.GEMINI
)

/**
 * Репозиторий для хранения пользовательских настроек (DataStore).
 * Хранит API ключи и выбранного провайдера.
 */
@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object PreferencesKeys {
        val GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
        val OPENAI_API_KEY = stringPreferencesKey("openai_api_key")
        val SELECTED_PROVIDER = stringPreferencesKey("selected_provider")
    }
    
    /**
     * Flow с текущими настройками.
     */
    val userPreferences: Flow<UserPreferences> = context.dataStore.data.map { preferences ->
        UserPreferences(
            geminiApiKey = preferences[PreferencesKeys.GEMINI_API_KEY] ?: "",
            openaiApiKey = preferences[PreferencesKeys.OPENAI_API_KEY] ?: "",
            selectedProvider = try {
                AiProvider.valueOf(preferences[PreferencesKeys.SELECTED_PROVIDER] ?: AiProvider.GEMINI.name)
            } catch (e: Exception) {
                AiProvider.GEMINI
            }
        )
    }
    
    /**
     * Получить текущие настройки синхронно (для разового использования).
     */
    suspend fun getPreferences(): UserPreferences = userPreferences.first()
    
    /**
     * Сохранить Gemini API ключ.
     */
    suspend fun setGeminiApiKey(key: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.GEMINI_API_KEY] = key
        }
    }
    
    /**
     * Сохранить OpenAI API ключ.
     */
    suspend fun setOpenAiApiKey(key: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.OPENAI_API_KEY] = key
        }
    }
    
    /**
     * Сохранить выбранного провайдера.
     */
    suspend fun setSelectedProvider(provider: AiProvider) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SELECTED_PROVIDER] = provider.name
        }
    }
    
    /**
     * Получить API ключ для текущего провайдера.
     */
    suspend fun getCurrentApiKey(): String {
        val prefs = getPreferences()
        return when (prefs.selectedProvider) {
            AiProvider.GEMINI -> prefs.geminiApiKey
            AiProvider.OPENAI -> prefs.openaiApiKey
        }
    }
}
