package com.nanji.lootarchive.data.repository

import com.nanji.lootarchive.data.local.dao.CategoryDao
import com.nanji.lootarchive.data.local.dao.ItemDao
import com.nanji.lootarchive.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepository @Inject constructor(
    private val categoryDao: CategoryDao,
    private val itemDao: ItemDao
) {
    fun getAllCategories(): Flow<List<CategoryEntity>> = categoryDao.getAllCategories()

    suspend fun getCategoryById(id: Long): CategoryEntity? = categoryDao.getCategoryById(id)

    suspend fun createCategory(name: String, iconName: String = "category"): Long {
        val count = categoryDao.getCount()
        return categoryDao.insertCategory(
            CategoryEntity(name = name, iconName = iconName, sortOrder = count)
        )
    }

    suspend fun updateCategory(category: CategoryEntity) = categoryDao.updateCategory(category)

    suspend fun deleteCategory(category: CategoryEntity) {
        // 将该分类下的物品移到"其他"分类（id=0 表示默认其他分类）
        // 实际实现中需要先检查或创建"其他"分类
        categoryDao.deleteCategory(category)
    }
}
