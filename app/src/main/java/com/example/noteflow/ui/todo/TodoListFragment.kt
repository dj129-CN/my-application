package com.example.noteflow.ui.todo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.noteflow.R
import com.example.noteflow.databinding.FragmentTodoListBinding
import kotlinx.coroutines.launch

class TodoListFragment : Fragment() {

    private var _binding: FragmentTodoListBinding? = null
    private val binding: FragmentTodoListBinding
        get() = checkNotNull(_binding) { "Fragment view has already been destroyed" }

    private val viewModel: TodoListViewModel by activityViewModels()

    private val todoAdapter = TodoAdapter(
        onItemClick = { item -> openEditor(item.id) },
        onCheckedChange = { item, checked -> viewModel.setCompleted(item.id, checked) },
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

        observeTodos()
    }

    private fun observeTodos() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.todos.collect { todos ->
                    todoAdapter.submitList(todos)
                    binding.toolbar.subtitle = getString(
                        R.string.todo_list_subtitle_format,
                        todos.size,
                    )
                    binding.emptyState.isVisible = todos.isEmpty()
                    binding.todoList.isVisible = todos.isNotEmpty()
                }
            }
        }
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
}
