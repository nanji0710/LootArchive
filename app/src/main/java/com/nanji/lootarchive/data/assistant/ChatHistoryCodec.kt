package com.nanji.lootarchive.data.assistant

import com.nanji.lootarchive.data.local.entity.ItemEntity
import org.json.JSONArray
import org.json.JSONObject

/**
 * 把聊天里的结构化块序列化成 JSON 存库，回来时再还原。
 *
 * 只保存展示所需的物品快照字段（名称/价格/状态/位置/保修到期），
 * 历史记录打开后即使原物品被删除也还能正常显示当时的回答。
 */
object ChatHistoryCodec {

    fun encodeBlocks(blocks: List<AnswerBlock>): String {
        if (blocks.isEmpty()) return ""
        val arr = JSONArray()
        blocks.forEach { block ->
            val obj = JSONObject()
            obj.put("kind", block.kind.name)
            obj.put("title", block.title)
            obj.put("lines", JSONArray(block.lines))
            if (block.items.isNotEmpty()) {
                val items = JSONArray()
                block.items.forEach { item ->
                    items.put(JSONObject().apply {
                        put("id", item.id)
                        put("name", item.name)
                        put("price", item.purchasePrice)
                        put("status", item.status)
                        put("location", item.storageLocation)
                        item.warrantyExpiryDate?.let { put("warranty", it) }
                    })
                }
                obj.put("items", items)
            }
            arr.put(obj)
        }
        return arr.toString()
    }

    fun decodeBlocks(json: String): List<AnswerBlock> {
        if (json.isBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(json)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val kind = runCatching { AnswerBlock.Kind.valueOf(obj.optString("kind")) }
                        .getOrDefault(AnswerBlock.Kind.TEXT)
                    val lines = obj.optJSONArray("lines")?.let { a ->
                        buildList { for (j in 0 until a.length()) add(a.optString(j)) }
                    }.orEmpty()
                    val items = obj.optJSONArray("items")?.let { a ->
                        buildList {
                            for (j in 0 until a.length()) {
                                val o = a.optJSONObject(j) ?: continue
                                add(
                                    ItemEntity(
                                        id = o.optLong("id"),
                                        name = o.optString("name"),
                                        purchasePrice = o.optDouble("price", 0.0),
                                        status = o.optString("status", "active"),
                                        storageLocation = o.optString("location"),
                                        warrantyExpiryDate = if (o.has("warranty")) o.optLong("warranty") else null,
                                    )
                                )
                            }
                        }
                    }.orEmpty()
                    add(AnswerBlock(kind, obj.optString("title"), lines, items))
                }
            }
        }.getOrDefault(emptyList())
    }

    fun encodeSuggestions(suggestions: List<String>): String =
        if (suggestions.isEmpty()) "" else JSONArray(suggestions).toString()

    fun decodeSuggestions(json: String): List<String> {
        if (json.isBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(json)
            buildList { for (i in 0 until arr.length()) add(arr.optString(i)) }
        }.getOrDefault(emptyList())
    }
}
