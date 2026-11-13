package com.example.noteflow

import android.content.Context
import com.example.noteflow.data.local.AppDatabase
import com.example.noteflow.data.work.NoteSyncScheduler
import com.example.noteflow.provider.NoteContract
import com.example.noteflow.repository.NoteRepository
import com.example.noteflow.repository.TodoRepository
import com.example.noteflow.widget.CompanionBridge
import com.example.noteflow.widget.NoteWidgetUpdater

/**
 * Manually managed dependency graph for this app module. The companion app is
 * deliberately not a compile-time dependency; it is notified at runtime.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val database: AppDatabase by lazy { AppDatabase.getInstance(appContext) }

    val syncScheduler: NoteSyncScheduler by lazy { NoteSyncScheduler(appContext) }

    val noteRepository: NoteRepository by lazy {
        NoteRepository(database.noteDao()) {
            NoteWidgetUpdater.requestUpdate(appContext)
            CompanionBridge.notifyNotesChanged(appContext)
            appContext.contentResolver.notifyChange(NoteContract.CONTENT_URI, null)
        }
    }

    val todoRepository: TodoRepository by lazy {
        TodoRepository(database.todoDao())
    }
}
