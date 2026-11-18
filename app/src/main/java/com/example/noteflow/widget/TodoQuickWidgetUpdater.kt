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
import com.example.noteflow.ui.MainActivity
import com.example.noteflow.util.ThemePreferences
import com.example.noteflow.util.TodoDeepLinks
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Shared renderer for the 2x2 quick-action todo widget. Three buttons:
 * quick-add, today's list, and mark-all-complete. Reuses the same theme
 * palette, deep-link helpers, and refresh manager as the 4x1 widget.
 */
object TodoQuickWidgetUpdater {

    private const val TAG = "TodoQuickWidgetUpdater"
    private val widgetScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    const val ACTION_COMPLETE_ALL =
        "com.example.noteflow.action.TODO_QUICK_COMPLETE_ALL"
    private const val REQUEST_ADD_BASE = 800
    private const val REQUEST_LIST_BASE = 810
    private const val REQUEST_COMPLETE_BASE = 820

    fun requestUpdate(context: Context) {
        val appContext = context.applicationContext
        widgetScope.launch {
            try {
                updateAll(appContext)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                Log.w(TAG, "Unable to refresh quick widgets", throwable)
            }
        }
    }

    suspend fun updateAll(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val widgetIds = manager.getAppWidgetIds(
            ComponentName(context, TodoQuickWidgetProvider::class.java),
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
        val views = buildRemoteViews(context)
        widgetIds.forEach { widgetId ->
            manager.updateAppWidget(widgetId, views)
        }
    }

    private fun buildRemoteViews(context: Context): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.todo_quick_widget)
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
            R.id.quickWidgetRoot,
            "setBackgroundResource",
            if (darkTheme) R.drawable.bg_widget_dark else R.drawable.bg_widget_light,
        )
        views.setTextColor(R.id.quickWidgetTitle, primaryColor)
        views.setTextColor(R.id.quickBtnAdd, accentColor)
        views.setTextColor(R.id.quickBtnList, accentColor)
        views.setTextColor(R.id.quickBtnComplete, accentColor)
        views.setTextColor(R.id.quickLabelAdd, secondaryColor)
        views.setTextColor(R.id.quickLabelList, secondaryColor)
        views.setTextColor(R.id.quickLabelComplete, secondaryColor)

        views.setImageViewResource(
            R.id.quickIconAdd,
            if (darkTheme) R.drawable.ic_todo_quick_add_dark else R.drawable.ic_todo_quick_add,
        )
        views.setImageViewResource(
            R.id.quickIconList,
            if (darkTheme) R.drawable.ic_todo_quick_list_dark else R.drawable.ic_todo_quick_list,
        )
        views.setImageViewResource(
            R.id.quickIconComplete,
            if (darkTheme) R.drawable.ic_todo_quick_complete_dark else R.drawable.ic_todo_quick_complete,
        )

        // Quick add → deep link to new todo editor
        views.setOnClickPendingIntent(
            R.id.quickBtnAdd,
            deepLinkPendingIntent(context, TodoDeepLinks.ADD, REQUEST_ADD_BASE),
        )

        // Today's list → deep link to todo list
        views.setOnClickPendingIntent(
            R.id.quickBtnList,
            deepLinkPendingIntent(context, TodoDeepLinks.LIST, REQUEST_LIST_BASE),
        )

        // Mark all complete → broadcast to provider
        val completeIntent = Intent(context, TodoQuickWidgetProvider::class.java).apply {
            action = ACTION_COMPLETE_ALL
        }
        val completePendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_COMPLETE_BASE,
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        views.setOnClickPendingIntent(R.id.quickBtnComplete, completePendingIntent)

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
