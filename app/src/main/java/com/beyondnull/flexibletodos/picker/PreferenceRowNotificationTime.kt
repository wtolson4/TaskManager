package com.beyondnull.flexibletodos.picker

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.beyondnull.flexibletodos.AppNotificationManager
import com.beyondnull.flexibletodos.R
import com.beyondnull.flexibletodos.data.Settings
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

// A custom preference to show a time picker
class PreferenceRowNotificationTime(context: Context, attrs: AttributeSet?) :
    Preference(context, attrs),
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
                context.getString(R.string.notification_time_description)
                        +
                        ": ${formatter.format(currentTime)}"
            )

        // Set the click listener
        holder.itemView.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        val currentTime = Settings.NotificationTime.get(context)

        // Show the time picker
        createTimePicker(context, currentTime) {
            Settings.NotificationTime.set(context, it)
            AppNotificationManager().updateNotificationsAndAlarms(context)

            view.text =
                String.format(
                    context.getString(R.string.notification_time_description)
                            +
                            ": ${formatter.format(it)}"
                )
        }.show((context as AppCompatActivity).supportFragmentManager, "materialTimePicker")
    }

}