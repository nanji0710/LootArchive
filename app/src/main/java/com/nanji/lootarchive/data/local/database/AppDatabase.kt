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
    version = 3,
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

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Drop then recreate to guarantee schema matches Room's expectation
                db.execSQL("DROP TABLE IF EXISTS experience_log")
                db.execSQL("DROP TABLE IF EXISTS achievements")
                db.execSQL("DROP TABLE IF EXISTS user_profile")
                db.execSQL("CREATE TABLE user_profile (id INTEGER NOT NULL PRIMARY KEY DEFAULT 1, exp INTEGER NOT NULL DEFAULT 0, level INTEGER NOT NULL DEFAULT 1, totalItemsAdded INTEGER NOT NULL DEFAULT 0, totalPhotosAdded INTEGER NOT NULL DEFAULT 0, totalDescriptionsFilled INTEGER NOT NULL DEFAULT 0, streakDays INTEGER NOT NULL DEFAULT 0, lastActiveDate INTEGER DEFAULT NULL, updatedAt INTEGER NOT NULL DEFAULT 0)")
                db.execSQL("CREATE TABLE achievements (`key` TEXT NOT NULL, title TEXT NOT NULL, description TEXT NOT NULL, icon TEXT NOT NULL DEFAULT '', category TEXT NOT NULL DEFAULT 'collection', isUnlocked INTEGER NOT NULL DEFAULT 0, unlockedAt INTEGER DEFAULT NULL, progress INTEGER NOT NULL DEFAULT 0, target INTEGER NOT NULL DEFAULT 100, PRIMARY KEY(`key`))")
                db.execSQL("CREATE TABLE experience_log (id INTEGER PRIMARY KEY AUTOINCREMENT, source TEXT NOT NULL, amount INTEGER NOT NULL, itemId INTEGER DEFAULT NULL, createdAt INTEGER NOT NULL DEFAULT 0)")
                // Seed data
                val titles = mapOf(
                    "items_5" to "初级收藏", "items_20" to "中级收藏家", "items_50" to "高级收藏家",
                    "items_100" to "百物之主", "value_10000" to "万元户", "value_100000" to "小富翁",
                    "value_500000" to "财富自由", "photos_10" to "随手拍", "photos_50" to "摄影师",
                    "desc_10" to "细节控", "desc_50" to "文字家", "streak_7" to "坚持一周", "streak_30" to "月常打卡"
                )
                titles.forEach { (k, t) -> db.execSQL("INSERT OR IGNORE INTO achievements (`key`,title,description,target) VALUES ('$k','$t','',1)") }
                db.execSQL("INSERT OR IGNORE INTO user_profile (id,exp,level) VALUES (1,0,1)")
            }
        }
    }
}
