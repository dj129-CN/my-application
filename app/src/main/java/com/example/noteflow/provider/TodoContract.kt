package com.example.noteflow.provider

import android.content.ContentUris
import android.net.Uri
import android.provider.BaseColumns
import com.example.noteflow.BuildConfig

/**
 * Public, controlled IPC contract for todos.
 *
 * URI forms:
 * - content://<authority>/todos
 * - content://<authority>/todos?completed=true|false
 * - content://<authority>/todos/<id>
 *
 * Observers should register on [CONTENT_URI]; the provider notifies both the
 * collection and the concrete item URI after mutations.
 */
object TodoContract {

    const val PATH_TODOS = "todos"
    const val QUERY_COMPLETED = "completed"

    const val COLUMN_TITLE = "title"
    const val COLUMN_CONTENT = "content"
    const val COLUMN_CREATED_AT = "created_at"
    const val COLUMN_IS_COMPLETED = "is_completed"
    const val COLUMN_PRIORITY = "priority"

    const val PRIORITY_LOW = "LOW"
    const val PRIORITY_MEDIUM = "MEDIUM"
    const val PRIORITY_HIGH = "HIGH"

    val AUTHORITY: String = "${BuildConfig.APPLICATION_ID}.todo"
    val PERMISSION: String = "${BuildConfig.APPLICATION_ID}.permission.ACCESS_TODOS"

    val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY/$PATH_TODOS")

    val MIME_TYPE_DIR: String = "vnd.android.cursor.dir/vnd.$AUTHORITY.todo"
    val MIME_TYPE_ITEM: String = "vnd.android.cursor.item/vnd.$AUTHORITY.todo"

    val DEFAULT_PROJECTION: Array<String> = arrayOf(
        BaseColumns._ID,
        COLUMN_TITLE,
        COLUMN_CONTENT,
        COLUMN_CREATED_AT,
        COLUMN_IS_COMPLETED,
        COLUMN_PRIORITY,
    )

    fun contentUriFor(todoId: Long): Uri = ContentUris.withAppendedId(CONTENT_URI, todoId)

    fun contentUriForCompleted(completed: Boolean): Uri = CONTENT_URI.buildUpon()
        .appendQueryParameter(QUERY_COMPLETED, completed.toString())
        .build()
}
