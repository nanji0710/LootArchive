package com.nanji.lootarchive.data.local.dao

import androidx.room.*
import com.nanji.lootarchive.data.local.entity.ItemPhotoEntity
import kotlinx.coroutines.flow.Flow

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

    @Query("SELECT photoPath FROM item_photos")
    suspend fun getAllPhotoPaths(): List<String>

    @Query("SELECT * FROM item_photos")
    suspend fun getAllPhotos(): List<ItemPhotoEntity>
}
