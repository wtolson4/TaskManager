package com.beyondnull.flexibletodos.picker

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.beyondnull.flexibletodos.R
import com.beyondnull.flexibletodos.calculation.GlobalFrequencyScaling
import com.beyondnull.flexibletodos.data.Settings

class PreferenceRowNotificationFrequency(context: Context, attrs: AttributeSet?) :
    Preference(context, attrs),
    View.OnClickListener {
    private lateinit var view: TextView

    private fun formatDescriptionString(currentFrequency: Int): String {
        val examples = intArrayOf(1, 2, 3, 5, 7, 14, 30, 60, 90, 180, 365, 730).joinToString("\n") {
            "Task due every $it days: Notification every ${
                GlobalFrequencyScaling.scale(
                    it,
                    currentFrequency
                )
            } day(s)"
        }
        return String.format(
            context.getString(R.string.notification_frequency_description)
                    + " ($currentFrequency of ${Settings.NotificationFrequency.max})"
                    + "\n" + examples
        )
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val currentFrequency = Settings.NotificationFrequency.get(context)
        view = holder.findViewById(R.id.notificationFrequencyDescription) as TextView

        // Format the time
        view.text = formatDescriptionString(currentFrequency)

        // Set the click listener
        holder.itemView.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        val currentFrequency = Settings.NotificationFrequency.get(context)
        val newFrequency =
            (currentFrequency % Settings.NotificationFrequency.max) + Settings.NotificationFrequency.min
        // TODO: (P2) replace with a slider
        Settings.NotificationFrequency.set(context, newFrequency)
        view.text = formatDescriptionString(newFrequency)
    }

}