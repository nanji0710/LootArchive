package com.nanji.lootarchive.data.local.dao

import androidx.room.*
import com.nanji.lootarchive.data.local.entity.UserProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = 1")
    fun getProfile(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile WHERE id = 1")
    suspend fun getProfileSync(): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: UserProfileEntity)

    @Query("UPDATE user_profile SET exp = exp + :amount, updatedAt = :now WHERE id = 1")
    suspend fun addExp(amount: Int, now: Long = System.currentTimeMillis())

    @Query("UPDATE user_profile SET level = :level, updatedAt = :now WHERE id = 1")
    suspend fun setLevel(level: Int, now: Long = System.currentTimeMillis())

    @Query("UPDATE user_profile SET totalItemsAdded = totalItemsAdded + 1, updatedAt = :now WHERE id = 1")
    suspend fun incrementItemsAdded(now: Long = System.currentTimeMillis())

    @Query("UPDATE user_profile SET totalPhotosAdded = totalPhotosAdded + :count, updatedAt = :now WHERE id = 1")
    suspend fun incrementPhotosAdded(count: Int, now: Long = System.currentTimeMillis())

    @Query("UPDATE user_profile SET totalDescriptionsFilled = totalDescriptionsFilled + 1, updatedAt = :now WHERE id = 1")
    suspend fun incrementDescriptionsFilled(now: Long = System.currentTimeMillis())

    @Query("UPDATE user_profile SET streakDays = :days, lastActiveDate = :now WHERE id = 1")
    suspend fun updateStreak(days: Int, now: Long = System.currentTimeMillis())
}
