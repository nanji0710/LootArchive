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

        val ownedCount = itemDao.getOwnedCountSync()
        val ownedValue = itemDao.getOwnedValueSync()
        val totalCount = itemDao.getTotalCountSync()
        val descCount = itemDao.getItemsWithDescriptionCount()
        val photoCount = itemPhotoDao.getTotalPhotoCount()

        // 连续活跃天数
        val oldProfile = userProfileDao.getProfileSync()
        val newStreak = computeStreak(oldProfile, now)

        // EXP 基于拥有物品计算（数量分 + 价值分）
        val countExp = ownedCount * ExpCalculator.Rewards.ITEM_COUNT_EXP
        val valueExp = ExpCalculator.Rewards.valueExp(ownedValue)
        val descExp = descCount * ExpCalculator.Rewards.COMPLETE_DESCRIPTION
        val photoExp = photoCount * ExpCalculator.Rewards.ADD_PHOTO
        val totalExp = countExp + valueExp + descExp + photoExp

        val newLevel = ExpCalculator.getLevel(totalExp)

        userProfileDao.upsertProfile(
            UserProfileEntity(
                id = 1,
                exp = totalExp,
                level = newLevel,
                totalItemsAdded = ownedCount,
                totalPhotosAdded = photoCount,
                totalDescriptionsFilled = descCount,
                streakDays = newStreak,
                lastActiveDate = now,
                updatedAt = now
            )
        )

        checkAchievements(ownedCount, ownedValue, photoCount, descCount)
    }

    private fun computeStreak(oldProfile: UserProfileEntity?, now: Long): Int {
        if (oldProfile == null) return 1
        val cal = java.util.Calendar.getInstance()
        cal.timeInMillis = now; cal.set(java.util.Calendar.HOUR_OF_DAY, 0); cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0); cal.set(java.util.Calendar.MILLISECOND, 0)
        val todayStart = cal.timeInMillis

        val lastActive = oldProfile.lastActiveDate ?: return 1
        cal.timeInMillis = lastActive; cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0); cal.set(java.util.Calendar.SECOND, 0); cal.set(java.util.Calendar.MILLISECOND, 0)
        val lastActiveDay = cal.timeInMillis

        val diffDays = ((todayStart - lastActiveDay) / (24 * 60 * 60 * 1000)).toInt()
        return when {
            diffDays == 0 -> oldProfile.streakDays      // 同一天，不变
            diffDays == 1 -> oldProfile.streakDays + 1   // 连续
            else -> 1                                     // 中断，重新开始
        }
    }

    /** 仅记录新增事件日志（用于追溯），EXP 由 recalculate 保证准确性 */
    suspend fun logAddItem(itemId: Long) { }

    private suspend fun ensureProfile() {
        if (userProfileDao.getProfileSync() == null) {
            userProfileDao.upsertProfile(UserProfileEntity(id = 1, exp = 0, level = 1))
        }
    }

    private suspend fun checkAchievements(ownedCount: Int, ownedValue: Double, photoCount: Int, descCount: Int) {
        val now = System.currentTimeMillis()
        val profile = userProfileDao.getProfileSync() ?: return
        val unlocked = achievementDao.getUnlockedAchievementsSync()
        val unlockedKeys = unlocked.map { it.key }.toSet()

        val checks = mapOf(
            "items_5" to (ownedCount >= 5),
            "items_20" to (ownedCount >= 20),
            "items_50" to (ownedCount >= 50),
            "items_100" to (ownedCount >= 100),
            "value_10000" to (ownedValue >= 10_000),
            "value_100000" to (ownedValue >= 100_000),
            "value_500000" to (ownedValue >= 500_000),
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
