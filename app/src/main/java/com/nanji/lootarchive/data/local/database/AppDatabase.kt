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
    version = 6,
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
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS user_profile (
                        id INTEGER NOT NULL PRIMARY KEY DEFAULT 1,
                        exp INTEGER NOT NULL DEFAULT 0,
                        level INTEGER NOT NULL DEFAULT 1,
                        totalItemsAdded INTEGER NOT NULL DEFAULT 0,
                        totalPhotosAdded INTEGER NOT NULL DEFAULT 0,
                        totalDescriptionsFilled INTEGER NOT NULL DEFAULT 0,
                        streakDays INTEGER NOT NULL DEFAULT 0,
                        lastActiveDate INTEGER DEFAULT NULL,
                        updatedAt INTEGER NOT NULL DEFAULT 0
                    )
                """)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS achievements (
                        `key` TEXT NOT NULL PRIMARY KEY,
                        title TEXT NOT NULL,
                        description TEXT NOT NULL,
                        icon TEXT NOT NULL DEFAULT '',
                        category TEXT NOT NULL DEFAULT 'collection',
                        isUnlocked INTEGER NOT NULL DEFAULT 0,
                        unlockedAt INTEGER DEFAULT NULL,
                        progress INTEGER NOT NULL DEFAULT 0,
                        target INTEGER NOT NULL DEFAULT 100
                    )
                """)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS experience_log (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        source TEXT NOT NULL,
                        amount INTEGER NOT NULL,
                        itemId INTEGER DEFAULT NULL,
                        createdAt INTEGER NOT NULL DEFAULT 0
                    )
                """)
                val achs = listOf(
                    listOf("items_5","初级收藏","收集5件物品","collection","5"),
                    listOf("items_20","中级收藏家","收集20件物品","collection","20"),
                    listOf("items_50","高级收藏家","收集50件物品","collection","50"),
                    listOf("items_100","百物之主","收集100件物品","collection","100"),
                    listOf("value_10000","万元户","总资产超过1万","value","10000"),
                    listOf("value_100000","小富翁","总资产超过10万","value","100000"),
                    listOf("value_500000","财富自由","总资产超过50万","value","500000"),
                    listOf("photos_10","随手拍","拍摄10张照片","photo","10"),
                    listOf("photos_50","摄影师","拍摄50张照片","photo","50"),
                    listOf("desc_10","细节控","完善10件物品描述","detail","10"),
                    listOf("desc_50","文字家","完善50件物品描述","detail","50"),
                    listOf("streak_7","坚持一周","连续7天活跃","streak","7"),
                    listOf("streak_30","月常打卡","连续30天活跃","streak","30")
                )
                achs.forEach { a ->
                    db.execSQL("INSERT OR IGNORE INTO achievements (`key`,title,description,category,target) VALUES ('${a[0]}','${a[1]}','${a[2]}','${a[3]}',${a[4]})")
                }
                db.execSQL("INSERT OR IGNORE INTO user_profile (id,exp,level) VALUES (1,0,1)")
            }
        }
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // v3→v4 无 schema 列变更（v5.4.2 仅移除 MIGRATION_2_3 并跳版本号），no-op
            }
        }
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE items ADD COLUMN salePrice REAL DEFAULT NULL")
            }
        }
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE items ADD COLUMN saleDate INTEGER DEFAULT NULL")
            }
        }
    }
}
