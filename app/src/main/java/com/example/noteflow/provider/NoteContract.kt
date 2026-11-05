package com.example.noteflow.provider

import android.content.ContentUris
import android.net.Uri
import android.provider.BaseColumns
import com.example.noteflow.BuildConfig

/**
 * Public contract for the note ContentProvider. The authority is derived from
 * applicationId so debug builds do not collide with release builds.
 */
object NoteContract {

    const val PATH_NOTES = "notes"

    const val COLUMN_TITLE = "title"
    const val COLUMN_CONTENT = "content"
    const val COLUMN_CREATED_AT = "created_at"
    const val COLUMN_UPDATED_AT = "updated_at"

    val AUTHORITY: String = "${BuildConfig.APPLICATION_ID}.provider"
    val PERMISSION: String = "${BuildConfig.APPLICATION_ID}.permission.ACCESS_NOTES"

    val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY/$PATH_NOTES")

    val MIME_TYPE_DIR: String = "vnd.android.cursor.dir/vnd.$AUTHORITY.note"
    val MIME_TYPE_ITEM: String = "vnd.android.cursor.item/vnd.$AUTHORITY.note"

    val DEFAULT_PROJECTION: Array<String> = arrayOf(
        BaseColumns._ID,
        COLUMN_TITLE,
        COLUMN_CONTENT,
        COLUMN_CREATED_AT,
        COLUMN_UPDATED_AT,
    )

    fun contentUriFor(noteId: Long): Uri = ContentUris.withAppendedId(CONTENT_URI, noteId)
}
