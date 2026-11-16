package com.example.noteflow.widget

import android.content.Context

/**
 * Per-widget category preference stored in SharedPreferences. The key includes
 * the appWidgetId so each widget instance can show a different filter.
 */
object TodoWidgetPrefs {

    private const val PREFS_NAME = "todo_widget_prefs"
    private const val KEY_CATEGORY_PREFIX = "category_"

    const val CATEGORY_ALL = "all"
    const val CATEGORY_COMPLETED = "completed"
    const val CATEGORY_INCOMPLETE = "incomplete"

    fun getCategory(context: Context, appWidgetId: Int): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_CATEGORY_PREFIX + appWidgetId, CATEGORY_ALL) ?: CATEGORY_ALL
    }

    fun saveCategory(context: Context, appWidgetId: Int, category: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_CATEGORY_PREFIX + appWidgetId, category).apply()
    }

    fun removeCategory(context: Context, appWidgetId: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_CATEGORY_PREFIX + appWidgetId).apply()
    }
}
