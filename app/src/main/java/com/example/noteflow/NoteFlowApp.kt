package com.example.noteflow

import android.app.Application
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import com.example.noteflow.data.work.WidgetRefreshScheduler
import com.example.noteflow.util.ThemePreferences
import com.example.noteflow.widget.NoteWidgetProvider
import com.example.noteflow.widget.NoteWidgetUpdater
import com.example.noteflow.widget.TodoQuickWidgetProvider
import com.example.noteflow.widget.TodoQuickWidgetUpdater
import com.example.noteflow.widget.TodoWidgetProvider
import com.example.noteflow.widget.TodoWidgetUpdater
import com.example.noteflow.widget.WidgetRefreshManager

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
            AppCompatDelegate.setDefaultNightMode(
                ThemePreferences(this).mode.nightMode,
            )
        }

        container = AppContainer(this)

        if (mainProcess) {
            // Register all widget types with the unified refresh manager.
            WidgetRefreshManager.register(NoteWidgetProvider::class.java) { ctx ->
                NoteWidgetUpdater.updateAll(ctx)
            }
            WidgetRefreshManager.register(TodoWidgetProvider::class.java) { ctx ->
                TodoWidgetUpdater.updateAll(ctx)
            }
            WidgetRefreshManager.register(TodoQuickWidgetProvider::class.java) { ctx ->
                TodoQuickWidgetUpdater.updateAll(ctx)
            }

            // Schedule periodic 30-minute refresh for all registered widgets.
            WidgetRefreshScheduler.schedule(this)

            // Initial render.
            NoteWidgetUpdater.requestUpdate(this)
            TodoWidgetUpdater.requestUpdate(this)
            TodoQuickWidgetUpdater.requestUpdate(this)
        }
    }

    private fun isMainProcess(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.P ||
            Application.getProcessName() == packageName
}
