package com.example.flexibletodolistapp2

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
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
        viewModel = ViewModelProvider(this, factory)[TaskViewModel::class.java]

        val bundle = intent.extras
        val existingId = bundle?.getInt("taskId")
        val existingTask = existingId?.let { viewModel.getLiveTaskById(it) }

        val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(
            Locale.US // TODO: use device locale
        )

        val taskNameEditText = findViewById<EditText>(R.id.taskNameEditText)
        val descriptionEditText = findViewById<EditText>(R.id.descriptionEditText)
        val selectedDateTextView = findViewById<TextView>(R.id.nextDueEditText)
        var selectedDate: LocalDate? = null
        val frequencyEditText = findViewById<EditText>(R.id.frequencyEditText)
        val addTaskButton = findViewById<Button>(R.id.addTaskButton)
        val cancelButton = findViewById<Button>(R.id.cancelButton)

        val taskObserver = Observer<Task?> { newTask ->
            newTask?.let {
                taskNameEditText.setText(it.definition.name)
                descriptionEditText.setText(it.definition.description)
                frequencyEditText.setText(it.definition.frequency.toString())
                selectedDateTextView.text = it.definition.initialDueDate.format(dateFormatter)
                selectedDate = it.definition.initialDueDate
            }
        }
        existingTask?.observe(this, taskObserver)

        selectedDateTextView.setOnClickListener {
            val calendar = Calendar.getInstance()
            val todayYear = calendar[Calendar.YEAR]
            val todayMonth = calendar[Calendar.MONTH]
            val todayDay = calendar[Calendar.DAY_OF_MONTH]

            val datePickerDialog = DatePickerDialog(this, { _, chosenYear, chosenMonth, chosenDay ->
                val month1Index = chosenMonth + 1 // chosenMonth is 0-indexed
                val localDate = LocalDate.of(chosenYear, month1Index, chosenDay)
                selectedDateTextView.text = localDate.format(dateFormatter)
                selectedDate = localDate
            }, todayYear, todayMonth, todayDay)

            datePickerDialog.show()
        }

        addTaskButton.setOnClickListener {
            val taskName = taskNameEditText.text.toString().trim()
            val description = descriptionEditText.text.toString().trim()
            val dueDate = selectedDate
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
                )
                if (existingId == null) {
                    viewModel.insertTask(newTask)
                } else {
                    viewModel.update(newTask)
                }
                finish()
            } else {
                Toast.makeText(this, "Please enter all details", Toast.LENGTH_SHORT).show()
            }
        }

        cancelButton.setOnClickListener {
            finish()  // Simply finishes the activity to return to the previous screen.
        }
    }
}