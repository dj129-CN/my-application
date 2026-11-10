package com.example.noteflow.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class TodoPriorityTest {

    @Test
    fun fromLevel_mapsKnownLevels() {
        assertEquals(TodoPriority.LOW, TodoPriority.fromLevel(1))
        assertEquals(TodoPriority.MEDIUM, TodoPriority.fromLevel(2))
        assertEquals(TodoPriority.HIGH, TodoPriority.fromLevel(3))
    }

    @Test
    fun fromLevel_fallsBackToMediumForUnknownValues() {
        assertEquals(TodoPriority.MEDIUM, TodoPriority.fromLevel(0))
        assertEquals(TodoPriority.MEDIUM, TodoPriority.fromLevel(99))
    }
}
