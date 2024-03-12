package com.beyondnull.flexibletodos.manager

import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.content.Context
import android.content.Intent
import com.beyondnull.flexibletodos.BuildConfig
import com.beyondnull.flexibletodos.data.AppDatabase
import com.beyondnull.flexibletodos.data.TaskRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.ZoneId

class AlarmManager(
    private val appContext: Context,
    private val externalScope: CoroutineScope,
) {
    companion object {
        const val alarmActionNextNotificationTime =
            BuildConfig.APPLICATION_ID + ".nextNotificationTime"
    }

    fun watchTasksAndSetAlarm() {
        val dao = AppDatabase.getDatabase(appContext).taskDao()
        val taskRepository = TaskRepository(dao, externalScope)
        externalScope.launch {
            Timber.d("Starting to watch for task changes to trigger alarms")
            taskRepository.nextNotificationTime(appContext).collect { nextAlarmTime ->
                if (nextAlarmTime == null) {
                    Timber.d("No next alarm time")
                } else {
                    val timeInMillis =
                        nextAlarmTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    val alarmManager =
                        appContext.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
                    val intent =
                        Intent(appContext, BroadcastReceiver::class.java).apply {
                            action = alarmActionNextNotificationTime
                        }
                    val pendingIntent =
                        PendingIntent.getBroadcast(appContext, 0, intent, FLAG_IMMUTABLE)
                    alarmManager.set(
                        android.app.AlarmManager.RTC_WAKEUP,
                        timeInMillis,
                        pendingIntent
                    )
                    Timber.d("Set next alarm for %s (%d)", nextAlarmTime, timeInMillis)
                }
            }
        }
    }
}