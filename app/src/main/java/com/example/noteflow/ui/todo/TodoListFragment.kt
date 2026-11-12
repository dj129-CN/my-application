package com.example.noteflow.ui.todo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import com.example.noteflow.R
import com.example.noteflow.data.local.TodoEntity
import com.example.noteflow.databinding.FragmentTodoListBinding
import com.example.noteflow.ui.common.appContainer
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class TodoListFragment : Fragment() {

    private var _binding: FragmentTodoListBinding? = null
    private val binding: FragmentTodoListBinding
        get() = checkNotNull(_binding) { "Fragment view has already been destroyed" }

    private val viewModel: TodoListViewModel by viewModels {
        TodoListViewModel.factory(appContainer.todoRepository)
    }

    private val todoAdapter = TodoAdapter(
        onItemClick = { item -> openEditor(item.id) },
        onItemLongClick = { item -> confirmDelete(item) },
        onCheckedChange = { item, checked -> viewModel.setCompleted(item, checked) },
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentTodoListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.todoList.adapter = todoAdapter
        binding.todoList.setHasFixedSize(true)
        (binding.todoList.itemAnimator as? DefaultItemAnimator)?.apply {
            addDuration = ITEM_ANIMATION_DURATION
            changeDuration = ITEM_ANIMATION_DURATION
            removeDuration = ITEM_ANIMATION_DURATION
        }

        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_todo_settings -> {
                    findNavController().navigate(R.id.action_todoList_to_todoSettings)
                    true
                }
                else -> false
            }
        }

        binding.fabAddTodo.setOnClickListener {
            openEditor(TodoEditFragment.NEW_TODO_ID)
        }

        observeUiState()
        observeEvents()
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    todoAdapter.submitList(state.todos)
                    binding.toolbar.subtitle = getString(
                        R.string.todo_list_subtitle_format,
                        state.todos.size,
                    )
                    binding.listProgress.isVisible = state.isLoading
                    binding.emptyState.isVisible = !state.isLoading && state.todos.isEmpty()
                    binding.todoList.isVisible = state.todos.isNotEmpty()
                }
            }
        }
    }

    private fun observeEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    val messageRes = when (event) {
                        is TodoListEvent.ShowToast -> event.messageRes
                    }
                    Toast.makeText(
                        requireContext(),
                        messageRes,
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
        }
    }

    private fun confirmDelete(todo: TodoEntity) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.todo_delete_title)
            .setMessage(getString(R.string.todo_delete_message, todo.title))
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_delete) { _, _ ->
                viewModel.delete(todo)
            }
            .show()
    }

    private fun openEditor(todoId: Long) {
        findNavController().navigate(
            R.id.action_todoList_to_todoEdit,
            bundleOf(TodoEditFragment.ARG_TODO_ID to todoId),
        )
    }

    override fun onDestroyView() {
        binding.todoList.adapter = null
        _binding = null
        super.onDestroyView()
    }

    private companion object {
        const val ITEM_ANIMATION_DURATION = 180L
    }
}
