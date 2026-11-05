package com.example.noteflow

import android.app.Application
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
        container = AppContainer(this)
        container.syncScheduler.scheduleWidgetRefresh()
        NoteWidgetUpdater.requestUpdate(this)
    }
}
