package com.example.flexibletodolistapp2

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import timber.log.Timber

class BootOrTzReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.intent.action.BOOT_COMPLETED") {
            // Reset alarms when device reboots
            // https://developer.android.com/develop/background-work/services/alarms/schedule#boot
            Timber.d("Received boot notification")
            AppAlarmManager().setAlarm(context)
        }
        if (intent.action == "android.intent.action.TIMEZONE_CHANGED") {
            Timber.d("Received timezone change notification")
            AppAlarmManager().setAlarm(context)
        }
    }
}
