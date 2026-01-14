package com.example.voicenotes

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.voicenotes.navigation.Screen
import com.example.voicenotes.ui.NoteDetailsScreen
import com.example.voicenotes.ui.OnboardingScreen
import com.example.voicenotes.ui.SettingsScreen
import com.example.voicenotes.ui.theme.VoiceNotesTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

import com.example.voicenotes.util.AudioCacheManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope // Или лучше lifecycleScope, но здесь onCreate

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Очистка старых файлов в фоне
        val cacheDir = cacheDir
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            AudioCacheManager.cleanOldFiles(applicationContext)
        }

        enableEdgeToEdge()
        setContent {
            VoiceNotesTheme {
                val navController = rememberNavController()
                val notesViewModel: NotesViewModel = hiltViewModel()
                val settingsViewModel: SettingsViewModel = hiltViewModel()
                val userPrefsState by settingsViewModel.userPreferences.collectAsState()

                // Фон чтобы не было белой вспышки при переходах
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (userPrefsState == null) {
                        // Loading state (можно пустой экран или сплэш)
                        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
                    } else if (userPrefsState?.isOnboardingCompleted == false) {
                        OnboardingScreen(
                            viewModel = settingsViewModel,
                            onComplete = {
                                // OnboardingScreen сам вызывает completeOnboarding(), 
                                // а MainActivity реактивно переключится, так как наблюдает за userPrefs
                            }
                        )
                    } else {
                        NavHost(
                        navController = navController,
                        startDestination = Screen.NotesList.route,
                        modifier = Modifier.background(MaterialTheme.colorScheme.background),
                        // Анимации перехода без fade (убирает белую вспышку)
                        enterTransition = {
                            slideIntoContainer(
                                towards = AnimatedContentTransitionScope.SlideDirection.Left,
                                animationSpec = tween(300)
                            )
                        },
                        exitTransition = {
                            slideOutOfContainer(
                                towards = AnimatedContentTransitionScope.SlideDirection.Left,
                                animationSpec = tween(300)
                            )
                        },
                        popEnterTransition = {
                            slideIntoContainer(
                                towards = AnimatedContentTransitionScope.SlideDirection.Right,
                                animationSpec = tween(300)
                            )
                        },
                        popExitTransition = {
                            slideOutOfContainer(
                                towards = AnimatedContentTransitionScope.SlideDirection.Right,
                                animationSpec = tween(300)
                            )
                        }
                    ) {
                        // Экран списка заметок
                        composable(Screen.NotesList.route) {
                            NotesListScreen(
                                viewModel = notesViewModel,
                                cacheDir = cacheDir,
                                onNoteClick = { noteId ->
                                    navController.navigate(Screen.NoteDetails.createRoute(noteId))
                                },
                                onSettingsClick = {
                                    navController.navigate(Screen.Settings.route)
                                }
                            )
                        }

                        // Экран деталей заметки
                        composable(
                            route = Screen.NoteDetails.route,
                            arguments = listOf(navArgument("noteId") { type = NavType.LongType })
                        ) {
                            NoteDetailsScreen(
                                onBackClick = { navController.popBackStack() }
                            )
                        }
                        
                        // Экран настроек
                        composable(Screen.Settings.route) {
                            SettingsScreen(
                                onBackClick = { navController.popBackStack() }
                            )
                        }
                        }
                    } // NavHost
                } // Surface (else)
            } // Surface (loading/onboarding check)

        }
    }
}