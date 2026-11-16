package com.example.noteflow.data.work

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

/**
 * Manual sync entry point. Periodic widget refresh has moved to
 * [WidgetRefreshScheduler] which covers all registered widget types.
 */
class NoteSyncScheduler(context: Context) {

    private val workManager = WorkManager.getInstance(context.applicationContext)

    fun enqueueSync() {
        val request = OneTimeWorkRequestBuilder<NoteSyncWorker>()
            .addTag(TAG_SYNC)
            .build()
        workManager.enqueueUniqueWork(UNIQUE_SYNC_WORK, ExistingWorkPolicy.REPLACE, request)
    }

    companion object {
        const val UNIQUE_SYNC_WORK = "note_manual_sync"

        private const val TAG_SYNC = "note-sync"
    }
}
