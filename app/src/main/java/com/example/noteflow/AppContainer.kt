package com.example.noteflow

import android.content.Context
import com.example.noteflow.data.local.AppDatabase
import com.example.noteflow.data.work.NoteSyncScheduler
import com.example.noteflow.provider.NoteContract
import com.example.noteflow.repository.NoteRepository
import com.example.noteflow.widget.NoteWidgetUpdater

/**
 * Manually managed dependency graph for this single-module app. It can later be
 * replaced by Hilt without changing the ViewModel/repository boundaries.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val database: AppDatabase by lazy { AppDatabase.getInstance(appContext) }

    val syncScheduler: NoteSyncScheduler = NoteSyncScheduler(appContext)

    val noteRepository: NoteRepository by lazy {
        NoteRepository(database.noteDao()) {
            NoteWidgetUpdater.requestUpdate(appContext)
            appContext.contentResolver.notifyChange(NoteContract.CONTENT_URI, null)
        }
    }
}
