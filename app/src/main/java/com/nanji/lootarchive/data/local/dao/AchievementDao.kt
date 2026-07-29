package com.nanji.lootarchive.data.local.dao

import androidx.room.*
import com.nanji.lootarchive.data.local.entity.AchievementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AchievementDao {
    @Query("SELECT * FROM achievements ORDER BY isUnlocked DESC, category ASC")
    fun getAllAchievements(): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM achievements WHERE isUnlocked = 1 ORDER BY unlockedAt DESC")
    fun getUnlockedAchievements(): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM achievements WHERE isUnlocked = 1 ORDER BY unlockedAt DESC")
    suspend fun getUnlockedAchievementsSync(): List<AchievementEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAchievement(achievement: AchievementEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(achievements: List<AchievementEntity>)

    @Query("UPDATE achievements SET isUnlocked = 1, unlockedAt = :now, progress = target WHERE `key` = :key AND isUnlocked = 0")
    suspend fun unlockAchievement(key: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE achievements SET progress = :progress WHERE `key` = :key")
    suspend fun updateProgress(key: String, progress: Int)

    @Query("SELECT COUNT(*) FROM achievements WHERE isUnlocked = 0")
    suspend fun getLockedCount(): Int
}
