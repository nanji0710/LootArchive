package com.nanji.lootarchive.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.nanji.lootarchive.data.local.dao.*
import com.nanji.lootarchive.data.local.entity.*

@Database(
    entities = [
        CategoryEntity::class,
        ItemEntity::class,
        ItemPhotoEntity::class,
        BackupRecordEntity::class,
        UserProfileEntity::class,
        AchievementEntity::class,
        ExperienceLogEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun categoryDao(): CategoryDao
    abstract fun itemDao(): ItemDao
    abstract fun itemPhotoDao(): ItemPhotoDao
    abstract fun backupRecordDao(): BackupRecordDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun achievementDao(): AchievementDao
    abstract fun experienceLogDao(): ExperienceLogDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE items ADD COLUMN status TEXT NOT NULL DEFAULT 'active'")
                db.execSQL("ALTER TABLE items ADD COLUMN tags TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE items ADD COLUMN lastStatusChangedAt INTEGER DEFAULT NULL")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_items_status ON items (status)")
            }
        }
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE items ADD COLUMN salePrice REAL DEFAULT NULL")
            }
        }
    }
}
