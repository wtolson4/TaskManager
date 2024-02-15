package com.beyondnull.flexibletodos.manager

import android.app.NotificationChannel
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat.getString
import com.beyondnull.flexibletodos.BuildConfig
import com.beyondnull.flexibletodos.R
import com.beyondnull.flexibletodos.activity.MainActivity
import com.beyondnull.flexibletodos.data.AppDatabase
import com.beyondnull.flexibletodos.data.Task
import com.beyondnull.flexibletodos.data.TaskRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDateTime

class NotificationManager {
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
                android.app.NotificationManager.IMPORTANCE_DEFAULT // IMPORTANCE_DEFAULT == "high", with sound and shows in the status bar
            val mChannel = NotificationChannel(notificationChannelId, name, importance)
            mChannel.description = descriptionText
            // Register the channel with the system. You can't change the importance
            // or other notification behaviors after this.
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
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
            val markAsDoneIntent = Intent(context, BroadcastReceiver::class.java).apply {
                action = notificationActionMarkAsDone
                putExtra(notificationExtraTaskIdKey, task.id)
            }
            val markAsDonePendingIntent: PendingIntent =
                PendingIntent.getBroadcast(
                    context,
                    task.id, // Use a different request code, otherwise this intent will get merged with others: https://stackoverflow.com/a/20205696
                    markAsDoneIntent,
                    FLAG_IMMUTABLE
                )

            // Create an intent for dismissing the notification
            val cancelIntent = Intent(context, BroadcastReceiver::class.java).apply {
                action = notificationActionCancelled
                putExtra(notificationExtraTaskIdKey, task.id)
            }
            // Use a different request code, otherwise this intent will get merged with others: https://stackoverflow.com/a/20205696
            val cancelPendingIntent: PendingIntent =
                PendingIntent.getBroadcast(
                    context,
                    task.id,
                    cancelIntent,
                    FLAG_IMMUTABLE
                )

            // Set a group key
            val groupKey = "notifications_due"

            val notification = NotificationCompat.Builder(context, notificationChannelId)
                .setSmallIcon(R.drawable.baseline_task_alt_24)
                .setContentTitle(task.name)
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
                notify(task.id, notification)
                notify(notificationIdForGroup, summaryNotification)
            }


        }

        private fun updateNotifications(context: Context, tasks: List<Task>) {
            var anyNotifsPresent = false

            Timber.d("Checking for notification updates")
            for (task in tasks) {
                // Check for any notifications that should be firing
                val nextNotification = task.nextNotification(context)
                if (nextNotification != null && nextNotification < LocalDateTime.now()) {
                    // Check if the notification already exists before firing a new one
                    if (NotificationManagerCompat.from(context).activeNotifications.find { it.id == task.id } == null) {
                        createNotification(context, task)
                    } else {
                        // Timber.d("Notification already exists for ID ${task.id} ")
                    }
                    anyNotifsPresent = true
                } else {
                    // Clear all other notification
                    with(NotificationManagerCompat.from(context)) {
                        cancel(task.id)
                    }
                }
            }
            if (!anyNotifsPresent) {
                // Clear group
                with(NotificationManagerCompat.from(context)) {
                    cancel(notificationIdForGroup)
                }
            }
        }

        fun watchTasksAndUpdateNotifications(
            context: Context,
            externalScope: CoroutineScope
        ) {
            val dao = AppDatabase.getDatabase(context).taskDao()
            val repository = TaskRepository(dao, externalScope)
            externalScope.launch {
                Timber.d("Starting to watch for task changes to trigger alarms")
                repository.allTasks.collect { tasks ->
                    Timber.d("Got updated tasks")
                    updateNotifications(
                        context,
                        tasks
                    )
                }
            }
        }
    }
}