package com.example.noteflow.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.example.noteflow.R
import com.example.noteflow.data.local.AppDatabase
import com.example.noteflow.data.local.TodoEntity
import com.example.noteflow.ui.MainActivity
import com.example.noteflow.util.ThemePreferences
import com.example.noteflow.util.TodoDeepLinks
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

/**
 * Shared renderer for the 4x1 todo widget. Reads from Room, applies the
 * per-widget category filter, and builds RemoteViews with day/night palettes.
 */
object TodoWidgetUpdater {

    private const val TAG = "TodoWidgetUpdater"
    private val widgetScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    const val ACTION_REFRESH = "com.example.noteflow.action.TODO_WIDGET_REFRESH"
    private const val REQUEST_REFRESH_BASE = 500
    private const val REQUEST_LIST_BASE = 600
    private const val REQUEST_DETAIL_BASE = 700

    fun requestUpdate(context: Context) {
        val appContext = context.applicationContext
        widgetScope.launch {
            try {
                updateAll(appContext)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                Log.w(TAG, "Unable to refresh todo widgets", throwable)
            }
        }
    }

    suspend fun updateAll(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val widgetIds = manager.getAppWidgetIds(
            ComponentName(context, TodoWidgetProvider::class.java),
        )
        if (widgetIds.isEmpty()) return
        updateWidgets(context, manager, widgetIds)
    }

    suspend fun updateWidgets(context: Context, widgetIds: IntArray) {
        if (widgetIds.isEmpty()) return
        updateWidgets(context, AppWidgetManager.getInstance(context), widgetIds)
    }

    suspend fun updateWidget(context: Context, appWidgetId: Int) {
        updateWidgets(context, intArrayOf(appWidgetId))
    }

    private suspend fun updateWidgets(
        context: Context,
        manager: AppWidgetManager,
        widgetIds: IntArray,
    ) {
        val dao = AppDatabase.getInstance(context).todoDao()
        val allTodos = dao.queryAllNewestFirst()
        val todayStart = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val todayEnd = LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        widgetIds.forEach { widgetId ->
            val category = TodoWidgetPrefs.getCategory(context, widgetId)
            val filtered = filterTodos(allTodos, category, todayStart, todayEnd)
            val views = buildRemoteViews(context, filtered, widgetId)
            manager.updateAppWidget(widgetId, views)
        }
    }

    private fun filterTodos(
        todos: List<TodoEntity>,
        category: String,
        todayStart: Long,
        todayEnd: Long,
    ): List<TodoEntity> {
        val todayTodos = todos.filter { it.createdAt in todayStart until todayEnd }
        return when (category) {
            TodoWidgetPrefs.CATEGORY_COMPLETED -> todayTodos.filter { it.isCompleted }
            TodoWidgetPrefs.CATEGORY_INCOMPLETE -> todayTodos.filter { !it.isCompleted }
            else -> todayTodos
        }
    }

    private fun buildRemoteViews(
        context: Context,
        todos: List<TodoEntity>,
        appWidgetId: Int,
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.todo_widget)
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
            R.id.todoWidgetRoot,
            "setBackgroundResource",
            if (darkTheme) R.drawable.bg_widget_dark else R.drawable.bg_widget_light,
        )
        views.setTextColor(R.id.todoWidgetCount, primaryColor)
        views.setTextColor(R.id.todoWidgetTitle, primaryColor)
        views.setTextColor(R.id.todoWidgetLabel, secondaryColor)
        views.setTextColor(R.id.todoWidgetRefresh, accentColor)
        views.setImageViewResource(
            R.id.todoWidgetRefresh,
            if (darkTheme) R.drawable.ic_todo_widget_refresh_dark else R.drawable.ic_todo_widget_refresh_light,
        )

        val count = todos.size
        views.setTextViewText(
            R.id.todoWidgetCount,
            context.getString(R.string.todo_widget_count_format, count),
        )

        val latest = todos.firstOrNull()
        if (latest == null) {
            views.setTextViewText(
                R.id.todoWidgetTitle,
                context.getString(R.string.todo_widget_empty),
            )
        } else {
            views.setTextViewText(R.id.todoWidgetTitle, latest.title)
        }

        // Refresh button → broadcast to provider
        val refreshIntent = Intent(context, TodoWidgetProvider::class.java).apply {
            action = ACTION_REFRESH
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        val refreshPendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_REFRESH_BASE + appWidgetId,
            refreshIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        views.setOnClickPendingIntent(R.id.todoWidgetRefresh, refreshPendingIntent)

        // Count/label area → open todo list
        val listPendingIntent = deepLinkPendingIntent(
            context,
            TodoDeepLinks.LIST,
            REQUEST_LIST_BASE + appWidgetId,
        )
        views.setOnClickPendingIntent(R.id.todoWidgetCountGroup, listPendingIntent)

        // Title area → open detail (or add if no latest)
        val detailUri = latest?.let { TodoDeepLinks.detail(it.id) } ?: TodoDeepLinks.ADD
        val detailPendingIntent = deepLinkPendingIntent(
            context,
            detailUri,
            REQUEST_DETAIL_BASE + appWidgetId,
        )
        views.setOnClickPendingIntent(R.id.todoWidgetTitle, detailPendingIntent)

        return views
    }

    private fun deepLinkPendingIntent(
        context: Context,
        uri: String,
        requestCode: Int,
    ): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = android.net.Uri.parse(uri)
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
