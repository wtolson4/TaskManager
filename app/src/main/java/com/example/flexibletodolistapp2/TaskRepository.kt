package com.example.flexibletodolistapp2

import androidx.lifecycle.LiveData
import timber.log.Timber
import java.time.LocalDate

/**
 * Abstracted Repository as promoted by the Architecture Guide.
 * https://developer.android.com/topic/libraries/architecture/guide.html
 */
class TaskRepository(private val taskDao: TaskDao) {

    val allTasks: LiveData<List<Task>> = taskDao.getTasks()

    fun insertTask(task: TaskDefinition) {
        taskDao.insertTask(task)
    }

    fun updateTask(task: TaskDefinition) {
        Timber.d("Updated task ID %s", task.id)
        taskDao.updateTask(task)
    }

    fun deleteTask(task: Task) {
        taskDao.deleteTask(task.definition)
    }

    fun insertCompletion(taskId: Int, completionDate: LocalDate) {
        val completion = CompletionDate(
            id = 0, // Insert methods treat 0 as not-set while inserting the item. (i.e. use
            taskId = taskId,
            date = completionDate
        )
        taskDao.insertCompletion(completion)
    }

    fun deleteCompletion(completion: CompletionDate) {
        taskDao.deleteCompletion(completion)
    }

    fun markTaskAsCompleted(taskId: Int, isCompleted: Boolean) {
        val taskDefinition = taskDao.getTaskById(taskId)?.definition
        val updatedDefinition = taskDefinition?.copy(isCompleted = isCompleted)
        if (updatedDefinition != null) {
            taskDao.updateTask(updatedDefinition)
        } else {
            Timber.e("Failed to mark completed taskID %s", taskId)
        }
    }

    fun getTaskById(taskId: Int): Task? {
        return taskDao.getTaskById(taskId)
    }
}
