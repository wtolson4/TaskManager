package com.beyondnull.flexibletodos

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat.getString
import com.beyondnull.flexibletodos.activity.MainActivity
import com.beyondnull.flexibletodos.data.AppDatabase
import com.beyondnull.flexibletodos.data.Task
import com.beyondnull.flexibletodos.data.TaskRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.coroutines.EmptyCoroutineContext

class AppNotificationManager {
    class AppBroadcastReceiver : BroadcastReceiver() {
        @OptIn(DelicateCoroutinesApi::class)
        override fun onReceive(
            context: Context, intent: Intent
        ) {
            when {
                intent.action == "android.intent.action.BOOT_COMPLETED" -> {
                    // Must reset alarms when device reboots
                    // https://developer.android.com/develop/background-work/services/alarms/schedule#boot
                    Timber.d("Received boot notification")
                }

                intent.action == "android.intent.action.TIMEZONE_CHANGED" -> {
                    Timber.d("Received timezone change notification")
                }

                intent.action == notificationActionCancelled || intent.action == notificationActionMarkAsDone -> {
                    val taskId = intent.extras?.getInt(notificationExtraTaskIdKey)
                    if (taskId == null) {
                        Timber.e("Received %s without ID!", intent.action)
                    } else {
                        Timber.d("Received %s for %d", intent.action, taskId)

                        // Let ourselves run async operations (coroutines)
                        // https://stackoverflow.com/a/74112211
                        // https://developer.android.com/reference/android/content/BroadcastReceiver#goAsync()
                        val pendingResult = goAsync()
                        GlobalScope.launch(EmptyCoroutineContext) {
                            try {
                                // Initialize DB
                                val dao = AppDatabase.getDatabase(context).taskDao()
                                val repository =
                                    TaskRepository(dao, MainApplication.applicationScope)
                                // Find task
                                val task = repository.getTaskById(taskId).first()
                                if (task == null) {
                                    Timber.e("Task %d not found for %s", taskId, intent.action)
                                } else {
                                    when (intent.action) {
                                        notificationActionCancelled -> {
                                            val newTaskDef =
                                                task.definition.copy(
                                                    notificationLastDismissed = LocalDateTime.now()
                                                )
                                            repository.updateTask(newTaskDef)
                                        }

                                        notificationActionMarkAsDone -> {
                                            repository.insertCompletion(
                                                taskId = task.definition.id,
                                                completionDate = LocalDate.now(),
                                                frequencyWhenCompleted = task.definition.frequency
                                            )
                                        }
                                    }
                                }
                            } finally {
                                pendingResult.finish()
                            }
                        }
                    }
                }

                intent.action == null && intent.component?.className == AppBroadcastReceiver::class.java.name.toString() -> {
                    Timber.d("Received alarm")
                }

                else -> Timber.w("Unknown intent %s %s", intent.action, intent.component)
            }

        }
    }

    companion object {
        const val notificationActionMarkAsDone = BuildConfig.APPLICATION_ID + ".markAsDone"
        const val notificationActionCancelled = BuildConfig.APPLICATION_ID + ".swipedAway"
        const val notificationExtraTaskIdKey = "taskId"
        private const val notificationIdForGroup =
            Int.MAX_VALUE - 20 // Use an ID that won't conflict with any task IDs

        // This should stay constant, otherwise stale channels will appear in user's settings
        private const val notificationChannelId = "task_notifications"

        // It's safe to call this repeatedly, because creating an existing notification channel performs no operation.
        private fun createNotificationChannel(context: Context): String {
            // Create the NotificationChannel.
            val name = getString(context, R.string.notification_channel_name)
            val descriptionText = getString(context, R.string.notification_channel_description)
            val importance =
                NotificationManager.IMPORTANCE_DEFAULT // IMPORTANCE_DEFAULT == "high", with sound and shows in the status bar
            val mChannel = NotificationChannel(notificationChannelId, name, importance)
            mChannel.description = descriptionText
            // Register the channel with the system. You can't change the importance
            // or other notification behaviors after this.
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(mChannel)
            return mChannel.id
        }

        private fun createNotification(context: Context, task: Task) {
            // Doesn't hurt to (re-)create the notification channel
            createNotificationChannel(context)

            // Create an explicit intent for the target click activity.
            val genericClickIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            // This one is generic, so we can re-use the same request code
            val genericClickPendingIntent: PendingIntent =
                PendingIntent.getActivity(context, 0, genericClickIntent, FLAG_IMMUTABLE)

            // Create an intent for "mark as done"
            val markAsDoneIntent = Intent(context, AppBroadcastReceiver::class.java).apply {
                action = notificationActionMarkAsDone
                putExtra(notificationExtraTaskIdKey, task.definition.id)
            }
            val markAsDonePendingIntent: PendingIntent =
                PendingIntent.getBroadcast(
                    context,
                    task.definition.id, // Use a different request code, otherwise this intent will get merged with others: https://stackoverflow.com/a/20205696
                    markAsDoneIntent,
                    FLAG_IMMUTABLE
                )

            // Create an intent for dismissing the notification
            val cancelIntent = Intent(context, AppBroadcastReceiver::class.java).apply {
                action = notificationActionCancelled
                putExtra(notificationExtraTaskIdKey, task.definition.id)
            }
            // Use a different request code, otherwise this intent will get merged with others: https://stackoverflow.com/a/20205696
            val cancelPendingIntent: PendingIntent =
                PendingIntent.getBroadcast(
                    context,
                    task.definition.id,
                    cancelIntent,
                    FLAG_IMMUTABLE
                )

            // Set a group key
            val groupKey = "notifications_due"

            val notification = NotificationCompat.Builder(context, notificationChannelId)
                .setSmallIcon(R.drawable.baseline_task_alt_24)
                .setContentTitle(task.definition.name)
                .setContentText(task.getDueDaysString(context))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                // Set the intent that fires when the user taps the notification.
                .setContentIntent(genericClickPendingIntent)
                // Automatically removes the notification when the user taps it.
                .setAutoCancel(false)
                // Add an action to mark as done
                .addAction(
                    R.drawable.baseline_task_alt_24,
                    getString(context, R.string.notification_action_mark_as_done),
                    markAsDonePendingIntent
                )
                // Handle swiping away / dismissing
                .setDeleteIntent(cancelPendingIntent)
                // Group the notifications
                .setGroup(groupKey)
                .build()

            val summaryNotification = NotificationCompat.Builder(context, notificationChannelId)
                .setContentTitle("Tasks are due")
                .setSmallIcon(R.drawable.baseline_task_alt_24)
                // Set the intent that fires when the user taps the notification.
                .setContentIntent(genericClickPendingIntent)
                // Automatically removes the notification when the user taps it.
                .setAutoCancel(false)
                // Build summary info into InboxStyle template.
                .setStyle(
                    NotificationCompat.InboxStyle()
                        .setSummaryText("Tasks are due")
                )
                // Specify which group this notification belongs to.
                .setGroup(groupKey)
                // Set this notification as the summary for the group.
                .setGroupSummary(true)
                .build()


            with(NotificationManagerCompat.from(context)) {
                if (ActivityCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    Timber.e("No permission to send notification")
                    return
                }
                // notificationId is a unique int for each notification that you must define.
                notify(task.definition.id, notification)
                notify(notificationIdForGroup, summaryNotification)
            }


        }

        private fun updateNotificationsAndAlarms(context: Context, tasks: List<Task>) {
            // Check for any notifications that should be firing
            var anyNotifsPresent = false
            for (task in tasks) {
                if (task.nextNotification(context) < LocalDateTime.now()) {
                    createNotification(context, task)
                    anyNotifsPresent = true
                } else {
                    // Clear all other notification
                    with(NotificationManagerCompat.from(context)) {
                        cancel(task.definition.id)
                    }
                }
            }
            if (!anyNotifsPresent) {
                // Clear group
                with(NotificationManagerCompat.from(context)) {
                    cancel(notificationIdForGroup)
                }
            }

            when (val it = calculateNextNotificationTime(context, tasks)) {
                null -> Timber.i("No future alarms expected")
                else -> setAlarmInternal(context, it)
            }
        }

        fun watchTasksAndUpdateNotificationsAndAlarms(
            context: Context,
            externalScope: CoroutineScope
        ) {
            val dao = AppDatabase.getDatabase(context).taskDao()
            val repository = TaskRepository(dao, externalScope)
            externalScope.launch {
                Timber.d("Starting to watch for task changes to trigger notifications and alarms")
                repository.allTasks.collect { tasks ->
                    updateNotificationsAndAlarms(
                        context,
                        tasks
                    )
                }
            }
        }

        private fun calculateNextNotificationTime(
            context: Context,
            tasks: List<Task>
        ): LocalDateTime? {
            // Find the next alarm time
            return tasks
                // Only future alarms
                .filter { it.nextNotification(context) > LocalDateTime.now() }
                .minByOrNull { it.nextNotification(context) }
                ?.nextNotification(context)
        }

        private fun setAlarmInternal(context: Context, nextAlarmTime: LocalDateTime) {
            val timeInMillis =
                nextAlarmTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, AppBroadcastReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, FLAG_IMMUTABLE)
            alarmManager.set(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
            Timber.d("Set next alarm for %s (%d)", nextAlarmTime, timeInMillis)
        }
    }
}