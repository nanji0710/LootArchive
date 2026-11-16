package com.nanji.lootarchive.data.local.dao

import androidx.room.*
import com.nanji.lootarchive.data.local.entity.AssistantMessageEntity
import com.nanji.lootarchive.data.local.entity.ConversationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Query("SELECT * FROM assistant_conversations ORDER BY updatedAt DESC")
    fun getAll(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM assistant_conversations ORDER BY updatedAt DESC LIMIT 1")
    suspend fun latest(): ConversationEntity?

    @Insert
    suspend fun insert(conversation: ConversationEntity): Long

    @Query("UPDATE assistant_conversations SET title = :title, updatedAt = :updatedAt WHERE id = :id")
    suspend fun touch(id: Long, title: String, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM assistant_conversations WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)
}

@Dao
interface AssistantMessageDao {
    @Query("SELECT * FROM assistant_messages WHERE conversationId = :conversationId ORDER BY createdAt ASC, id ASC")
    fun getByConversation(conversationId: Long): Flow<List<AssistantMessageEntity>>

    @Query("SELECT * FROM assistant_messages WHERE conversationId = :conversationId ORDER BY createdAt ASC, id ASC")
    suspend fun listByConversation(conversationId: Long): List<AssistantMessageEntity>

    @Insert
    suspend fun insert(message: AssistantMessageEntity): Long

    @Query("DELETE FROM assistant_messages WHERE conversationId = :conversationId")
    suspend fun deleteByConversation(conversationId: Long)
}
