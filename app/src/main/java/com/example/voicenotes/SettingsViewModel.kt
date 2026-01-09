package com.example.voicenotes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicenotes.ai.AiProvider
import com.example.voicenotes.data.UserPreferences
import com.example.voicenotes.data.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel для экрана настроек.
 * Управляет API ключами и выбором провайдера.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    /**
     * Текущие настройки пользователя.
     */
    val userPreferences: StateFlow<UserPreferences> = userPreferencesRepository.userPreferences
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPreferences()
        )

    /**
     * Состояние сохранения.
     */
    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    /**
     * Сохранить Gemini API ключ.
     */
    fun setGeminiApiKey(key: String) {
        viewModelScope.launch {
            _isSaving.value = true
            userPreferencesRepository.setGeminiApiKey(key)
            _isSaving.value = false
        }
    }

    /**
     * Сохранить OpenAI API ключ.
     */
    fun setOpenAiApiKey(key: String) {
        viewModelScope.launch {
            _isSaving.value = true
            userPreferencesRepository.setOpenAiApiKey(key)
            _isSaving.value = false
        }
    }

    /**
     * Выбрать провайдера.
     */
    fun setProvider(provider: AiProvider) {
        viewModelScope.launch {
            _isSaving.value = true
            userPreferencesRepository.setSelectedProvider(provider)
            _isSaving.value = false
        }
    }
}
