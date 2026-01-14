package com.example.voicenotes.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.voicenotes.ai.AiProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Extension для DataStore (для не-секретных настроек).
 */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

/**
 * Данные пользовательских настроек.
 */
data class UserPreferences(
    val geminiApiKey: String = "",
    val openaiApiKey: String = "",
    val selectedProvider: AiProvider = AiProvider.GEMINI,
    val isOnboardingCompleted: Boolean = false
)

/**
 * Репозиторий для хранения пользовательских настроек.
 * 
 * ВАЖНО: API ключи хранятся в EncryptedSharedPreferences (зашифрованы),
 * а не-секретные настройки (выбранный провайдер) — в обычном DataStore.
 */
@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "UserPreferencesRepo"
        private const val ENCRYPTED_PREFS_NAME = "secure_api_keys"
        private const val KEY_GEMINI_API = "gemini_api_key"
        private const val KEY_OPENAI_API = "openai_api_key"
    }
    
    private object PreferencesKeys {
        val SELECTED_PROVIDER = stringPreferencesKey("selected_provider")
        val IS_ONBOARDING_COMPLETED = androidx.datastore.preferences.core.booleanPreferencesKey("is_onboarding_completed")
        // Миграционные ключи (для переноса старых ключей в зашифрованное хранилище)
        val LEGACY_GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
        val LEGACY_OPENAI_API_KEY = stringPreferencesKey("openai_api_key")
    }
    
    /**
     * EncryptedSharedPreferences для безопасного хранения API ключей.
     */
    private val encryptedPrefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            
            EncryptedSharedPreferences.create(
                context,
                ENCRYPTED_PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create EncryptedSharedPreferences, falling back to regular prefs", e)
            // Fallback на обычные SharedPreferences (не должно случаться в проде)
            context.getSharedPreferences(ENCRYPTED_PREFS_NAME, Context.MODE_PRIVATE)
        }
    }
    
    /**
     * Flow с текущими настройками.
     * Комбинирует данные из DataStore и EncryptedSharedPreferences.
     */
    val userPreferences: Flow<UserPreferences> = context.dataStore.data.map { preferences ->
        UserPreferences(
            geminiApiKey = getGeminiApiKey(),
            openaiApiKey = getOpenAiApiKey(),
            selectedProvider = try {
                AiProvider.valueOf(preferences[PreferencesKeys.SELECTED_PROVIDER] ?: AiProvider.GEMINI.name)
            } catch (e: Exception) {
                AiProvider.GEMINI
            },
            isOnboardingCompleted = preferences[PreferencesKeys.IS_ONBOARDING_COMPLETED] ?: false
        )
    }
    
    init {
        // Миграция старых ключей в зашифрованное хранилище при первом запуске
        migrateOldKeys()
    }
    
    /**
     * Мигрирует старые незашифрованные ключи в EncryptedSharedPreferences.
     * Удаляет старые ключи после миграции.
     */
    private fun migrateOldKeys() {
        // Используем runBlocking только для миграции при инициализации
        kotlinx.coroutines.runBlocking {
            try {
                val prefs = context.dataStore.data.first()
                
                // Миграция Gemini ключа
                prefs[PreferencesKeys.LEGACY_GEMINI_API_KEY]?.let { oldKey ->
                    if (oldKey.isNotBlank() && getGeminiApiKey().isBlank()) {
                        Log.d(TAG, "Migrating Gemini API key to encrypted storage")
                        setGeminiApiKey(oldKey)
                        // Удаляем старый ключ
                        context.dataStore.edit { it.remove(PreferencesKeys.LEGACY_GEMINI_API_KEY) }
                    }
                }
                
                // Миграция OpenAI ключа
                prefs[PreferencesKeys.LEGACY_OPENAI_API_KEY]?.let { oldKey ->
                    if (oldKey.isNotBlank() && getOpenAiApiKey().isBlank()) {
                        Log.d(TAG, "Migrating OpenAI API key to encrypted storage")
                        setOpenAiApiKey(oldKey)
                        // Удаляем старый ключ
                        context.dataStore.edit { it.remove(PreferencesKeys.LEGACY_OPENAI_API_KEY) }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Migration failed", e)
            }
        }
    }
    
    /**
     * Получить текущие настройки синхронно (для разового использования).
     */
    suspend fun getPreferences(): UserPreferences = userPreferences.first()
    
    /**
     * Получить Gemini API ключ из зашифрованного хранилища.
     */
    private fun getGeminiApiKey(): String {
        return encryptedPrefs.getString(KEY_GEMINI_API, "") ?: ""
    }
    
    /**
     * Получить OpenAI API ключ из зашифрованного хранилища.
     */
    private fun getOpenAiApiKey(): String {
        return encryptedPrefs.getString(KEY_OPENAI_API, "") ?: ""
    }
    
    /**
     * Сохранить Gemini API ключ в зашифрованное хранилище.
     */
    suspend fun setGeminiApiKey(key: String) {
        encryptedPrefs.edit().putString(KEY_GEMINI_API, key).apply()
        // Триггерим обновление Flow через DataStore
        context.dataStore.edit { /* trigger reemission */ }
    }
    
    /**
     * Сохранить OpenAI API ключ в зашифрованное хранилище.
     */
    suspend fun setOpenAiApiKey(key: String) {
        encryptedPrefs.edit().putString(KEY_OPENAI_API, key).apply()
        // Триггерим обновление Flow через DataStore
        context.dataStore.edit { /* trigger reemission */ }
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
     * Установить флаг завершения онбординга.
     */
    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_ONBOARDING_COMPLETED] = completed
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
