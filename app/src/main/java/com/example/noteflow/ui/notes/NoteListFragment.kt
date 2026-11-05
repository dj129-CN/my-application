package com.example.noteflow.ui.notes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.work.WorkManager
import com.example.noteflow.R
import com.example.noteflow.data.local.SyncPreferences
import com.example.noteflow.data.work.NoteSyncScheduler
import com.example.noteflow.databinding.FragmentNoteListBinding
import com.example.noteflow.ui.common.appContainer
import com.example.noteflow.ui.detail.NoteDetailFragment
import com.example.noteflow.ui.edit.NoteEditFragment
import com.example.noteflow.util.DateFormats
import com.example.noteflow.util.ThemeMode
import com.example.noteflow.util.ThemePreferences
import com.example.noteflow.widget.NoteWidgetUpdater
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class NoteListFragment : Fragment() {

    private var _binding: FragmentNoteListBinding? = null
    private val binding: FragmentNoteListBinding
        get() = checkNotNull(_binding) { "Fragment view has already been destroyed" }

    private val viewModel: NoteListViewModel by viewModels {
        NoteListViewModel.factory(
            noteRepository = appContainer.noteRepository,
            syncScheduler = appContainer.syncScheduler,
        )
    }

    private val noteAdapter = NoteAdapter { note -> openNoteDetail(note.id) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentNoteListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.noteList.adapter = noteAdapter
        binding.noteList.setHasFixedSize(true)

        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_theme -> {
                    showThemeChooser()
                    true
                }
                R.id.action_sync -> {
                    viewModel.requestSync()
                    true
                }
                else -> false
            }
        }

        binding.searchEdit.doAfterTextChanged { editable ->
            viewModel.onQueryChanged(editable?.toString().orEmpty())
        }

        binding.fabNewNote.setOnClickListener {
            openNoteEditor(NoteEditFragment.NEW_NOTE_ID)
        }

        observeUiState()
        observeEvents()
        observeSyncWork()
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        // The EditText restores its own text before this callback. Re-align the
        // ViewModel query after process recreation so UI state matches the input.
        viewModel.onQueryChanged(binding.searchEdit.text?.toString().orEmpty())
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

    private fun render(state: NoteListUiState) {
        noteAdapter.submitList(state.notes)
        binding.toolbar.subtitle =
            getString(R.string.list_subtitle_format, state.notes.size)

        val showEmptyState = !state.isLoading && state.notes.isEmpty()
        binding.emptyState.isVisible = showEmptyState
        binding.noteList.isVisible = !showEmptyState

        if (showEmptyState && state.query.isBlank()) {
            binding.emptyTitle.setText(R.string.list_empty_title)
            binding.emptyMessage.setText(R.string.list_empty_message)
        } else if (showEmptyState) {
            binding.emptyTitle.setText(R.string.list_search_empty_title)
            binding.emptyMessage.setText(R.string.list_search_empty_message)
        }
    }

    private fun observeEvents() {
        viewModel.events.observe(viewLifecycleOwner) { event ->
            when (event.getContentIfNotHandled() ?: return@observe) {
                NoteListEvent.SyncEnqueued ->
                    showSnackbar(getString(R.string.sync_queued))
                NoteListEvent.SyncFailed ->
                    showSnackbar(getString(R.string.error_unknown))
            }
        }
    }

    private fun observeSyncWork() {
        var wasRunning = false
        WorkManager.getInstance(requireContext())
            .getWorkInfosForUniqueWorkLiveData(NoteSyncScheduler.UNIQUE_SYNC_WORK)
            .observe(viewLifecycleOwner) { workInfos ->
                val isRunning = workInfos.any { !it.state.isFinished }
                binding.syncProgress.isVisible = isRunning

                if (wasRunning && !isRunning) {
                    val lastSyncAt = SyncPreferences(requireContext()).lastSyncAt()
                    val message = if (lastSyncAt > 0L) {
                        getString(R.string.sync_finished, DateFormats.full(lastSyncAt))
                    } else {
                        getString(R.string.sync_finished_short)
                    }
                    showSnackbar(message, Snackbar.LENGTH_LONG)
                }
                wasRunning = isRunning
            }
    }

    private fun openNoteDetail(noteId: Long) {
        findNavController().navigate(
            R.id.action_noteList_to_noteDetail,
            bundleOf(NoteDetailFragment.ARG_NOTE_ID to noteId),
        )
    }

    private fun showThemeChooser() {
        val preferences = ThemePreferences(requireContext())
        val modes = ThemeMode.values()
        val labels = modes.map { mode ->
            getString(
                when (mode) {
                    ThemeMode.SYSTEM -> R.string.theme_system
                    ThemeMode.LIGHT -> R.string.theme_light
                    ThemeMode.DARK -> R.string.theme_dark
                },
            )
        }.toTypedArray()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.theme_dialog_title)
            .setSingleChoiceItems(
                labels,
                modes.indexOf(preferences.mode),
            ) { dialog, selectedIndex ->
                val selectedMode = modes[selectedIndex]
                preferences.mode = selectedMode
                AppCompatDelegate.setDefaultNightMode(selectedMode.nightMode)
                NoteWidgetUpdater.requestUpdate(requireContext())
                dialog.dismiss()
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun openNoteEditor(noteId: Long) {
        findNavController().navigate(
            R.id.action_noteList_to_noteEdit,
            bundleOf(NoteEditFragment.ARG_NOTE_ID to noteId),
        )
    }

    private fun showSnackbar(message: CharSequence, length: Int = Snackbar.LENGTH_SHORT) {
        Snackbar.make(binding.root, message, length)
            .setAnchorView(binding.fabNewNote)
            .show()
    }

    override fun onDestroyView() {
        binding.noteList.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
