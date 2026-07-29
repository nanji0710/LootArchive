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
                }
            })
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3)
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
        } catch (_: Exception) {
            // 默认分类插入失败不影响启动
        }
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
