package com.rodrigo.googletask_clone.data

import androidx.lifecycle.LiveData

class TaskRepository(private val taskDao: TaskDao, private val categoryDao: CategoryDao) {

    fun getTasks(categoryId: Int?, searchQuery: String): LiveData<List<TaskWithSubtasks>> {
        val formattedQuery = "%${searchQuery}%"
        return when {
            categoryId != null && searchQuery.isNotBlank() -> taskDao.getTasksWithSubtasks(categoryId, formattedQuery)
            categoryId != null -> taskDao.getTasksWithSubtasksByCategory(categoryId)
            searchQuery.isNotBlank() -> taskDao.getTasksWithSubtasksBySearch(formattedQuery)
            else -> taskDao.getAllTasksWithSubtasks()
        }
    }

    fun getTaskWithSubtasks(taskId: Int): LiveData<TaskWithSubtasks?> {
        return taskDao.getTaskWithSubtasks(taskId)
    }

    val allCategories: LiveData<List<Category>> = categoryDao.getAllCategories()

    suspend fun insert(task: Task) {
        taskDao.insert(task)
    }

    suspend fun update(task: Task) {
        taskDao.update(task)
    }

    // Atualiza uma lista de tarefas. Perfeito para reordenar.
    suspend fun updateAll(tasks: List<Task>) {
        taskDao.updateAll(tasks)
    }

    suspend fun delete(task: Task) {
        taskDao.delete(task)
    }

    suspend fun insert(category: Category) {
        categoryDao.insert(category)
    }
}
