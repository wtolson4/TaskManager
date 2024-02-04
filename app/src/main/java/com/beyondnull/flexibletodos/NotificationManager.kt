package com.beyondnull.flexibletodos

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.preference.PreferenceManager
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.coroutines.EmptyCoroutineContext

class NotificationManager {
    fun updateNotificationsAndAlarms(context: Context) {
        val desiredNotifications = calculateCurrentNotifications(context)
        // TODO: fire notifications
        Timber.e(
            "Would fire notifications for: %s",
            desiredNotifications.joinToString(','.toString()) { it.definition.name }
        )

        when (val it = calculateNextNotificationTime(context)) {
            null -> Timber.i("No future alarms expected")
            else -> setAlarmInternal(context, it)
        }
    }

    private fun calculateCurrentNotifications(context: Context): List<Task> {
        // Global app data
        val dao = AppDatabase.getDatabase(context).taskDao()
        val repository = TaskRepository(dao)

        // Check for any notifications that should be firing
        return repository.getAllTasks()
            .filter { it.nextNotification(context) < LocalDateTime.now() }
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
            .minBy { it.nextNotification(context) }
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
            when {
                intent.action == "android.intent.action.BOOT_COMPLETED" -> {
                    // Must reset alarms when device reboots
                    // https://developer.android.com/develop/background-work/services/alarms/schedule#boot
                    Timber.d("Received boot notification")
                }

                intent.action == "android.intent.action.TIMEZONE_CHANGED" -> Timber.d("Received timezone change notification")
                intent.component?.className == AppBroadcastReceiver::class.java.name.toString() -> Timber.d(
                    "Received alarm"
                )

                else -> Timber.w("Unknown intent %s %s", intent.action, intent.component)
            }

            // https://stackoverflow.com/a/74112211
            // https://developer.android.com/reference/android/content/BroadcastReceiver#goAsync()
            val pendingResult = goAsync()
            GlobalScope.launch(EmptyCoroutineContext) {
                try {
                    // Update notifications
                    NotificationManager().updateNotificationsAndAlarms(context)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    private class LatestNotificationStorage {
        private val preferenceName = "last_notification"
        private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

        fun get(context: Context): LocalDateTime {
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            return preferences.getString(preferenceName, null)
                ?.let { LocalDateTime.parse(it, formatter) }
                ?: run {
                    Timber.d("No recorded last notification time")
                    LocalDateTime.MIN
                }
        }

        fun set(context: Context, dateTime: LocalDateTime) {
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            val contents = dateTime.format(formatter)
            with(preferences.edit()) {
                putString(preferenceName, contents)
                apply()
            }
        }
    }
}