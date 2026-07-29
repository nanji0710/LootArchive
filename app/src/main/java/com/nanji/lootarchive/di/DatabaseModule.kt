package com.nanji.lootarchive.di

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import com.nanji.lootarchive.data.local.database.AppDatabase
import com.nanji.lootarchive.data.local.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "lootarchive.db"
        )
            .addCallback(object : androidx.room.RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    seedDefaultCategories(db)
                    seedAchievementsAndProfile(db)
                }
            })
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .fallbackToDestructiveMigration()
            .build()
    }

    private fun seedDefaultCategories(db: SupportSQLiteDatabase) {
        try {
            db.execSQL("INSERT INTO categories (name, iconName, sortOrder) VALUES ('食品饮料', 'restaurant', 0)")
            db.execSQL("INSERT INTO categories (name, iconName, sortOrder) VALUES ('药品保健', 'medical_services', 1)")
            db.execSQL("INSERT INTO categories (name, iconName, sortOrder) VALUES ('日用百货', 'local_mall', 2)")
            db.execSQL("INSERT INTO categories (name, iconName, sortOrder) VALUES ('数码电子', 'smartphone', 3)")
            db.execSQL("INSERT INTO categories (name, iconName, sortOrder) VALUES ('服饰鞋包', 'checkroom', 4)")
            db.execSQL("INSERT INTO categories (name, iconName, sortOrder) VALUES ('书籍文具', 'menu_book', 5)")
            db.execSQL("INSERT INTO categories (name, iconName, sortOrder) VALUES ('工具器材', 'build', 6)")
            db.execSQL("INSERT INTO categories (name, iconName, sortOrder) VALUES ('藏品摆件', 'diamond', 7)")
            db.execSQL("INSERT INTO categories (name, iconName, sortOrder) VALUES ('家居家具', 'chair', 8)")
            db.execSQL("INSERT INTO categories (name, iconName, sortOrder) VALUES ('其他', 'category', 9)")
        } catch (_: Exception) { }
    }

    private fun seedAchievementsAndProfile(db: SupportSQLiteDatabase) {
        try {
            db.execSQL("INSERT OR IGNORE INTO user_profile (id,exp,level) VALUES (1,0,1)")
            val seeds = listOf(
                "items_5" to "初级收藏", "items_20" to "中级收藏家", "items_50" to "高级收藏家",
                "items_100" to "百物之主", "value_10000" to "万元户", "value_100000" to "小富翁",
                "value_500000" to "财富自由", "photos_10" to "随手拍", "photos_50" to "摄影师",
                "desc_10" to "细节控", "desc_50" to "文字家", "streak_7" to "坚持一周", "streak_30" to "月常打卡"
            )
            seeds.forEach { (k, t) ->
                db.execSQL("INSERT OR IGNORE INTO achievements (\"key\",title,description,target) VALUES ('$k','$t','',1)")
            }
        } catch (_: Exception) { }
    }

    @Provides
    fun provideCategoryDao(db: AppDatabase): CategoryDao = db.categoryDao()

    @Provides
    fun provideItemDao(db: AppDatabase): ItemDao = db.itemDao()

    @Provides
    fun provideItemPhotoDao(db: AppDatabase): ItemPhotoDao = db.itemPhotoDao()

    @Provides
    fun provideBackupRecordDao(db: AppDatabase): BackupRecordDao = db.backupRecordDao()

    @Provides
    fun provideUserProfileDao(db: AppDatabase): UserProfileDao = db.userProfileDao()

    @Provides
    fun provideAchievementDao(db: AppDatabase): AchievementDao = db.achievementDao()

    @Provides
    fun provideExperienceLogDao(db: AppDatabase): ExperienceLogDao = db.experienceLogDao()
}
