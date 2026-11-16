package com.nanji.lootarchive.data.assistant

/**
 * 兼容不同 OpenAI 风格服务的地址写法。
 *
 * 用户可能填：
 * - `http://172.16.12.164:3425`（Magpie 网关首页给的地址）
 * - `http://172.16.12.164:3425/v1`
 * - `https://api.openai.com/v1/chat/completions`（完整地址）
 *
 * 统一先试用户填的地址，再补一个 `/v1` 兜底 —— 大多数本地网关
 * 的 OpenAI 兼容接口都挂在 `/v1` 下，但直接把根地址当接口的也有。
 */
object EndpointUtils {

    fun normalizeBase(endpoint: String): String {
        var base = endpoint.trim().trimEnd('/')
        base = base.removeSuffix("/chat/completions")
        base = base.removeSuffix("/models")
        return base.trimEnd('/')
    }

    /** 依次尝试的根地址（去重、保序）。 */
    fun candidateBases(endpoint: String): List<String> {
        val base = normalizeBase(endpoint)
        return buildList {
            add(base)
            if (!base.endsWith("/v1")) add("$base/v1")
        }.distinct()
    }

    fun modelsUrls(endpoint: String): List<String> =
        candidateBases(endpoint).map { "$it/models" }

    fun completionsUrls(endpoint: String): List<String> =
        candidateBases(endpoint).map { "$it/chat/completions" }
}
