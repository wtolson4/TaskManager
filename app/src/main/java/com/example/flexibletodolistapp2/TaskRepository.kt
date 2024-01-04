package com.example.flexibletodolistapp2

import androidx.lifecycle.LiveData
import timber.log.Timber

class TaskRepository(private val taskDao: TaskDao) {

    val allTasks: LiveData<List<Task>> = taskDao.getTasks()

    fun insert(task: TaskDefinition) {
        taskDao.insert(task)
    }

    fun update(task: TaskDefinition) {
        Timber.d("Updated task ID %s", task.id);
        taskDao.update(task)
    }

//    fun delete(task: Task) {
//        taskDao.delete(task)
//    }

    fun markTaskAsCompleted(taskId: Int, isCompleted: Boolean) {
        val taskDefinition = taskDao.getTaskById(taskId)?.definition
        val updatedDefinition = taskDefinition?.copy(isCompleted = isCompleted)
        if (updatedDefinition != null) {
            taskDao.update(updatedDefinition)
        } else {
            Timber.e("Failed to mark completed taskID %s", taskId);
        }
    }

    fun getTaskById(taskId: Int): Task? {
        return taskDao.getTaskById(taskId)
    }
}
