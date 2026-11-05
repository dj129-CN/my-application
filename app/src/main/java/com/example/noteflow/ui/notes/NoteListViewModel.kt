package com.example.noteflow.ui.notes

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.noteflow.data.local.NoteEntity
import com.example.noteflow.data.work.NoteSyncScheduler
import com.example.noteflow.repository.NoteRepository
import com.example.noteflow.util.Event
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class NoteListUiState(
    val notes: List<NoteEntity> = emptyList(),
    val query: String = "",
    val isLoading: Boolean = true,
)

sealed interface NoteListEvent {
    data object SyncEnqueued : NoteListEvent
    data object SyncFailed : NoteListEvent
}

/**
 * Screen state is a cold Room Flow combined with the search query and exposed as
 * a lifecycle-aware StateFlow. One-shot UI signals stay on LiveData.
 */
class NoteListViewModel(
    private val noteRepository: NoteRepository,
    private val syncScheduler: NoteSyncScheduler,
) : ViewModel() {

    private val query = MutableStateFlow("")

    private val _events = MutableLiveData<Event<NoteListEvent>>()
    val events: LiveData<Event<NoteListEvent>> = _events

    val uiState: StateFlow<NoteListUiState> = combine(
        noteRepository.observeNotes(),
        query,
    ) { notes, rawQuery ->
        val normalizedQuery = rawQuery.trim()
        val visibleNotes = if (normalizedQuery.isEmpty()) {
            notes
        } else {
            notes.filter { note ->
                note.title.contains(normalizedQuery, ignoreCase = true) ||
                    note.content.contains(normalizedQuery, ignoreCase = true)
            }
        }
        NoteListUiState(
            notes = visibleNotes,
            query = normalizedQuery,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = NoteListUiState(),
    )

    fun onQueryChanged(newQuery: String) {
        query.value = newQuery
    }

    fun requestSync() {
        try {
            syncScheduler.enqueueSync()
            _events.value = Event(NoteListEvent.SyncEnqueued)
        } catch (_: IllegalStateException) {
            _events.value = Event(NoteListEvent.SyncFailed)
        }
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        fun factory(
            noteRepository: NoteRepository,
            syncScheduler: NoteSyncScheduler,
        ) = viewModelFactory {
            initializer {
                NoteListViewModel(noteRepository, syncScheduler)
            }
        }
    }
}
