package com.example.flexibletodolistapp2

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: TaskViewModel
    private lateinit var taskRecyclerView: RecyclerView
    private lateinit var addTaskButton: FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize database and repository
        val dao = AppDatabase.getDatabase(this).taskDao()
        val repository = TaskRepository(dao)
        val factory = TaskViewModelFactory(repository)

        // Initialize ViewModel using the factory
        viewModel = ViewModelProvider(this, factory).get(TaskViewModel::class.java)

        // Reference UI components
        taskRecyclerView = findViewById(R.id.taskRecyclerView)
        addTaskButton = findViewById(R.id.addTaskButton)

        // Set up the RecyclerView
        taskRecyclerView.layoutManager = LinearLayoutManager(this)
        val taskAdapter = TaskAdapter(viewModel)  // Pass the viewModel here
        taskRecyclerView.adapter = taskAdapter

        // Observe changes in the data and update the RecyclerView
        viewModel.incompleteTasks.observe(this, { tasks ->
            taskAdapter.submitList(tasks)
        })

        // Set click listener for the add task button
        addTaskButton.setOnClickListener {
            val intent = Intent(this, AddTaskActivity::class.java)
            startActivity(intent)
        }
    }
}
