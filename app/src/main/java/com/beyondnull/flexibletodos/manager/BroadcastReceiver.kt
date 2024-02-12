package com.beyondnull.flexibletodos.manager

import android.content.Context
import android.content.Intent
import com.beyondnull.flexibletodos.MainApplication
import com.beyondnull.flexibletodos.data.AppDatabase
import com.beyondnull.flexibletodos.data.TaskRepository
import com.beyondnull.flexibletodos.manager.AlarmManager.Companion.alarmActionNextNotificationTime
import com.beyondnull.flexibletodos.manager.NotificationManager.Companion.notificationActionCancelled
import com.beyondnull.flexibletodos.manager.NotificationManager.Companion.notificationActionMarkAsDone
import com.beyondnull.flexibletodos.manager.NotificationManager.Companion.notificationExtraTaskIdKey
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.coroutines.EmptyCoroutineContext

class BroadcastReceiver : android.content.BroadcastReceiver() {
    @OptIn(DelicateCoroutinesApi::class)
    override fun onReceive(
        context: Context, intent: Intent
    ) {
        // Let ourselves run async operations (coroutines)
        // https://stackoverflow.com/a/74112211
        // https://developer.android.com/reference/android/content/BroadcastReceiver#goAsync()
        val pendingResult = goAsync()
        GlobalScope.launch(EmptyCoroutineContext) {
            try {
                // Initialize DB
                val dao = AppDatabase.getDatabase(context).taskDao()
                val repository = TaskRepository(dao, MainApplication.applicationScope)

                when (intent.action) {
                    // Must reset alarms when the time/TZ changes, and also upon reboot:
                    // https://developer.android.com/develop/background-work/services/alarms/schedule#boot
                    "android.intent.action.TIMEZONE_CHANGED", "android.intent.action.TIME_SET", "android.intent.action.BOOT_COMPLETED",
                        // This is an alarm for when the next task notifiction should fire.
                    alarmActionNextNotificationTime
                    -> {
                        Timber.d("Received %s", intent.action)
                        // Need to take some action here that triggers a change in the Alarm and Notitication managers
                        // TODO(P3): Make this a setting instead of modifying the Tasks table, and make Alarm + Notif manager flows listen to this
                        val allTasks = repository.allTasks.first()
                        val randomTask = allTasks.firstOrNull()
                        randomTask?.let {
                            Timber.d("Hack: Updating ${randomTask.definition.id} to kick db")
                            val existingDescription = randomTask.definition.description
                            val newTaskDef = randomTask.definition.copy(description = "")
                            repository.updateTask(newTaskDef)
                            repository.updateTask(randomTask.definition)
                        }


                    }

                    notificationActionCancelled, notificationActionMarkAsDone -> {
                        val taskId = intent.extras?.getInt(notificationExtraTaskIdKey)
                        if (taskId == null) {
                            Timber.e("Received %s without ID!", intent.action)
                        } else {
                            Timber.d("Received %s for %d", intent.action, taskId)
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
                                        )
                                    }
                                }
                            }
                        }
                    }

                    else -> Timber.w("Unknown intent %s %s", intent.action, intent.component)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}