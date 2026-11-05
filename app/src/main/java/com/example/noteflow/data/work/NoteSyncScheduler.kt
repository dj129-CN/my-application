package com.example.noteflow.data.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * All WorkManager entry points live here so UI code only expresses intent.
 */
class NoteSyncScheduler(context: Context) {

    private val workManager = WorkManager.getInstance(context.applicationContext)

    fun enqueueSync() {
        val request = OneTimeWorkRequestBuilder<NoteSyncWorker>()
            .addTag(TAG_SYNC)
            .build()
        workManager.enqueueUniqueWork(UNIQUE_SYNC_WORK, ExistingWorkPolicy.REPLACE, request)
    }

    fun scheduleWidgetRefresh() {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()
        val request = PeriodicWorkRequestBuilder<WidgetRefreshWorker>(
            WIDGET_REFRESH_INTERVAL_MINUTES,
            TimeUnit.MINUTES,
        )
            .setConstraints(constraints)
            .addTag(TAG_WIDGET)
            .build()
        workManager.enqueueUniquePeriodicWork(
            UNIQUE_WIDGET_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    companion object {
        const val UNIQUE_SYNC_WORK = "note_manual_sync"
        const val UNIQUE_WIDGET_WORK = "note_widget_refresh"

        private const val TAG_SYNC = "note-sync"
        private const val TAG_WIDGET = "note-widget"
        private const val WIDGET_REFRESH_INTERVAL_MINUTES = 15L
    }
}
