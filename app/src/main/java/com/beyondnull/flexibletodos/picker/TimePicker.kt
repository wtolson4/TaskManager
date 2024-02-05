package com.beyondnull.flexibletodos.picker

import android.content.Context
import android.text.format.DateFormat.is24HourFormat
import com.beyondnull.flexibletodos.R
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.MaterialTimePicker.INPUT_MODE_CLOCK
import com.google.android.material.timepicker.TimeFormat
import java.time.LocalTime

/*  Use with
* createTimePicker().show(supportFragmentManager, "materialDatePicker")*/
fun createTimePicker(
    context: Context,
    initializer: LocalTime?,
    callback: (LocalTime) -> Unit
): MaterialTimePicker {
    val currentHour = initializer?.hour ?: 0
    val currentMinute = initializer?.minute ?: 0
    val timePicker =
        MaterialTimePicker.Builder()
            .setHour(currentHour)
            .setMinute(currentMinute)
            .setTitleText(R.string.select_time)
            .setTimeFormat(if (is24HourFormat(context)) TimeFormat.CLOCK_24H else TimeFormat.CLOCK_12H)
            .setInputMode(INPUT_MODE_CLOCK)
            .build()

    timePicker.addOnPositiveButtonClickListener {
        val localTime = LocalTime.of(timePicker.hour, timePicker.minute)
        callback(localTime)
    }
    return timePicker
}
