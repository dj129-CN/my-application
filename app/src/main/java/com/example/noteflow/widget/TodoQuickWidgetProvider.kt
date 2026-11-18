package com.example.noteflow.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.noteflow.data.local.AppDatabase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Home-screen 2x2 quick-action todo widget. Handles system updates and the
 * "mark all complete" broadcast. Reuses [WidgetRefreshManager] for data-change
 * and periodic refreshes.
 */
class TodoQuickWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                TodoQuickWidgetUpdater.updateWidgets(context, appWidgetIds)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                Log.e(TAG, "Quick widget update failed", throwable)
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == TodoQuickWidgetUpdater.ACTION_COMPLETE_ALL) {
            val pendingResult = goAsync()
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                try {
                    val dao = AppDatabase.getInstance(context).todoDao()
                    val incomplete = dao.queryByCompletion(false)
                    incomplete.forEach { todo ->
                        dao.updateCompletion(todo.id, true)
                    }
                    // Trigger full refresh through the manager so all widget
                    // types (4x1 and 2x2) reflect the change.
                    WidgetRefreshManager.refreshAll(context)
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (throwable: Throwable) {
                    Log.e(TAG, "Mark-all-complete failed", throwable)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    private companion object {
        const val TAG = "TodoQuickWidgetProvider"
    }
}
