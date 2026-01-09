package com.example.voicenotes.navigation

/**
 * Маршруты навигации.
 */
sealed class Screen(val route: String) {
    object NotesList : Screen("notes_list")
    object NoteDetails : Screen("note_details/{noteId}") {
        fun createRoute(noteId: Long) = "note_details/$noteId"
    }
}
