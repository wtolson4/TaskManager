package com.example.flexibletodolistapp2

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit


/**
 * A basic class representing an entity that is a row in a one-column database table.
 *
 * @ Entity - You must annotate the class as an entity and supply a table name if not class name.
 * @ PrimaryKey - You must identify the primary key.
 * @ ColumnInfo - You must supply the column name if it is different from the variable name.
 *
 * See the documentation for the full rich set of annotations.
 * https://developer.android.com/topic/libraries/architecture/room.html
 */
@Entity
data class TaskDefinition(
    @PrimaryKey(autoGenerate = true) val id: Int,
    val name: String,
    val description: String,
    val creationDate: LocalDate,
    val initialDueDate: LocalDate,
    val frequency: Int,
)

@Entity(tableName = "completion_date_table")
data class CompletionDate(
    @PrimaryKey(autoGenerate = true) val id: Int,
    val taskId: Int,
    val date: LocalDate,
    val frequencyWhenCompleted: Int,
)

// https://developer.android.com/training/data-storage/room/relationships
data class Task(
    @Embedded val definition: TaskDefinition,
    @Relation(
        parentColumn = "id",
        entityColumn = "taskId"
    )
    val completions: List<CompletionDate>,
) {
    /* The compiler only uses the properties defined inside the primary constructor for the
     automatically generated functions. To exclude a property from the generated implementations,
     declare it inside the class body
     */
    val nextDueDate: LocalDate
        get() {
            return if (this.completions.isEmpty()) this.definition.initialDueDate
            else {
                val latestCompletion = this.completions.last()
                latestCompletion.date.plusDays(this.definition.frequency.toLong())
            }
        }

    val daysUntilDue: Int
        get() {
            val today = LocalDate.now()
            return ChronoUnit.DAYS.between(today, this.nextDueDate).toInt()
        }

    // TODO: impl urgency
    val urgency: Int
        get() = 0

    fun nextNotification(lastNotification: LocalDateTime): LocalDateTime {
        val nextNotifDate = if (nextDueDate > lastNotification.toLocalDate()) {
            // Due date is in the future, notify on due date
            nextDueDate
        } else {
            // Due date is in the past, calculate next notification date
            // TODO: nextNotification date should be based on the task's notification rules
            LocalDate.now()
        }

        // TODO: notification time should be based on the task's notification rule
        val time = LocalTime.now().plusSeconds(30)
        return nextNotifDate.atTime(time)
    }

}
