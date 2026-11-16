package com.example.noteflow.data.work

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.noteflow.widget.WidgetRefreshManager
import kotlinx.coroutines.CancellationException

/**
 * Periodic safety net that refreshes every registered widget. Individual writes
 * still trigger immediate refreshes through [WidgetRefreshManager.refreshAll];
 * this worker covers edge cases where a broadcast was missed.
 */
class AllWidgetRefreshWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            WidgetRefreshManager.registeredProviders().forEach { providerClass ->
                WidgetRefreshManager.refreshProvider(applicationContext, providerClass)
            }
            Result.success()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            Log.w(TAG, "Periodic widget refresh failed", throwable)
            Result.retry()
        }
    }

    private companion object {
        const val TAG = "AllWidgetRefreshWorker"
    }
}
