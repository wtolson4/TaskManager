package com.beyondnull.flexibletodos.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.beyondnull.flexibletodos.AppDatabase
import com.beyondnull.flexibletodos.R
import com.beyondnull.flexibletodos.Task
import com.beyondnull.flexibletodos.TaskDiffCallback
import com.beyondnull.flexibletodos.TaskRepository
import com.beyondnull.flexibletodos.TaskViewModel
import com.beyondnull.flexibletodos.TaskViewModelFactory
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: TaskViewModel
    private lateinit var topAppBarView: MaterialToolbar
    private lateinit var taskRecyclerView: RecyclerView
    private lateinit var addTaskButton: FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize database and repository
        val dao = AppDatabase.getDatabase(this).taskDao()
        val repository = TaskRepository(dao)
        val factory = TaskViewModelFactory(repository)

        // Initialize ViewModel using the factory
        viewModel = ViewModelProvider(this, factory)[TaskViewModel::class.java]

        // Reference UI components
        topAppBarView = findViewById(R.id.topAppBar)
        taskRecyclerView = findViewById(R.id.taskRecyclerView)
        addTaskButton = findViewById(R.id.addTaskButton)

        // Set up the RecyclerView
        taskRecyclerView.layoutManager = LinearLayoutManager(this)
        val taskAdapter = TaskAdapter(viewModel)  // Pass the viewModel here
        taskRecyclerView.adapter = taskAdapter

        // Add margins to handle the edge-to-edge
        // https://developer.android.com/develop/ui/views/layout/edge-to-edge
        ViewCompat.setOnApplyWindowInsetsListener(topAppBarView) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Apply the insets as a margin to the view.
            view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                this.topMargin = insets.top
            }

            // Return CONSUMED if you don't want want the window insets to keep passing
            // down to descendant views.
            WindowInsetsCompat.CONSUMED
        }
        ViewCompat.setOnApplyWindowInsetsListener(addTaskButton) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                this.bottomMargin = insets.bottom + 32
                this.rightMargin = insets.right + 32
            }
            // Return CONSUMED if you don't want want the window insets to keep passing
            // down to descendant views.
            WindowInsetsCompat.CONSUMED
        }

        // Observe changes in the data and update the RecyclerView
        viewModel.allTasks.observe(this) { tasks ->
            taskAdapter.submitList(tasks)
        }

        // Set click listener for the add task button
        addTaskButton.setOnClickListener {
            val intent = Intent(this, EditTaskActivity::class.java)
            startActivity(intent)
        }

        // Set click listener for top app bar navigation button
        topAppBarView.setNavigationOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
    }
}

class TaskAdapter(private val viewModel: TaskViewModel) :
    ListAdapter<Task, TaskAdapter.TaskViewHolder>(TaskDiffCallback()) {

    class TaskViewHolder(itemView: View, private val context: Context) :
        RecyclerView.ViewHolder(itemView) {
        private val taskNameTextView: TextView = itemView.findViewById(R.id.taskNameTextView)
        private val dueDateTextView: TextView = itemView.findViewById(R.id.dueDateTextView)

        fun bind(currentTask: Task, viewModel: TaskViewModel) {
            taskNameTextView.text = currentTask.definition.name
            val dueDays = currentTask.daysUntilDue
            dueDateTextView.text = when {
                (dueDays < -1) -> String.format(
                    context.getString(R.string.due_n_days_ago),
                    -dueDays
                )

                (dueDays == -1) -> context.getString(R.string.due_one_day_ago)
                (dueDays == 0) -> context.getString(R.string.due_today)
                (dueDays == 1) -> context.getString(R.string.due_tomorrow)
                else -> String.format(
                    context.getString(R.string.due_in_n_days),
                    dueDays
                )
            }

            itemView.setOnClickListener {
                val intent = Intent(itemView.context, ViewTaskActivity::class.java)
                intent.putExtra("taskId", currentTask.definition.id)
                itemView.context.startActivity(intent)
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.list_item_task, parent, false)
        return TaskViewHolder(view, parent.context)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(getItem(position), viewModel)
    }
}