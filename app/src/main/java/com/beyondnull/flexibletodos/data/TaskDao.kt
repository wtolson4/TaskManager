package com.beyondnull.flexibletodos.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.TypeConverter
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * The Room Magic is in this file, where you map a method call to an SQL query.
 *
 * When you are using complex data types, such as Date, you have to also supply type converters.
 * To keep this example basic, no types that require type converters are used.
 * See the documentation at
 * https://developer.android.com/topic/libraries/architecture/room.html#type-converters
 */

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks_table")
    fun getTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks_table WHERE id = :taskId")
    fun getTaskById(taskId: Long): Flow<Task?>

    @Query("SELECT * FROM completion_date_table WHERE taskId = :taskId")
    fun getTaskCompletions(taskId: Long): Flow<List<CompletionDate>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTask(task: Task): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCompletion(completion: CompletionDate): Long

    @Update
    suspend fun updateTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)

    @Delete
    suspend fun deleteCompletion(completion: CompletionDate)

    @Query("DELETE FROM completion_date_table WHERE taskId = :taskId")
    suspend fun deleteTaskCompletions(taskId: Long)
}

class Converters {
    @TypeConverter
    fun stringToDate(value: String): LocalDate {
        return LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE)
    }

    @TypeConverter
    fun dateToString(date: LocalDate): String {
        return date.format(DateTimeFormatter.ISO_LOCAL_DATE)
    }

    @TypeConverter
    fun stringToTime(value: String): LocalTime {
        return LocalTime.parse(value, DateTimeFormatter.ISO_LOCAL_TIME)
    }

    @TypeConverter
    fun timeToString(time: LocalTime): String {
        return time.format(DateTimeFormatter.ISO_LOCAL_TIME)
    }

    @TypeConverter
    fun stringToDateTime(value: String): LocalDateTime {
        return LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    }

    @TypeConverter
    fun dateTimeToString(time: LocalDateTime): String {
        return time.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    }
}



