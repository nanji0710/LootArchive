package com.nanji.lootarchive.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "items",
    indices = [
        Index("categoryId"),
        Index("name"),
        Index("isDeleted"),
        Index("status"),
        Index("warrantyExpiryDate"),
        Index("purchaseDate"),
        Index("updatedAt")
    ]
)
data class ItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val categoryId: Long = 0,
    val purchasePrice: Double = 0.0,
    val currency: String = "CNY",
    val storageLocation: String = "",
    val purchaseDate: Long? = null,          // epoch millis
    val warrantyExpiryDate: Long? = null,     // epoch millis
    val warrantyPeriodDays: Int? = null,      // 保修天数，用于计算到期日
    val description: String = "",
    val isDeleted: Boolean = false,           // 回收站标记
    val deletedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val status: String = "active",            // active|idle|sold|repair|lost
    val tags: String = "",                    // comma-separated tag names
    val lastStatusChangedAt: Long? = null     // epoch millis
)
