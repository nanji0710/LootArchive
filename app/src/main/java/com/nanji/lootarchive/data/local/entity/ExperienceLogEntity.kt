package com.nanji.lootarchive.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "experience_log")
data class ExperienceLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val source: String,
    val amount: Int,
    val itemId: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
