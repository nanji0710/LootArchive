package com.nanji.lootarchive.di

import android.content.Context
import androidx.room.Room
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
