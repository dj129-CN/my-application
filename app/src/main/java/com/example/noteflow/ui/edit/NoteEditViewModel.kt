package com.example.noteflow.ui.edit

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.noteflow.repository.NoteRepository
import com.example.noteflow.util.Event
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NoteEditUiState(
    val title: String = "",
    val content: String = "",
    val updatedAt: Long? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val titleError: Boolean = false,
)

sealed interface NoteEditEvent {
    data object Saved : NoteEditEvent
    data object NotFound : NoteEditEvent
    data object LoadFailed : NoteEditEvent
    data object SaveFailed : NoteEditEvent
}

class NoteEditViewModel(
    private val noteRepository: NoteRepository,
    private val noteId: Long,
) : ViewModel() {

    private val isNewNote = noteId <= 0L

    private val _uiState = MutableStateFlow(
        NoteEditUiState(isLoading = !isNewNote),
    )
    val uiState: StateFlow<NoteEditUiState> = _uiState.asStateFlow()

    private val _events = MutableLiveData<Event<NoteEditEvent>>()
    val events: LiveData<Event<NoteEditEvent>> = _events

    init {
        if (!isNewNote) {
            loadNote()
        }
    }

    fun onTitleChanged(value: String) {
        _uiState.update { state ->
            if (state.title == value) {
                state
            } else {
                state.copy(title = value, titleError = false)
            }
        }
    }

    fun onContentChanged(value: String) {
        _uiState.update { state ->
            if (state.content == value) state else state.copy(content = value)
        }
    }

    fun save() {
        val snapshot = _uiState.value
        if (snapshot.isLoading || snapshot.isSaving) return

        if (snapshot.title.isBlank()) {
            _uiState.update { it.copy(titleError = true) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, titleError = false) }
            try {
                noteRepository.saveNote(
                    noteId = noteId,
                    title = snapshot.title,
                    content = snapshot.content,
                )
                _events.value = Event(NoteEditEvent.Saved)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                _events.value = Event(NoteEditEvent.SaveFailed)
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    private fun loadNote() {
        viewModelScope.launch {
            try {
                val note = noteRepository.getNote(noteId)
                if (note == null) {
                    _uiState.update { it.copy(isLoading = false) }
                    _events.value = Event(NoteEditEvent.NotFound)
                } else {
                    _uiState.value = NoteEditUiState(
                        title = note.title,
                        content = note.content,
                        updatedAt = note.updatedAt,
                        isLoading = false,
                    )
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoading = false) }
                _events.value = Event(NoteEditEvent.LoadFailed)
            }
        }
    }

    companion object {
        fun factory(noteRepository: NoteRepository, noteId: Long) = viewModelFactory {
            initializer {
                NoteEditViewModel(noteRepository, noteId)
            }
        }
    }
}
