package com.nanji.lootarchive.data.local.dao

import androidx.room.*
import com.nanji.lootarchive.data.local.entity.ItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {

    // ========== 查询 ==========

    @Query("SELECT * FROM items WHERE isDeleted = 0 ORDER BY updatedAt DESC")
    fun getAllItems(): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE id = :id")
    suspend fun getItemById(id: Long): ItemEntity?

    @Query("SELECT * FROM items WHERE isDeleted = 0 AND id = :id")
    fun getItemByIdFlow(id: Long): Flow<ItemEntity?>

    @Query("SELECT * FROM items WHERE isDeleted = 0 AND categoryId = :categoryId ORDER BY updatedAt DESC")
    fun getItemsByCategory(categoryId: Long): Flow<List<ItemEntity>>

    // ========== 搜索 ==========

    @Query("SELECT * FROM items WHERE isDeleted = 0 AND (name LIKE '%' || :keyword || '%' OR storageLocation LIKE '%' || :keyword || '%' OR description LIKE '%' || :keyword || '%') ORDER BY updatedAt DESC")
    fun searchItems(keyword: String): Flow<List<ItemEntity>>

    // ========== 筛选 ==========

    @Query("""
        SELECT * FROM items WHERE isDeleted = 0
        AND (:categoryId IS NULL OR categoryId = :categoryId)
        AND (:startDate IS NULL OR purchaseDate >= :startDate)
        AND (:endDate IS NULL OR purchaseDate <= :endDate)
        ORDER BY updatedAt DESC
    """)
    fun filterItems(
        categoryId: Long? = null,
        startDate: Long? = null,
        endDate: Long? = null
    ): Flow<List<ItemEntity>>

    // ========== 保修提醒 ==========

    @Query("SELECT * FROM items WHERE isDeleted = 0 AND warrantyExpiryDate IS NOT NULL AND warrantyExpiryDate <= :threshold ORDER BY warrantyExpiryDate ASC")
    fun getWarrantyExpiringItems(threshold: Long): Flow<List<ItemEntity>>

    @Query("SELECT COUNT(*) FROM items WHERE isDeleted = 0 AND warrantyExpiryDate IS NOT NULL AND warrantyExpiryDate <= :threshold")
    fun getWarrantyExpiringCount(threshold: Long): Flow<Int>

    // ========== 统计 ==========

    @Query("SELECT COUNT(*) FROM items WHERE isDeleted = 0")
    fun getTotalCount(): Flow<Int>

    @Query("SELECT COALESCE(SUM(purchasePrice), 0) FROM items WHERE isDeleted = 0")
    fun getTotalValue(): Flow<Double>

    @Query("SELECT COALESCE(SUM(purchasePrice), 0) FROM items WHERE isDeleted = 0 AND categoryId = :categoryId")
    suspend fun getCategoryTotalValue(categoryId: Long): Double

    @Query("SELECT COUNT(*) FROM items WHERE isDeleted = 0 AND categoryId = :categoryId")
    fun getCategoryItemCount(categoryId: Long): Flow<Int>

    // ========== 回收站 ==========

    @Query("SELECT * FROM items WHERE isDeleted = 1 ORDER BY deletedAt DESC")
    fun getDeletedItems(): Flow<List<ItemEntity>>

    // ========== 写入 ==========

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ItemEntity): Long

    @Update
    suspend fun updateItem(item: ItemEntity)

    @Query("UPDATE items SET isDeleted = 1, deletedAt = :deletedAt WHERE id = :itemId")
    suspend fun softDeleteItem(itemId: Long, deletedAt: Long = System.currentTimeMillis())

    @Query("UPDATE items SET isDeleted = 0, deletedAt = NULL WHERE id = :itemId")
    suspend fun restoreItem(itemId: Long)

    @Query("DELETE FROM items WHERE id = :itemId")
    suspend fun hardDeleteItem(itemId: Long)

    @Query("DELETE FROM items WHERE isDeleted = 1")
    suspend fun emptyTrash()
}
