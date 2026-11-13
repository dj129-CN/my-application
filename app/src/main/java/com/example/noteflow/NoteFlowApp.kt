package com.example.noteflow

import android.app.Application
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import com.example.noteflow.util.ThemePreferences
import com.example.noteflow.widget.NoteWidgetUpdater

/**
 * Application composition root. Dependencies are wired once here instead of
 * being created ad hoc inside UI classes.
 *
 * Secondary processes (such as the debug provider probe) still create the
 * container, but must not initialize WorkManager or the widget side effects.
 */
class NoteFlowApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        val mainProcess = isMainProcess()

        if (mainProcess) {
            // Restore the user's light/dark/system choice before any activity starts.
            AppCompatDelegate.setDefaultNightMode(
                ThemePreferences(this).mode.nightMode,
            )
        }

        container = AppContainer(this)

        if (mainProcess) {
            container.syncScheduler.scheduleWidgetRefresh()
            NoteWidgetUpdater.requestUpdate(this)
        }
    }

    private fun isMainProcess(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.P ||
            Application.getProcessName() == packageName
}
