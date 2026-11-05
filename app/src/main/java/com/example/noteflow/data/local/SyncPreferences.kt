package com.example.noteflow.data.local

import android.content.Context

/**
 * Lightweight preference store for the demo sync worker. A real remote sync can
 * replace this with a server cursor/timestamp.
 */
class SyncPreferences(context: Context) {

    private val preferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun recordSync(noteCount: Int, timestamp: Long = System.currentTimeMillis()) {
        preferences.edit()
            .putLong(KEY_LAST_SYNC_AT, timestamp)
            .putInt(KEY_SYNCED_NOTE_COUNT, noteCount)
            .apply()
    }

    fun lastSyncAt(): Long = preferences.getLong(KEY_LAST_SYNC_AT, 0L)

    fun lastSyncedNoteCount(): Int = preferences.getInt(KEY_SYNCED_NOTE_COUNT, 0)

    private companion object {
        const val PREFS_NAME = "note_sync"
        const val KEY_LAST_SYNC_AT = "last_sync_at"
        const val KEY_SYNCED_NOTE_COUNT = "synced_note_count"
    }
}
