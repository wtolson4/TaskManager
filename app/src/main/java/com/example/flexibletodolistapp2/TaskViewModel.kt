package com.example.flexibletodolistapp2

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class TaskViewModel(private val repository: TaskRepository) : ViewModel() {

    val incompleteTasks: LiveData<List<Task>> = repository.incompleteTasks

    fun insert(task: Task) = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            repository.insert(task)
        }
    }

    fun update(task: Task) = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            repository.update(task)
        }
    }

    fun delete(task: Task) = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            repository.delete(task)
        }
    }

    fun markAsCompleted(taskId: Int) = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            val completedTask = repository.getTaskById(taskId)
            completedTask?.let {
                repository.markTaskAsCompleted(taskId, true)

                if (it.recurrenceType.isNotEmpty()) {
                    val completionCalendar = Calendar.getInstance()
                    when (it.recurrenceType) {
                        "DAILY" -> completionCalendar.add(Calendar.DAY_OF_YEAR, it.frequency)
                        "WEEKLY" -> completionCalendar.add(Calendar.WEEK_OF_YEAR, it.frequency)
                        "BIWEEKLY" -> completionCalendar.add(Calendar.WEEK_OF_YEAR, 2 * it.frequency)
                        "MONTHLY" -> completionCalendar.add(Calendar.MONTH, it.frequency)
                        "BIYEARLY" -> completionCalendar.add(Calendar.MONTH, 6 * it.frequency)
                    }

                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val newTask = Task(
                        id = 0,
                        taskName = completedTask.taskName,
                        dueDate = sdf.format(completionCalendar.time),
                        frequency = completedTask.frequency,
                        recurrenceType = completedTask.recurrenceType
                    )
                    repository.insert(newTask)
                }
            }
        }
    }
}
