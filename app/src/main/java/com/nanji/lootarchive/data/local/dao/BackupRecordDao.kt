package com.nanji.lootarchive.data.local.dao

import androidx.room.*
import com.nanji.lootarchive.data.local.entity.BackupRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BackupRecordDao {

    @Query("SELECT * FROM backup_records ORDER BY createdAt DESC")
    fun getAllRecords(): Flow<List<BackupRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: BackupRecordEntity): Long

    @Delete
    suspend fun deleteRecord(record: BackupRecordEntity)

    @Query("DELETE FROM backup_records")
    suspend fun deleteAllRecords()
}
