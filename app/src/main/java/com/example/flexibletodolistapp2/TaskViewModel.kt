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
            }
        }
    }
}