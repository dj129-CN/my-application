package com.example.noteflow.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [NoteEntity::class, TodoEntity::class],
    version = 2,
    exportSchema = true,
)
@TypeConverters(TodoConverters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun noteDao(): NoteDao

    abstract fun todoDao(): TodoDao

    companion object {
        private const val DATABASE_NAME = "noteflow.db"

        /**
         * v1 -> v2 adds the todo table without touching the existing notes.
         * Keep this migration in future versions when adding more tables.
         */
        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `todos` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `content` TEXT NOT NULL,
                        `created_at` INTEGER NOT NULL,
                        `is_completed` INTEGER NOT NULL,
                        `priority` INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_todos_created_at` " +
                        "ON `todos` (`created_at`)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_todos_is_completed` " +
                        "ON `todos` (`is_completed`)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_todos_priority` " +
                        "ON `todos` (`priority`)",
                )
            }
        }

        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME,
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }
    }
}
