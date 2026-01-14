package com.example.voicenotes.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.voicenotes.R
import com.example.voicenotes.SettingsViewModel
import com.example.voicenotes.ai.AiProvider
import kotlinx.coroutines.launch

import androidx.compose.runtime.LaunchedEffect

@Composable
fun OnboardingScreen(
    viewModel: SettingsViewModel,
    onComplete: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()
    
    // UI для настройки API к API (шаг 3)
    val userPreferencesState by viewModel.userPreferences.collectAsState()
    val userPreferences = userPreferencesState ?: return
    
    // Фон
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                when (page) {
                    0 -> OnboardingWelcomePage()
                    1 -> OnboardingHowItWorksPage()
                    2 -> OnboardingApiSetupPage(viewModel)
                }
            }
            
            // Индикаторы и кнопки
            BottomControls(
                pagerState = pagerState,
                onNext = {
                    scope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                    }
                },
                onComplete = {
                    viewModel.completeOnboarding()
                    onComplete()
                }
            )
        }
    }
}

@Composable
fun OnboardingWelcomePage() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Иконка или изображение
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = stringResource(R.string.onboarding_welcome_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = stringResource(R.string.onboarding_welcome_description),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun OnboardingHowItWorksPage() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        
        // Пример шагов
        HowItWorksItem(icon = Icons.Default.Mic, text = "1. Tap record")
        Spacer(modifier = Modifier.height(24.dp))
        HowItWorksItem(icon = Icons.Default.Settings, text = "2. Speak thoughts")
        Spacer(modifier = Modifier.height(24.dp))
        HowItWorksItem(icon = Icons.Default.Check, text = "3. Get Summary")
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Text(
            text = stringResource(R.string.onboarding_how_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = stringResource(R.string.onboarding_how_description),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun HowItWorksItem(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun OnboardingApiSetupPage(viewModel: SettingsViewModel) {
    val userPreferencesState by viewModel.userPreferences.collectAsState()
    val userPreferences = userPreferencesState ?: return
    
    var selectedProvider by remember { mutableStateOf(AiProvider.GEMINI) }
    var apiKey by remember { mutableStateOf("") }
    
    // Синхронизируем локальное состояние с preferences при первом отображении
    LaunchedEffect(userPreferences) {
        selectedProvider = userPreferences.selectedProvider
        // Не показываем ключ в поле ввода в целях безопасности, но можно плейсхолдер
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.onboarding_api_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = stringResource(R.string.onboarding_api_description),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Выбор провайдера (Tabs)
        TabRow(
            selectedTabIndex = if (selectedProvider == AiProvider.GEMINI) 0 else 1,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            indicator = { tabPositions ->
                if (selectedProvider == AiProvider.GEMINI) {
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[0]),
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[1]),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        ) {
            Tab(
                selected = selectedProvider == AiProvider.GEMINI,
                onClick = { 
                    selectedProvider = AiProvider.GEMINI 
                    viewModel.setProvider(AiProvider.GEMINI)
                },
                text = { Text(stringResource(R.string.settings_provider_gemini)) }
            )
            Tab(
                selected = selectedProvider == AiProvider.OPENAI,
                onClick = { 
                    selectedProvider = AiProvider.OPENAI
                    viewModel.setProvider(AiProvider.OPENAI)
                },
                text = { Text("OpenAI") }
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Ввод ключа
        OutlinedTextField(
            value = apiKey,
            onValueChange = { 
                apiKey = it
                if (selectedProvider == AiProvider.GEMINI) {
                    viewModel.setGeminiApiKey(it)
                } else {
                    viewModel.setOpenAiApiKey(it)
                }
            },
            label = { 
                Text(
                    if (selectedProvider == AiProvider.GEMINI) 
                        stringResource(R.string.settings_api_key_gemini) 
                    else 
                        stringResource(R.string.settings_api_key_openai)
                ) 
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "You can change this later in Settings",
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray
        )
    }
}

@Composable
fun BottomControls(
    pagerState: androidx.compose.foundation.pager.PagerState,
    onNext: () -> Unit,
    onComplete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Индикаторы страниц
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(pagerState.pageCount) { iteration ->
                val color = if (pagerState.currentPage == iteration) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.primaryContainer
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(color)
                        .size(10.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        if (pagerState.currentPage == pagerState.pageCount - 1) {
            Button(
                onClick = onComplete,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 32.dp)
            ) {
                Text(stringResource(R.string.onboarding_finish))
            }
        } else {
            TextButton(
                onClick = onComplete // Кнопка Skip
            ) {
                Text(stringResource(R.string.onboarding_skip))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onNext
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = stringResource(R.string.onboarding_next)
                )
            }
        }
    }
}
