package com.example.noteflow.ui.edit

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.noteflow.R
import com.example.noteflow.databinding.FragmentNoteEditBinding
import com.example.noteflow.ui.common.appContainer
import com.example.noteflow.util.DateFormats
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class NoteEditFragment : Fragment() {

    private var _binding: FragmentNoteEditBinding? = null
    private val binding: FragmentNoteEditBinding
        get() = checkNotNull(_binding) { "Fragment view has already been destroyed" }

    private val noteId: Long by lazy {
        arguments?.getLong(ARG_NOTE_ID) ?: NEW_NOTE_ID
    }

    private val viewModel: NoteEditViewModel by viewModels {
        NoteEditViewModel.factory(
            noteRepository = appContainer.noteRepository,
            noteId = noteId,
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentNoteEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.title = getString(
            if (noteId <= 0L) R.string.edit_title_new else R.string.edit_title_edit,
        )
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        binding.titleInput.doAfterTextChanged { editable ->
            viewModel.onTitleChanged(editable?.toString().orEmpty())
        }
        binding.contentInput.doAfterTextChanged { editable ->
            viewModel.onContentChanged(editable?.toString().orEmpty())
        }
        binding.saveButton.setOnClickListener {
            viewModel.save()
        }

        observeUiState()
        observeEvents()
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    render(state)
                }
            }
        }
    }

    private fun render(state: NoteEditUiState) {
        binding.editLoading.isVisible = state.isLoading
        binding.titleInput.isEnabled = !state.isLoading && !state.isSaving
        binding.contentInput.isEnabled = !state.isLoading && !state.isSaving
        binding.saveButton.isEnabled = !state.isLoading && !state.isSaving
        binding.saveButton.setText(
            if (state.isSaving) R.string.edit_saving else R.string.action_save,
        )
        binding.titleLayout.error = if (state.titleError) {
            getString(R.string.edit_title_required)
        } else {
            null
        }

        if (binding.titleInput.text?.toString() != state.title) {
            binding.titleInput.setText(state.title)
            binding.titleInput.setSelection(state.title.length)
        }
        if (binding.contentInput.text?.toString() != state.content) {
            binding.contentInput.setText(state.content)
            binding.contentInput.setSelection(0)
        }

        binding.editMeta.text = state.updatedAt?.let { updatedAt ->
            getString(R.string.edit_meta_saved, DateFormats.full(updatedAt))
        } ?: getString(R.string.edit_meta_new)
    }

    private fun observeEvents() {
        viewModel.events.observe(viewLifecycleOwner) { event ->
            when (event.getContentIfNotHandled() ?: return@observe) {
                NoteEditEvent.Saved -> {
                    hideKeyboard()
                    findNavController().navigateUp()
                }
                NoteEditEvent.NotFound -> {
                    Toast.makeText(
                        requireContext(),
                        R.string.note_not_found,
                        Toast.LENGTH_SHORT,
                    ).show()
                    findNavController().navigateUp()
                }
                NoteEditEvent.LoadFailed,
                NoteEditEvent.SaveFailed -> {
                    Snackbar.make(
                        binding.root,
                        R.string.error_unknown,
                        Snackbar.LENGTH_LONG,
                    ).show()
                }
            }
        }
    }

    private fun hideKeyboard() {
        val inputMethodManager = requireContext()
            .getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        inputMethodManager?.hideSoftInputFromWindow(binding.root.windowToken, 0)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val ARG_NOTE_ID = "noteId"
        const val NEW_NOTE_ID = -1L
    }
}
