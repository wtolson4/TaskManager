package com.example.flexibletodolistapp2

import androidx.lifecycle.LiveData
import androidx.room.*
import java.time.LocalDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Dao
interface TaskDao {
    @Query(
        "SELECT * FROM task_table JOIN completion_date_table ON task_table.id = completion_date_table.taskId"
    )
    fun loadTaskAndCompletionDates(): Map<Task, List<CompletionDate>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(task: Task): Long

    @Update
    fun update(task: Task): Int

    @Delete
    fun delete(task: Task): Int

    @Query("SELECT * FROM task_table WHERE isCompleted = 0 ORDER BY dueDate ASC")
    fun getIncompleteTasks(): LiveData<List<Task>>

    @Query("UPDATE task_table SET isCompleted = :isCompleted WHERE id = :taskId")
    fun markTaskAsCompleted(taskId: Int, isCompleted: Boolean = true): Int

    // New method to get a Task by its ID
    @Query("SELECT * FROM task_table WHERE id = :taskId")
    fun getTaskById(taskId: Int): Task?
}

class Converters {
    @TypeConverter
    fun fromTimestamp(value: String): LocalDate {
        return  LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE)
    }

    @TypeConverter
    fun dateToTimestamp(date: LocalDate): String {
        return date.format(DateTimeFormatter.ISO_LOCAL_DATE)
    }
}



