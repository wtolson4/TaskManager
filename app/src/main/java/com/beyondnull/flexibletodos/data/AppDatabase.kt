// AppDatabase.kt
package com.beyondnull.flexibletodos.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import timber.log.Timber

@Database(
    entities = [Task::class, CompletionDate::class],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        private const val DATABASE_NAME = "task_database"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                Timber.i("Migrating database from 1 to 2")
                database.execSQL(
                    "ALTER TABLE tasks_table ADD COLUMN lastCompleted TEXT"
                )
                database.execSQL(
                    "UPDATE tasks_table SET lastCompleted = " +
                            "(SELECT c.date FROM completion_date_table AS c " +
                            "WHERE c.taskId = tasks_table.id " +
                            "ORDER BY c.date DESC " +
                            "LIMIT 1)"
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            val tempInstance = INSTANCE
            if (tempInstance != null) {
                return tempInstance
            }
            synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                ).addMigrations(MIGRATION_1_2).build()
                INSTANCE = instance
                return instance
            }
        }
    }
}
