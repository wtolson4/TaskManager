package com.example.flexibletodolistapp2

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.DiffUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * View Model to keep a reference to the task repository and
 * an up-to-date list of all words.
 */

class TaskViewModel(private val repository: TaskRepository) : ViewModel() {
    // Using LiveData and caching what allTasks returns has several benefits:
    // - We can put an observer on the data (instead of polling for changes) and only update the
    //   the UI when the data actually changes.
    // - Repository is completely separated from the UI through the ViewModel.
    val allTasks: LiveData<List<Task>> = repository.allTasks
    fun getLiveTaskById(taskId: Int): LiveData<Task?> {
        return repository.getLiveTaskById(taskId)
    }

    fun getLiveCompletionsByTaskId(taskId: Int): LiveData<List<CompletionDate>> {
        return repository.getLiveCompletionsByTaskId(taskId)
    }

    /**
     * Launching a new coroutine to insert the data in a non-blocking way
     */
    fun insertTask(task: TaskDefinition) = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            repository.insertTask(task)
        }
    }

    fun update(task: TaskDefinition) = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            repository.updateTask(task)
        }
    }

    fun delete(task: Task) = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            repository.deleteTask(task)
        }
    }

    fun insertCompletion(task: Task, date: LocalDate) = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            repository.insertCompletion(task.definition.id, date)
        }
    }
}

class TaskViewModelFactory(private val repository: TaskRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TaskViewModel::class.java)) {
            return TaskViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class TaskDiffCallback : DiffUtil.ItemCallback<Task>() {

    override fun areItemsTheSame(oldItem: Task, newItem: Task): Boolean {
        return oldItem.definition.id == newItem.definition.id
    }

    override fun areContentsTheSame(oldItem: Task, newItem: Task): Boolean {
        return oldItem == newItem
    }
}

class CompletionDiffCallback : DiffUtil.ItemCallback<CompletionDate>() {

    override fun areItemsTheSame(oldItem: CompletionDate, newItem: CompletionDate): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: CompletionDate, newItem: CompletionDate): Boolean {
        return oldItem == newItem
    }
}