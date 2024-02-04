package com.beyondnull.flexibletodos

import androidx.lifecycle.LiveData
import timber.log.Timber
import java.time.LocalDate

/**
 * Abstracted Repository as promoted by the Architecture Guide.
 * https://developer.android.com/topic/libraries/architecture/guide.html
 */
class TaskRepository(private val taskDao: TaskDao) {

    val allTasksLive: LiveData<List<Task>> = taskDao.getTasksLive()
    fun getAllTasks(): List<Task> {
        return taskDao.getTasks()
    }

    fun getLiveTaskById(taskId: Int): LiveData<Task?> {
        return taskDao.getLiveTaskById(taskId)
    }


    fun insertTask(task: TaskDefinition): Task? {
        val id = taskDao.insertTask(task)
        return taskDao.getTaskById(id)
    }

    fun updateTask(task: TaskDefinition) {
        Timber.d("Updated task ID %s", task.id)
        taskDao.updateTask(task)
    }

    fun deleteTask(task: Task) {
        Timber.d("Deleted task ID %s", task.definition.id)
        taskDao.deleteTask(task.definition)
    }

    fun insertCompletion(taskId: Int, completionDate: LocalDate, frequencyWhenCompleted: Int) {
        Timber.d("Add completion for task ID %s: %s", taskId, completionDate)
        val completion = CompletionDate(
            id = 0, // Insert methods treat 0 as not-set while inserting the item. (i.e. use
            taskId = taskId,
            date = completionDate,
            frequencyWhenCompleted = frequencyWhenCompleted,
        )
        val id = taskDao.insertCompletion(completion)
        Timber.d("New ID %s", id)
    }

    fun deleteCompletion(completion: CompletionDate) {
        Timber.d("Delete completion %s", completion)
        taskDao.deleteCompletion(completion)
    }
}
