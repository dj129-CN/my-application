package com.example.noteflow.data.local

import androidx.room.TypeConverter
import com.example.noteflow.data.model.TodoPriority

class TodoConverters {

    @TypeConverter
    fun priorityToLevel(priority: TodoPriority): Int = priority.level

    @TypeConverter
    fun levelToPriority(level: Int): TodoPriority = TodoPriority.fromLevel(level)
}
