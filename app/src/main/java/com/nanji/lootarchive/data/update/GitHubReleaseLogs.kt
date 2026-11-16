package com.nanji.lootarchive.data.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

/** GitHub Release 里的一条版本记录。 */
data class GitHubReleaseLog(
    val version: String,
    val date: String,
    val body: String,
)

/**
 * 直接读 GitHub Releases API 的历史更新日志。
 *
 * 每次发版由 GitHub Actions 自动创建 Release，Release 的 body 就是 version.json 里的
 * updateLog。历史页直接拉这个接口，不再需要在 App 里手写一份版本日志。
 */
object GitHubReleaseLogs {

    private const val API_URL =
        "https://api.github.com/repos/nanji0710/LootArchive/releases?per_page=50"

    suspend fun fetch(): Result<List<GitHubReleaseLog>> = withContext(Dispatchers.IO) {
        try {
            val conn = (URL(API_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 15_000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "LootArchive/${com.nanji.lootarchive.BuildConfig.VERSION_NAME}")
            }
            try {
                val code = conn.responseCode
                if (code !in 200..299) {
                    val error = conn.errorStream?.bufferedReader()?.use { it.readText() }?.take(160).orEmpty()
                    return@withContext Result.failure(IllegalStateException("GitHub API $code：$error"))
                }
                val json = conn.inputStream.bufferedReader().use { it.readText() }
                val array = JSONArray(json)
                val releases = buildList {
                    for (i in 0 until array.length()) {
                        val item = array.optJSONObject(i) ?: continue
                        if (item.optBoolean("draft", false)) continue
                        val tag = item.optString("tag_name").trim().trimStart('v', 'V')
                        if (tag.isBlank()) continue
                        add(
                            GitHubReleaseLog(
                                version = tag,
                                date = item.optString("published_at").take(10),
                                body = item.optString("body").trim(),
                            )
                        )
                    }
                }
                Result.success(releases)
            } finally {
                conn.disconnect()
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
