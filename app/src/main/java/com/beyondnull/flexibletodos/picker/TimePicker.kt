package com.beyondnull.flexibletodos.picker

import com.beyondnull.flexibletodos.R
import com.google.android.material.timepicker.MaterialTimePicker
import java.time.LocalTime

/*  Use with
* createTimePicker().show(supportFragmentManager, "materialDatePicker")*/
fun createTimePicker(
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
            .build()

    timePicker.addOnPositiveButtonClickListener {
        val localTime = LocalTime.of(timePicker.hour, timePicker.minute)
        callback(localTime)
    }
    return timePicker
}
