package com.example.noteflow.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import com.example.noteflow.R
import com.example.noteflow.data.local.AppDatabase
import com.example.noteflow.data.local.NoteEntity
import com.example.noteflow.ui.MainActivity
import com.example.noteflow.util.DateFormats
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Shared renderer used by the widget provider, WorkManager and in-process write
 * notifications. All database and RemoteViews work stays off the main thread.
 */
object NoteWidgetUpdater {

    private const val TAG = "NoteWidgetUpdater"
    private val widgetScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun requestUpdate(context: Context) {
        val appContext = context.applicationContext
        widgetScope.launch {
            try {
                updateAll(appContext)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                Log.w(TAG, "Unable to refresh note widgets", throwable)
            }
        }
    }

    suspend fun updateAll(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val widgetIds = manager.getAppWidgetIds(
            ComponentName(context, NoteWidgetProvider::class.java),
        )
        if (widgetIds.isEmpty()) return
        updateWidgets(context, manager, widgetIds)
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
        val notes = AppDatabase.getInstance(context).noteDao().getAll()
        val views = buildRemoteViews(context, notes.firstOrNull(), notes.size)
        widgetIds.forEach { widgetId ->
            manager.updateAppWidget(widgetId, views)
        }
    }

    private fun buildRemoteViews(
        context: Context,
        latest: NoteEntity?,
        noteCount: Int,
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_note)
        views.setTextViewText(
            R.id.widgetCount,
            context.getString(R.string.widget_count, noteCount),
        )

        if (latest == null) {
            views.setTextViewText(
                R.id.widgetTitle,
                context.getString(R.string.widget_empty_title),
            )
            views.setTextViewText(
                R.id.widgetContent,
                context.getString(R.string.widget_empty_content),
            )
            views.setTextViewText(
                R.id.widgetFooter,
                context.getString(R.string.widget_open_hint),
            )
        } else {
            views.setTextViewText(R.id.widgetTitle, latest.title)
            views.setTextViewText(
                R.id.widgetContent,
                latest.content.ifBlank { context.getString(R.string.note_no_content) },
            )
            views.setTextViewText(
                R.id.widgetFooter,
                context.getString(
                    R.string.widget_updated,
                    DateFormats.widgetLabel(latest.updatedAt),
                ),
            )
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        views.setOnClickPendingIntent(R.id.widgetRoot, pendingIntent)
        return views
    }
}
