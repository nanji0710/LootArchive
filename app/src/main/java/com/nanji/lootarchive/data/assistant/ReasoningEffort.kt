package com.nanji.lootarchive.data.assistant

import org.json.JSONArray
import org.json.JSONObject

/**
 * 模型的思考强度（reasoning effort）。
 *
 * 不同模型支持的档位不一样：
 * - GPT-5 系列：minimal / low / medium / high
 * - o1 / o3 / o4：low / medium / high
 * - Claude：low / medium / high / max
 * - Gemini：low / medium / high
 * - 未知模型：保守给 off / low / medium / high
 *
 * 优先从 `/models` 返回的元数据里读（不同网关字段名不一样），
 * 读不到再按模型名推断。`off` 永远保留 —— 表示不向接口发送 reasoning_effort。
 */
object ReasoningEffort {

    val DefaultLevels = listOf("low", "medium", "high")

    fun label(key: String): String = when (key.lowercase()) {
        "off", "none" -> "关闭"
        "minimal" -> "极低"
        "low" -> "低"
        "medium" -> "中"
        "high" -> "高"
        "xhigh", "extra_high" -> "极高"
        "max" -> "最高"
        else -> key
    }

    fun defaultLevel(levels: List<String>): String =
        levels.firstOrNull { it == "medium" }
            ?: levels.firstOrNull { it != "off" }
            ?: "off"

    fun inferForModel(modelId: String): List<String> {
        val id = modelId.lowercase()
        // 非对话模型（向量、重排、图像/视频、语音、搜索、抓取等）没有思考档位
        val nonReasoning = listOf(
            "embed", "rerank", "image", "video", "tts", "asr", "audio", "search", "fetch",
            "ocr", "vision", "layout", "web-reader", "web-search",
        )
        if (nonReasoning.any { id.contains(it) }) return emptyList()
        return when {
            id.contains("gpt-5") || id.contains("gpt5") -> listOf("minimal", "low", "medium", "high")
            id.startsWith("o1") || id.startsWith("o3") || id.startsWith("o4") ||
                id.contains("/o1") || id.contains("/o3") || id.contains("/o4") -> listOf("low", "medium", "high")
            id.contains("claude") || id.contains("anthropic") -> listOf("low", "medium", "high", "max")
            id.contains("gemini") -> listOf("low", "medium", "high")
            id.contains("qwen3") || id.contains("qwq") -> listOf("low", "medium", "high")
            id.contains("deepseek") -> listOf("low", "medium", "high")
            else -> DefaultLevels
        }
    }

    /** 从 /models 的单个模型对象里提取思考档位（兼容多种网关字段）。 */
    fun extractFromJson(obj: JSONObject): List<String> {
        val found = linkedSetOf<String>()
        val arrayKeys = listOf(
            "reasoning_efforts", "reasoningEfforts", "reasoning_levels", "reasoningLevels",
            "thinking_levels", "thinkingLevels", "supported_reasoning_efforts",
            "supportedReasoningEfforts", "efforts", "reasoning_effort_values",
        )
        fun collect(o: JSONObject) {
            arrayKeys.forEach { key ->
                o.optJSONArray(key)?.let { array -> found += array.strings() }
            }
        }
        collect(obj)
        listOf("reasoning", "thinking", "capabilities", "metadata", "config").forEach { key ->
            obj.optJSONObject(key)?.let { collect(it) }
        }
        // OpenCode Go / Magpie 的格式：capabilities.reasoning.effort = ["low","high","max"]
        val reasoning = obj.optJSONObject("capabilities")?.optJSONObject("reasoning")
        if (reasoning != null && reasoning.optBoolean("supported", true)) {
            reasoning.optJSONArray("effort")?.let { found += it.strings() }
        }
        // 有些网关用对象数组：supported_reasoning_levels: [{ "effort": "low" }, ...]
        listOf("supported_reasoning_levels", "supportedReasoningLevels").forEach { key ->
            obj.optJSONArray(key)?.let { arr ->
                for (i in 0 until arr.length()) {
                    arr.optJSONObject(i)?.optString("effort")?.takeIf { it.isNotBlank() }?.let { found += it }
                }
            }
        }
        return found.map { it.lowercase() }
            .filter { it in setOf("minimal", "low", "medium", "high", "xhigh", "extra_high", "max") }
            .distinct()
    }

    private fun JSONArray.strings(): List<String> = buildList {
        for (i in 0 until length()) {
            val value = optString(i).trim()
            if (value.isNotBlank()) add(value)
        }
    }
}

/** `/models` 返回的一项：模型 id + 该模型支持的思考档位。 */
data class ModelOption(
    val id: String,
    val reasoningLevels: List<String>,
)
