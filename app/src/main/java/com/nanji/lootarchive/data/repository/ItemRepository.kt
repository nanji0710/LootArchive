package com.nanji.lootarchive.data.repository

import com.nanji.lootarchive.data.local.dao.CategoryDao
import com.nanji.lootarchive.data.local.dao.ItemDao
import com.nanji.lootarchive.data.local.dao.ItemPhotoDao
import com.nanji.lootarchive.data.local.entity.ItemEntity
import com.nanji.lootarchive.data.local.entity.ItemPhotoEntity
import com.nanji.lootarchive.domain.model.ItemWithPhotos
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ItemRepository @Inject constructor(
    private val itemDao: ItemDao,
    private val categoryDao: CategoryDao,
    private val itemPhotoDao: ItemPhotoDao
) {
    // ========== 物品列表 ==========

    fun getAllItems(): Flow<List<ItemEntity>> = itemDao.getAllItems()

    suspend fun getItemById(id: Long): ItemEntity? = itemDao.getItemById(id)

    fun getItemByIdFlow(id: Long): Flow<ItemEntity?> = itemDao.getItemByIdFlow(id)

    fun getItemsByCategory(categoryId: Long): Flow<List<ItemEntity>> =
        itemDao.getItemsByCategory(categoryId)

    // ========== 聚合查询 ==========

    fun getItemsWithPhotos(): Flow<List<ItemWithPhotos>> {
        return itemDao.getAllItems().combine(categoryDao.getAllCategories()) { items, categories ->
            items.map { item ->
                val category = categories.find { it.id == item.categoryId }
                // photos will be loaded lazily per item when needed
                ItemWithPhotos(item = item, category = category, photos = emptyList())
            }
        }
    }

    suspend fun getItemWithPhotos(itemId: Long): ItemWithPhotos? {
        val item = itemDao.getItemById(itemId) ?: return null
        val category = categoryDao.getCategoryById(item.categoryId)
        val photos = itemPhotoDao.getPhotosByItemIdOnce(itemId)
        return ItemWithPhotos(item = item, category = category, photos = photos)
    }

    fun getItemPhotos(itemId: Long): Flow<List<ItemPhotoEntity>> =
        itemPhotoDao.getPhotosByItemId(itemId)

    // ========== 搜索和筛选 ==========

    fun searchItems(keyword: String): Flow<List<ItemEntity>> = itemDao.searchItems(keyword)

    fun filterItems(
        categoryId: Long? = null,
        startDate: Long? = null,
        endDate: Long? = null
    ): Flow<List<ItemEntity>> = itemDao.filterItems(categoryId, startDate, endDate)

    // ========== 保修提醒 ==========

    fun getWarrantyExpiringItems(threshold: Long): Flow<List<ItemEntity>> =
        itemDao.getWarrantyExpiringItems(threshold)

    fun getWarrantyExpiringCount(threshold: Long): Flow<Int> =
        itemDao.getWarrantyExpiringCount(threshold)

    // ========== 统计 ==========

    fun getTotalCount(): Flow<Int> = itemDao.getTotalCount()

    fun getTotalValue(): Flow<Double> = itemDao.getTotalValue()

    fun getOwnedCount(): Flow<Int> = itemDao.getOwnedCount()

    fun getOwnedValue(): Flow<Double> = itemDao.getOwnedValue()

    suspend fun getCategoryTotalValue(categoryId: Long): Double =
        itemDao.getCategoryTotalValue(categoryId)

    fun getCategoryItemCount(categoryId: Long): Flow<Int> =
        itemDao.getCategoryItemCount(categoryId)

    // ========== 写入操作 ==========

    suspend fun insertItem(item: ItemEntity): Long = itemDao.insertItem(item)

    suspend fun updateItem(item: ItemEntity) = itemDao.updateItem(item)

    suspend fun softDeleteItem(itemId: Long) = itemDao.softDeleteItem(itemId)

    suspend fun restoreItem(itemId: Long) = itemDao.restoreItem(itemId)

    suspend fun hardDeleteItem(itemId: Long) {
        // 删除关联照片文件
        val photos = itemPhotoDao.getPhotosByItemIdOnce(itemId)
        photos.forEach { java.io.File(it.photoPath).delete() }
        itemPhotoDao.deletePhotosByItemId(itemId)
        itemDao.hardDeleteItem(itemId)
    }

    suspend fun emptyTrash() {
        val deletedItems = itemDao.getDeletedItems().first()
        deletedItems.forEach { item ->
            val photos = itemPhotoDao.getPhotosByItemIdOnce(item.id)
            photos.forEach { java.io.File(it.photoPath).delete() }
            itemPhotoDao.deletePhotosByItemId(item.id)
        }
        itemDao.emptyTrash()
    }

    // ========== 照片管理 ==========

    suspend fun addPhoto(photo: ItemPhotoEntity): Long = itemPhotoDao.insertPhoto(photo)

    suspend fun addPhotos(photos: List<ItemPhotoEntity>) = itemPhotoDao.insertPhotos(photos)

    suspend fun deletePhoto(photo: ItemPhotoEntity) {
        java.io.File(photo.photoPath).delete()
        itemPhotoDao.deletePhoto(photo)
    }

    suspend fun getAllPhotoPaths(): List<String> = itemPhotoDao.getAllPhotoPaths()

    suspend fun getAllPhotos(): List<ItemPhotoEntity> = itemPhotoDao.getAllPhotos()

    suspend fun addPhotosForItem(itemId: Long, photoFiles: List<java.io.File>) {
        val photos = photoFiles.mapIndexed { index, file ->
            ItemPhotoEntity(
                itemId = itemId,
                photoPath = file.absolutePath,
                sortOrder = index
            )
        }
        addPhotos(photos)
    }

    // 删除照片记录+文件（用于彻底删除物品）
    suspend fun deletePhotosByItemId(itemId: Long) {
        val photos = itemPhotoDao.getPhotosByItemIdOnce(itemId)
        photos.forEach { java.io.File(it.photoPath).delete() }
        itemPhotoDao.deletePhotosByItemId(itemId)
    }

    // 仅删除照片记录，不删文件（用于编辑时重建照片列表）
    suspend fun deletePhotoRecordsByItemId(itemId: Long) {
        itemPhotoDao.deletePhotosByItemId(itemId)
    }

    suspend fun getPhotosByItemId(itemId: Long): List<ItemPhotoEntity> =
        itemPhotoDao.getPhotosByItemIdOnce(itemId)

    suspend fun getFirstPhotoPath(itemId: Long): String? =
        itemPhotoDao.getFirstPhotoByItemId(itemId)?.photoPath

    // ========== 回收站 ==========

    fun getDeletedItems(): Flow<List<ItemEntity>> = itemDao.getDeletedItems()

    // ========== v5.2 状态 & 标签 ==========

    fun getItemsByStatus(status: String): Flow<List<ItemEntity>> = itemDao.getItemsByStatus(status)

    fun getItemsByTag(tag: String): Flow<List<ItemEntity>> = itemDao.getItemsByTag(tag)

    fun getAllTags(): Flow<List<String>> = itemDao.getAllTagsRaw().map { rawList ->
        rawList.flatMap { it.split(",") }.map { it.trim() }.filter { it.isNotEmpty() }.distinct().sorted()
    }

    suspend fun updateItemStatus(itemId: Long, status: String) = itemDao.updateItemStatus(itemId, status)

    fun searchItemsWithTags(keyword: String): Flow<List<ItemEntity>> = itemDao.searchItemsWithTags(keyword)
}
