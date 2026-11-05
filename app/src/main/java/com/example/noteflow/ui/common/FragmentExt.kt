package com.example.noteflow.ui.common

import androidx.fragment.app.Fragment
import com.example.noteflow.AppContainer
import com.example.noteflow.NoteFlowApp

/**
 * Resolves the app-scoped dependency graph. Only access this property from a
 * lifecycle callback after the fragment is attached.
 */
val Fragment.appContainer: AppContainer
    get() = (requireActivity().application as NoteFlowApp).container
