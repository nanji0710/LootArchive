package com.nanji.lootarchive.data

import com.nanji.lootarchive.data.local.dao.AchievementDao
import com.nanji.lootarchive.data.local.dao.ItemDao
import com.nanji.lootarchive.data.local.dao.ItemPhotoDao
import com.nanji.lootarchive.data.local.dao.UserProfileDao
import com.nanji.lootarchive.data.local.entity.UserProfileEntity
import com.nanji.lootarchive.util.ExpCalculator
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExpService @Inject constructor(
    private val userProfileDao: UserProfileDao,
    private val achievementDao: AchievementDao,
    private val itemDao: ItemDao,
    private val itemPhotoDao: ItemPhotoDao
) {

    /**
     * 从DB真实数据重新计算全部EXP、等级和成就。
     * 在新增/编辑/删除/导入后调用，确保数据始终准确。
     */
    suspend fun recalculateProfile() {
        val now = System.currentTimeMillis()
        ensureProfile()

        val totalCount = itemDao.getTotalCountSync()
        val totalValue = itemDao.getTotalValueSync()
        val descCount = itemDao.getItemsWithDescriptionCount()
        val photoCount = itemPhotoDao.getTotalPhotoCount()

        // EXP = 数量分 + 价值分 + 描述分 + 照片分
        val countExp = totalCount * ExpCalculator.Rewards.ITEM_COUNT_EXP
        val valueExp = ExpCalculator.Rewards.valueExp(totalValue)
        val descExp = descCount * ExpCalculator.Rewards.COMPLETE_DESCRIPTION
        val photoExp = photoCount * ExpCalculator.Rewards.ADD_PHOTO
        val totalExp = countExp + valueExp + descExp + photoExp

        val newLevel = ExpCalculator.getLevel(totalExp)

        userProfileDao.upsertProfile(
            UserProfileEntity(
                id = 1,
                exp = totalExp,
                level = newLevel,
                totalItemsAdded = totalCount,
                totalPhotosAdded = photoCount,
                totalDescriptionsFilled = descCount,
                streakDays = userProfileDao.getProfileSync()?.streakDays ?: 0,
                updatedAt = now
            )
        )

        checkAchievements(totalCount, totalValue, photoCount, descCount)
    }

    /** 仅记录新增事件日志（用于追溯），EXP 由 recalculate 保证准确性 */
    suspend fun logAddItem(itemId: Long) {
        // Lightweight log only; actual EXP computed by recalculateProfile()
    }

    private suspend fun ensureProfile() {
        if (userProfileDao.getProfileSync() == null) {
            userProfileDao.upsertProfile(UserProfileEntity(id = 1, exp = 0, level = 1))
        }
    }

    private suspend fun checkAchievements(totalCount: Int, totalValue: Double, photoCount: Int, descCount: Int) {
        val now = System.currentTimeMillis()
        val profile = userProfileDao.getProfileSync() ?: return
        val unlocked = achievementDao.getUnlockedAchievementsSync()
        val unlockedKeys = unlocked.map { it.key }.toSet()

        val checks = mapOf(
            "items_5" to (totalCount >= 5),
            "items_20" to (totalCount >= 20),
            "items_50" to (totalCount >= 50),
            "items_100" to (totalCount >= 100),
            "value_10000" to (totalValue >= 10_000),
            "value_100000" to (totalValue >= 100_000),
            "value_500000" to (totalValue >= 500_000),
            "photos_10" to (photoCount >= 10),
            "photos_50" to (photoCount >= 50),
            "desc_10" to (descCount >= 10),
            "desc_50" to (descCount >= 50),
            "streak_7" to (profile.streakDays >= 7),
            "streak_30" to (profile.streakDays >= 30)
        )
        checks.forEach { (key, achieved) ->
            if (achieved && key !in unlockedKeys) {
                achievementDao.unlockAchievement(key, now)
            }
        }
    }
}
