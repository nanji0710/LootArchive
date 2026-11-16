package com.nanji.lootarchive.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.nanji.lootarchive.data.local.entity.AssistantMemoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AssistantMemoryDao {

    @Query("SELECT * FROM assistant_memories ORDER BY importance DESC, updatedAt DESC")
    fun observeAll(): Flow<List<AssistantMemoryEntity>>

    @Query("SELECT * FROM assistant_memories ORDER BY importance DESC, updatedAt DESC")
    suspend fun getAll(): List<AssistantMemoryEntity>

    @Insert
    suspend fun insert(memory: AssistantMemoryEntity): Long

    @Update
    suspend fun update(memory: AssistantMemoryEntity)

    @Query("DELETE FROM assistant_memories WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM assistant_memories WHERE importance <= 2 AND updatedAt < :before")
    suspend fun deleteOldLowImportance(before: Long)
}
