package com.example.flexibletodolistapp2

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "task_table")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Int,
    val taskName: String,
    val dueDate: LocalDate,
    val frequency: Int,   // e.g., 2 for every two weeks when combined with recurrenceType
    val recurrenceType: String,  // DAILY, WEEKLY, BIWEEKLY, MONTHLY, BIYEARLY
    val isCompleted: Boolean = false,
)

@Entity(tableName = "completion_date_table")
data class CompletionDate(
    @PrimaryKey(autoGenerate = true) val id: Int,
    val taskId: Int,
    val date: LocalDate,
)
