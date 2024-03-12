package com.beyondnull.flexibletodos

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.beyondnull.flexibletodos.data.AppDatabase
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {
    private val TEST_DB = "migration-test"

    // Array of all migrations.
    private val ALL_MIGRATIONS = arrayOf(
        AppDatabase.MIGRATION_1_2
    )

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    @Throws(IOException::class)
    fun migrateAll() {
        // Create earliest version of the database.
        helper.createDatabase(TEST_DB, 1).apply {
            // Database has schema version 1. Insert some data using SQL queries.
            // You can't use DAO classes because they expect the latest schema.
            execSQL(
                "INSERT INTO tasks_table (" +
//                    "id," +
                        "name," +
                        "description," +
                        "creationDate," +
                        "initialDueDate," +
                        "period," +
                        "notificationsEnabled," +
                        "notificationLastDismissed," +
                        "notificationTime," +
                        "notificationPeriod)" +
                        "VALUES (" +
                        "'test name'," +
                        "'no description'," +
                        "'2023-12-10'," +
                        "'2023-12-15'," +
                        "2," +
                        "1," +
                        "NULL," +
                        "NULL," +
                        "1)"
            )

            // Prepare for the next version.
            close()
        }


        // Open latest version of the database. Room validates the schema
        // once all migrations execute.
        Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            AppDatabase::class.java,
            TEST_DB
        ).addMigrations(*ALL_MIGRATIONS).build().apply {
            openHelper.writableDatabase.close()
        }
    }
}
