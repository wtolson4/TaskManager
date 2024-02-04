package com.beyondnull.flexibletodos

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.TypeConverter
import androidx.room.Update
import java.time.LocalDate
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
    // This method requires Room to run two queries, so add the @Transaction annotation to this
    // method so that the whole operation is performed atomically.
    // https://developer.android.com/training/data-storage/room/relationships#one-to-many
    @Transaction
    @Query("SELECT * FROM TaskDefinition")
    fun getTasksLive(): LiveData<List<Task>>

    @Transaction
    @Query("SELECT * FROM TaskDefinition")
    fun getTasks(): List<Task>

    @Transaction
    @Query("SELECT * FROM TaskDefinition WHERE id = :taskId")
    fun getLiveTaskById(taskId: Int): LiveData<Task?>

    @Transaction
    @Query("SELECT * FROM TaskDefinition WHERE id = :taskId")
    fun getTaskById(taskId: Long): Task?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertTask(task: TaskDefinition): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertCompletion(completion: CompletionDate): Long

    @Update
    fun updateTask(task: TaskDefinition)

    @Delete
    fun deleteTask(task: TaskDefinition)

    @Delete
    fun deleteCompletion(completion: CompletionDate)
}

class Converters {
    @TypeConverter
    fun fromTimestamp(value: String): LocalDate {
        return LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE)
    }

    @TypeConverter
    fun dateToTimestamp(date: LocalDate): String {
        return date.format(DateTimeFormatter.ISO_LOCAL_DATE)
    }

    @TypeConverter
    fun fromTimeString(value: String): LocalTime {
        return LocalTime.parse(value, DateTimeFormatter.ISO_LOCAL_TIME)
    }

    @TypeConverter
    fun timeToString(time: LocalTime): String {
        return time.format(DateTimeFormatter.ISO_LOCAL_TIME)
    }
}



