package com.beyondnull.flexibletodos.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDate

/**
 * Abstracted Repository as promoted by the Architecture Guide.
 * https://developer.android.com/topic/libraries/architecture/guide.html
 */
class TaskRepository(private val taskDao: TaskDao, private val externalScope: CoroutineScope) {

    val allTasks: Flow<List<Task>> = taskDao.getTasks()

    fun getTaskById(taskId: Int): Flow<Task?> {
        return taskDao.getTaskById(taskId.toLong())
    }

    // These follow the "Coroutines & Patterns for work that shouldn’t be cancelled".
    // They use an injected coroutine scope that lasts as long as the application, rather than
    // relying on the caller's scope, which may be shorter (e.g. ViewModel's, which may be tied
    // to a particular activity).
    // https://medium.com/androiddevelopers/coroutines-patterns-for-work-that-shouldnt-be-cancelled-e26c40f142ad
    suspend fun insertTask(task: TaskDefinition): Task? {
        return externalScope.async {
            val id = taskDao.insertTask(task)
            taskDao.getTaskById(id).first()
        }.await()
    }

    suspend fun updateTask(task: TaskDefinition) {
        externalScope.launch {
            Timber.d("Updated task ID %s", task.id)
            taskDao.updateTask(task)
        }
    }

    suspend fun deleteTask(task: Task) {
        externalScope.launch {
            Timber.d("Deleted task ID %s", task.definition.id)
            taskDao.deleteTask(task.definition)
        }
    }

    suspend fun insertCompletion(
        taskId: Int,
        completionDate: LocalDate,
        frequencyWhenCompleted: Int
    ) {
        externalScope.launch {
            Timber.d("Add completion for task ID %s: %s", taskId, completionDate)
            val completion = CompletionDate(
                id = 0, // Insert methods treat 0 as not-set while inserting the item. (i.e. use
                taskId = taskId,
                date = completionDate,
                frequencyWhenCompleted = frequencyWhenCompleted,
            )
            taskDao.insertCompletion(completion)
        }
    }

    suspend fun deleteCompletion(completion: CompletionDate) {
        externalScope.launch {
            Timber.d("Delete completion %s", completion)
            taskDao.deleteCompletion(completion)
        }
    }
}
