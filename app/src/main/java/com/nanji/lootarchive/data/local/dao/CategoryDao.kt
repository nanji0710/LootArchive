package com.nanji.lootarchive.data.local.dao

import androidx.room.*
import com.nanji.lootarchive.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

/** 分类 id → 未删除物品数 结果行 */
data class CategoryItemCountRow(val categoryId: Long, val count: Int)

@Dao
interface CategoryDao {

    @Query("SELECT * FROM categories ORDER BY sortOrder ASC, name ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategoryById(id: Long): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Delete
    suspend fun deleteCategory(category: CategoryEntity)

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun getCount(): Int

    /** 批量取各分类未删除物品数（消除逐分类 N+1 查询） */
    @Query(
        "SELECT i.categoryId AS categoryId, COUNT(*) AS count FROM items i " +
            "WHERE i.isDeleted = 0 GROUP BY i.categoryId"
    )
    suspend fun getItemCountByCategory(): List<CategoryItemCountRow>
}
