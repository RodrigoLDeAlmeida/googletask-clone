package com.rodrigo.googletask_clone.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.rodrigo.googletask_clone.data.AppDatabase
import com.rodrigo.googletask_clone.data.Category
import com.rodrigo.googletask_clone.data.CategoryRepository
import kotlinx.coroutines.launch

class CategoryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: CategoryRepository
    val allCategories: LiveData<List<Category>>

    init {
        val categoryDao = AppDatabase.getDatabase(application).categoryDao()
        repository = CategoryRepository(categoryDao)
        allCategories = repository.allCategories
    }

    fun insert(category: Category) = viewModelScope.launch {
        repository.insert(category)
    }
}
