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
                    // 首次创建数据库时预填默认分类
                    db.execSQL("INSERT INTO categories (name, iconName, sortOrder) VALUES ('数码产品', 'smartphone', 0)")
                    db.execSQL("INSERT INTO categories (name, iconName, sortOrder) VALUES ('服饰鞋包', 'checkroom', 1)")
                    db.execSQL("INSERT INTO categories (name, iconName, sortOrder) VALUES ('书籍文具', 'menu_book', 2)")
                    db.execSQL("INSERT INTO categories (name, iconName, sortOrder) VALUES ('工具器材', 'build', 3)")
                    db.execSQL("INSERT INTO categories (name, iconName, sortOrder) VALUES ('藏品摆件', 'diamond', 4)")
                    db.execSQL("INSERT INTO categories (name, iconName, sortOrder) VALUES ('其他', 'category', 5)")
                }
            })
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideCategoryDao(db: AppDatabase): CategoryDao = db.categoryDao()

    @Provides
    fun provideItemDao(db: AppDatabase): ItemDao = db.itemDao()

    @Provides
    fun provideItemPhotoDao(db: AppDatabase): ItemPhotoDao = db.itemPhotoDao()

    @Provides
    fun provideBackupRecordDao(db: AppDatabase): BackupRecordDao = db.backupRecordDao()
}
