package com.nanji.lootarchive.data.repository

import com.nanji.lootarchive.data.local.dao.AssistantMemoryDao
import com.nanji.lootarchive.data.local.entity.AssistantMemoryEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 长期记忆仓库（轻量结构化记忆，不做向量检索）。
 *
 * 检索方式：把当前问题拆成关键词，和记忆的 content / keywords 做包含匹配，
 * 按重要度与更新时间排序，取前 6 条注入 System Prompt。记忆量很小，本地即可完成。
 */
@Singleton
class AssistantMemoryRepository @Inject constructor(
    private val dao: AssistantMemoryDao,
) {
    fun observeAll(): Flow<List<AssistantMemoryEntity>> = dao.observeAll()

    suspend fun all(): List<AssistantMemoryEntity> = dao.getAll()

    suspend fun relevant(query: String, limit: Int = 6): List<AssistantMemoryEntity> {
        val q = query.trim()
        if (q.isBlank()) return dao.getAll().take(limit)
        val words = buildList {
            add(q)
            q.split(Regex("[\\s，,。？?！!、；;：:]+")).filter { it.length >= 2 }.forEach { add(it) }
            // 中文没有空格，补 2 字片段，避免「平板多少钱」匹配不到「用户有一台平板」
            if (q.length >= 4) {
                for (i in 0..q.length - 2) add(q.substring(i, i + 2))
            }
        }.distinct()
        return dao.getAll()
            .map { memory ->
                val haystack = memory.content + " " + memory.keywords
                val score = words.count { it.isNotBlank() && haystack.contains(it, ignoreCase = true) }
                memory to score
            }
            .filter { it.second > 0 }
            .sortedWith(compareByDescending<Pair<AssistantMemoryEntity, Int>> { it.second }
                .thenByDescending { it.first.importance }
                .thenByDescending { it.first.updatedAt })
            .take(limit)
            .map { it.first }
    }

    /**
     * 写入一条长期记忆：内容相同或高度相似时合并，避免同一句反复存。
     * 不做语义向量，先用「去掉标点后的包含关系」做去重。
     */
    suspend fun remember(
        type: String,
        content: String,
        keywords: String = "",
        importance: Int = 3,
    ) {
        val clean = content.trim().take(200).trimEnd('。', '，', ',', '.')
        if (clean.length < 2) return
        val safeKeywords = keywords.trim().take(200)
        val existing = dao.getAll().firstOrNull { memory ->
            val old = normalize(memory.content)
            old == normalize(clean) || old.contains(normalize(clean)) || normalize(clean).contains(old)
        }
        val now = System.currentTimeMillis()
        if (existing != null) {
            val mergedContent = if (clean.length > existing.content.length) clean else existing.content
            dao.update(
                existing.copy(
                    content = mergedContent,
                    keywords = (existing.keywords + "," + safeKeywords).trim(',').take(400),
                    importance = maxOf(existing.importance, importance),
                    updatedAt = now,
                )
            )
        } else {
            dao.insert(
                AssistantMemoryEntity(
                    type = type,
                    content = clean,
                    keywords = safeKeywords,
                    importance = importance.coerceIn(1, 5),
                    createdAt = now,
                    updatedAt = now,
                )
            )
        }
    }

    /** 每 90 天清一次低重要度旧记忆，避免记忆无限膨胀。 */
    suspend fun prune() {
        dao.deleteOldLowImportance(System.currentTimeMillis() - 90L * 24 * 60 * 60 * 1000)
    }

    private fun normalize(text: String): String =
        text.replace(Regex("[\\s，,。？?！!、；;：:]"), "").lowercase()
}
