package com.nanji.lootarchive.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "backup_records")
data class BackupRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fileName: String,
    val backupType: String,       // "database" | "excel"
    val filePath: String,
    val itemCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
