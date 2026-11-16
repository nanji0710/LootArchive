package com.nanji.lootarchive.data.repository

import com.nanji.lootarchive.data.assistant.AnswerBlock
import com.nanji.lootarchive.data.assistant.ChatHistoryCodec
import com.nanji.lootarchive.data.local.dao.AssistantMessageDao
import com.nanji.lootarchive.data.local.dao.ConversationDao
import com.nanji.lootarchive.data.local.entity.AssistantMessageEntity
import com.nanji.lootarchive.data.local.entity.ConversationEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/** AI 沟通历史：所有会话和消息都保存在本地 Room，不上传。 */
@Singleton
class AssistantHistoryRepository @Inject constructor(
    private val conversationDao: ConversationDao,
    private val messageDao: AssistantMessageDao,
) {
    fun conversations(): Flow<List<ConversationEntity>> = conversationDao.getAll()

    suspend fun latestConversation(): ConversationEntity? = conversationDao.latest()

    suspend fun createConversation(title: String): Long =
        conversationDao.insert(ConversationEntity(title = title.ifBlank { "新对话" }))

    suspend fun touchConversation(id: Long, title: String) {
        conversationDao.touch(id, title)
    }

    fun messages(conversationId: Long): Flow<List<AssistantMessageEntity>> =
        messageDao.getByConversation(conversationId)

    suspend fun listMessages(conversationId: Long): List<AssistantMessageEntity> =
        messageDao.listByConversation(conversationId)

    suspend fun appendMessage(
        conversationId: Long,
        fromUser: Boolean,
        text: String,
        blocks: List<AnswerBlock> = emptyList(),
        suggestions: List<String> = emptyList(),
        error: Boolean = false,
    ) {
        messageDao.insert(
            AssistantMessageEntity(
                conversationId = conversationId,
                fromUser = fromUser,
                text = text,
                blocksJson = ChatHistoryCodec.encodeBlocks(blocks),
                suggestionsJson = ChatHistoryCodec.encodeSuggestions(suggestions),
                error = error,
            )
        )
    }

    suspend fun deleteConversations(ids: List<Long>) {
        if (ids.isNotEmpty()) conversationDao.deleteByIds(ids)
    }

    suspend fun clearMessages(conversationId: Long) {
        messageDao.deleteByConversation(conversationId)
    }
}
