package com.beyondnull.flexibletodos.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.beyondnull.flexibletodos.MainApplication
import com.beyondnull.flexibletodos.R
import com.beyondnull.flexibletodos.data.AppDatabase
import com.beyondnull.flexibletodos.data.Task
import com.beyondnull.flexibletodos.data.TaskRepository
import com.beyondnull.flexibletodos.data.TaskViewModel
import com.beyondnull.flexibletodos.data.TaskViewModelFactory
import com.beyondnull.flexibletodos.manager.PermissionRequester
import com.beyondnull.flexibletodos.picker.createDatePicker
import com.beyondnull.flexibletodos.picker.createTimePicker
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Objects.isNull


class EditTaskActivity : AppCompatActivity() {

    private lateinit var viewModel: TaskViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_task)

        // Global app data
        val dao = AppDatabase.getDatabase(this).taskDao()
        val repository = TaskRepository(dao, MainApplication.applicationScope)
        val factory = TaskViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[TaskViewModel::class.java]

        // Get data passed into this activity
        val bundle = intent.extras
        val existingId = bundle?.getInt("taskId")
        val existingTask = existingId?.let { viewModel.getTaskById(it) }

        // Data for this activity
        val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)
        val timeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
        val permissionRequester = PermissionRequester(this)

        // Reference UI components
        val taskNameEditText = findViewById<EditText>(R.id.taskNameEditText)
        val descriptionEditText = findViewById<EditText>(R.id.descriptionEditText)
        val initialDueEditText = findViewById<TextView>(R.id.initialDueEditText)
        val nextDueEditText = findViewById<TextView>(R.id.nextDueEditText)
        val enableTaskNotificationSwitch =
            findViewById<MaterialSwitch>(R.id.enableIndividualTaskNotifications)
        val overrideGlobalNotificationSwitch =
            findViewById<MaterialSwitch>(R.id.overrideGlobalNotificationTimeSwitch)
        val notificationTimeEditText = findViewById<TextView>(R.id.notificationTimeEditText)
        var initialDueDate: LocalDate? = null
        var notificationTime: LocalTime? = null
        val periodEditText = findViewById<EditText>(R.id.periodEditText)
        val addTaskButton = findViewById<Button>(R.id.saveTaskButton)
        val cancelButton = findViewById<Button>(R.id.cancelButton)
        val topAppBar = findViewById<MaterialToolbar>(R.id.topAppBar)

        // Initialize UI
        enableTaskNotificationSwitch.isChecked = true

        // Fill in data for modifying existing task
        val taskObserver = Observer<Task?> { incomingTask ->
            incomingTask?.let {
                // Set visibility
                (initialDueEditText.parent.parent as View).visibility = View.GONE
                (nextDueEditText.parent.parent as View).visibility = View.VISIBLE

                // Fill in data
                taskNameEditText.setText(it.name)
                descriptionEditText.setText(it.description)
                periodEditText.setText(it.period.toString())
                nextDueEditText.text = it.nextDueDate.format(dateFormatter)
                initialDueDate = it.initialDueDate
                notificationTime = it.notificationTime
                enableTaskNotificationSwitch.isChecked = it.notificationsEnabled
                overrideGlobalNotificationSwitch.isChecked = !isNull(notificationTime)
                notificationTimeEditText.text =
                    it.notificationTime?.format(timeFormatter)

                // Menu buttons
                topAppBar.menu.findItem(R.id.deleteButton)?.setVisible(true)
                topAppBar.setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId) {
                        R.id.deleteButton -> {
                            // Delete task button
                            MaterialAlertDialogBuilder(this)
                                .setTitle(R.string.delete_task_dialog_title)
                                .setMessage(incomingTask.name)
                                .setNegativeButton(R.string.dialog_cancel) { dialog, which ->
                                    // Respond to negative button press
                                }
                                .setPositiveButton(R.string.dialog_delete) { dialog, which ->
                                    viewModel.delete(incomingTask, baseContext)
                                    Toast.makeText(this, "Task deleted", Toast.LENGTH_SHORT)
                                        .show()
                                    // TODO: (P2) use jetpack navigation, replace activities with fragments: https://developer.android.com/guide/navigation/migrate
                                    navigateUpTo(Intent(baseContext, MainActivity::class.java))
                                }
                                .show()
                            true
                        }

                        else -> false
                    }
                }
            }
        }
        existingTask?.observe(this, taskObserver)

        // Task saving logic
        val saveTask = suspend {
            val taskName = taskNameEditText.text.toString().trim()
            val description = descriptionEditText.text.toString().trim()
            val dueDate = initialDueDate
            val period = periodEditText.text.toString().toIntOrNull() ?: 0
            val notificationsEnabled = enableTaskNotificationSwitch.isChecked

            if (notificationsEnabled) {
                val notificationPermissionGranted =
                    permissionRequester(android.Manifest.permission.POST_NOTIFICATIONS)
                if (notificationPermissionGranted) {
                    Timber.d("Notifications permissions allowed")
                } else {
                    Toast.makeText(
                        this,
                        "Task Due notification unavailable because permission is denied",
                        Toast.LENGTH_LONG
                    ).show()
                    Timber.w("User denied notifications permission prompt")
                }
            }

            if (taskName.isNotEmpty() && dueDate != null) {
                val newTask = Task(
                    id = existingId
                        ?: 0, // Insert methods treat 0 as not-set while inserting the item.
                    name = taskName,
                    description,
                    creationDate = existingTask?.value?.creationDate ?: LocalDate.now(),
                    initialDueDate = dueDate,
                    period = period,
                    notificationsEnabled = notificationsEnabled,
                    notificationLastDismissed = existingTask?.value?.notificationLastDismissed,
                    notificationTime = notificationTime,
                    // TODO: (P2) add a way to edit per-task notification period in the UI
                    notificationPeriod = existingTask?.value?.notificationPeriod,
                    lastCompleted = existingTask?.value?.lastCompleted
                )
                if (existingId == null) {
                    viewModel.insertTask(newTask, baseContext)
                } else {
                    viewModel.update(newTask, baseContext)
                }
                Toast.makeText(this, "Task saved", Toast.LENGTH_SHORT).show()
                true
            } else {
                Toast.makeText(this, "Task missing details, not saved", Toast.LENGTH_SHORT).show()
                false
            }
        }

        // Save on back button press
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                lifecycleScope.launch {
                    if (saveTask()) finish()
                }
            }
        })

        // Handle calendar picker
        initialDueEditText.setOnClickListener {
            createDatePicker(initialDueDate) { localDate ->
                initialDueEditText.text = localDate.format(dateFormatter)
                initialDueDate = localDate
            }.show(supportFragmentManager, "materialDatePicker")
        }

        // Handle time picker
        overrideGlobalNotificationSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                val baseTime = notificationTime ?: LocalTime.now().withMinute(0)
                notificationTimeEditText.text = baseTime.format(timeFormatter)
                notificationTime = baseTime
                (notificationTimeEditText.parent.parent as View).visibility = View.VISIBLE

            } else {
                notificationTime = null
                (notificationTimeEditText.parent.parent as View).visibility = View.GONE
            }
        }
        notificationTimeEditText.setOnClickListener {
            createTimePicker(baseContext, notificationTime) { localTime ->
                notificationTimeEditText.text = localTime.format(timeFormatter)
                notificationTime = localTime
            }.show(supportFragmentManager, "materialDatePicker")
        }

        addTaskButton.setOnClickListener {
            lifecycleScope.launch {
                if (saveTask()) finish()
            }
        }
        cancelButton.setOnClickListener {
            finish()  // Simply finishes the activity to return to the previous screen.
        }
    }
}
