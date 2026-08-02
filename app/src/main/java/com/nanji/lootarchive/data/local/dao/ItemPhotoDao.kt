package com.nanji.lootarchive.data.local.dao

import androidx.room.*
import com.nanji.lootarchive.data.local.entity.ItemPhotoEntity
import kotlinx.coroutines.flow.Flow

/** 物品 id → 首张照片路径 结果行 */
data class ItemPhotoPathRow(val itemId: Long, val photoPath: String)

@Dao
interface ItemPhotoDao {

    @Query("SELECT * FROM item_photos WHERE itemId = :itemId ORDER BY sortOrder ASC")
    fun getPhotosByItemId(itemId: Long): Flow<List<ItemPhotoEntity>>

    @Query("SELECT * FROM item_photos WHERE itemId = :itemId ORDER BY sortOrder ASC")
    suspend fun getPhotosByItemIdOnce(itemId: Long): List<ItemPhotoEntity>

    @Query("SELECT * FROM item_photos WHERE id = :id")
    suspend fun getPhotoById(id: Long): ItemPhotoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhoto(photo: ItemPhotoEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhotos(photos: List<ItemPhotoEntity>)

    @Update
    suspend fun updatePhoto(photo: ItemPhotoEntity)

    @Delete
    suspend fun deletePhoto(photo: ItemPhotoEntity)

    @Query("DELETE FROM item_photos WHERE itemId = :itemId")
    suspend fun deletePhotosByItemId(itemId: Long)

    @Query("SELECT * FROM item_photos WHERE itemId = :itemId ORDER BY sortOrder ASC LIMIT 1")
    suspend fun getFirstPhotoByItemId(itemId: Long): ItemPhotoEntity?

    @Query("SELECT COUNT(*) FROM item_photos WHERE itemId IN (SELECT id FROM items WHERE isDeleted = 0)")
    suspend fun getTotalPhotoCount(): Int

    @Query("SELECT photoPath FROM item_photos")
    suspend fun getAllPhotoPaths(): List<String>

    /** 批量取未删除物品的首张照片（消除逐物品 N+1 查询） */
    @Query(
        "SELECT ip.itemId AS itemId, ip.photoPath AS photoPath FROM item_photos ip " +
            "WHERE ip.id IN (SELECT MIN(id) FROM item_photos GROUP BY itemId) " +
            "AND ip.itemId IN (SELECT id FROM items WHERE isDeleted = 0)"
    )
    suspend fun getFirstPhotosForActiveItems(): List<ItemPhotoPathRow>

    @Query("SELECT * FROM item_photos")
    suspend fun getAllPhotos(): List<ItemPhotoEntity>
}
