package com.example.noteflow.provider

import android.app.Service
import android.content.ContentUris
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.Process
import android.provider.BaseColumns

/**
 * Debug-only helper that executes ContentResolver operations in the :todoProbe
 * process. The instrumentation test binds to this service and compares its pid
 * with the test pid to prove that reads/writes cross a real process boundary.
 */
class TodoProviderProbeService : Service() {

    private val handler = Handler(Looper.getMainLooper()) { message ->
        val request = message.data ?: Bundle()
        val response = Bundle()
        response.putInt(KEY_PID, Process.myPid())

        try {
            when (request.getString(KEY_OPERATION)) {
                OP_INSERT -> insert(request, response)
                OP_QUERY -> query(request, response)
                OP_UPDATE -> update(request, response)
                OP_DELETE -> delete(request, response)
                else -> throw IllegalArgumentException("Unknown operation")
            }
            response.putBoolean(KEY_SUCCESS, true)
        } catch (throwable: Throwable) {
            response.putBoolean(KEY_SUCCESS, false)
            response.putString(KEY_ERROR, throwable.message ?: throwable.javaClass.name)
        }

        message.replyTo?.send(
            Message.obtain(null, MSG_REPLY).apply { data = response },
        )
        true
    }

    override fun onBind(intent: Intent?): IBinder = Messenger(handler).binder

    private fun insert(request: Bundle, response: Bundle) {
        val values = ContentValues().apply {
            put(TodoContract.COLUMN_TITLE, request.getString(KEY_TITLE))
            put(TodoContract.COLUMN_CONTENT, request.getString(KEY_CONTENT).orEmpty())
            put(TodoContract.COLUMN_PRIORITY, request.getString(KEY_PRIORITY))
            put(TodoContract.COLUMN_IS_COMPLETED, request.getInt(KEY_COMPLETED, 0))
        }
        val uri = contentResolver.insert(TodoContract.CONTENT_URI, values)
        response.putString(KEY_URI, uri?.toString())
        response.putLong(KEY_ID, uri?.let(ContentUris::parseId) ?: -1L)
    }

    private fun query(request: Bundle, response: Bundle) {
        val uri = request.getString(KEY_URI)
            ?.let(Uri::parse)
            ?: TodoContract.CONTENT_URI
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            response.putInt(KEY_COUNT, cursor.count)
            if (cursor.moveToFirst()) {
                response.putLong(KEY_ID, cursor.getLong(cursor.getColumnIndexOrThrow(BaseColumns._ID)))
                response.putString(
                    KEY_TITLE,
                    cursor.getString(cursor.getColumnIndexOrThrow(TodoContract.COLUMN_TITLE)),
                )
                response.putInt(
                    KEY_COMPLETED,
                    cursor.getInt(cursor.getColumnIndexOrThrow(TodoContract.COLUMN_IS_COMPLETED)),
                )
                response.putString(
                    KEY_PRIORITY,
                    cursor.getString(cursor.getColumnIndexOrThrow(TodoContract.COLUMN_PRIORITY)),
                )
            }
        }
    }

    private fun update(request: Bundle, response: Bundle) {
        val id = request.getLong(KEY_ID)
        val values = ContentValues().apply {
            if (request.containsKey(KEY_TITLE)) {
                put(TodoContract.COLUMN_TITLE, request.getString(KEY_TITLE))
            }
            if (request.containsKey(KEY_CONTENT)) {
                put(TodoContract.COLUMN_CONTENT, request.getString(KEY_CONTENT))
            }
            if (request.containsKey(KEY_COMPLETED)) {
                put(TodoContract.COLUMN_IS_COMPLETED, request.getInt(KEY_COMPLETED))
            }
            if (request.containsKey(KEY_PRIORITY)) {
                put(TodoContract.COLUMN_PRIORITY, request.getString(KEY_PRIORITY))
            }
        }
        val rows = contentResolver.update(
            TodoContract.contentUriFor(id),
            values,
            null,
            null,
        )
        response.putInt(KEY_ROWS, rows)
    }

    private fun delete(request: Bundle, response: Bundle) {
        val id = request.getLong(KEY_ID, -1L)
        val uri = if (id > 0L) TodoContract.contentUriFor(id) else TodoContract.CONTENT_URI
        response.putInt(KEY_ROWS, contentResolver.delete(uri, null, null))
    }

    companion object {
        const val MSG_REQUEST = 1
        const val MSG_REPLY = 2

        const val KEY_OPERATION = "operation"
        const val KEY_SUCCESS = "success"
        const val KEY_ERROR = "error"
        const val KEY_PID = "pid"
        const val KEY_URI = "uri"
        const val KEY_ID = "id"
        const val KEY_TITLE = "title"
        const val KEY_CONTENT = "content"
        const val KEY_COMPLETED = "completed"
        const val KEY_PRIORITY = "priority"
        const val KEY_ROWS = "rows"
        const val KEY_COUNT = "count"

        const val OP_INSERT = "insert"
        const val OP_QUERY = "query"
        const val OP_UPDATE = "update"
        const val OP_DELETE = "delete"
    }
}
