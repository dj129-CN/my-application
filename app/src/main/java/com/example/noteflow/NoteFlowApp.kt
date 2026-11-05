package com.example.noteflow

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.example.noteflow.util.ThemePreferences
import com.example.noteflow.widget.NoteWidgetUpdater

/**
 * Application composition root. Dependencies are wired once here instead of
 * being created ad hoc inside UI classes.
 */
class NoteFlowApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        // Restore the user's light/dark/system choice before any activity starts.
        AppCompatDelegate.setDefaultNightMode(
            ThemePreferences(this).mode.nightMode,
        )
        container = AppContainer(this)
        container.syncScheduler.scheduleWidgetRefresh()
        NoteWidgetUpdater.requestUpdate(this)
    }
}
