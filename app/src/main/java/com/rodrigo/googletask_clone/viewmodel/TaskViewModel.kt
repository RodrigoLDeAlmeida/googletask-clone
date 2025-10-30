package com.rodrigo.googletask_clone.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.rodrigo.googletask_clone.data.AppDatabase
import com.rodrigo.googletask_clone.data.Category
import com.rodrigo.googletask_clone.data.Task
import com.rodrigo.googletask_clone.data.TaskRepository
import com.rodrigo.googletask_clone.data.TaskWithSubtasks
import kotlinx.coroutines.launch

class TaskViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TaskRepository
    val allCategories: LiveData<List<Category>>

    private val searchQuery = MutableLiveData("")
    private val _selectedCategoryId = MutableLiveData<Int?>()
    val selectedCategoryId: LiveData<Int?> = _selectedCategoryId

    private val _allFilteredTasks = MediatorLiveData<List<TaskWithSubtasks>>()
    
    val pendingTasks: LiveData<List<TaskWithSubtasks>>
    val completedTasks: LiveData<List<TaskWithSubtasks>>

    private var currentTaskSource: LiveData<List<TaskWithSubtasks>>? = null

    init {
        val database = AppDatabase.getDatabase(application)
        repository = TaskRepository(database.taskDao(), database.categoryDao())
        allCategories = repository.allCategories

        _allFilteredTasks.addSource(searchQuery) { filterTasks(_selectedCategoryId.value, it) }
        _allFilteredTasks.addSource(_selectedCategoryId) { filterTasks(it, searchQuery.value) }

        pendingTasks = _allFilteredTasks.map { tasks -> tasks.filter { !it.task.isCompleted } }
        completedTasks = _allFilteredTasks.map { tasks -> tasks.filter { it.task.isCompleted } }
    }

    private fun filterTasks(categoryId: Int?, query: String?) {
        currentTaskSource?.let { _allFilteredTasks.removeSource(it) }
        val newSource = repository.getTasks(categoryId, query ?: "")
        _allFilteredTasks.addSource(newSource) { _allFilteredTasks.value = it }
        currentTaskSource = newSource
    }

    fun getTaskWithSubtasks(taskId: Int): LiveData<TaskWithSubtasks?> {
        return repository.getTaskWithSubtasks(taskId)
    }

    fun selectCategory(categoryId: Int?) {
        _selectedCategoryId.value = categoryId
    }

    // ... (resto do ViewModel não muda) ...
    fun setSearchQuery(query: String) { searchQuery.value = query }
    fun updateTaskOrder(tasks: List<Task>) = viewModelScope.launch { repository.updateAll(tasks) }
    fun insert(task: Task) = viewModelScope.launch { repository.insert(task) }
    fun update(task: Task) = viewModelScope.launch { repository.update(task) }
    fun delete(task: Task) = viewModelScope.launch { repository.delete(task) }
    fun insert(category: Category) = viewModelScope.launch { repository.insert(category) }
}
