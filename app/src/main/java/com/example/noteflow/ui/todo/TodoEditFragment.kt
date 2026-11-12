package com.example.noteflow.ui.todo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.noteflow.R
import com.example.noteflow.databinding.FragmentTodoEditBinding
import com.example.noteflow.ui.common.appContainer
import kotlinx.coroutines.launch

class TodoEditFragment : Fragment() {

    private var _binding: FragmentTodoEditBinding? = null
    private val binding: FragmentTodoEditBinding
        get() = checkNotNull(_binding) { "Fragment view has already been destroyed" }

    private val todoId: Long
        get() = arguments?.getLong(ARG_TODO_ID) ?: NEW_TODO_ID

    private val viewModel: TodoEditViewModel by viewModels {
        TodoEditViewModel.factory(appContainer.todoRepository, todoId)
    }

    private var prefillEnabled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefillEnabled = savedInstanceState == null
    }

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
        binding.storageHint.text = getString(R.string.todo_edit_storage_hint)

        binding.cancelButton.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.saveButton.setOnClickListener {
            saveTodo()
        }

        observeUiState()
        observeEvents()
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    val editable = !state.isLoading && !state.isSaving
                    binding.editProgress.isVisible = state.isLoading || state.isSaving
                    binding.titleInput.isEnabled = editable
                    binding.contentInput.isEnabled = editable
                    binding.saveButton.isEnabled = editable
                    binding.cancelButton.isEnabled = !state.isSaving
                    binding.saveButton.text = getString(
                        if (state.isSaving) R.string.todo_saving else R.string.todo_action_save,
                    )

                    if (prefillEnabled && state.initialized) {
                        prefillEnabled = false
                        binding.titleInput.setText(state.title)
                        binding.contentInput.setText(state.content)
                    }
                }
            }
        }
    }

    private fun observeEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    when (event) {
                        TodoEditEvent.Saved -> {
                            Toast.makeText(requireContext(), R.string.todo_saved, Toast.LENGTH_SHORT)
                                .show()
                            findNavController().navigateUp()
                        }
                        TodoEditEvent.Missing -> {
                            Toast.makeText(
                                requireContext(),
                                R.string.todo_missing_item,
                                Toast.LENGTH_SHORT,
                            ).show()
                            findNavController().navigateUp()
                        }
                        TodoEditEvent.LoadFailed -> {
                            Toast.makeText(
                                requireContext(),
                                R.string.todo_operation_failed,
                                Toast.LENGTH_SHORT,
                            ).show()
                            findNavController().navigateUp()
                        }
                        TodoEditEvent.OperationFailed -> {
                            Toast.makeText(
                                requireContext(),
                                R.string.todo_operation_failed,
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    }
                }
            }
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
        viewModel.save(
            title = title,
            content = binding.contentInput.text?.toString().orEmpty(),
        )
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
