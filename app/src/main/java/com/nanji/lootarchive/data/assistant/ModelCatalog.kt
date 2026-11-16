package com.nanji.lootarchive.data.assistant

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * 模型目录：用「接口地址 + API Key」向 OpenAI 兼容服务拉取可用模型列表。
 *
 * 只做一件事：`GET {endpoint}/models` → 解析 `data[].id`。
 * 不缓存到磁盘 —— 模型列表属于服务端状态，换了 Key 或服务商就应该重新拉。
 */
object ModelCatalog {

    private fun normalizeBase(endpoint: String): String {
        val trimmed = endpoint.trim().trimEnd('/')
        // 用户可能直接粘贴了完整的 /chat/completions 地址，这里统一退回到根路径
        return trimmed.removeSuffix("/chat/completions")
    }

    suspend fun fetch(endpoint: String, apiKey: String): Result<List<ModelOption>> = withContext(Dispatchers.IO) {
        if (endpoint.isBlank()) return@withContext Result.failure(IllegalArgumentException("请先填写接口地址"))
        if (apiKey.isBlank()) return@withContext Result.failure(IllegalArgumentException("请先填写 API Key"))
        var lastError: Exception? = null
        for (urlText in EndpointUtils.modelsUrls(endpoint)) {
            try {
                val url = URL(urlText)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 10_000
                    readTimeout = 15_000
                    setRequestProperty("Authorization", "Bearer ${apiKey.trim()}")
                    setRequestProperty("Accept", "application/json")
                }
                try {
                    val code = conn.responseCode
                    val body = (if (code in 200..299) conn.inputStream else conn.errorStream)
                        ?.bufferedReader()?.use { it.readText() }.orEmpty()
                    if (code !in 200..299) {
                        lastError = IllegalStateException("服务返回 $code：${body.take(120)}")
                        continue
                    }
                    val data = JSONObject(body).optJSONArray("data")
                    if (data == null) {
                        lastError = IllegalStateException("返回内容缺少 data 数组")
                        continue
                    }
                    val models = buildList {
                        for (i in 0 until data.length()) {
                            val item = data.optJSONObject(i) ?: continue
                            val id = item.optString("id").orEmpty()
                            if (id.isBlank()) continue
                            val levels = ReasoningEffort.extractFromJson(item)
                            add(ModelOption(id = id, reasoningLevels = levels))
                        }
                    }.sortedBy { it.id }
                    if (models.isEmpty()) {
                        lastError = IllegalStateException("该接口没有返回任何模型")
                    } else {
                        return@withContext Result.success(models)
                    }
                } finally {
                    conn.disconnect()
                }
            } catch (e: Exception) {
                lastError = e
            }
        }
        Result.failure(lastError ?: IllegalStateException("模型接口不可用"))
    }
}
