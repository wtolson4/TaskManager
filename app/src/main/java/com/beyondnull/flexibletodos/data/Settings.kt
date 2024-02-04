package com.beyondnull.flexibletodos.data

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


    class NotificationFrequency {

        companion object {
            private const val key = "global_notification_frequency"
            private const val default = 7
            fun get(context: Context): Int {
                val preferences = PreferenceManager.getDefaultSharedPreferences(context)
                return preferences.getInt(key, default)
            }

            fun set(context: Context, frequency: Int) {
                val preferences = PreferenceManager.getDefaultSharedPreferences(context)
                with(preferences.edit()) {
                    putInt(key, frequency)
                    apply()
                }
            }
        }
    }
}