package com.example.noteflow.ui.todo

import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.noteflow.R
import com.example.noteflow.data.local.TodoEntity
import com.example.noteflow.repository.TodoRepository
import com.example.noteflow.util.DataResult
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TodoListUiState(
    val todos: List<TodoEntity> = emptyList(),
    val isLoading: Boolean = true,
)

sealed interface TodoListEvent {
    data class ShowToast(@StringRes val messageRes: Int) : TodoListEvent
}

/**
 * Room is the single source of truth: the list observes the repository Flow,
 * while mutations go through the same repository and re-emit automatically.
 */
class TodoListViewModel(
    private val todoRepository: TodoRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TodoListUiState())
    val uiState: StateFlow<TodoListUiState> = _uiState.asStateFlow()

    private val _events = Channel<TodoListEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            todoRepository.observeTodosNewestFirst().collect { result ->
                when (result) {
                    is DataResult.Success -> {
                        _uiState.update {
                            it.copy(todos = result.data, isLoading = false)
                        }
                    }
                    is DataResult.Failure -> {
                        Log.e(TAG, "Unable to observe todos", result.cause)
                        _uiState.update { it.copy(isLoading = false) }
                        _events.send(TodoListEvent.ShowToast(R.string.todo_operation_failed))
                    }
                }
            }
        }
    }

    fun setCompleted(todo: TodoEntity, completed: Boolean) {
        if (todo.isCompleted == completed) return

        // Keep the checkbox responsive; Room will confirm the change with a new
        // list emission. On failure, restore the previous visual state.
        val previous = todo.isCompleted
        _uiState.update { state ->
            state.copy(
                todos = state.todos.map {
                    if (it.id == todo.id) it.copy(isCompleted = completed) else it
                },
            )
        }

        viewModelScope.launch {
            when (val result = todoRepository.updateCompletion(todo.id, completed)) {
                is DataResult.Success -> {
                    _events.send(
                        TodoListEvent.ShowToast(
                            if (completed) R.string.todo_completed else R.string.todo_reopened,
                        ),
                    )
                }
                is DataResult.Failure -> {
                    Log.e(TAG, "Unable to update todo completion", result.cause)
                    _uiState.update { state ->
                        state.copy(
                            todos = state.todos.map {
                                if (it.id == todo.id) it.copy(isCompleted = previous) else it
                            },
                        )
                    }
                    _events.send(TodoListEvent.ShowToast(R.string.todo_operation_failed))
                }
            }
        }
    }

    fun delete(todo: TodoEntity) {
        viewModelScope.launch {
            when (val result = todoRepository.delete(todo)) {
                is DataResult.Success -> {
                    _events.send(TodoListEvent.ShowToast(R.string.todo_deleted))
                }
                is DataResult.Failure -> {
                    Log.e(TAG, "Unable to delete todo", result.cause)
                    _events.send(TodoListEvent.ShowToast(R.string.todo_operation_failed))
                }
            }
        }
    }

    companion object {
        private const val TAG = "TodoListViewModel"

        fun factory(todoRepository: TodoRepository) = viewModelFactory {
            initializer { TodoListViewModel(todoRepository) }
        }
    }
}
