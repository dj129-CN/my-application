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
import com.example.noteflow.data.local.TodoEntity
import com.example.noteflow.data.model.TodoPriority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.util.Locale

/**
 * Binder-backed CRUD endpoint for todos.
 *
 * The provider shares the app's Room singleton, so writes from another process
 * are immediately visible to the UI Flow. The URI surface is intentionally
 * fixed: selection and sort arguments are not accepted. The collection supports
 * a `completed=true|false` filter and mutations notify both collection and item
 * URIs for external ContentObservers.
 */
class TodoContentProvider : ContentProvider() {

    private lateinit var database: AppDatabase

    private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
        addURI(TodoContract.AUTHORITY, TodoContract.PATH_TODOS, MATCH_TODOS)
        addURI(TodoContract.AUTHORITY, "${TodoContract.PATH_TODOS}/#", MATCH_TODO)
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
        val match = uriMatcher.match(uri)
        val todos = runBlocking(Dispatchers.IO) {
            when (match) {
                MATCH_TODOS -> {
                    val completedFilter = completedFilter(uri)
                    if (completedFilter == null) {
                        database.todoDao().queryAllNewestFirst()
                    } else {
                        database.todoDao().queryByCompletion(completedFilter)
                    }
                }
                MATCH_TODO -> listOfNotNull(
                    database.todoDao().queryById(ContentUris.parseId(uri)),
                )
                else -> throw IllegalArgumentException("Unsupported URI: $uri")
            }
        }

        return buildCursor(todos, projection).also { cursor ->
            cursor.setNotificationUri(contextOrThrow().contentResolver, TodoContract.CONTENT_URI)
        }
    }

    override fun getType(uri: Uri): String? = when (uriMatcher.match(uri)) {
        MATCH_TODOS -> TodoContract.MIME_TYPE_DIR
        MATCH_TODO -> TodoContract.MIME_TYPE_ITEM
        else -> null
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri {
        requireMatch(uri, MATCH_TODOS)
        val contentValues = requireNotNull(values) { "ContentValues must not be null" }

        val title = contentValues.getAsString(TodoContract.COLUMN_TITLE)
            ?.trim()
            .orEmpty()
        require(title.isNotEmpty()) {
            "Column '${TodoContract.COLUMN_TITLE}' must not be empty"
        }

        val now = System.currentTimeMillis()
        val createdAt = contentValues.getAsLong(TodoContract.COLUMN_CREATED_AT)
            ?.takeIf { it > 0L }
            ?: now

        val todo = TodoEntity(
            title = title,
            content = contentValues.getAsString(TodoContract.COLUMN_CONTENT).orEmpty(),
            createdAt = createdAt,
            isCompleted = completedFrom(contentValues, default = false),
            priority = priorityFrom(contentValues, default = TodoPriority.MEDIUM),
        )
        val id = runBlocking(Dispatchers.IO) { database.todoDao().insert(todo) }
        val itemUri = TodoContract.contentUriFor(id)
        notifyChanged(itemUri)
        return itemUri
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int {
        requireMatch(uri, MATCH_TODO)
        if (values == null || values.size() == 0) return 0

        val todoId = ContentUris.parseId(uri)
        val current = runBlocking(Dispatchers.IO) { database.todoDao().queryById(todoId) }
            ?: return 0

        val newTitle = if (values.containsKey(TodoContract.COLUMN_TITLE)) {
            values.getAsString(TodoContract.COLUMN_TITLE)?.trim().orEmpty()
        } else {
            current.title
        }
        require(newTitle.isNotEmpty()) {
            "Column '${TodoContract.COLUMN_TITLE}' must not be empty"
        }

        val updated = current.copy(
            title = newTitle,
            content = if (values.containsKey(TodoContract.COLUMN_CONTENT)) {
                values.getAsString(TodoContract.COLUMN_CONTENT).orEmpty()
            } else {
                current.content
            },
            isCompleted = completedFrom(values, default = current.isCompleted),
            priority = priorityFrom(values, default = current.priority),
        )
        val rows = runBlocking(Dispatchers.IO) { database.todoDao().update(updated) }
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
                MATCH_TODOS -> database.todoDao().deleteAll()
                MATCH_TODO -> database.todoDao().deleteById(ContentUris.parseId(uri))
                else -> throw IllegalArgumentException("Unsupported URI: $uri")
            }
        }
        if (rows > 0) {
            notifyChanged(uri)
        }
        return rows
    }

    private fun buildCursor(
        todos: List<TodoEntity>,
        projection: Array<out String>?,
    ): Cursor {
        val columns = projection ?: TodoContract.DEFAULT_PROJECTION
        val cursor = MatrixCursor(columns)
        todos.forEach { todo ->
            cursor.addRow(
                columns.map { column ->
                    when (column) {
                        BaseColumns._ID -> todo.id
                        TodoContract.COLUMN_TITLE -> todo.title
                        TodoContract.COLUMN_CONTENT -> todo.content
                        TodoContract.COLUMN_CREATED_AT -> todo.createdAt
                        TodoContract.COLUMN_IS_COMPLETED -> if (todo.isCompleted) 1 else 0
                        TodoContract.COLUMN_PRIORITY -> todo.priority.name
                        else -> throw IllegalArgumentException("Unknown column: $column")
                    }
                }.toTypedArray(),
            )
        }
        return cursor
    }

    /**
     * Notifies both the collection and item URI. Observers registered on the
     * collection (with descendants) and on the concrete item are both informed.
     */
    private fun notifyChanged(changedUri: Uri) {
        val resolver = contextOrThrow().contentResolver
        resolver.notifyChange(TodoContract.CONTENT_URI, null)
        if (changedUri != TodoContract.CONTENT_URI) {
            resolver.notifyChange(changedUri, null)
        }
    }

    private fun completedFilter(uri: Uri): Boolean? =
        when (uri.getQueryParameter(TodoContract.QUERY_COMPLETED)) {
            null -> null
            "true" -> true
            "false" -> false
            else -> throw IllegalArgumentException("Unsupported completed filter: $uri")
        }

    private fun completedFrom(values: ContentValues, default: Boolean): Boolean {
        if (!values.containsKey(TodoContract.COLUMN_IS_COMPLETED)) return default
        return when (val raw = values.get(TodoContract.COLUMN_IS_COMPLETED)) {
            is Boolean -> raw
            is Number -> raw.toInt() != 0
            is String -> when (raw.trim().lowercase(Locale.ROOT)) {
                "1", "true" -> true
                "0", "false" -> false
                else -> throw IllegalArgumentException("Invalid is_completed value: $raw")
            }
            else -> throw IllegalArgumentException("Invalid is_completed value: $raw")
        }
    }

    private fun priorityFrom(values: ContentValues, default: TodoPriority): TodoPriority {
        if (!values.containsKey(TodoContract.COLUMN_PRIORITY)) return default
        val raw = values.getAsString(TodoContract.COLUMN_PRIORITY)
            ?: throw IllegalArgumentException("Invalid priority value")
        return runCatching {
            TodoPriority.valueOf(raw.trim().uppercase(Locale.ROOT))
        }.getOrElse {
            throw IllegalArgumentException("Priority must be LOW, MEDIUM or HIGH")
        }
    }

    private fun requireMatch(uri: Uri, expectedMatch: Int) {
        require(uriMatcher.match(uri) == expectedMatch) {
            "Unsupported URI for this operation: $uri"
        }
    }

    private fun contextOrThrow(): Context =
        checkNotNull(context) { "ContentProvider is not attached yet" }

    private companion object {
        const val MATCH_TODOS = 1
        const val MATCH_TODO = 2
    }
}
