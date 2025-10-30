package com.rodrigo.googletask_clone.data

import androidx.lifecycle.LiveData

class CategoryRepository(private val categoryDao: CategoryDao) {

    val allCategories: LiveData<List<Category>> = categoryDao.getAllCategories()

    suspend fun insert(category: Category) {
        categoryDao.insert(category)
    }
}
