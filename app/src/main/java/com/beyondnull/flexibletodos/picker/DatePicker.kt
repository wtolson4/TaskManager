package com.beyondnull.flexibletodos.picker

import com.beyondnull.flexibletodos.R
import com.google.android.material.datepicker.MaterialDatePicker
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/*  Use with
* createDatePicker().show(supportFragmentManager, "materialDatePicker")*/
fun createDatePicker(
    initializer: LocalDate?,
    callback: (LocalDate) -> Unit
): MaterialDatePicker<Long> {
    val initialDate = initializer?.atStartOfDay(
        ZoneOffset.UTC
    )?.toInstant()?.toEpochMilli() ?: MaterialDatePicker.todayInUtcMilliseconds()
    val datePicker =
        MaterialDatePicker.Builder.datePicker()
            .setTitleText(R.string.select_date)
            .setSelection(initialDate)
            .build()
    datePicker.addOnPositiveButtonClickListener {
        val localDate = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
        callback(localDate)
    }
    return datePicker
}
