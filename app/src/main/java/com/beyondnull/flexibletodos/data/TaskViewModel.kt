package com.beyondnull.flexibletodos.data

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.DiffUtil
import com.beyondnull.flexibletodos.AppNotificationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * View Model to keep a reference to the task repository and an up-to-date list of tasks.
 */

class TaskViewModel(private val repository: TaskRepository) : ViewModel() {
    // Using LiveData and caching what allTasks returns has several benefits:
    // - We can put an observer on the data (instead of polling for changes) and only update the
    //   the UI when the data actually changes.
    // - Repository is completely separated from the UI through the ViewModel.
    private val _allTasks: LiveData<List<Task>> = repository.allTasksLive
    private fun sortCompletionsByDate(t: Task): Task {
        val newCompletions = t.completions.sortedBy { it.date }
        return t.copy(completions = newCompletions)
    }

    val allTasks: LiveData<List<Task>> =
        this._allTasks.map { tasks ->
            val withSortedCompletions = tasks.map { sortCompletionsByDate(it) }
            withSortedCompletions.sortedBy { it.daysUntilDue }
        }

    fun getLiveTaskById(taskId: Int): LiveData<Task?> {
        return repository.getLiveTaskById(taskId).map { it?.let { sortCompletionsByDate(it) } }
    }

    /**
     * Launching a new coroutine to insert the data in a non-blocking way
     */
    fun insertTask(task: TaskDefinition, context: Context) = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            repository.insertTask(task)
            AppNotificationManager().updateNotificationsAndAlarms(context)
        }
    }

    fun update(task: TaskDefinition, context: Context) = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            repository.updateTask(task)
            AppNotificationManager().updateNotificationsAndAlarms(context)
        }
    }

    fun delete(task: Task, context: Context) = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            repository.deleteTask(task)
            AppNotificationManager().updateNotificationsAndAlarms(context)
        }
    }

    fun insertCompletion(task: Task, date: LocalDate, context: Context) =
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.insertCompletion(
                    taskId = task.definition.id,
                    completionDate = date,
                    frequencyWhenCompleted = task.definition.frequency
                )
                AppNotificationManager().updateNotificationsAndAlarms(context)
            }
        }

    fun deleteCompletion(completion: CompletionDate, context: Context) = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            repository.deleteCompletion(completion)
            AppNotificationManager().updateNotificationsAndAlarms(context)
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