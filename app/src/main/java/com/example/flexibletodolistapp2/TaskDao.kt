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
    // This method requires Room to run two queries, so add the @Transaction annotation to this
    // method so that the whole operation is performed atomically.
    // https://developer.android.com/training/data-storage/room/relationships#one-to-many
    @Transaction
    @Query("SELECT * FROM TaskDefinition")
    fun getTasks(): LiveData<List<Task>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(task: TaskDefinition): Long

    @Update
    fun update(task: TaskDefinition)

    @Transaction
    @Query("SELECT * FROM TaskDefinition WHERE id = :taskId")
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



