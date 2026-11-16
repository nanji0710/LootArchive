package com.nanji.lootarchive.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 跨会话的长期记忆：用户的偏好、固定事实、重要事件。
 * 只存短句，不存整段对话；由模型定期从对话中提取。
 */
@Entity(tableName = "assistant_memories")
data class AssistantMemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** preference / fact / event */
    val type: String = "fact",
    val content: String,
    /** 逗号分隔的关键词，用于本地粗检索 */
    val keywords: String = "",
    /** 1–5，越高越优先注入，也越不容易被清理 */
    val importance: Int = 3,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
