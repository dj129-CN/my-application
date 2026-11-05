package com.example.noteflow.companion.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * The main app sends this explicit refresh action after each note write. The
 * receiver is exported but protected by a signature permission declared for the
 * ecosystem (see the main app manifest).
 */
class CompanionRefreshReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_REFRESH_WIDGET) return

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                CompanionWidgetUpdater.updateAll(context)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                Log.w(TAG, "Cross-app widget refresh failed", throwable)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        const val TAG = "CompanionRefresh"
        const val ACTION_REFRESH_WIDGET =
            "com.example.noteflow.companion.action.REFRESH_WIDGET"
    }
}
