package com.example.flexibletodolistapp2

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation
import java.time.LocalDate

// https://developer.android.com/training/data-storage/room/relationships

@Entity
data class TaskDefinition(
    @PrimaryKey(autoGenerate = true) val id: Int,
    val taskName: String,
    val initialDueDate: LocalDate,
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

data class Task(
    @Embedded val definition: TaskDefinition,
    @Relation(
        parentColumn = "id",
        entityColumn = "taskId"
    )
    val completions: List<CompletionDate>
)