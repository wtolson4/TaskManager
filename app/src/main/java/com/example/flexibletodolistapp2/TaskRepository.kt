package com.example.flexibletodolistapp2

import androidx.lifecycle.LiveData

class TaskRepository(private val taskDao: TaskDao) {

    val incompleteTasks: LiveData<List<Task>> = taskDao.getIncompleteTasks()
    val allTasks: LiveData<List<Task>> = taskDao.getTasks()

    fun insert(task: TaskDefinition) {
        taskDao.insert(task)
    }

    fun update(task: TaskDefinition) {
        taskDao.update(task)
    }

//    fun delete(task: Task) {
//        taskDao.delete(task)
//    }

    fun markTaskAsCompleted(taskId: Int, isCompleted: Boolean) {
        taskDao.markTaskAsCompleted(taskId, isCompleted)
    }

    fun getTaskById(taskId: Int): Task? {
        return taskDao.getTaskById(taskId)
    }
}
