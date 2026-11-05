package com.example.noteflow.data.work

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.noteflow.data.local.AppDatabase
import com.example.noteflow.data.local.SyncPreferences
import com.example.noteflow.widget.NoteWidgetUpdater
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

/**
 * Simulated one-shot sync. Replace the delay with a remote API call; WorkManager
 * already handles process death, retries and unique-work deduplication.
 */
class NoteSyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            delay(700L)
            val noteCount = AppDatabase.getInstance(applicationContext).noteDao().count()
            SyncPreferences(applicationContext).recordSync(noteCount)
            NoteWidgetUpdater.updateAll(applicationContext)
            Result.success()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            Log.e(TAG, "Sync failed", throwable)
            Result.failure()
        }
    }

    private companion object {
        const val TAG = "NoteSyncWorker"
    }
}
