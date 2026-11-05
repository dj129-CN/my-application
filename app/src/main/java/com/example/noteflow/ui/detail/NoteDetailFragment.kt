package com.example.noteflow.ui.detail

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
import com.example.noteflow.R
import com.example.noteflow.databinding.FragmentNoteDetailBinding
import com.example.noteflow.ui.common.appContainer
import com.example.noteflow.ui.edit.NoteEditFragment
import com.example.noteflow.util.DateFormats
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class NoteDetailFragment : Fragment() {

    private var _binding: FragmentNoteDetailBinding? = null
    private val binding: FragmentNoteDetailBinding
        get() = checkNotNull(_binding) { "Fragment view has already been destroyed" }

    private val noteId: Long by lazy {
        requireArguments().getLong(ARG_NOTE_ID)
    }

    private val viewModel: NoteDetailViewModel by viewModels {
        NoteDetailViewModel.factory(
            noteRepository = appContainer.noteRepository,
            noteId = noteId,
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentNoteDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_edit -> {
                    openEditor()
                    true
                }
                R.id.action_delete -> {
                    confirmDelete()
                    true
                }
                else -> false
            }
        }
        setActionsVisible(false)

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

    private fun render(state: NoteDetailUiState) {
        val note = state.note
        binding.detailLoading.isVisible = state.isLoading
        binding.detailScroll.isVisible = note != null
        binding.missingState.isVisible = !state.isLoading && note == null
        setActionsVisible(note != null)

        if (note != null) {
            binding.detailTitle.text = note.title
            binding.detailCreated.text = getString(
                R.string.detail_created,
                DateFormats.full(note.createdAt),
            )
            binding.detailUpdated.text = getString(
                R.string.detail_updated,
                DateFormats.full(note.updatedAt),
            )
            binding.detailContent.text = note.content.ifBlank {
                getString(R.string.detail_content_placeholder)
            }
        }
    }

    private fun setActionsVisible(visible: Boolean) {
        binding.toolbar.menu.findItem(R.id.action_edit)?.isVisible = visible
        binding.toolbar.menu.findItem(R.id.action_edit)?.isEnabled = visible
        binding.toolbar.menu.findItem(R.id.action_delete)?.isVisible = visible
        binding.toolbar.menu.findItem(R.id.action_delete)?.isEnabled = visible
    }

    private fun observeEvents() {
        viewModel.events.observe(viewLifecycleOwner) { event ->
            when (event.getContentIfNotHandled() ?: return@observe) {
                NoteDetailEvent.Deleted -> {
                    Toast.makeText(
                        requireContext(),
                        R.string.detail_deleted,
                        Toast.LENGTH_SHORT,
                    ).show()
                    findNavController().navigateUp()
                }
                NoteDetailEvent.DeleteFailed -> {
                    Snackbar.make(
                        binding.root,
                        R.string.error_unknown,
                        Snackbar.LENGTH_LONG,
                    ).show()
                }
            }
        }
    }

    private fun openEditor() {
        val note = viewModel.uiState.value.note ?: return
        findNavController().navigate(
            R.id.action_noteDetail_to_noteEdit,
            bundleOf(NoteEditFragment.ARG_NOTE_ID to note.id),
        )
    }

    private fun confirmDelete() {
        if (viewModel.uiState.value.note == null) return
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.detail_delete_title)
            .setMessage(R.string.detail_delete_message)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_confirm_delete) { _, _ ->
                viewModel.deleteNote()
            }
            .show()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val ARG_NOTE_ID = "noteId"
    }
}
