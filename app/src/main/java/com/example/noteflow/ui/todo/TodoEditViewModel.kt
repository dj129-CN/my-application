package com.example.noteflow.ui.todo

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.noteflow.data.local.TodoEntity
import com.example.noteflow.repository.TodoRepository
import com.example.noteflow.util.DataError
import com.example.noteflow.util.DataResult
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TodoEditUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val title: String = "",
    val content: String = "",
    val initialized: Boolean = false,
)

sealed interface TodoEditEvent {
    data object Saved : TodoEditEvent
    data object Missing : TodoEditEvent
    data object LoadFailed : TodoEditEvent
    data object OperationFailed : TodoEditEvent
}

class TodoEditViewModel(
    private val todoRepository: TodoRepository,
    private val todoId: Long,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TodoEditUiState())
    val uiState: StateFlow<TodoEditUiState> = _uiState.asStateFlow()

    private val _events = Channel<TodoEditEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var loadedTodo: TodoEntity? = null

    init {
        viewModelScope.launch {
            loadTodo()
        }
    }

    private suspend fun loadTodo() {
        if (todoId <= 0L) {
            _uiState.update { it.copy(isLoading = false, initialized = true) }
            return
        }

        when (val result = todoRepository.getById(todoId)) {
            is DataResult.Success -> {
                loadedTodo = result.data
                _uiState.value = TodoEditUiState(
                    isLoading = false,
                    title = result.data.title,
                    content = result.data.content,
                    initialized = true,
                )
            }
            is DataResult.Failure -> {
                Log.e(TAG, "Unable to load todo $todoId", result.cause)
                _uiState.update { it.copy(isLoading = false, initialized = true) }
                _events.send(
                    if (result.type == DataError.NOT_FOUND) {
                        TodoEditEvent.Missing
                    } else {
                        TodoEditEvent.LoadFailed
                    },
                )
            }
        }
    }

    fun save(title: String, content: String) {
        val state = _uiState.value
        if (state.isLoading || state.isSaving) return

        val normalizedTitle = title.trim()
        if (normalizedTitle.isEmpty()) return
        val normalizedContent = content.trim()

        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val result: DataResult<TodoEntity> = if (todoId > 0L) {
                val existing = loadedTodo
                if (existing == null) {
                    DataResult.Failure(
                        type = DataError.NOT_FOUND,
                        message = "Todo $todoId was not loaded",
                    )
                } else {
                    todoRepository.update(
                        existing.copy(
                            title = normalizedTitle,
                            content = normalizedContent,
                        ),
                    )
                }
            } else {
                todoRepository.insert(
                    TodoEntity(
                        title = normalizedTitle,
                        content = normalizedContent,
                    ),
                )
            }

            _uiState.update { it.copy(isSaving = false) }

            when (result) {
                is DataResult.Success -> _events.send(TodoEditEvent.Saved)
                is DataResult.Failure -> {
                    Log.e(TAG, "Unable to save todo", result.cause)
                    _events.send(
                        if (result.type == DataError.NOT_FOUND) {
                            TodoEditEvent.Missing
                        } else {
                            TodoEditEvent.OperationFailed
                        },
                    )
                }
            }
        }
    }

    companion object {
        private const val TAG = "TodoEditViewModel"

        fun factory(todoRepository: TodoRepository, todoId: Long) = viewModelFactory {
            initializer { TodoEditViewModel(todoRepository, todoId) }
        }
    }
}
