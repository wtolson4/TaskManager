package com.beyondnull.flexibletodos.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.beyondnull.flexibletodos.BuildConfig
import com.beyondnull.flexibletodos.R
import com.beyondnull.flexibletodos.calculation.GlobalFrequencyScaling
import com.beyondnull.flexibletodos.data.AppDatabase
import com.beyondnull.flexibletodos.data.Settings
import com.beyondnull.flexibletodos.picker.createTimePicker
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import de.raphaelebner.roomdatabasebackup.core.RoomBackup
import timber.log.Timber
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.system.exitProcess

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Reference UI components
        val topAppBar = findViewById<MaterialToolbar>(R.id.topAppBar)
        val notificationFrequencyRow = findViewById<LinearLayout>(R.id.notificationFrequencyRow)
        val notificationTimeRow = findViewById<LinearLayout>(R.id.notificationTimeRow)
        val backupRow = findViewById<LinearLayout>(R.id.backupRow)
        val licensesRow = findViewById<LinearLayout>(R.id.licensesRow)

        // Set click listener for top app bar navigation button
        topAppBar.setNavigationOnClickListener { finish() }

        // Setup other settings rows
        PreferenceRowNotificationFrequency(this, notificationFrequencyRow)
        PreferenceRowNotificationTime(this, notificationTimeRow)
        PreferenceRowBackup(this, backupRow)
        PreferenceRowLicenses(this, licensesRow)
    }
    // TODO: (P1) Export / import functionality
    // TODO: (P1) write a script on PC to convert regularly DB to this format
    // TODO: (P2) Add a settings link to the notification settings

    class PreferenceRowNotificationFrequency(private val context: Context, holder: View) :
        View.OnClickListener {
        private var titleView: TextView
        private var descriptionView: TextView

        init {
            val currentFrequency = Settings.NotificationFrequency.get(context)

            titleView = holder.findViewById(R.id.preferenceTitle) as TextView
            titleView.text = context.getString(R.string.notification_frequency_name)
            descriptionView = holder.findViewById(R.id.preferenceDescription) as TextView

            // Format the time
            descriptionView.text = formatDescriptionString(currentFrequency)

            // Set the click listener
            holder.setOnClickListener(this)
        }


        private fun formatDescriptionString(currentFrequency: Int): String {
            val examples =
                intArrayOf(1, 2, 3, 5, 7, 14, 30, 60, 90, 180, 365, 730).joinToString("\n") {
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

        override fun onClick(v: View) {
            val currentFrequency = Settings.NotificationFrequency.get(context)
            val newFrequency =
                (currentFrequency % Settings.NotificationFrequency.max) + Settings.NotificationFrequency.min
            // TODO: (P2) replace with a slider
            Settings.NotificationFrequency.set(context, newFrequency)
            descriptionView.text = formatDescriptionString(newFrequency)
        }

    }

    class PreferenceRowNotificationTime(private val context: Context, holder: View) :
        View.OnClickListener {
        private val formatter: DateTimeFormatter =
            DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
        private var titleView: TextView
        private var descriptionView: TextView

        init {
            val currentTime = Settings.NotificationTime.get(context)

            titleView = holder.findViewById(R.id.preferenceTitle) as TextView
            titleView.text = context.getString(R.string.notification_time_name)
            descriptionView = holder.findViewById(R.id.preferenceDescription) as TextView

            // Format the time
            descriptionView.text =
                String.format(
                    context.getString(R.string.notification_time_description)
                            +
                            ": ${formatter.format(currentTime)}"
                )

            // Set the click listener
            holder.setOnClickListener(this)
        }

        override fun onClick(v: View) {
            val currentTime = Settings.NotificationTime.get(context)

            // Show the time picker
            createTimePicker(context, currentTime) {
                Settings.NotificationTime.set(context, it)

                descriptionView.text =
                    String.format(
                        context.getString(R.string.notification_time_description)
                                +
                                ": ${formatter.format(it)}"
                    )
            }.show((context as AppCompatActivity).supportFragmentManager, "materialTimePicker")
        }
    }

    class PreferenceRowBackup(
        private val context: Context,
        holder: View,
    ) :
        View.OnClickListener {
        private var titleView: TextView
        private var descriptionView: TextView
        private val backup: RoomBackup

        init {
            titleView = holder.findViewById(R.id.preferenceTitle) as TextView
            titleView.text = context.getString(R.string.backup_setting_title)
            descriptionView = holder.findViewById(R.id.preferenceDescription) as TextView
            descriptionView.text = context.getString(R.string.backup_setting_description)

            // Initialize the backup
            backup = RoomBackup(context).database(AppDatabase.getDatabase(context))
                .enableLogDebug(BuildConfig.DEBUG)
                .backupLocation(RoomBackup.BACKUP_FILE_LOCATION_CUSTOM_DIALOG)
                .customBackupFileName(context.getString(R.string.backup_setting_title) + LocalDate.now() + ".sqlite3")
                .apply {
                    onCompleteListener { success, message, exitCode ->
                        Timber.d("RoomBackup: success: $success, message: $message, exitCode: $exitCode")
                        val i =
                            context.packageManager.getLaunchIntentForPackage(context.packageName)
                        i!!.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        context.startActivity(i)
                        exitProcess(0)
                        //if (success) restartApp(Intent(context@ MainActivity, MainActivity::class.java))
                    }
                }
            // Set the click listener
            holder.setOnClickListener(this)
        }

        override fun onClick(v: View) {
            MaterialAlertDialogBuilder(context)
                .setTitle("temp do backup?")
                .setMessage("temp restore or backup")
                .setNegativeButton(R.string.dialog_ok) { _, _ ->
                    backup.restore()
                }
                .setPositiveButton(R.string.dialog_ok) { _, _ ->
                    backup.backup()
                }
                .show()
        }
    }

    class PreferenceRowLicenses(
        private val context: Context,
        holder: View,
    ) :
        View.OnClickListener {
        private var titleView: TextView
        private var descriptionView: TextView

        init {
            titleView = holder.findViewById(R.id.preferenceTitle) as TextView
            titleView.text = context.getString(R.string.licenses_settings_title)
            descriptionView = holder.findViewById(R.id.preferenceDescription) as TextView
            descriptionView.text = context.getString(R.string.licenses_settings_description)
            holder.setOnClickListener(this)
        }

        override fun onClick(v: View) {
            MaterialAlertDialogBuilder(context)
                .setTitle(R.string.licenses_settings_title)
                .setMessage(R.string.licenses_list)
                .setPositiveButton(R.string.dialog_ok) { _, _ ->
                    // No-op
                }
                .show()
        }
    }
}