package com.example.noteflow.ui.todo

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * In-memory demo state for the UI shell. It deliberately does not touch Room or
 * TodoRepository: this stage only verifies navigation, forms and list rendering.
 * The ViewModel is activity-scoped so the list and editor share the same items.
 */
data class TodoDemoItem(
    val id: Long,
    val title: String,
    val content: String = "",
    val isCompleted: Boolean = false,
)

class TodoListViewModel : ViewModel() {

    private val _todos = MutableStateFlow<List<TodoDemoItem>>(emptyList())
    val todos: StateFlow<List<TodoDemoItem>> = _todos.asStateFlow()

    private var nextId = 1L

    fun findItem(id: Long): TodoDemoItem? =
        _todos.value.firstOrNull { it.id == id }

    fun saveTodo(id: Long, title: String, content: String): Long {
        val normalizedTitle = title.trim()
        require(normalizedTitle.isNotEmpty()) { "Todo title must not be blank" }
        val normalizedContent = content.trim()

        if (id > 0L) {
            require(_todos.value.any { it.id == id }) { "Demo todo $id does not exist" }
            _todos.update { current ->
                current.map { item ->
                    if (item.id == id) {
                        item.copy(
                            title = normalizedTitle,
                            content = normalizedContent,
                        )
                    } else {
                        item
                    }
                }
            }
            return id
        }

        val item = TodoDemoItem(
            id = nextId++,
            title = normalizedTitle,
            content = normalizedContent,
        )
        _todos.update { listOf(item) + it }
        return item.id
    }

    fun setCompleted(id: Long, completed: Boolean) {
        _todos.update { current ->
            current.map { item ->
                if (item.id == id) item.copy(isCompleted = completed) else item
            }
        }
    }
}
