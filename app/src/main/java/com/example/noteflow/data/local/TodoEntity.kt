package com.example.noteflow.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.noteflow.data.model.TodoPriority

@Entity(
    tableName = "todos",
    indices = [
        Index(value = ["created_at"]),
        Index(value = ["is_completed"]),
        Index(value = ["priority"]),
    ],
)
data class TodoEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,
    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "content")
    val content: String = "",
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "is_completed")
    val isCompleted: Boolean = false,
    @ColumnInfo(name = "priority")
    val priority: TodoPriority = TodoPriority.MEDIUM,
)
