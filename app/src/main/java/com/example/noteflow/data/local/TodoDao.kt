package com.example.noteflow.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.noteflow.data.model.TodoPriority
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {

    // ---------------------------------------------------------------- create

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(todo: TodoEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(todos: List<TodoEntity>): List<Long>

    // ---------------------------------------------------------------- update

    @Update
    suspend fun update(todo: TodoEntity): Int

    @Query("UPDATE todos SET is_completed = :completed WHERE id = :id")
    suspend fun updateCompletion(id: Long, completed: Boolean): Int

    @Query("UPDATE todos SET priority = :priority WHERE id = :id")
    suspend fun updatePriority(id: Long, priority: TodoPriority): Int

    // ---------------------------------------------------------------- delete

    @Delete
    suspend fun delete(todo: TodoEntity): Int

    @Query("DELETE FROM todos WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    @Query("DELETE FROM todos WHERE is_completed = 1")
    suspend fun deleteCompleted(): Int

    @Query("DELETE FROM todos")
    suspend fun deleteAll(): Int

    // ------------------------------------------------------------- observe

    @Query("SELECT * FROM todos ORDER BY created_at DESC, id DESC")
    fun observeAllNewestFirst(): Flow<List<TodoEntity>>

    @Query("SELECT * FROM todos ORDER BY created_at ASC, id ASC")
    fun observeAllOldestFirst(): Flow<List<TodoEntity>>

    @Query(
        "SELECT * FROM todos WHERE is_completed = :completed " +
            "ORDER BY created_at DESC, id DESC",
    )
    fun observeByCompletion(completed: Boolean): Flow<List<TodoEntity>>

    @Query(
        "SELECT * FROM todos WHERE priority = :priority " +
            "ORDER BY created_at DESC, id DESC",
    )
    fun observeByPriority(priority: TodoPriority): Flow<List<TodoEntity>>

    @Query("SELECT * FROM todos WHERE id = :id")
    fun observeById(id: Long): Flow<TodoEntity?>

    @Query(
        "SELECT * FROM todos WHERE title LIKE '%' || :keyword || '%' " +
            "OR content LIKE '%' || :keyword || '%' " +
            "ORDER BY created_at DESC, id DESC",
    )
    fun searchByKeyword(keyword: String): Flow<List<TodoEntity>>

    // --------------------------------------------------------------- query

    @Query("SELECT * FROM todos ORDER BY created_at DESC, id DESC")
    suspend fun queryAllNewestFirst(): List<TodoEntity>

    @Query("SELECT * FROM todos ORDER BY created_at ASC, id ASC")
    suspend fun queryAllOldestFirst(): List<TodoEntity>

    @Query(
        "SELECT * FROM todos WHERE is_completed = :completed " +
            "ORDER BY created_at DESC, id DESC",
    )
    suspend fun queryByCompletion(completed: Boolean): List<TodoEntity>

    @Query(
        "SELECT * FROM todos WHERE priority = :priority " +
            "ORDER BY created_at DESC, id DESC",
    )
    suspend fun queryByPriority(priority: TodoPriority): List<TodoEntity>

    @Query("SELECT * FROM todos WHERE id = :id")
    suspend fun queryById(id: Long): TodoEntity?

    @Query(
        "SELECT * FROM todos WHERE title LIKE '%' || :keyword || '%' " +
            "OR content LIKE '%' || :keyword || '%' " +
            "ORDER BY created_at DESC, id DESC",
    )
    suspend fun queryByKeyword(keyword: String): List<TodoEntity>

    @Query("SELECT COUNT(*) FROM todos")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM todos WHERE is_completed = 1")
    suspend fun countCompleted(): Int
}
