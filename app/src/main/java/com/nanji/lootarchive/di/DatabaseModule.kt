package com.nanji.lootarchive.di

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import com.nanji.lootarchive.data.local.database.AppDatabase
import com.nanji.lootarchive.data.local.dao.*
import com.nanji.lootarchive.util.Quintet
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
            // (key, title, description, target)
            val seeds = listOf(
                Quintet("items_5","初级收藏","收集5件物品","collection",5),
                Quintet("items_20","中级收藏家","收集20件物品","collection",20),
                Quintet("items_50","高级收藏家","收集50件物品","collection",50),
                Quintet("items_100","百物之主","收集100件物品","collection",100),
                Quintet("value_10000","万元户","总资产超过1万","value",10000),
                Quintet("value_100000","小富翁","总资产超过10万","value",100000),
                Quintet("value_500000","财富自由","总资产超过50万","value",500000),
                Quintet("photos_10","随手拍","拍摄10张照片","photo",10),
                Quintet("photos_50","摄影师","拍摄50张照片","photo",50),
                Quintet("desc_10","细节控","完善10件物品描述","detail",10),
                Quintet("desc_50","文字家","完善50件物品描述","detail",50),
                Quintet("streak_7","坚持一周","连续7天活跃","streak",7),
                Quintet("streak_30","月常打卡","连续30天活跃","streak",30)
            )
            seeds.forEach { seed ->
                db.execSQL("INSERT OR REPLACE INTO achievements (\"key\",title,description,category,target,icon) VALUES ('${seed.first}','${seed.second}','${seed.third}','${seed.fourth}',${seed.fifth},'')")
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
