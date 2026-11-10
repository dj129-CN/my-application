package com.example.noteflow.ui.todo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TodoListViewModelTest {

    @Test
    fun saveTodo_addsNewItemAndReturnsId() {
        val viewModel = TodoListViewModel()

        val id = viewModel.saveTodo(id = -1L, title = "  写界面  ", content = "  空壳  ")

        assertTrue(id > 0L)
        val item = viewModel.findItem(id)
        assertEquals("写界面", item?.title)
        assertEquals("空壳", item?.content)
        assertEquals(listOf(item), viewModel.todos.value)
    }

    @Test
    fun saveTodo_updatesExistingItemWithoutAddingAnother() {
        val viewModel = TodoListViewModel()
        val id = viewModel.saveTodo(-1L, "旧标题", "旧内容")

        viewModel.saveTodo(id, "新标题", "新内容")

        assertEquals(1, viewModel.todos.value.size)
        assertEquals("新标题", viewModel.findItem(id)?.title)
        assertEquals("新内容", viewModel.findItem(id)?.content)
    }

    @Test
    fun setCompleted_togglesOnlyTargetItem() {
        val viewModel = TodoListViewModel()
        val first = viewModel.saveTodo(-1L, "一", "")
        val second = viewModel.saveTodo(-1L, "二", "")

        viewModel.setCompleted(second, true)

        assertEquals(false, viewModel.findItem(first)?.isCompleted)
        assertEquals(true, viewModel.findItem(second)?.isCompleted)
        assertNull(viewModel.findItem(999L))
    }
}
