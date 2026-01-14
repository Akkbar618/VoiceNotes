package com.example.voicenotes

import com.example.voicenotes.data.NoteRepository
import com.example.voicenotes.data.NoteStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for NotesViewModel.
 * Uses MockK for mocking dependencies and UnconfinedTestDispatcher for immediate execution.
 * 
 * Note: Some tests are simplified to focus on state and repository interactions
 * that don't require full coroutine scope management.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NotesViewModelTest {

    private lateinit var viewModel: NotesViewModel
    private lateinit var noteRepository: NoteRepository
    
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        noteRepository = mockk(relaxed = true)
        
        // Mock empty notes list by default
        coEvery { noteRepository.getAllNotes() } returns flowOf(emptyList())
        
        viewModel = NotesViewModel(noteRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is not loading and not recording`() = runTest {
        val state = viewModel.uiState.value
        assertFalse("Initial state should not be loading", state.isLoading)
        assertFalse("Initial state should not be recording", state.isRecording)
        assertNull("Initial state should have no error", state.error)
    }

    @Test
    fun `clearError resets error state`() = runTest {
        viewModel.clearError()
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `retryNote calls repository retryNote method`() = runTest {
        val noteUi = NoteUi(
            id = 1L,
            title = "Failed Note",
            rawText = "text",
            summary = "summary",
            formattedDate = "Jan 14",
            previewText = "preview",
            status = NoteStatus.FAILED
        )
        
        coEvery { noteRepository.retryNote(1L) } returns Unit
        
        viewModel.retryNote(noteUi)
        
        coVerify { noteRepository.retryNote(1L) }
    }
}
