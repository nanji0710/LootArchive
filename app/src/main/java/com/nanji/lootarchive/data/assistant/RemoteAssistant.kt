package com.nanji.lootarchive.data.assistant

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * 可选的 OpenAI 兼容接口客户端（BYOK：用户自己填地址/密钥/模型）。
 *
 * 三条硬约束，写在这里而不是调用点：
 * 1. **只有用户把 endpoint + apiKey + model 都配置了才会发请求**（[Config.isReady]），
 *    默认为空 → 助理保持纯本地，App 的离线可用性不受影响；
 * 2. 请求体里只带 [AssistantEngine.buildInventoryDigest] 生成的库存摘要 ——
 *    只含名称/分类/价格/状态/购买日期/保修到期/存放位置/标签，
 *    **不含照片路径、备注、数据库 id**；密钥只放在请求头，不落日志；
 * 3. 一切异常（超时、HTTP 错误、返回体解析失败）都收敛成 [Result.failure]，
 *    由 ViewModel 回退到本地分析，界面不会因为模型不可用而空着。
 *
 * 实现只用 [HttpURLConnection] + `org.json`（Android 平台自带），
 * 不新增任何第三方网络/序列化依赖。
 */
class RemoteAssistant {

    /** OpenCode Go 要求每个会话带上稳定的 x-opencode-session，用随机 UUID 作本进程会话 ID。 */
    private val sessionId = java.util.UUID.randomUUID().toString()

    /** 模型的一次回复：最终回答 + 可选思考过程（reasoning_content）。 */
    data class RemoteReply(
        val content: String,
        val reasoning: String = "",
    )

    /** 用户在设置页填的模型接入参数。空 [endpoint]/[apiKey]/[model] 任意一项都不算配置好。 */
    data class Config(
        val endpoint: String = "",
        val apiKey: String = "",
        val model: String = "",
        val reasoningEffort: String = "",
    ) {
        val isReady: Boolean
            get() = endpoint.isNotBlank() && apiKey.isNotBlank() && model.isNotBlank()
    }

    /**
     * 向 `{endpoint}/chat/completions` 发一次问答请求。
     *
     * @param inventoryDigest 紧凑库存摘要 JSON，由 [AssistantEngine.buildInventoryDigest] 生成
     * @return 成功时是模型返回的正文（已 trim）；失败原因不包含密钥
     */
    suspend fun ask(
        config: Config,
        question: String,
        inventoryDigest: String,
    ): Result<RemoteReply> = withContext(Dispatchers.IO) {
        if (!config.isReady) {
            return@withContext Result.failure(IllegalStateException("未配置模型接口"))
        }
        val body = buildRequestBody(config.model, question, inventoryDigest, config.reasoningEffort)
        var lastError: Exception? = null
        // 先按用户填的地址请求；失败再试补 /v1 的地址（兼容 Magpie、Ollama、LM Studio 等本地网关）
        for (urlText in EndpointUtils.completionsUrls(config.endpoint)) {
            val connection = try {
                (URL(urlText).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = CONNECT_TIMEOUT_MS
                    readTimeout = READ_TIMEOUT_MS
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("Authorization", "Bearer ${config.apiKey.trim()}")
                    setRequestProperty("x-opencode-session", sessionId)
                    setRequestProperty("User-Agent", "LootArchive/${com.nanji.lootarchive.BuildConfig.VERSION_NAME}")
                }
            } catch (e: Exception) {
                Log.w(TAG, "模型接口地址无效", e)
                lastError = e
                continue
            }
            try {
                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                val code = connection.responseCode
                if (code !in 200..299) {
                    val errorText = connection.errorStream?.bufferedReader()?.use { it.readText() }?.take(200).orEmpty()
                    Log.w(TAG, "模型接口返回 HTTP $code: $errorText")
                    lastError = IllegalStateException("HTTP $code")
                    continue
                }
                val raw = connection.inputStream.bufferedReader().use { it.readText() }
                val reply = parseReply(raw)
                if (reply != null && (reply.content.isNotBlank() || reply.reasoning.isNotBlank())) {
                    return@withContext Result.success(
                        reply.copy(content = reply.content.ifBlank { "（模型只返回了思考过程，没有给出最终回答）" })
                    )
                }
                lastError = IllegalStateException("模型返回内容为空")
            } catch (e: Exception) {
                Log.w(TAG, "模型请求失败", e)
                lastError = e
            } finally {
                connection.disconnect()
            }
        }
        Result.failure(lastError ?: IllegalStateException("模型接口不可用"))
    }

    /** 把用户填的地址规范化成 `.../chat/completions`，容忍结尾斜杠与「已经填完整路径」两种写法。 */
    private fun completionsUrl(endpoint: String): String {
        val trimmed = endpoint.trim().trimEnd('/')
        return if (trimmed.endsWith("/chat/completions")) trimmed else "$trimmed/chat/completions"
    }

    /** 请求体：model + messages(system, user) + temperature 0.6（要稳定、可复述的回答，不用会跑偏的高温）。 */
    private fun buildRequestBody(
        model: String,
        question: String,
        inventoryDigest: String,
        reasoningEffort: String,
    ): String {
        val messages = JSONArray()
        messages.put(JSONObject().put("role", "system").put("content", systemPrompt(inventoryDigest)))
        messages.put(JSONObject().put("role", "user").put("content", question))
        val body = JSONObject()
            .put("model", model.trim())
            .put("messages", messages)
            .put("temperature", 0.6)
            .put("stream", false)
        // off / 空 = 不发送该字段，按服务端默认；其余按模型支持的档位原样传
        val effort = reasoningEffort.trim().lowercase()
        if (effort.isNotBlank() && effort != "off" && effort != "none") {
            body.put("reasoning_effort", reasoningEffort.trim())
        }
        return body.toString()
    }

    /**
     * system 提示：把模型限定成「只看这份摘要的物品助手」。
     *
     * 明确写「不要编造摘要里没有的物品」很关键：模型手里没有照片、没有备注，
     * 一旦开始自由发挥，用户会看到不存在的物品名，反而比本地规则更糟。
     */
    private fun systemPrompt(inventoryDigest: String): String = buildString {
        append("你是「拾物集」App 里的物品管理助手。")
        append("用户的所有数据都保存在这台手机上，下面这份库存摘要是你唯一可用的资料，")
        append("不要编造摘要里没有的物品，也不要猜测摘要里没有的字段。")
        append("只回答与物品、资产、保修、存放位置、整理建议相关的问题；与物品无关的话题，简短说明你只懂物品管理。")
        append("用简体中文、口语化回答，尽量简短（不超过 120 字），金额写成 ¥1,234 这种千分位格式，不要输出 Markdown 表格。")
        append("库存摘要（JSON）：")
        append(inventoryDigest)
    }

    /** 解析 `choices[0].message.content` 与 `reasoning_content`（不同网关也可能叫 reasoning）。 */
    private fun parseReply(raw: String): RemoteReply? = runCatching {
        val choices = JSONObject(raw).optJSONArray("choices") ?: return null
        val message = choices.optJSONObject(0)?.optJSONObject("message") ?: return null
        val content = message.optString("content").trim()
        val reasoning = message.optString("reasoning_content")
            .ifBlank { message.optString("reasoning") }
            .trim()
        RemoteReply(content = content, reasoning = reasoning)
    }.getOrNull()

    private companion object {
        private const val TAG = "RemoteAssistant"
        private const val CONNECT_TIMEOUT_MS = 10_000
        private const val READ_TIMEOUT_MS = 30_000
    }
}
