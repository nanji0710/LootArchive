package com.nanji.lootarchive.data.repository

import androidx.room.withTransaction
import com.nanji.lootarchive.data.local.dao.CategoryDao
import com.nanji.lootarchive.data.local.dao.ItemDao
import com.nanji.lootarchive.data.local.dao.ItemPhotoDao
import com.nanji.lootarchive.data.local.database.AppDatabase
import com.nanji.lootarchive.data.local.entity.ItemEntity
import com.nanji.lootarchive.data.local.entity.ItemPhotoEntity
import com.nanji.lootarchive.domain.model.ItemWithPhotos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ItemRepository @Inject constructor(
    private val itemDao: ItemDao,
    private val categoryDao: CategoryDao,
    private val itemPhotoDao: ItemPhotoDao,
    private val database: AppDatabase
) {
    // ========== 物品列表 ==========

    fun getAllItems(): Flow<List<ItemEntity>> = itemDao.getAllItems()

    suspend fun getItemById(id: Long): ItemEntity? = itemDao.getItemById(id)

    /** 含回收站物品，恢复备份做匹配时用（见 ItemDao.getAllItemsIncludingDeleted）。 */
    suspend fun getAllItemsIncludingDeleted(): List<ItemEntity> = itemDao.getAllItemsIncludingDeleted()

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

    suspend fun hardDeleteItem(itemId: Long) = hardDeleteItemWithPhotos(itemId)

    /**
     * 彻底删除物品 + 照片文件：先删文件（IO 线程），再事务删除 DB 记录，
     * 避免文件/记录不同步产生孤儿数据。
     */
    suspend fun hardDeleteItemWithPhotos(itemId: Long) {
        val photos = itemPhotoDao.getPhotosByItemIdOnce(itemId)
        withContext(Dispatchers.IO) { photos.forEach { java.io.File(it.photoPath).delete() } }
        database.withTransaction {
            itemPhotoDao.deletePhotosByItemId(itemId)
            itemDao.hardDeleteItem(itemId)
        }
    }

    suspend fun emptyTrash() {
        val deletedItems = itemDao.getDeletedItems().first()
        withContext(Dispatchers.IO) {
            deletedItems.forEach { item ->
                itemPhotoDao.getPhotosByItemIdOnce(item.id).forEach { java.io.File(it.photoPath).delete() }
            }
        }
        database.withTransaction {
            deletedItems.forEach { item -> itemPhotoDao.deletePhotosByItemId(item.id) }
            itemDao.emptyTrash()
        }
    }

    /**
     * 保存物品及照片（新建/编辑）。文件删除在 IO 线程，DB 写入在事务内原子执行。
     * item.id == 0 → 新建；否则编辑（先删旧照片记录再重建）。
     * @return 保存后的物品 id（新建为自增主键，编辑为 item.id）——保存后动作条（补照片/撤销）需要它。
     */
    suspend fun saveItemWithPhotos(item: ItemEntity, photoPaths: List<String>): Long {
        return if (item.id == 0L) {
            val id = database.withTransaction { itemDao.insertItem(item) }
            database.withTransaction {
                itemPhotoDao.insertPhotos(
                    photoPaths.mapIndexed { index, p ->
                        ItemPhotoEntity(itemId = id, photoPath = p, sortOrder = index)
                    }
                )
            }
            id
        } else {
            val oldPaths = itemPhotoDao.getPhotosByItemIdOnce(item.id).map { it.photoPath }.toSet()
            val toDelete = oldPaths - photoPaths.toSet()
            withContext(Dispatchers.IO) { toDelete.forEach { java.io.File(it).delete() } }
            database.withTransaction {
                itemPhotoDao.deletePhotosByItemId(item.id)
                itemPhotoDao.insertPhotos(
                    photoPaths.mapIndexed { index, p ->
                        ItemPhotoEntity(itemId = item.id, photoPath = p, sortOrder = index)
                    }
                )
                itemDao.updateItem(item)
            }
            item.id
        }
    }

    // ========== 照片管理 ==========

    suspend fun addPhoto(photo: ItemPhotoEntity): Long = itemPhotoDao.insertPhoto(photo)

    suspend fun addPhotos(photos: List<ItemPhotoEntity>) = itemPhotoDao.insertPhotos(photos)

    suspend fun deletePhoto(photo: ItemPhotoEntity) {
        withContext(Dispatchers.IO) { java.io.File(photo.photoPath).delete() }
        itemPhotoDao.deletePhoto(photo)
    }

    suspend fun getAllPhotoPaths(): List<String> = itemPhotoDao.getAllPhotoPaths()

    suspend fun getAllPhotos(): List<ItemPhotoEntity> = itemPhotoDao.getAllPhotos()

    /** 最近使用过的存放位置（最多 limit 条），供新增物品页位置联想。 */
    suspend fun getRecentLocations(limit: Int = 3): List<String> = itemDao.getRecentLocations(limit)

    suspend fun addPhotosForItem(itemId: Long, photoFiles: List<java.io.File>) {
        // 追加到已有照片之后：起始序号取现有照片数，避免与既有 sortOrder 冲突（此前从 0 起会导致顺序错乱）
        val startOrder = itemPhotoDao.getPhotosByItemIdOnce(itemId).size
        val photos = photoFiles.mapIndexed { index, file ->
            ItemPhotoEntity(
                itemId = itemId,
                photoPath = file.absolutePath,
                sortOrder = startOrder + index
            )
        }
        addPhotos(photos)
    }

    /**
     * 恢复导入用：用备份里的照片整体替换该物品的现有照片。
     * 先删旧记录与旧文件（避免孤儿文件），再登记刚解出来的新文件。
     */
    suspend fun replacePhotosForItem(itemId: Long, photoFiles: List<java.io.File>) {
        deletePhotosByItemId(itemId)
        addPhotosForItem(itemId, photoFiles)
    }

    // 删除照片记录+文件（用于彻底删除物品）
    suspend fun deletePhotosByItemId(itemId: Long) {
        val photos = itemPhotoDao.getPhotosByItemIdOnce(itemId)
        withContext(Dispatchers.IO) { photos.forEach { java.io.File(it.photoPath).delete() } }
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

    /** 批量取未删除物品的首张照片映射（消除列表页 N+1 查询） */
    suspend fun getAllFirstPhotos(): Map<Long, String> =
        itemPhotoDao.getFirstPhotosForActiveItems().associate { it.itemId to it.photoPath }

    // ========== 回收站 ==========

    fun getDeletedItems(): Flow<List<ItemEntity>> = itemDao.getDeletedItems()

    suspend fun getDeletedItemsBefore(threshold: Long): List<ItemEntity> = itemDao.getDeletedItemsBefore(threshold)

    // ========== v5.2 状态 & 标签 ==========

    fun getItemsByStatus(status: String): Flow<List<ItemEntity>> = itemDao.getItemsByStatus(status)

    suspend fun updateItemStatus(itemId: Long, status: String) = itemDao.updateItemStatus(itemId, status)

    fun searchItemsWithTags(keyword: String): Flow<List<ItemEntity>> = itemDao.searchItemsWithTags(keyword)
}
