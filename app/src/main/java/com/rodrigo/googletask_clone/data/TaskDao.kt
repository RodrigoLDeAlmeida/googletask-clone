package com.rodrigo.googletask_clone.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update

@Dao
interface TaskDao {

    @Transaction
    @Query("SELECT * FROM tasks WHERE id = :taskId")
    fun getTaskWithSubtasks(taskId: Int): LiveData<TaskWithSubtasks?>

    @Transaction
    @Query("SELECT * FROM tasks WHERE parentId IS NULL AND (:categoryId IS NULL OR categoryId = :categoryId) AND (title LIKE :searchQuery OR description LIKE :searchQuery) ORDER BY orderPosition ASC")
    fun getTasksWithSubtasks(categoryId: Int?, searchQuery: String): LiveData<List<TaskWithSubtasks>>

    @Transaction
    @Query("SELECT * FROM tasks WHERE parentId IS NULL AND (title LIKE :searchQuery OR description LIKE :searchQuery) ORDER BY orderPosition ASC")
    fun getTasksWithSubtasksBySearch(searchQuery: String): LiveData<List<TaskWithSubtasks>>

    @Transaction
    @Query("SELECT * FROM tasks WHERE parentId IS NULL AND categoryId = :categoryId ORDER BY orderPosition ASC")
    fun getTasksWithSubtasksByCategory(categoryId: Int): LiveData<List<TaskWithSubtasks>>

    @Transaction
    @Query("SELECT * FROM tasks WHERE parentId IS NULL ORDER BY orderPosition ASC")
    fun getAllTasksWithSubtasks(): LiveData<List<TaskWithSubtasks>>

    // Operação para atualizar uma lista de tarefas (usada para reordenar)
    @Update
    suspend fun updateAll(tasks: List<Task>)

    @Insert
    suspend fun insert(task: Task)

    @Update
    suspend fun update(task: Task)

    @Delete
    suspend fun delete(task: Task)
}
