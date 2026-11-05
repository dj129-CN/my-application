package com.example.noteflow.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.example.noteflow.R
import com.example.noteflow.data.local.AppDatabase
import com.example.noteflow.data.local.NoteEntity
import com.example.noteflow.ui.MainActivity
import com.example.noteflow.util.DateFormats
import com.example.noteflow.util.NoteDeepLinks
import com.example.noteflow.util.ThemePreferences
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

    private const val REQUEST_OPEN_NOTES = 400
    private const val REQUEST_NEW_NOTE = 401
    private const val REQUEST_OPEN_ALL = 402
    private const val REQUEST_OPEN_LATEST = 403

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
        val darkTheme = ThemePreferences(context).isDark(context)
        val primaryColor = ContextCompat.getColor(
            context,
            if (darkTheme) R.color.widget_dark_text_primary else R.color.widget_light_text_primary,
        )
        val secondaryColor = ContextCompat.getColor(
            context,
            if (darkTheme) R.color.widget_dark_text_secondary else R.color.widget_light_text_secondary,
        )
        val accentColor = ContextCompat.getColor(
            context,
            if (darkTheme) R.color.widget_dark_accent else R.color.widget_light_accent,
        )

        views.setInt(
            R.id.widgetRoot,
            "setBackgroundResource",
            if (darkTheme) R.drawable.bg_widget_dark else R.drawable.bg_widget_light,
        )
        views.setTextColor(R.id.widgetHeader, secondaryColor)
        views.setTextColor(R.id.widgetCount, secondaryColor)
        views.setTextColor(R.id.widgetTitle, primaryColor)
        views.setTextColor(R.id.widgetContent, secondaryColor)
        views.setTextColor(R.id.widgetFooter, secondaryColor)
        views.setTextColor(R.id.widgetQuickAdd, accentColor)
        views.setTextColor(R.id.widgetOpenAll, accentColor)

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

        val openNotesPendingIntent = deepLinkPendingIntent(
            context,
            NoteDeepLinks.OPEN_NOTES,
            REQUEST_OPEN_NOTES,
        )
        val newNotePendingIntent = deepLinkPendingIntent(
            context,
            NoteDeepLinks.NEW_NOTE,
            REQUEST_NEW_NOTE,
        )
        val latestPendingIntent = deepLinkPendingIntent(
            context,
            latest?.let { NoteDeepLinks.noteDetail(it.id) } ?: NoteDeepLinks.NEW_NOTE,
            REQUEST_OPEN_LATEST,
        )

        views.setOnClickPendingIntent(R.id.widgetRoot, openNotesPendingIntent)
        views.setOnClickPendingIntent(R.id.widgetLatestGroup, latestPendingIntent)
        views.setOnClickPendingIntent(R.id.widgetQuickAdd, newNotePendingIntent)
        views.setOnClickPendingIntent(
            R.id.widgetOpenAll,
            deepLinkPendingIntent(context, NoteDeepLinks.OPEN_NOTES, REQUEST_OPEN_ALL),
        )
        return views
    }

    private fun deepLinkPendingIntent(
        context: Context,
        uri: String,
        requestCode: Int,
    ): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse(uri)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
