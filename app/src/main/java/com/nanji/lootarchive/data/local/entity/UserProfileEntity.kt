package com.nanji.lootarchive.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Int = 1,
    val exp: Int = 0,
    val level: Int = 1,
    val totalItemsAdded: Int = 0,
    val totalPhotosAdded: Int = 0,
    val totalDescriptionsFilled: Int = 0,
    val streakDays: Int = 0,
    val lastActiveDate: Long? = null,
    val updatedAt: Long = System.currentTimeMillis()
)
