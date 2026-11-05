package com.example.noteflow.repository

import com.example.noteflow.data.local.NoteDao
import com.example.noteflow.data.local.NoteEntity
import kotlinx.coroutines.flow.Flow

/**
 * The single write path for notes owned by the app process. A callback keeps
 * side effects (widget refresh / ContentResolver notification) out of Room.
 */
class NoteRepository(
    private val noteDao: NoteDao,
    private val onNotesChanged: () -> Unit = {},
) {

    fun observeNotes(): Flow<List<NoteEntity>> = noteDao.observeAll()

    fun observeNote(noteId: Long): Flow<NoteEntity?> = noteDao.observeById(noteId)

    suspend fun getNote(noteId: Long): NoteEntity? = noteDao.findById(noteId)

    suspend fun saveNote(noteId: Long, title: String, content: String): Long {
        require(title.isNotBlank()) { "Note title must not be blank" }

        val now = System.currentTimeMillis()
        val normalizedTitle = title.trim()
        val normalizedContent = content.trimEnd()

        val resultId = if (noteId <= 0L) {
            noteDao.insert(
                NoteEntity(
                    title = normalizedTitle,
                    content = normalizedContent,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
        } else {
            val existing = noteDao.findById(noteId)
                ?: throw IllegalStateException("Note $noteId does not exist")
            noteDao.update(
                existing.copy(
                    title = normalizedTitle,
                    content = normalizedContent,
                    updatedAt = now,
                ),
            )
            noteId
        }

        onNotesChanged()
        return resultId
    }

    suspend fun deleteNote(noteId: Long): Boolean {
        val deleted = noteDao.deleteById(noteId) > 0
        if (deleted) {
            onNotesChanged()
        }
        return deleted
    }

    suspend fun noteCount(): Int = noteDao.count()
}
