package com.example.noteflow.data.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Schedules the periodic 30-minute widget refresh via WorkManager. Call
 * [schedule] once during application startup; WorkManager handles deduplication
 * through the unique work name.
 */
object WidgetRefreshScheduler {

    const val UNIQUE_WORK_NAME = "all_widget_periodic_refresh"
    private const val INTERVAL_MINUTES = 30L

    fun schedule(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()
        val request = PeriodicWorkRequestBuilder<AllWidgetRefreshWorker>(
            INTERVAL_MINUTES,
            TimeUnit.MINUTES,
        )
            .setConstraints(constraints)
            .addTag(TAG_WIDGET_REFRESH)
            .build()
        WorkManager.getInstance(context.applicationContext)
            .enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context.applicationContext)
            .cancelUniqueWork(UNIQUE_WORK_NAME)
    }

    private const val TAG_WIDGET_REFRESH = "widget-periodic-refresh"
}
