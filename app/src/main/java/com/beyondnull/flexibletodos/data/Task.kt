package com.beyondnull.flexibletodos.data

import android.content.Context
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.beyondnull.flexibletodos.R
import com.beyondnull.flexibletodos.calculation.GlobalPeriodScaling
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Period
import java.time.temporal.ChronoUnit
import java.util.Objects.isNull


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
    val period: Int,
    val notificationsEnabled: Boolean,
    val notificationLastDismissed: LocalDateTime?,
    val notificationTime: LocalTime?,
    val notificationPeriod: Int?,
)

@Entity(tableName = "completion_date_table")
data class CompletionDate(
    @PrimaryKey(autoGenerate = true) val id: Int,
    val taskId: Int,
    val date: LocalDate,
)

// https://developer.android.com/training/data-storage/room/relationships
data class Task(
    @Embedded val definition: TaskDefinition,
    @Relation(
        parentColumn = "id",
        entityColumn = "taskId"
    )
    val completionsUnsorted: List<CompletionDate>,
) {
    /* The compiler only uses the properties defined inside the primary constructor for the
     automatically generated functions. To exclude a property from the generated implementations,
     declare it inside the class body
     */
    val completions: List<CompletionDate>
        get() {
            return this.completionsUnsorted.sortedBy { it.date }
        }

    val nextDueDate: LocalDate
        get() {
            return if (this.completions.isEmpty()) this.definition.initialDueDate
            else {
                val latestCompletion = this.completions.last()
                latestCompletion.date.plusDays(this.definition.period.toLong())
            }
        }

    val daysUntilDue: Int
        get() {
            val today = LocalDate.now()
            return ChronoUnit.DAYS.between(today, this.nextDueDate).toInt()
        }


    fun getDueDaysString(context: Context): String {
        return when {
            (daysUntilDue < -1) -> String.format(
                context.getString(R.string.due_n_days_ago),
                -daysUntilDue
            )

            (daysUntilDue == -1) -> context.getString(R.string.due_one_day_ago)
            (daysUntilDue == 0) -> context.getString(R.string.due_today)
            (daysUntilDue == 1) -> context.getString(R.string.due_tomorrow)
            else -> String.format(
                context.getString(R.string.due_in_n_days),
                daysUntilDue
            )
        }
    }

    // TODO: impl urgency
    // TODO: (P2) Color due text (notification + main view) based on urgency
    val urgency: Int
        get() = 0

    fun nextNotification(context: Context): LocalDateTime? {
        if (!this.definition.notificationsEnabled) {
            return null
        }

        val notificationTime =
            this.definition.notificationTime ?: Settings.NotificationTime.get(context)
        val nextDueDateTime = nextDueDate.atTime(notificationTime)

        val nextNotificationDateTime =
            if (isNull(definition.notificationLastDismissed)) {
                // Notification has never been dismissed, show at next due date
                nextDueDateTime
            } else if (nextDueDateTime > definition.notificationLastDismissed) {
                // Due date is after the last dismissal, notify on due date
                nextDueDateTime
            } else {
                // Due date is before the last dismissal.
                // Based on notification period, determine the closest scheduled notification times to today
                val notificationPeriod =
                    definition.notificationPeriod ?: GlobalPeriodScaling.scale(
                        definition.period,
                        Settings.NotificationPeriodScale.get(context)
                    )
                val daysSinceDue = Period.between(nextDueDate, LocalDate.now()).days
                val remainder = daysSinceDue % notificationPeriod
                val lastScheduledNotification =
                    LocalDate.now().minusDays(remainder.toLong()).atTime(notificationTime)
                val nextScheduledNotification =
                    lastScheduledNotification.plusDays(notificationPeriod.toLong())
                // Compare the scheduled notifications to the last smissal
                if (lastScheduledNotification > definition.notificationLastDismissed) lastScheduledNotification
                else nextScheduledNotification
            }

        return nextNotificationDateTime
    }
}
