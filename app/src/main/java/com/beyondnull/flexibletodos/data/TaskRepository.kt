package com.beyondnull.flexibletodos.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Abstracted Repository as promoted by the Architecture Guide.
 * https://developer.android.com/topic/libraries/architecture/guide.html
 */
class TaskRepository(private val taskDao: TaskDao, private val externalScope: CoroutineScope) {

    val allTasks: Flow<List<Task>> = taskDao.getTasks()

    fun getTaskById(taskId: Int): Flow<Task?> {
        return taskDao.getTaskById(taskId.toLong())
    }

    fun getTaskCompletions(taskId: Int): Flow<List<CompletionDate>> {
        return taskDao.getTaskCompletions(taskId.toLong())
    }

    fun getLatestTaskCompletion(taskId: Int): Flow<CompletionDate?> {
        return taskDao.getTaskCompletions(taskId.toLong())
            .map { completion -> completion.maxByOrNull { it.date } }
    }

    // These follow the "Coroutines & Patterns for work that shouldn’t be cancelled".
    // They use an injected coroutine scope that lasts as long as the application, rather than
    // relying on the caller's scope, which may be shorter (e.g. ViewModel's, which may be tied
    // to a particular activity).
    // https://medium.com/androiddevelopers/coroutines-patterns-for-work-that-shouldnt-be-cancelled-e26c40f142ad
    suspend fun insertTask(task: Task): Task? {
        return externalScope.async {
            val id = taskDao.insertTask(task)
            taskDao.getTaskById(id).first()
        }.await()
    }

    suspend fun updateTask(task: Task) {
        externalScope.launch {
            Timber.d("Updated task ID %s", task.id)
            taskDao.updateTask(task)
        }
    }

    suspend fun deleteTask(task: Task) {
        externalScope.launch {
            Timber.d("Deleted task ID %s", task.id)
            taskDao.deleteTask(task)
            taskDao.deleteTaskCompletions(task.id.toLong())
        }
    }

    suspend fun insertCompletion(
        task: Task,
        completionDate: LocalDate,
    ) {
        externalScope.launch {
            Timber.d("Add completion for task ID %s: %s", task.id, completionDate)
            val completion = CompletionDate(
                id = 0, // Insert methods treat 0 as not-set while inserting the item. (i.e. use
                taskId = task.id,
                date = completionDate,
                note = null,
            )
            taskDao.insertCompletion(completion)
            // Update task's latest completion
            val updatedTask =
                task.copy(lastCompleted = getLatestTaskCompletion(task.id).first()?.date)
            taskDao.updateTask(updatedTask)
        }
    }

    suspend fun deleteCompletion(task: Task, completion: CompletionDate) {
        externalScope.launch {
            Timber.d("Delete completion %s", completion)
            taskDao.deleteCompletion(completion)
            // Update task's latest completion
            val updatedTask =
                task.copy(lastCompleted = getLatestTaskCompletion(task.id).first()?.date)
            taskDao.updateTask(updatedTask)
        }
    }

    fun nextNotificationTime(context: Context): Flow<LocalDateTime?> {
        // Find the next alarm time
        return allTasks
            .map { tasks ->
                tasks
                    .filter {
                        val nextNotification = it.nextNotification(context)
                        // Only tasks with notifications, which are in the future
                        nextNotification != null && nextNotification > LocalDateTime.now()
                    }
                    .minByOrNull { it.nextNotification(context)!! }
                    ?.nextNotification(context)
            }.distinctUntilChanged()
    }
}
