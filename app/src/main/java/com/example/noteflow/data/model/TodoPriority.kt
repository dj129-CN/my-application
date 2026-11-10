package com.example.noteflow.data.model

/**
 * Priority is persisted as an integer by TodoConverters, so SQL sorting stays
 * deterministic and migration-friendly.
 */
enum class TodoPriority(val level: Int) {
    LOW(1),
    MEDIUM(2),
    HIGH(3),
    ;

    companion object {
        fun fromLevel(level: Int): TodoPriority =
            entries.firstOrNull { it.level == level } ?: MEDIUM
    }
}
