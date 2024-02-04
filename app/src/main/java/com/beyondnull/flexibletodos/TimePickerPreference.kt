package com.beyondnull.flexibletodos

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.beyondnull.flexibletodos.R
import com.beyondnull.flexibletodos.activity.MainActivity
import com.beyondnull.flexibletodos.picker.createTimePicker
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

// A custom preference to show a time picker
class TimePickerPreference(context: Context, attrs: AttributeSet?) : Preference(context, attrs),
    View.OnClickListener {
    private val formatter: DateTimeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
    private lateinit var view: TextView

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val currentTime = Settings.NotificationTime.get(context)
        view = holder.findViewById(R.id.timePickerDescription) as TextView

        // Format the time
        view.text =
            String.format(
                context.getString(R.string.notification_time_description),
                "~${formatter.format(currentTime)}"
            )

        // Set the click listener
        holder.findViewById(R.id.timePickerPreferenceRow).setOnClickListener(this)
    }

    override fun onClick(v: View) {
        val currentTime = Settings.NotificationTime.get(context)

        // Show the time picker
        createTimePicker(currentTime) {
            Settings.NotificationTime.set(context, it)

            view.text =
                String.format(
                    context.getString(R.string.notification_time_description),
                    "~${formatter.format(it)}"
                )
        }.show((context as MainActivity).supportFragmentManager, "materialTimePicker")
    }

}