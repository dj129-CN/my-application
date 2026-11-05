package com.example.noteflow.ui.detail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.noteflow.data.local.NoteEntity
import com.example.noteflow.repository.NoteRepository
import com.example.noteflow.util.Event
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class NoteDetailUiState(
    val note: NoteEntity? = null,
    val isLoading: Boolean = true,
)

sealed interface NoteDetailEvent {
    data object Deleted : NoteDetailEvent
    data object DeleteFailed : NoteDetailEvent
}

class NoteDetailViewModel(
    private val noteRepository: NoteRepository,
    private val noteId: Long,
) : ViewModel() {

    private val _events = MutableLiveData<Event<NoteDetailEvent>>()
    val events: LiveData<Event<NoteDetailEvent>> = _events

    val uiState: StateFlow<NoteDetailUiState> = noteRepository.observeNote(noteId)
        .map { note -> NoteDetailUiState(note = note, isLoading = false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = NoteDetailUiState(),
        )

    fun deleteNote() {
        viewModelScope.launch {
            try {
                val deleted = noteRepository.deleteNote(noteId)
                _events.value = Event(
                    if (deleted) NoteDetailEvent.Deleted else NoteDetailEvent.DeleteFailed,
                )
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                _events.value = Event(NoteDetailEvent.DeleteFailed)
            }
        }
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        fun factory(noteRepository: NoteRepository, noteId: Long) = viewModelFactory {
            initializer {
                NoteDetailViewModel(noteRepository, noteId)
            }
        }
    }
}
