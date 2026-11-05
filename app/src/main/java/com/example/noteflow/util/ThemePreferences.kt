package com.example.noteflow.util

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate

enum class ThemeMode(
    val storageValue: String,
    val nightMode: Int,
) {
    SYSTEM("system", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM),
    LIGHT("light", AppCompatDelegate.MODE_NIGHT_NO),
    DARK("dark", AppCompatDelegate.MODE_NIGHT_YES),
    ;

    companion object {
        fun fromStorage(value: String?): ThemeMode =
            entries.firstOrNull { it.storageValue == value } ?: SYSTEM
    }
}

/**
 * Persisted app-wide theme preference. The widget cannot read AppCompat's night
 * mode, so it asks this class for the effective light/dark palette too.
 */
class ThemePreferences(context: Context) {

    private val preferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var mode: ThemeMode
        get() = ThemeMode.fromStorage(preferences.getString(KEY_MODE, null))
        set(value) {
            preferences.edit().putString(KEY_MODE, value.storageValue).apply()
        }

    fun isDark(context: Context): Boolean = when (mode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> {
            val uiMode = context.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK
            uiMode == Configuration.UI_MODE_NIGHT_YES
        }
    }

    companion object {
        private const val PREFS_NAME = "note_theme"
        private const val KEY_MODE = "theme_mode"
    }
}
