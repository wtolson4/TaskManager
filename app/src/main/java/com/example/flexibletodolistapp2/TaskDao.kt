package com.example.flexibletodolistapp2

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface TaskDao {

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
