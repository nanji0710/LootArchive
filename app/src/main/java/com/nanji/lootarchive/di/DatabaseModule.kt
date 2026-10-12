package com.nanji.lootarchive.di

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import com.nanji.lootarchive.data.DefaultCategories
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
            .addMigrations(
                AppDatabase.MIGRATION_1_2,
                AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_3_4,
                AppDatabase.MIGRATION_4_5,
                AppDatabase.MIGRATION_5_6
            )
            .build()
    }

    private fun seedDefaultCategories(db: SupportSQLiteDatabase) {
        try {
            DefaultCategories.all.forEachIndexed { index, (name, icon) ->
                db.execSQL("INSERT INTO categories (name, iconName, sortOrder) VALUES ('$name', '$icon', $index)")
            }
        } catch (e: Exception) {
            android.util.Log.e("DatabaseModule", "Seed categories failed", e)
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
}
