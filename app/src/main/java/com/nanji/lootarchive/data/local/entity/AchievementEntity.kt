package com.nanji.lootarchive.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey val key: String,
    val title: String,
    val description: String,
    val icon: String = "",
    val category: String = "collection",
    val isUnlocked: Boolean = false,
    val unlockedAt: Long? = null,
    val progress: Int = 0,
    val target: Int = 100
)
