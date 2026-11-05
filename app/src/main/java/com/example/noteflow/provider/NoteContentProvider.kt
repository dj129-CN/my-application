package com.example.noteflow.provider

import android.content.ContentProvider
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.BaseColumns
import com.example.noteflow.data.local.AppDatabase
import com.example.noteflow.data.local.NoteEntity
import com.example.noteflow.widget.CompanionBridge
import com.example.noteflow.widget.NoteWidgetUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

/**
 * Binder-backed CRUD endpoint for notes.
 *
 * This provider is intentionally URI-based: clients query the collection or one
 * item and supply an explicit projection. Selection/sort arguments are not
 * exposed because the contract is a small, controlled IPC surface.
 */
class NoteContentProvider : ContentProvider() {

    private lateinit var database: AppDatabase

    private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
        addURI(NoteContract.AUTHORITY, NoteContract.PATH_NOTES, MATCH_NOTES)
        addURI(NoteContract.AUTHORITY, "${NoteContract.PATH_NOTES}/#", MATCH_NOTE)
    }

    override fun onCreate(): Boolean {
        database = AppDatabase.getInstance(contextOrThrow())
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor {
        val notes = runBlocking(Dispatchers.IO) {
            when (uriMatcher.match(uri)) {
                MATCH_NOTES -> database.noteDao().getAll()
                MATCH_NOTE -> listOfNotNull(
                    database.noteDao().findById(ContentUris.parseId(uri)),
                )
                else -> throw IllegalArgumentException("Unsupported URI: $uri")
            }
        }

        val cursor = buildCursor(notes, projection)
        cursor.setNotificationUri(contextOrThrow().contentResolver, uri)
        return cursor
    }

    override fun getType(uri: Uri): String? = when (uriMatcher.match(uri)) {
        MATCH_NOTES -> NoteContract.MIME_TYPE_DIR
        MATCH_NOTE -> NoteContract.MIME_TYPE_ITEM
        else -> null
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri {
        requireMatch(uri, MATCH_NOTES)
        val contentValues = requireNotNull(values) { "ContentValues must not be null" }

        val title = contentValues.getAsString(NoteContract.COLUMN_TITLE)
            ?.trim()
            .orEmpty()
        require(title.isNotEmpty()) { "Column '${NoteContract.COLUMN_TITLE}' must not be empty" }

        val now = System.currentTimeMillis()
        val note = NoteEntity(
            title = title,
            content = contentValues.getAsString(NoteContract.COLUMN_CONTENT).orEmpty(),
            createdAt = now,
            updatedAt = now,
        )
        val id = runBlocking(Dispatchers.IO) { database.noteDao().insert(note) }
        val itemUri = NoteContract.contentUriFor(id)
        notifyChanged(itemUri)
        return itemUri
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int {
        requireMatch(uri, MATCH_NOTE)
        if (values == null || values.size() == 0) return 0

        val noteId = ContentUris.parseId(uri)
        val current = runBlocking(Dispatchers.IO) { database.noteDao().findById(noteId) }
            ?: return 0

        val newTitle = if (values.containsKey(NoteContract.COLUMN_TITLE)) {
            values.getAsString(NoteContract.COLUMN_TITLE)?.trim().orEmpty()
        } else {
            current.title
        }
        require(newTitle.isNotEmpty()) {
            "Column '${NoteContract.COLUMN_TITLE}' must not be empty"
        }

        val newContent = if (values.containsKey(NoteContract.COLUMN_CONTENT)) {
            values.getAsString(NoteContract.COLUMN_CONTENT).orEmpty()
        } else {
            current.content
        }

        val updated = current.copy(
            title = newTitle,
            content = newContent,
            updatedAt = System.currentTimeMillis(),
        )
        val rows = runBlocking(Dispatchers.IO) { database.noteDao().update(updated) }
        if (rows > 0) {
            notifyChanged(uri)
        }
        return rows
    }

    override fun delete(
        uri: Uri,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int {
        val rows = runBlocking(Dispatchers.IO) {
            when (uriMatcher.match(uri)) {
                MATCH_NOTES -> database.noteDao().deleteAll()
                MATCH_NOTE -> database.noteDao().deleteById(ContentUris.parseId(uri))
                else -> throw IllegalArgumentException("Unsupported URI: $uri")
            }
        }
        if (rows > 0) {
            notifyChanged(uri)
        }
        return rows
    }

    private fun buildCursor(
        notes: List<NoteEntity>,
        projection: Array<out String>?,
    ): Cursor {
        val columns = projection ?: NoteContract.DEFAULT_PROJECTION
        val cursor = MatrixCursor(columns)
        notes.forEach { note ->
            cursor.addRow(
                columns.map { column ->
                    when (column) {
                        BaseColumns._ID -> note.id
                        NoteContract.COLUMN_TITLE -> note.title
                        NoteContract.COLUMN_CONTENT -> note.content
                        NoteContract.COLUMN_CREATED_AT -> note.createdAt
                        NoteContract.COLUMN_UPDATED_AT -> note.updatedAt
                        else -> throw IllegalArgumentException("Unknown column: $column")
                    }
                }.toTypedArray(),
            )
        }
        return cursor
    }

    private fun notifyChanged(changedUri: Uri) {
        val resolver = contextOrThrow().contentResolver
        resolver.notifyChange(NoteContract.CONTENT_URI, null)
        if (changedUri != NoteContract.CONTENT_URI) {
            resolver.notifyChange(changedUri, null)
        }
        NoteWidgetUpdater.requestUpdate(contextOrThrow())
        CompanionBridge.notifyNotesChanged(contextOrThrow())
    }

    private fun requireMatch(uri: Uri, expectedMatch: Int) {
        val actualMatch = uriMatcher.match(uri)
        require(actualMatch == expectedMatch) {
            "Unsupported URI for this operation: $uri"
        }
    }

    private fun contextOrThrow(): Context =
        checkNotNull(context) { "ContentProvider is not attached yet" }

    private companion object {
        const val MATCH_NOTES = 1
        const val MATCH_NOTE = 2
    }
}
