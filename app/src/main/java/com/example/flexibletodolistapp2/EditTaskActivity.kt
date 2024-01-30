package com.example.flexibletodolistapp2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale


class EditTaskActivity : AppCompatActivity() {

    private lateinit var viewModel: TaskViewModel

    private fun promptForDate(initializer: LocalDate?, callback: (LocalDate) -> Unit) {
        val initialDate = initializer?.let {
            it.atStartOfDay(
                ZoneOffset.UTC
            )
                .toInstant()
                .toEpochMilli()
        } ?: MaterialDatePicker.todayInUtcMilliseconds()
        val datePicker =
            MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.select_date)
                .setSelection(initialDate)
                .build()
        datePicker.addOnPositiveButtonClickListener {
            val localDate = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
            callback(localDate)
        }
        datePicker.show(supportFragmentManager, "materialDatePicker")
    }

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

        val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(
            Locale.US // TODO: use device locale
        )

        // Reference UI components
        val taskNameEditText = findViewById<EditText>(R.id.taskNameEditText)
        val descriptionEditText = findViewById<EditText>(R.id.descriptionEditText)
        val initialDueEditText = findViewById<TextView>(R.id.initialDueEditText)
        val nextDueEditText = findViewById<TextView>(R.id.nextDueEditText)
        var initialDueDate: LocalDate? = null
        val frequencyEditText = findViewById<EditText>(R.id.frequencyEditText)
        val logCompletionButton = findViewById<Button>(R.id.addCompletionButton)
        val completionsRecyclerView = findViewById<RecyclerView>(R.id.completionsRecyclerView)
        val addTaskButton = findViewById<Button>(R.id.saveTaskButton)
        val cancelButton = findViewById<Button>(R.id.cancelButton)

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
                )
                if (existingId == null) {
                    viewModel.insertTask(newTask)
                } else {
                    viewModel.update(newTask)
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
                (completionsRecyclerView.parent as View).visibility = View.VISIBLE

                // Fill in data
                taskNameEditText.setText(it.definition.name)
                descriptionEditText.setText(it.definition.description)
                frequencyEditText.setText(it.definition.frequency.toString())
                nextDueEditText.text = it.nextDueDate.format(dateFormatter)
                initialDueDate = it.definition.initialDueDate

                // Set up the completions RecyclerView
                completionsRecyclerView.layoutManager = LinearLayoutManager(this)
                val completionsAdapter = CompletionsAdapter(viewModel)  // Pass the viewModel here
                completionsRecyclerView.adapter = completionsAdapter
                // Observe changes in the data and update the RecyclerView
                completionsAdapter.submitList(incomingTask.completions)

                // Button for adding more completions
                logCompletionButton.setOnClickListener {
                    promptForDate(null) { localDate ->
                        viewModel.insertCompletion(incomingTask, localDate)
                    }
                }
            }
        }
        existingTask?.observe(this, taskObserver)

        // Handle calendar picker
        initialDueEditText.setOnClickListener {
            promptForDate(initialDueDate) { localDate ->
                initialDueEditText.text = localDate.format(dateFormatter)
                initialDueDate = localDate
            }
        }

        addTaskButton.setOnClickListener {
            if (saveTask()) finish()
        }
        cancelButton.setOnClickListener {
            finish()  // Simply finishes the activity to return to the previous screen.
        }


    }
}

class CompletionsAdapter(private val viewModel: TaskViewModel) :
    ListAdapter<CompletionDate, CompletionsAdapter.CompletionsViewHolder>(CompletionDiffCallback()) {

    class CompletionsViewHolder(itemView: View, private val parent: ViewGroup) :
        RecyclerView.ViewHolder(itemView) {
        private val recurrenceDateTextView: TextView =
            itemView.findViewById(R.id.recurrenceDateTextView)
        private val recurrenceTimelinessTextView: TextView =
            itemView.findViewById(R.id.recurrenceTimelinessTextView)

        fun bind(currentCompletion: CompletionDate, viewModel: TaskViewModel) {
            val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(
                // TODO: use correct locale
                Locale.US
            )
            recurrenceDateTextView.text = currentCompletion.date.format(dateFormatter)

            // TODO: add "timeliness" of this completion
            // TODO: also consider logging the recurrence at the time of this completion
            recurrenceTimelinessTextView.text = currentCompletion.id.toString()

            itemView.setOnClickListener {
                MaterialAlertDialogBuilder(parent.context)
                    .setTitle(R.string.delete_occurence_dialog_title)
                    .setMessage(currentCompletion.date.format(dateFormatter))
                    .setNegativeButton(R.string.delete_occurence_dialog_decline) { dialog, which ->
                        // Respond to negative button press
                    }
                    .setPositiveButton(R.string.delete_occurence_dialog_accept) { dialog, which ->
                        viewModel.deleteCompletion(currentCompletion)
                    }
                    .show()
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CompletionsViewHolder {
        val view =
            LayoutInflater.from(parent.context)
                .inflate(R.layout.list_item_completion, parent, false)
        return CompletionsViewHolder(view, parent)
    }

    override fun onBindViewHolder(holder: CompletionsViewHolder, position: Int) {
        holder.bind(getItem(position), viewModel)
    }
}
