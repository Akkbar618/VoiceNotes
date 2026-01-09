package com.example.voicenotes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.voicenotes.navigation.Screen
import com.example.voicenotes.ui.NoteDetailsScreen
import com.example.voicenotes.ui.theme.VoiceNotesTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val app = application as App
        val viewModelFactory = NotesViewModelFactory(app.repository)
        
        enableEdgeToEdge()
        setContent {
            VoiceNotesTheme {
                val navController = rememberNavController()
                val notesViewModel: NotesViewModel = viewModel(factory = viewModelFactory)
                
                // Фон чтобы не было белой вспышки при переходах
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
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
                            }
                        )
                    }
                    
                    // Экран деталей заметки
                    composable(
                        route = Screen.NoteDetails.route,
                        arguments = listOf(navArgument("noteId") { type = NavType.LongType })
                    ) { backStackEntry ->
                        val noteId = backStackEntry.arguments?.getLong("noteId") ?: return@composable
                        val scope = rememberCoroutineScope()
                        var note by remember { mutableStateOf<NoteUi?>(null) }
                        
                        LaunchedEffect(noteId) {
                            note = notesViewModel.getNoteById(noteId)
                        }
                        
                        note?.let { currentNote ->
                            NoteDetailsScreen(
                                note = currentNote,
                                onBackClick = { navController.popBackStack() },
                                onDeleteClick = {
                                    scope.launch {
                                        notesViewModel.deleteNote(noteId)
                                        navController.popBackStack()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}