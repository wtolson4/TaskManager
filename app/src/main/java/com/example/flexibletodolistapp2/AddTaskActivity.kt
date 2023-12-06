package com.example.flexibletodolistapp2

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Calendar
import java.util.Locale


class AddTaskActivity : AppCompatActivity() {

    private lateinit var viewModel: TaskViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_task)

        val dao = AppDatabase.getDatabase(this).taskDao()
        val repository = TaskRepository(dao)
        val factory = TaskViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory).get(TaskViewModel::class.java)

        val taskNameEditText = findViewById<EditText>(R.id.taskNameEditText)
        val selectedDateTextView = findViewById<TextView>(R.id.selectedDateTextView)
        var selectedDate: LocalDate? = null
        val pickDateButton = findViewById<Button>(R.id.pickDateButton)
        val addTaskButton = findViewById<Button>(R.id.addTaskButton)
        val cancelButton = findViewById<Button>(R.id.cancelButton)
        val recurrenceTypeSpinner = findViewById<Spinner>(R.id.recurrenceTypeSpinner)
        val frequencyEditText = findViewById<EditText>(R.id.frequencyEditText)

        pickDateButton.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar[Calendar.YEAR]
            val month = calendar[Calendar.MONTH]
            val day = calendar[Calendar.DAY_OF_MONTH]

            val datePickerDialog = DatePickerDialog(this, { _, chosenYear, chosenMonth, chosenDay ->
                val localDate = LocalDate.of(chosenYear, chosenMonth, chosenDay)
                val DATE_FORMATTER = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(
                    Locale.US
                )
                selectedDateTextView.text = localDate.format(DATE_FORMATTER)
                selectedDate = localDate
            }, year, month, day)

            datePickerDialog.show()
        }

        addTaskButton.setOnClickListener {
            val taskName = taskNameEditText.text.toString().trim()
            val dueDate = selectedDate
            val recurrenceType = if (frequencyEditText.text.toString().toIntOrNull() == 1) {
                recurrenceTypeSpinner.selectedItem.toString()
            } else {
                "${frequencyEditText.text} ${recurrenceTypeSpinner.selectedItem}"
            }
            val frequency = frequencyEditText.text.toString().toIntOrNull() ?: 0

            if (taskName.isNotEmpty() && dueDate != null) {
                val newTask = TaskDefinition(
                    id = 0,
                    taskName = taskName,
                    dueDate = dueDate,
                    frequency = frequency,
                    recurrenceType = recurrenceType,
                    isCompleted = false
                )
                viewModel.insert(newTask)
                finish()
            } else {
                Toast.makeText(this, "Please enter all details correctly", Toast.LENGTH_SHORT).show()
            }
        }

        cancelButton.setOnClickListener {
            finish()  // Simply finishes the activity to return to the previous screen.
        }
    }
}