package com.example.noteflow.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Home-screen 4x1 todo widget entry point. Handles system updates and the
 * refresh broadcast from the widget button. Manual refresh goes through
 * [WidgetRefreshManager] so it shares the same code path as data-change and
 * periodic refreshes.
 */
class TodoWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                TodoWidgetUpdater.updateWidgets(context, appWidgetIds)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                Log.e(TAG, "Todo widget update failed", throwable)
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == TodoWidgetUpdater.ACTION_REFRESH) {
            val appWidgetId = intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID,
            )
            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                val pendingResult = goAsync()
                CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                    try {
                        WidgetRefreshManager.refreshProvider(
                            context,
                            TodoWidgetProvider::class.java,
                        )
                    } catch (cancellation: CancellationException) {
                        throw cancellation
                    } catch (throwable: Throwable) {
                        Log.e(TAG, "Todo widget refresh failed", throwable)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        appWidgetIds.forEach { TodoWidgetPrefs.removeCategory(context, it) }
    }

    private companion object {
        const val TAG = "TodoWidgetProvider"
    }
}
