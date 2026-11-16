package com.example.noteflow.widget

import android.appwidget.AppWidgetProvider
import android.content.Context
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch

/**
 * Central registry and dispatcher for all home-screen widgets.
 *
 * Adding a new widget only requires one [register] call — the manager handles
 * data-change broadcasts, manual refresh PendingIntents, and periodic
 * WorkManager refreshes through the same code path.
 */
object WidgetRefreshManager {

    private const val TAG = "WidgetRefreshManager"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private data class WidgetEntry(
        val providerClass: Class<out AppWidgetProvider>,
        val updater: suspend (Context) -> Unit,
    )

    private val entries = mutableListOf<WidgetEntry>()

    /**
     * Register a widget provider and its update function. Call once per widget
     * type during application startup.
     */
    fun register(
        providerClass: Class<out AppWidgetProvider>,
        updater: suspend (Context) -> Unit,
    ) {
        if (entries.none { it.providerClass == providerClass }) {
            entries.add(WidgetEntry(providerClass, updater))
        }
    }

    /**
     * Refresh every registered widget in parallel. Used by data-change
     * broadcasts and the periodic WorkManager task.
     */
    fun refreshAll(context: Context) {
        val appContext = context.applicationContext
        scope.launch {
            try {
                val deferred = entries.map { entry ->
                    async {
                        try {
                            entry.updater(appContext)
                        } catch (cancellation: CancellationException) {
                            throw cancellation
                        } catch (throwable: Throwable) {
                            Log.w(
                                TAG,
                                "Failed to refresh ${entry.providerClass.simpleName}",
                                throwable,
                            )
                        }
                    }
                }
                deferred.awaitAll()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                Log.w(TAG, "refreshAll failed", throwable)
            }
        }
    }

    /**
     * Refresh a single widget type. Used by manual-refresh PendingIntents that
     * target a specific provider.
     */
    suspend fun refreshProvider(
        context: Context,
        providerClass: Class<out AppWidgetProvider>,
    ) {
        val entry = entries.firstOrNull { it.providerClass == providerClass }
        if (entry == null) {
            Log.w(TAG, "No registered updater for ${providerClass.simpleName}")
            return
        }
        try {
            entry.updater(context.applicationContext)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            Log.w(TAG, "Failed to refresh ${providerClass.simpleName}", throwable)
        }
    }

    /** Snapshot of registered provider classes (for diagnostics). */
    fun registeredProviders(): List<Class<out AppWidgetProvider>> =
        entries.map { it.providerClass }
}
