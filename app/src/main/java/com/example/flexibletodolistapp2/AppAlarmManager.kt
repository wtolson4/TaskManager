package com.example.flexibletodolistapp2

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.FileNotFoundException
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.coroutines.EmptyCoroutineContext

class AppAlarmManager {

    class LatestNotification {
        private val storagePath = "last_notification.txt"
        private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
        fun get(context: Context): LocalDateTime {
            try {
                val read = context.openFileInput(storagePath).bufferedReader().readText()
                return LocalDateTime.parse(read, formatter)
            } catch (e: FileNotFoundException) {
                Timber.d("No recorded last notification time")
                return LocalDateTime.MIN
            }
        }

        fun set(context: Context, dateTime: LocalDateTime) {
            val fileContents = dateTime.format(formatter)
            context.openFileOutput(storagePath, Context.MODE_PRIVATE).use {
                it.write(fileContents.toByteArray())
            }
        }

    }

    fun setAlarm(context: Context) {
        // Global app data
        val dao = AppDatabase.getDatabase(context).taskDao()
        val repository = TaskRepository(dao)

        // Find the next alarm time
        val latestNotification = LatestNotification().get(context)
        val nextAlarmTime =
            repository.getAllTasks().minBy { it.nextNotification(latestNotification) }
                .nextNotification(latestNotification)
        val timeInMillis = nextAlarmTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        // Set the alarm
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, MyAlarm::class.java)
        val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, FLAG_IMMUTABLE)
        alarmManager.set(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
        Timber.d("Set next alarm for %s (%d)", nextAlarmTime, timeInMillis)
    }

    class MyAlarm : BroadcastReceiver() {
        @OptIn(DelicateCoroutinesApi::class)
        override fun onReceive(
            context: Context, intent: Intent
        ) {
            Timber.d("Alarm fired")

            // https://stackoverflow.com/a/74112211
            // https://developer.android.com/reference/android/content/BroadcastReceiver#goAsync()
            val pendingResult = goAsync()
            GlobalScope.launch(EmptyCoroutineContext) {
                try {
                    // Global app data
                    val dao = AppDatabase.getDatabase(context).taskDao()
                    val repository = TaskRepository(dao)

                    // Get prior notification time
                    val latestNotification = LatestNotification().get(context)
                    // Check for any notifications that should have fired between last notif and now
                    val expiredTasks = repository.getAllTasks()
                        .filter { it.nextNotification(latestNotification) < LocalDateTime.now() }
                    // TODO: fire notifications
                    Timber.e(
                        "Would fire notifs for: %s",
                        expiredTasks.joinToString(','.toString()) { it.definition.name }
                    )

                    // TODO: set the LatestNotification
                    // TODO: Set the next alarm to the earliest "next notification time", by calling self.setAlarm()
                } finally {
                    pendingResult.finish()
                }
            }


        }
    }
}