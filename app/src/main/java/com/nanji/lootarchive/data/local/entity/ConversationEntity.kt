package com.nanji.lootarchive.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** 一次「会话」：标题取首条提问，更新时间用于历史列表排序。 */
@Entity(tableName = "assistant_conversations")
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

/** 会话里的一条消息；blocks/suggestions 以 JSON 字符串保存，避免再建多张表。 */
@Entity(
    tableName = "assistant_messages",
    indices = [Index("conversationId")],
    foreignKeys = [
        androidx.room.ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = androidx.room.ForeignKey.CASCADE,
        )
    ],
)
data class AssistantMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val conversationId: Long,
    val fromUser: Boolean,
    val text: String,
    val blocksJson: String = "",
    val suggestionsJson: String = "",
    val error: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)
