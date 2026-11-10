package com.example.noteflow.repository

import com.example.noteflow.data.local.TodoDao
import com.example.noteflow.data.local.TodoEntity
import com.example.noteflow.data.model.TodoPriority
import com.example.noteflow.util.DataError
import com.example.noteflow.util.DataResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Data-layer gateway for todos.
 *
 * Every read/write executes on [ioDispatcher]. Return values are wrapped in
 * [DataResult] so validation, not-found and database failures are explicit.
 * A null single-item lookup is converted to NOT_FOUND. An empty list is a
 * successful empty result.
 */
class TodoRepository(
    private val todoDao: TodoDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {

    // --------------------------------------------------------------- observe

    fun observeTodosNewestFirst(): Flow<DataResult<List<TodoEntity>>> =
        resultFlow(todoDao.observeAllNewestFirst())

    fun observeTodosOldestFirst(): Flow<DataResult<List<TodoEntity>>> =
        resultFlow(todoDao.observeAllOldestFirst())

    fun observeByCompletion(completed: Boolean): Flow<DataResult<List<TodoEntity>>> =
        resultFlow(todoDao.observeByCompletion(completed))

    fun observeByPriority(priority: TodoPriority): Flow<DataResult<List<TodoEntity>>> =
        resultFlow(todoDao.observeByPriority(priority))

    /**
     * Emission with null means the todo was deleted or does not exist. This is
     * intentional for live single-item screens; one-shot [getById] converts the
     * same condition into a NOT_FOUND failure.
     */
    fun observeById(id: Long): Flow<DataResult<TodoEntity?>> =
        resultFlow(todoDao.observeById(id))

    fun searchByKeyword(keyword: String): Flow<DataResult<List<TodoEntity>>> {
        val normalized = keyword.trim()
        return resultFlow(
            if (normalized.isEmpty()) {
                todoDao.observeAllNewestFirst()
            } else {
                todoDao.searchByKeyword(normalized)
            },
        )
    }

    // --------------------------------------------------------------- create

    suspend fun insert(todo: TodoEntity): DataResult<TodoEntity> {
        validateNew(todo)?.let { return it }

        val normalized = normalizeNew(todo)
        return dbCall {
            val id = todoDao.insert(normalized)
            check(id > 0L) { "Insert did not return a valid row id" }
            normalized.copy(id = id)
        }
    }

    suspend fun insertAll(todos: List<TodoEntity>): DataResult<List<Long>> {
        if (todos.isEmpty()) return DataResult.Success(emptyList())
        if (todos.any { it.id != 0L }) {
            return failure(DataError.VALIDATION, "New todos must not already have ids")
        }
        if (todos.any { it.title.isBlank() }) {
            return failure(DataError.VALIDATION, "Todo title must not be blank")
        }

        val normalized = todos.map(::normalizeNew)
        return dbCall { todoDao.insertAll(normalized) }
    }

    // --------------------------------------------------------------- update

    suspend fun update(todo: TodoEntity): DataResult<TodoEntity> {
        if (todo.id <= 0L) {
            return failure(DataError.VALIDATION, "A persisted todo id is required")
        }
        if (todo.title.isBlank()) {
            return failure(DataError.VALIDATION, "Todo title must not be blank")
        }

        val normalized = todo.copy(
            title = todo.title.trim(),
            content = todo.content.trimEnd(),
        )
        return dbCall {
            if (todoDao.update(normalized) == 0) {
                throw TodoNotFoundException(normalized.id)
            }
            normalized
        }
    }

    suspend fun updateCompletion(id: Long, completed: Boolean): DataResult<Unit> {
        if (id <= 0L) return failure(DataError.VALIDATION, "A valid todo id is required")

        return dbCall {
            if (todoDao.updateCompletion(id, completed) == 0) {
                throw TodoNotFoundException(id)
            }
        }
    }

    suspend fun updatePriority(id: Long, priority: TodoPriority): DataResult<Unit> {
        if (id <= 0L) return failure(DataError.VALIDATION, "A valid todo id is required")

        return dbCall {
            if (todoDao.updatePriority(id, priority) == 0) {
                throw TodoNotFoundException(id)
            }
        }
    }

    // --------------------------------------------------------------- delete

    suspend fun delete(todo: TodoEntity): DataResult<Unit> {
        if (todo.id <= 0L) {
            return failure(DataError.VALIDATION, "A persisted todo id is required")
        }
        return dbCall {
            if (todoDao.delete(todo) == 0) {
                throw TodoNotFoundException(todo.id)
            }
        }
    }

    suspend fun deleteById(id: Long): DataResult<Unit> {
        if (id <= 0L) return failure(DataError.VALIDATION, "A valid todo id is required")

        return dbCall {
            if (todoDao.deleteById(id) == 0) {
                throw TodoNotFoundException(id)
            }
        }
    }

    suspend fun deleteCompleted(): DataResult<Int> =
        dbCall { todoDao.deleteCompleted() }

    suspend fun deleteAll(): DataResult<Int> =
        dbCall { todoDao.deleteAll() }

    // ---------------------------------------------------------------- query

    suspend fun getById(id: Long): DataResult<TodoEntity> {
        if (id <= 0L) return failure(DataError.VALIDATION, "A valid todo id is required")

        return dbCall {
            todoDao.queryById(id) ?: throw TodoNotFoundException(id)
        }
    }

    suspend fun getAllNewestFirst(): DataResult<List<TodoEntity>> =
        dbCall { todoDao.queryAllNewestFirst() }

    suspend fun getAllOldestFirst(): DataResult<List<TodoEntity>> =
        dbCall { todoDao.queryAllOldestFirst() }

    suspend fun getByCompletion(completed: Boolean): DataResult<List<TodoEntity>> =
        dbCall { todoDao.queryByCompletion(completed) }

    suspend fun getByPriority(priority: TodoPriority): DataResult<List<TodoEntity>> =
        dbCall { todoDao.queryByPriority(priority) }

    suspend fun searchOnce(keyword: String): DataResult<List<TodoEntity>> {
        val normalized = keyword.trim()
        if (normalized.isEmpty()) return getAllNewestFirst()
        return dbCall { todoDao.queryByKeyword(normalized) }
    }

    suspend fun count(): DataResult<Int> =
        dbCall { todoDao.count() }

    suspend fun countCompleted(): DataResult<Int> =
        dbCall { todoDao.countCompleted() }

    // -------------------------------------------------------------- helpers

    private fun validateNew(todo: TodoEntity): DataResult.Failure? {
        if (todo.id != 0L) {
            return failure(DataError.VALIDATION, "A new todo must not have an id")
        }
        if (todo.title.isBlank()) {
            return failure(DataError.VALIDATION, "Todo title must not be blank")
        }
        return null
    }

    private fun normalizeNew(todo: TodoEntity): TodoEntity = todo.copy(
        title = todo.title.trim(),
        content = todo.content.trimEnd(),
        createdAt = if (todo.createdAt > 0L) todo.createdAt else System.currentTimeMillis(),
    )

    private fun failure(type: DataError, message: String): DataResult.Failure =
        DataResult.Failure(type = type, message = message)

    private suspend fun <T> dbCall(block: suspend () -> T): DataResult<T> =
        withContext(ioDispatcher) {
            try {
                DataResult.Success(block())
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (notFound: TodoNotFoundException) {
                DataResult.Failure(
                    type = DataError.NOT_FOUND,
                    message = notFound.message ?: "Todo was not found",
                )
            } catch (exception: Exception) {
                DataResult.Failure(
                    type = DataError.DATABASE,
                    message = exception.message ?: "Todo database operation failed",
                    cause = exception,
                )
            }
        }

    private fun <T> resultFlow(source: Flow<T>): Flow<DataResult<T>> =
        source
            .map<T, DataResult<T>> { DataResult.Success(it) }
            .catch { throwable ->
                if (throwable is CancellationException) throw throwable
                emit(
                    DataResult.Failure(
                        type = DataError.DATABASE,
                        message = throwable.message ?: "Failed to observe todos",
                        cause = throwable,
                    ),
                )
            }
            .flowOn(ioDispatcher)

    private class TodoNotFoundException(id: Long) :
        NoSuchElementException("Todo with id=$id was not found")
}
