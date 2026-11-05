package com.example.noteflow.companion.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.util.Log
import android.widget.RemoteViews
import com.example.noteflow.companion.R
import com.example.noteflow.companion.data.CompanionProviderClient
import com.example.noteflow.companion.data.CompanionSnapshot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate

object CompanionWidgetUpdater {

    private const val TAG = "CompanionWidgetUpdater"
    private val widgetScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private const val REQUEST_COMPANION = 600
    private const val REQUEST_NEW_NOTE = 601
    private const val REQUEST_OPEN_NOTES = 602
    private const val REQUEST_OPEN_LATEST = 603

    fun requestUpdate(context: Context) {
        val appContext = context.applicationContext
        widgetScope.launch {
            try {
                updateAll(appContext)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                Log.w(TAG, "Unable to refresh companion widget", throwable)
            }
        }
    }

    suspend fun updateAll(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(
            ComponentName(context, CompanionWidgetProvider::class.java),
        )
        if (ids.isEmpty()) return
        updateWidgets(context, manager, ids)
    }

    suspend fun updateWidgets(context: Context, widgetIds: IntArray) {
        if (widgetIds.isEmpty()) return
        updateWidgets(context, AppWidgetManager.getInstance(context), widgetIds)
    }

    private suspend fun updateWidgets(
        context: Context,
        manager: AppWidgetManager,
        widgetIds: IntArray,
    ) {
        val snapshot = CompanionProviderClient.loadSnapshot(context)
        val views = buildRemoteViews(context, snapshot)
        widgetIds.forEach { widgetId ->
            manager.updateAppWidget(widgetId, views)
        }
    }

    private fun buildRemoteViews(context: Context, snapshot: CompanionSnapshot): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_companion)
        val latest = snapshot.latestNote
        val connected = snapshot.connected

        val title = when {
            latest != null -> latest.title
            connected -> context.getString(R.string.companion_widget_connected_title)
            else -> context.getString(R.string.companion_widget_local_title)
        }
        val content = if (latest != null) {
            latest.content.ifBlank {
                context.getString(R.string.companion_widget_no_content)
            }
        } else {
            dailyPrompt(context)
        }
        val footer = if (connected) {
            context.getString(R.string.companion_widget_connected_footer)
        } else {
            context.getString(R.string.companion_widget_local_footer)
        }

        views.setTextViewText(R.id.widgetTitle, title)
        views.setTextViewText(R.id.widgetContent, content)
        views.setTextViewText(R.id.widgetStatus, footer)
        views.setTextViewText(
            R.id.widgetQuickAdd,
            context.getString(
                if (connected) {
                    R.string.companion_widget_quick_add
                } else {
                    R.string.companion_widget_open_companion
                },
            ),
        )
        views.setTextViewText(
            R.id.widgetOpen,
            context.getString(
                if (connected) {
                    R.string.companion_widget_open_notes
                } else {
                    R.string.companion_widget_open_companion
                },
            ),
        )

        val companionPendingIntent = CompanionProviderClient.companionPendingIntent(
            context,
            REQUEST_COMPANION,
        )
        views.setOnClickPendingIntent(R.id.companionWidgetRoot, companionPendingIntent)

        val quickAddPendingIntent = if (connected) {
            CompanionProviderClient.mainAppPendingIntent(
                context,
                CompanionProviderClient.DEEP_LINK_NEW_NOTE,
                REQUEST_NEW_NOTE,
            )
        } else {
            null
        } ?: companionPendingIntent
        views.setOnClickPendingIntent(R.id.widgetQuickAdd, quickAddPendingIntent)

        val openPendingIntent = if (connected) {
            CompanionProviderClient.mainAppPendingIntent(
                context,
                CompanionProviderClient.DEEP_LINK_NOTES,
                REQUEST_OPEN_NOTES,
            )
        } else {
            null
        } ?: companionPendingIntent
        views.setOnClickPendingIntent(R.id.widgetOpen, openPendingIntent)

        val latestPendingIntent = if (connected && latest != null) {
            CompanionProviderClient.mainAppPendingIntent(
                context,
                CompanionProviderClient.deepLinkForNote(latest.id),
                REQUEST_OPEN_LATEST,
            )
        } else {
            null
        } ?: companionPendingIntent
        views.setOnClickPendingIntent(R.id.widgetLatestGroup, latestPendingIntent)

        return views
    }

    private fun dailyPrompt(context: Context): String {
        val prompts = context.resources.getStringArray(R.array.companion_prompts)
        if (prompts.isEmpty()) return context.getString(R.string.companion_widget_empty_content)
        val index = (LocalDate.now().dayOfYear - 1).mod(prompts.size)
        return prompts[index]
    }
}
