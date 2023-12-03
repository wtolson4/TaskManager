package com.example.flexibletodolistapp2

import androidx.lifecycle.LiveData

class TaskRepository(private val taskDao: TaskDao) {

    val incompleteTasks: LiveData<List<Task>> = taskDao.getIncompleteTasks()

    fun insert(task: Task) {
        taskDao.insert(task)
    }

    fun update(task: Task) {
        taskDao.update(task)
    }

    fun delete(task: Task) {
        taskDao.delete(task)
    }

    fun markTaskAsCompleted(taskId: Int, isCompleted: Boolean) {
        taskDao.markTaskAsCompleted(taskId, isCompleted)
    }

    fun getTaskById(taskId: Int): Task? {
        return taskDao.getTaskById(taskId)
    }
}
