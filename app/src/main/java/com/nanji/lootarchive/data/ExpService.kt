package com.nanji.lootarchive.data

import com.nanji.lootarchive.data.local.dao.AchievementDao
import com.nanji.lootarchive.data.local.dao.ExperienceLogDao
import com.nanji.lootarchive.data.local.dao.ItemDao
import com.nanji.lootarchive.data.local.dao.UserProfileDao
import com.nanji.lootarchive.data.local.entity.ExperienceLogEntity
import com.nanji.lootarchive.data.local.entity.UserProfileEntity
import com.nanji.lootarchive.util.ExpCalculator
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExpService @Inject constructor(
    private val userProfileDao: UserProfileDao,
    private val achievementDao: AchievementDao,
    private val experienceLogDao: ExperienceLogDao,
    private val itemDao: ItemDao
) {
    suspend fun recordAddItem(itemId: Long, price: Double, hasDesc: Boolean, photoCount: Int) {
        val now = System.currentTimeMillis()
        ensureProfile()

        // 数量分
        val countExp = ExpCalculator.Rewards.ITEM_COUNT_EXP
        experienceLogDao.insertLog(ExperienceLogEntity(source = "add_item", amount = countExp, itemId = itemId, createdAt = now))
        userProfileDao.addExp(countExp, now)
        userProfileDao.incrementItemsAdded(now)

        // 价值分
        val valueExp = ExpCalculator.Rewards.valueExp(price)
        if (valueExp > 0) {
            experienceLogDao.insertLog(ExperienceLogEntity(source = "value_score", amount = valueExp, itemId = itemId, createdAt = now))
            userProfileDao.addExp(valueExp, now)
        }

        // 描述分
        if (hasDesc) {
            experienceLogDao.insertLog(ExperienceLogEntity(source = "complete_desc", amount = ExpCalculator.Rewards.COMPLETE_DESCRIPTION, itemId = itemId, createdAt = now))
            userProfileDao.addExp(ExpCalculator.Rewards.COMPLETE_DESCRIPTION, now)
            userProfileDao.incrementDescriptionsFilled(now)
        }

        // 照片分
        if (photoCount > 0) {
            experienceLogDao.insertLog(ExperienceLogEntity(source = "add_photo", amount = ExpCalculator.Rewards.ADD_PHOTO * photoCount, itemId = itemId, createdAt = now))
            userProfileDao.addExp(ExpCalculator.Rewards.ADD_PHOTO * photoCount, now)
            userProfileDao.incrementPhotosAdded(photoCount, now)
        }

        // 重新计算等级
        val profile = userProfileDao.getProfileSync() ?: return
        val newLevel = ExpCalculator.getLevel(profile.exp)
        if (newLevel != profile.level) {
            userProfileDao.setLevel(newLevel, now)
        }

        // 检查成就
        checkAchievements(profile)
    }

    private suspend fun ensureProfile() {
        if (userProfileDao.getProfileSync() == null) {
            userProfileDao.upsertProfile(UserProfileEntity(id = 1, exp = 0, level = 1))
        }
    }

    private suspend fun checkAchievements(profile: UserProfileEntity) {
        val now = System.currentTimeMillis()
        val totalCount = itemDao.getTotalCountSync()
        val totalValue = itemDao.getTotalValueSync()
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
            "photos_10" to (profile.totalPhotosAdded >= 10),
            "photos_50" to (profile.totalPhotosAdded >= 50),
            "desc_10" to (profile.totalDescriptionsFilled >= 10),
            "desc_50" to (profile.totalDescriptionsFilled >= 50),
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
