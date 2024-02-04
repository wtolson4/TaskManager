package com.beyondnull.flexibletodos.picker

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.beyondnull.flexibletodos.R
import com.beyondnull.flexibletodos.data.Settings

class PreferenceRowNotificationFrequency(context: Context, attrs: AttributeSet?) :
    Preference(context, attrs),
    View.OnClickListener {
    private lateinit var view: TextView

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val currentFrequency = Settings.NotificationFrequency.get(context)
        view = holder.findViewById(R.id.notificationFrequencyDescription) as TextView

        // Format the time
        view.text =
            String.format(
                context.getString(R.string.notification_time_description)
                        +
                        ": $currentFrequency"
            )

        // Set the click listener
        holder.itemView.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        val currentFrequency = Settings.NotificationFrequency.get(context)
        val newFrequency = (currentFrequency % 10) + 1
        Settings.NotificationFrequency.set(context, newFrequency)
        // TODO: actually have a slider, show the effect in real time
        view.text =
            String.format(
                context.getString(R.string.notification_time_description)
                        +
                        ": $newFrequency"
            )

    }

}