package com.example.flexibletodolistapp2

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "task_table")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Int,
    val taskName: String,
    val dueDate: String,  // storing date as a string for simplicity
    val frequency: Int,   // e.g., 2 for every two weeks when combined with recurrenceType
    val recurrenceType: String,  // DAILY, WEEKLY, BIWEEKLY, MONTHLY, BIYEARLY
    val isCompleted: Boolean = false,
    val completedDate: String? = null  // Date when the task was completed. Null if not completed.
)
