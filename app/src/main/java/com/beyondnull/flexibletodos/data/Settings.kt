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


    class NotificationPeriodScale {

        companion object {
            private const val key = "global_notification_frequency"
            private const val default = 3
            const val min = 1
            const val max = 5
            fun get(context: Context): Int {
                val preferences = PreferenceManager.getDefaultSharedPreferences(context)
                return preferences.getInt(key, default).coerceAtLeast(min).coerceAtMost(max)
            }

            fun set(context: Context, frequency: Int) {
                val preferences = PreferenceManager.getDefaultSharedPreferences(context)
                with(preferences.edit()) {
                    putInt(key, frequency.coerceAtLeast(min).coerceAtMost(max))
                    apply()
                }
            }
        }
    }

    // TODO(P3): Consider the following settings from the old app:
    // - color prominence: bold (highlight entire row), subtle (only due date text is colored)
    // - color range: regular (green to red), extended (blue to red), discrete
    // - font weight: normal vs light
    // - task details page: scroll entire page, or just list of completions
    // - task list ordering: by due date, or by urgency score
    // - confirmation for logging task: enable / disable
    // - first day of week
    // - language
    // - widget: background (transparent, 25/50/75/100% black)
    // - widget: density
    // - anonymous crash reporting enabled
}