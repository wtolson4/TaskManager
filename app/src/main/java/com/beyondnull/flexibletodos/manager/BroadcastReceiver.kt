package com.beyondnull.flexibletodos.manager

import android.content.Context
import android.content.Intent
import com.beyondnull.flexibletodos.MainApplication
import com.beyondnull.flexibletodos.data.AppDatabase
import com.beyondnull.flexibletodos.data.TaskRepository
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
                            val repository = TaskRepository(dao, MainApplication.applicationScope)
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

            intent.action == null && intent.component?.className == BroadcastReceiver::class.java.name.toString() -> {
                Timber.d("Received alarm")
            }

            else -> Timber.w("Unknown intent %s %s", intent.action, intent.component)
        }

    }
}