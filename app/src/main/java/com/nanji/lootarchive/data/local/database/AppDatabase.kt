package com.nanji.lootarchive.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.nanji.lootarchive.data.local.dao.*
import com.nanji.lootarchive.data.local.entity.*

@Database(
    entities = [
        CategoryEntity::class,
        ItemEntity::class,
        ItemPhotoEntity::class,
        BackupRecordEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun categoryDao(): CategoryDao
    abstract fun itemDao(): ItemDao
    abstract fun itemPhotoDao(): ItemPhotoDao
    abstract fun backupRecordDao(): BackupRecordDao
}
