package com.nanji.lootarchive.data.local.dao

import androidx.room.*
import com.nanji.lootarchive.data.local.entity.ExperienceLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExperienceLogDao {
    @Insert
    suspend fun insertLog(log: ExperienceLogEntity)

    @Query("SELECT * FROM experience_log ORDER BY createdAt DESC LIMIT 20")
    fun getRecentLogs(): Flow<List<ExperienceLogEntity>>
}
