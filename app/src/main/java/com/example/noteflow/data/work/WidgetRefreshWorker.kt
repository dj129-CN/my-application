package com.example.noteflow.data.work

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.noteflow.widget.NoteWidgetUpdater
import kotlinx.coroutines.CancellationException

/**
 * Periodic safety net for the home-screen widget. Immediate refreshes still
 * happen on every note write through NoteWidgetUpdater.
 */
class WidgetRefreshWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            NoteWidgetUpdater.updateAll(applicationContext)
            Result.success()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            Log.w(TAG, "Periodic widget refresh failed", throwable)
            Result.failure()
        }
    }

    private companion object {
        const val TAG = "WidgetRefreshWorker"
    }
}
