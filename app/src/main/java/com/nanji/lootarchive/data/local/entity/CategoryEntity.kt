package com.nanji.lootarchive.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val iconName: String = "category",    // Material Icons name
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
