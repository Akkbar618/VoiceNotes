package com.example.voicenotes.ui

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.voicenotes.R
import com.example.voicenotes.SettingsViewModel
import com.example.voicenotes.ai.AiProvider

/**
 * Список поддерживаемых языков.
 */
enum class AppLanguage(val code: String, val displayNameResId: Int) {
    ENGLISH("en", R.string.settings_language_english),
    RUSSIAN("ru", R.string.settings_language_russian)
}

/**
 * Получить текущий язык приложения.
 */
fun getCurrentLanguage(): AppLanguage {
    val locales = AppCompatDelegate.getApplicationLocales()
    if (locales.isEmpty) {
        return AppLanguage.ENGLISH
    }
    val currentLocale = locales[0]?.language ?: "en"
    return AppLanguage.entries.find { it.code == currentLocale } ?: AppLanguage.ENGLISH
}

/**
 * Установить язык приложения.
 */
fun setAppLanguage(language: AppLanguage) {
    val localeList = LocaleListCompat.forLanguageTags(language.code)
    AppCompatDelegate.setApplicationLocales(localeList)
}

/**
 * Маскирует API ключ для отображения.
 * Показывает первые 4 и последние 4 символа.
 */
fun maskApiKey(key: String): String {
    if (key.length <= 8) return if (key.isNotEmpty()) "****" else ""
    return "${key.take(4)}****${key.takeLast(4)}"
}

/**
 * Экран настроек.
 * Позволяет выбрать AI провайдера, настроить API ключи и язык.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val userPreferencesState by viewModel.userPreferences.collectAsState()
    val userPreferences = userPreferencesState ?: return
    
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showProviderDialog by remember { mutableStateOf(false) }
    var showGeminiKeyDialog by remember { mutableStateOf(false) }
    var showOpenAiKeyDialog by remember { mutableStateOf(false) }
    var currentLanguage by remember { mutableStateOf(getCurrentLanguage()) }

    // Диалог выбора языка
    if (showLanguageDialog) {
        LanguageSelectionDialog(
            currentLanguage = currentLanguage,
            onLanguageSelected = { language ->
                currentLanguage = language
                setAppLanguage(language)
                showLanguageDialog = false
            },
            onDismiss = { showLanguageDialog = false }
        )
    }
    
    // Диалог выбора провайдера
    if (showProviderDialog) {
        ProviderSelectionDialog(
            currentProvider = userPreferences.selectedProvider,
            onProviderSelected = { provider ->
                viewModel.setProvider(provider)
                showProviderDialog = false
            },
            onDismiss = { showProviderDialog = false }
        )
    }
    
    // Диалог ввода Gemini API ключа
    if (showGeminiKeyDialog) {
        ApiKeyInputDialog(
            title = stringResource(R.string.settings_api_key_gemini),
            currentKey = userPreferences.geminiApiKey,
            onSave = { key ->
                viewModel.setGeminiApiKey(key)
                showGeminiKeyDialog = false
            },
            onDismiss = { showGeminiKeyDialog = false }
        )
    }
    
    // Диалог ввода OpenAI API ключа
    if (showOpenAiKeyDialog) {
        ApiKeyInputDialog(
            title = stringResource(R.string.settings_api_key_openai),
            currentKey = userPreferences.openaiApiKey,
            onSave = { key ->
                viewModel.setOpenAiApiKey(key)
                showOpenAiKeyDialog = false
            },
            onDismiss = { showOpenAiKeyDialog = false }
        )
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.headlineLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.note_details_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Секция: AI Provider
            Text(
                text = stringResource(R.string.settings_section_ai),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            // Выбор провайдера
            ListItem(
                headlineContent = {
                    Text(text = stringResource(R.string.settings_provider_title))
                },
                supportingContent = {
                    Text(text = when (userPreferences.selectedProvider) {
                        AiProvider.GEMINI -> stringResource(R.string.settings_provider_gemini)
                        AiProvider.OPENAI -> stringResource(R.string.settings_provider_openai)
                    })
                },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                modifier = Modifier.clickable { showProviderDialog = true }
            )
            
            // Gemini API Key
            ListItem(
                headlineContent = {
                    Text(text = stringResource(R.string.settings_api_key_gemini))
                },
                supportingContent = {
                    val maskedKey = maskApiKey(userPreferences.geminiApiKey)
                    Text(
                        text = if (maskedKey.isNotEmpty()) maskedKey 
                            else stringResource(R.string.settings_api_key_not_set)
                    )
                },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                modifier = Modifier.clickable { showGeminiKeyDialog = true }
            )
            
            // OpenAI API Key
            ListItem(
                headlineContent = {
                    Text(text = stringResource(R.string.settings_api_key_openai))
                },
                supportingContent = {
                    val maskedKey = maskApiKey(userPreferences.openaiApiKey)
                    Text(
                        text = if (maskedKey.isNotEmpty()) maskedKey 
                            else stringResource(R.string.settings_api_key_not_set)
                    )
                },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                modifier = Modifier.clickable { showOpenAiKeyDialog = true }
            )
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Секция: Language
            Text(
                text = stringResource(R.string.settings_section_app),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            // Language Setting
            ListItem(
                headlineContent = {
                    Text(text = stringResource(R.string.settings_language_title))
                },
                supportingContent = {
                    Text(text = stringResource(currentLanguage.displayNameResId))
                },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                modifier = Modifier.clickable { showLanguageDialog = true }
            )
        }
    }
}

/**
 * Диалог выбора AI провайдера.
 */
@Composable
fun ProviderSelectionDialog(
    currentProvider: AiProvider,
    onProviderSelected: (AiProvider) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_provider_title)) },
        text = {
            Column(Modifier.selectableGroup()) {
                AiProvider.entries.forEach { provider ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .selectable(
                                selected = (provider == currentProvider),
                                onClick = { onProviderSelected(provider) },
                                role = Role.RadioButton
                            )
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (provider == currentProvider),
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = when (provider) {
                                AiProvider.GEMINI -> stringResource(R.string.settings_provider_gemini)
                                AiProvider.OPENAI -> stringResource(R.string.settings_provider_openai)
                            },
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    )
}

/**
 * Диалог ввода API ключа.
 */
@Composable
fun ApiKeyInputDialog(
    title: String,
    currentKey: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var apiKey by remember { mutableStateOf(currentKey) }
    var passwordVisible by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text("API Key") },
                singleLine = true,
                visualTransformation = if (passwordVisible) 
                    VisualTransformation.None 
                else 
                    PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) 
                                Icons.Default.VisibilityOff 
                            else 
                                Icons.Default.Visibility,
                            contentDescription = stringResource(R.string.cd_visibility_toggle)
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(apiKey) }) {
                Text(stringResource(R.string.common_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    )
}

/**
 * Диалог выбора языка.
 */
@Composable
fun LanguageSelectionDialog(
    currentLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_language_title)) },
        text = {
            Column(Modifier.selectableGroup()) {
                AppLanguage.entries.forEach { language ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .selectable(
                                selected = (language == currentLanguage),
                                onClick = { onLanguageSelected(language) },
                                role = Role.RadioButton
                            )
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (language == currentLanguage),
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = stringResource(language.displayNameResId),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    )
}

