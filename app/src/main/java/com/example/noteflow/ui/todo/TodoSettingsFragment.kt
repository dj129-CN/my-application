package com.example.noteflow.ui.todo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.noteflow.R
import com.example.noteflow.databinding.FragmentTodoSettingsBinding
import com.example.noteflow.util.ThemeMode
import com.example.noteflow.util.ThemePreferences
import com.example.noteflow.widget.NoteWidgetUpdater
import com.google.android.material.snackbar.Snackbar

class TodoSettingsFragment : Fragment() {

    private var _binding: FragmentTodoSettingsBinding? = null
    private val binding: FragmentTodoSettingsBinding
        get() = checkNotNull(_binding) { "Fragment view has already been destroyed" }

    private lateinit var themePreferences: ThemePreferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentTodoSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        themePreferences = ThemePreferences(requireContext())

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        binding.themeGroup.check(
            when (themePreferences.mode) {
                ThemeMode.SYSTEM -> R.id.themeSystemButton
                ThemeMode.LIGHT -> R.id.themeLightButton
                ThemeMode.DARK -> R.id.themeDarkButton
            },
        )

        // Add the listener after the initial check so restoring the state does
        // not trigger an unnecessary activity recreation.
        binding.themeGroup.setOnCheckedChangeListener { _, checkedId ->
            val mode = when (checkedId) {
                R.id.themeSystemButton -> ThemeMode.SYSTEM
                R.id.themeLightButton -> ThemeMode.LIGHT
                R.id.themeDarkButton -> ThemeMode.DARK
                else -> return@setOnCheckedChangeListener
            }
            if (mode == themePreferences.mode) return@setOnCheckedChangeListener

            themePreferences.mode = mode
            NoteWidgetUpdater.requestUpdate(requireContext())
            AppCompatDelegate.setDefaultNightMode(mode.nightMode)
        }

        binding.refreshWidgetButton.setOnClickListener {
            NoteWidgetUpdater.requestUpdate(requireContext())
            Snackbar.make(
                binding.root,
                R.string.todo_settings_widget_refreshed,
                Snackbar.LENGTH_SHORT,
            ).show()
        }

        binding.openNotesButton.setOnClickListener {
            findNavController().navigate(R.id.action_todoSettings_to_noteList)
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
