package com.example.flexibletodolistapp2

import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: TaskViewModel
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
        taskRecyclerView = findViewById(R.id.taskRecyclerView)
        addTaskButton = findViewById(R.id.addTaskButton)

        // Set up the RecyclerView
        taskRecyclerView.layoutManager = LinearLayoutManager(this)
        val taskAdapter = TaskAdapter(viewModel)  // Pass the viewModel here
        taskRecyclerView.adapter = taskAdapter

        // Add margins to handle the edge-to-edge
        // https://developer.android.com/develop/ui/views/layout/edge-to-edge
        ViewCompat.setOnApplyWindowInsetsListener(taskRecyclerView) { view, windowInsets ->
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
    }
}
