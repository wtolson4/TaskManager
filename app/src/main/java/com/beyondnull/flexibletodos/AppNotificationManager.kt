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
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.coroutines.EmptyCoroutineContext

class AppNotificationManager {
    // This should stay constant, otherwise stale channels will appear in user's settings
    private val notificationChannelId = "task_notifications"

    // It's safe to call this repeatedly, because creating an existing notification channel performs no operation.
    private fun createNotificationChannel(context: Context): String {
        // Create the NotificationChannel.
        val name = getString(context, R.string.notification_channel_name)
        val descriptionText = getString(context, R.string.notification_channel_description)
        val importance =
            NotificationManager.IMPORTANCE_DEFAULT // IMPORTANCE_DEFAULT == "high", with sound and shows in the status bar
        val mChannel = NotificationChannel(notificationChannelId, name, importance)
        mChannel.setSound(null, null) // Set the notification to silent
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
        val clickIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        // Use a different request code, otherwise this intent will get merged with others: https://stackoverflow.com/a/20205696
        val clickPendingIntent: PendingIntent =
            PendingIntent.getActivity(context, task.definition.id, clickIntent, FLAG_IMMUTABLE)

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
            PendingIntent.getBroadcast(context, task.definition.id, cancelIntent, FLAG_IMMUTABLE)

        // Set a group key
        val groupKey = "notifications_due"

        val notification = NotificationCompat.Builder(context, notificationChannelId)
            .setSmallIcon(R.drawable.baseline_task_alt_24)
            .setContentTitle(task.definition.name)
            .setContentText(task.getDueDaysString(context))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            // Set the intent that fires when the user taps the notification.
            .setContentIntent(clickPendingIntent)
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

    fun updateNotificationsAndAlarms(context: Context) {
        // Global app data
        val dao = AppDatabase.getDatabase(context).taskDao()
        val repository = TaskRepository(dao)

        // Check for any notifications that should be firing
        var anyNotifsPresent = false
        for (task in repository.getAllTasks()) {
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

        when (val it = calculateNextNotificationTime(context)) {
            null -> Timber.i("No future alarms expected")
            else -> setAlarmInternal(context, it)
        }
    }

    private fun calculateNextNotificationTime(
        context: Context,
    ): LocalDateTime? {
        // Global app data
        val dao = AppDatabase.getDatabase(context).taskDao()
        val repository = TaskRepository(dao)

        // Find the next alarm time
        return repository.getAllTasks()
            // Only future alarms
            .filter { it.nextNotification(context) > LocalDateTime.now() }
            .minByOrNull { it.nextNotification(context) }
            ?.nextNotification(context)
    }

    private fun setAlarmInternal(context: Context, nextAlarmTime: LocalDateTime) {
        val timeInMillis = nextAlarmTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AppBroadcastReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, FLAG_IMMUTABLE)
        alarmManager.set(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
        Timber.d("Set next alarm for %s (%d)", nextAlarmTime, timeInMillis)
    }

    class AppBroadcastReceiver : BroadcastReceiver() {
        @OptIn(DelicateCoroutinesApi::class)
        override fun onReceive(
            context: Context, intent: Intent
        ) {
            // https://stackoverflow.com/a/74112211
            // https://developer.android.com/reference/android/content/BroadcastReceiver#goAsync()
            val pendingResult = goAsync()
            GlobalScope.launch(EmptyCoroutineContext) {
                try {
                    when {
                        intent.action == "android.intent.action.BOOT_COMPLETED" -> {
                            // Must reset alarms when device reboots
                            // https://developer.android.com/develop/background-work/services/alarms/schedule#boot
                            Timber.d("Received boot notification")
                            AppNotificationManager().updateNotificationsAndAlarms(context)
                        }

                        intent.action == "android.intent.action.TIMEZONE_CHANGED" -> {
                            Timber.d("Received timezone change notification")
                            AppNotificationManager().updateNotificationsAndAlarms(context)
                        }

                        intent.action == notificationActionCancelled -> {
                            // TODO: This handler is too heavy, and duplicates logic from the ViewModel
                            when (val taskId =
                                intent.extras?.getInt(notificationExtraTaskIdKey)) {
                                null -> Timber.e("Received cancel action without ID!")
                                else -> {
                                    Timber.d("Received cancel notification action for %d", taskId)
                                    val dao = AppDatabase.getDatabase(context).taskDao()
                                    val repository = TaskRepository(dao)
                                    when (val task = repository.getTaskById(taskId)) {
                                        null -> Timber.e("Task not found: %d", taskId)
                                        else -> {
                                            val newTaskDef =
                                                task.definition.copy(notificationLastDismissed = LocalDateTime.now())
                                            repository.updateTask(newTaskDef)
                                            AppNotificationManager().updateNotificationsAndAlarms(
                                                context
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        intent.action == notificationActionMarkAsDone -> {
                            // TODO: This handler is too heavy, and duplicates logic from the ViewModel
                            when (val taskId =
                                intent.extras?.getInt(notificationExtraTaskIdKey)) {
                                null -> Timber.e("Received mark as done action without ID!")
                                else -> {
                                    Timber.d("Received mark as done action for %d", taskId)
                                    // Global app data
                                    val dao = AppDatabase.getDatabase(context).taskDao()
                                    val repository = TaskRepository(dao)
                                    when (val task = repository.getTaskById(taskId)) {
                                        null -> Timber.e("Task not found: %d", taskId)
                                        else -> {
                                            repository.insertCompletion(
                                                taskId = task.definition.id,
                                                completionDate = LocalDate.now(),
                                                frequencyWhenCompleted = task.definition.frequency
                                            )
                                            AppNotificationManager().updateNotificationsAndAlarms(
                                                context
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Keep this after notificationActionMarkAsDone
                        intent.component?.className == AppBroadcastReceiver::class.java.name.toString() -> {
                            Timber.d("Received alarm, %s", intent.action)
                            AppNotificationManager().updateNotificationsAndAlarms(context)
                        }

                        else -> Timber.w("Unknown intent %s %s", intent.action, intent.component)
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    companion object {
        const val notificationActionMarkAsDone = BuildConfig.APPLICATION_ID + ".markAsDone"
        const val notificationActionCancelled = BuildConfig.APPLICATION_ID + ".swipedAway"
        const val notificationExtraTaskIdKey = "taskId"
        const val notificationIdForGroup =
            Int.MAX_VALUE - 20 // Use an ID that won't conflict with any task IDs
    }
}