package com.example.noteflow.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Query("SELECT * FROM notes ORDER BY updated_at DESC, id DESC")
    fun observeAll(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :noteId")
    fun observeById(noteId: Long): Flow<NoteEntity?>

    @Query("SELECT * FROM notes WHERE id = :noteId")
    suspend fun findById(noteId: Long): NoteEntity?

    @Query("SELECT * FROM notes ORDER BY updated_at DESC, id DESC")
    suspend fun getAll(): List<NoteEntity>

    @Insert
    suspend fun insert(note: NoteEntity): Long

    @Update
    suspend fun update(note: NoteEntity): Int

    @Query("DELETE FROM notes WHERE id = :noteId")
    suspend fun deleteById(noteId: Long): Int

    @Query("DELETE FROM notes")
    suspend fun deleteAll(): Int

    @Query("SELECT COUNT(*) FROM notes")
    suspend fun count(): Int
}
