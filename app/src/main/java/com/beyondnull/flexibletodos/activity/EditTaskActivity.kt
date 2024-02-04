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
import com.beyondnull.flexibletodos.AppDatabase
import com.beyondnull.flexibletodos.R
import com.beyondnull.flexibletodos.Task
import com.beyondnull.flexibletodos.TaskDefinition
import com.beyondnull.flexibletodos.TaskRepository
import com.beyondnull.flexibletodos.TaskViewModel
import com.beyondnull.flexibletodos.TaskViewModelFactory
import com.beyondnull.flexibletodos.picker.createDatePicker
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle


class EditTaskActivity : AppCompatActivity() {

    private lateinit var viewModel: TaskViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_task)

        // Global app data
        val dao = AppDatabase.getDatabase(this).taskDao()
        val repository = TaskRepository(dao)
        val factory = TaskViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[TaskViewModel::class.java]

        // Get data passed into this activity
        val bundle = intent.extras
        val existingId = bundle?.getInt("taskId")
        val existingTask = existingId?.let { viewModel.getLiveTaskById(it) }

        val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)

        // Reference UI components
        val taskNameEditText = findViewById<EditText>(R.id.taskNameEditText)
        val descriptionEditText = findViewById<EditText>(R.id.descriptionEditText)
        val initialDueEditText = findViewById<TextView>(R.id.initialDueEditText)
        val nextDueEditText = findViewById<TextView>(R.id.nextDueEditText)
        var initialDueDate: LocalDate? = null
        val frequencyEditText = findViewById<EditText>(R.id.frequencyEditText)
        val addTaskButton = findViewById<Button>(R.id.saveTaskButton)
        val cancelButton = findViewById<Button>(R.id.cancelButton)
        val topAppBar = findViewById<MaterialToolbar>(R.id.topAppBar)

        // Task saving logic
        val saveTask = {
            val taskName = taskNameEditText.text.toString().trim()
            val description = descriptionEditText.text.toString().trim()
            val dueDate = initialDueDate
            val frequency = frequencyEditText.text.toString().toIntOrNull() ?: 0

            if (taskName.isNotEmpty() && dueDate != null) {
                val newTask = TaskDefinition(
                    id = existingId
                        ?: 0, // Insert methods treat 0 as not-set while inserting the item. (i.e. use
                    name = taskName,
                    description,
                    creationDate = LocalDate.now(),
                    initialDueDate = dueDate,
                    frequency = frequency,
                    notificationLastDismissed = null,
                    notificationTime = null,
                    notificationFrequency = null
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
                if (saveTask()) finish()
            }
        })

        // Fill in data for modifying existing task
        val taskObserver = Observer<Task?> { incomingTask ->
            incomingTask?.let {
                // Set visibility
                (initialDueEditText.parent.parent as View).visibility = View.GONE
                (nextDueEditText.parent.parent as View).visibility = View.VISIBLE

                // Fill in data
                taskNameEditText.setText(it.definition.name)
                descriptionEditText.setText(it.definition.description)
                frequencyEditText.setText(it.definition.frequency.toString())
                nextDueEditText.text = it.nextDueDate.format(dateFormatter)
                initialDueDate = it.definition.initialDueDate

                // Menu buttons
                topAppBar.setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId) {
                        R.id.editButton -> {
                            // Delete task button
                            MaterialAlertDialogBuilder(this)
                                .setTitle(R.string.delete_task_dialog_title)
                                .setMessage(incomingTask.definition.name)
                                .setNegativeButton(R.string.dialog_cancel) { dialog, which ->
                                    // Respond to negative button press
                                }
                                .setPositiveButton(R.string.dialog_delete) { dialog, which ->
                                    viewModel.delete(incomingTask, baseContext)
                                    Toast.makeText(this, "Task deleted", Toast.LENGTH_SHORT)
                                        .show()
                                    // TODO: use jetpack navigation, replace activities with fragments: https://developer.android.com/guide/navigation/migrate
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

        // Handle calendar picker
        initialDueEditText.setOnClickListener {
            createDatePicker(initialDueDate) { localDate ->
                initialDueEditText.text = localDate.format(dateFormatter)
                initialDueDate = localDate
            }.show(supportFragmentManager, "materialDatePicker")
        }

        addTaskButton.setOnClickListener {
            if (saveTask()) finish()
        }
        cancelButton.setOnClickListener {
            finish()  // Simply finishes the activity to return to the previous screen.
        }
    }
}