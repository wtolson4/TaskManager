package com.beyondnull.flexibletodos.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.beyondnull.flexibletodos.R
import com.beyondnull.flexibletodos.data.AppDatabase
import com.beyondnull.flexibletodos.data.CompletionDate
import com.beyondnull.flexibletodos.data.CompletionDiffCallback
import com.beyondnull.flexibletodos.data.Task
import com.beyondnull.flexibletodos.data.TaskRepository
import com.beyondnull.flexibletodos.data.TaskViewModel
import com.beyondnull.flexibletodos.data.TaskViewModelFactory
import com.beyondnull.flexibletodos.picker.createDatePicker
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle


class ViewTaskActivity : AppCompatActivity() {

    private lateinit var viewModel: TaskViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_task)

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
        val dateTimeFormatter =
            DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL, FormatStyle.SHORT)

        // Reference UI components
        val taskNameEditText = findViewById<EditText>(R.id.taskNameEditText)
        val descriptionEditText = findViewById<EditText>(R.id.descriptionEditText)
        val nextDueEditText = findViewById<TextView>(R.id.nextDueEditText)
        val nextNotificationEditText = findViewById<TextView>(R.id.nextNotificationEditText)
        val frequencyEditText = findViewById<EditText>(R.id.frequencyEditText)
        val logCompletionButton = findViewById<Button>(R.id.addCompletionButton)
        val completionsRecyclerView = findViewById<RecyclerView>(R.id.completionsRecyclerView)
        val topAppBar = findViewById<MaterialToolbar>(R.id.topAppBar)

        // Fill in data for viewing existing task
        val taskObserver = Observer<Task?> { incomingTask ->
            incomingTask?.let {
                // Fill in data
                topAppBar.title = it.definition.name
                taskNameEditText.setText(it.definition.name)
                descriptionEditText.setText(it.definition.description)
                frequencyEditText.setText(it.definition.frequency.toString())
                nextDueEditText.text = it.nextDueDate.format(dateFormatter)
                nextNotificationEditText.text =
                    it.nextNotification(baseContext).format(dateTimeFormatter)

                // Set up the completions RecyclerView
                completionsRecyclerView.layoutManager = LinearLayoutManager(this)
                val completionsAdapter = CompletionsAdapter(viewModel)  // Pass the viewModel here
                completionsRecyclerView.adapter = completionsAdapter
                // Observe changes in the data and update the RecyclerView
                completionsAdapter.submitList(incomingTask.completions)

                // Button for adding more completions
                logCompletionButton.setOnClickListener {
                    createDatePicker(null) { localDate ->
                        viewModel.insertCompletion(incomingTask, localDate, baseContext)
                    }.show(supportFragmentManager, "materialDatePicker")
                }

                // Menu buttons
                topAppBar.setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId) {
                        R.id.editButton -> {
                            // Handle edit text press
                            val intent =
                                Intent(baseContext, EditTaskActivity::class.java)
                            intent.putExtra("taskId", incomingTask.definition.id)
                            startActivity(intent)
                            true
                        }

                        else -> false
                    }
                }
            }
        }
        existingTask?.observe(this, taskObserver)

        // Set click listener for top app bar navigation button
        topAppBar.setNavigationOnClickListener {
            finish()
        }
    }
}

class CompletionsAdapter(private val viewModel: TaskViewModel) :
    ListAdapter<CompletionDate, CompletionsAdapter.CompletionsViewHolder>(CompletionDiffCallback()) {

    class CompletionsViewHolder(itemView: View, private val context: Context) :
        RecyclerView.ViewHolder(itemView) {
        private val recurrenceDateTextView: TextView =
            itemView.findViewById(R.id.recurrenceDateTextView)
        private val recurrenceTimelinessTextView: TextView =
            itemView.findViewById(R.id.recurrenceTimelinessTextView)

        fun bind(currentCompletion: CompletionDate, viewModel: TaskViewModel) {
            val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)
            recurrenceDateTextView.text = currentCompletion.date.format(dateFormatter)

            // TODO: (P1) add "timeliness" of this completion
            // TODO: (P3) also consider logging the recurrence at the time of this completion
            recurrenceTimelinessTextView.text = currentCompletion.id.toString()

            itemView.setOnClickListener {
                MaterialAlertDialogBuilder(context)
                    .setTitle(R.string.delete_occurrence_dialog_title)
                    .setMessage(currentCompletion.date.format(dateFormatter))
                    .setNegativeButton(R.string.dialog_cancel) { dialog, which ->
                        // Respond to negative button press
                    }
                    .setPositiveButton(R.string.dialog_delete) { dialog, which ->
                        viewModel.deleteCompletion(currentCompletion, context)
                    }
                    .show()
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CompletionsViewHolder {
        val view =
            LayoutInflater.from(parent.context)
                .inflate(R.layout.list_item_completion, parent, false)
        return CompletionsViewHolder(view, parent.context)
    }

    override fun onBindViewHolder(holder: CompletionsViewHolder, position: Int) {
        holder.bind(getItem(position), viewModel)
    }
}
