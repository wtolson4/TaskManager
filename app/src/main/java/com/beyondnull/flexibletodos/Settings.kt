package com.beyondnull.flexibletodos

import android.content.Context
import androidx.preference.PreferenceManager
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class Settings {
    class NotificationTime {

        companion object {
            private const val key = "global_notification_time"
            private val formatter = DateTimeFormatter.ISO_LOCAL_TIME
            private val default = LocalTime.of(10, 0)
            fun get(context: Context): LocalTime {
                val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)
                val preferences = PreferenceManager.getDefaultSharedPreferences(context)
                return preferences.getString(key, null)
                    ?.let { LocalTime.parse(it, formatter) }
                    ?: default
            }

            fun set(context: Context, time: LocalTime) {
                val preferences = PreferenceManager.getDefaultSharedPreferences(context)
                val contents = time.format(formatter)
                with(preferences.edit()) {
                    putString(key, contents)
                    apply()
                }
            }
        }
    }


    class Other {

    }
}