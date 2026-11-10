package com.example.noteflow.ui.todo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.noteflow.R
import com.example.noteflow.databinding.FragmentTodoEditBinding

class TodoEditFragment : Fragment() {

    private var _binding: FragmentTodoEditBinding? = null
    private val binding: FragmentTodoEditBinding
        get() = checkNotNull(_binding) { "Fragment view has already been destroyed" }

    private val viewModel: TodoListViewModel by activityViewModels()

    private val todoId: Long
        get() = arguments?.getLong(ARG_TODO_ID) ?: NEW_TODO_ID

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentTodoEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val editing = todoId > 0L
        binding.toolbar.title = getString(
            if (editing) R.string.todo_edit_title_edit else R.string.todo_edit_title_new,
        )
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
        binding.demoHint.text = getString(R.string.todo_edit_demo_hint, todoId)

        if (editing) {
            val item = viewModel.findItem(todoId)
            if (item == null) {
                Toast.makeText(requireContext(), R.string.todo_missing_item, Toast.LENGTH_SHORT)
                    .show()
                findNavController().navigateUp()
                return
            }
            binding.titleInput.setText(item.title)
            binding.contentInput.setText(item.content)
        }

        binding.cancelButton.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.saveButton.setOnClickListener {
            saveTodo()
        }
    }

    private fun saveTodo() {
        val title = binding.titleInput.text?.toString().orEmpty().trim()
        if (title.isEmpty()) {
            binding.titleLayout.error = getString(R.string.todo_edit_title_required)
            binding.titleInput.requestFocus()
            return
        }

        binding.titleLayout.error = null
        viewModel.saveTodo(
            id = todoId,
            title = title,
            content = binding.contentInput.text?.toString().orEmpty(),
        )
        findNavController().navigateUp()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val ARG_TODO_ID = "todoId"
        const val NEW_TODO_ID = -1L
    }
}
