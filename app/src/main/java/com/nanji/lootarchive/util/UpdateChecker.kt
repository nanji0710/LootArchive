package com.nanji.lootarchive.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val versionName: String,
    val versionCode: Int,
    val updateDate: String,
    val updateLog: String,
    val apkDownloadUrl: String
)

private val ALLOWED_DOWNLOAD_HOSTS = setOf("github.com", "raw.githubusercontent.com")

/**
 * 校验更新下载 URL：仅允许 HTTPS 且域名在白名单内（防任意 URL 下发恶意 APK）。
 */
internal fun isValidDownloadUrl(url: String): Boolean {
    return try {
        val u = java.net.URI(url)
        u.scheme == "https" && u.host in ALLOWED_DOWNLOAD_HOSTS
    } catch (e: Exception) {
        false
    }
}

/**
 * 校验版本号格式：`v?数字(.数字){1,3}`（防远端 versionName 拼接路径注入）。
 */
internal fun isValidVersionName(v: String): Boolean =
    Regex("""^v?\d+(\.\d+){1,3}$""").matches(v)

object UpdateChecker {

    private const val VERSION_URL =
        "https://raw.githubusercontent.com/nanji0710/LootArchive/main/version.json"

    suspend fun check(currentVersionCode: Int): Result<UpdateInfo?> {
        return withContext(Dispatchers.IO) {
            try {
                // 加时间戳绕过 GitHub CDN 缓存
                val url = URL("$VERSION_URL?t=${System.currentTimeMillis()}")
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                conn.requestMethod = "GET"

                val json = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()

                val obj = JSONObject(json)
                val remoteCode = obj.getInt("versionCode")

                if (remoteCode <= currentVersionCode) {
                    Result.success(null) // 已是最新
                } else {
                    val versionName = obj.getString("versionName")
                    val apkUrl = obj.optString("apkDownloadUrl", "")
                    if (!isValidVersionName(versionName) || !isValidDownloadUrl(apkUrl)) {
                        Result.failure(IllegalStateException("Invalid update metadata"))
                    } else {
                        Result.success(UpdateInfo(
                            versionName = versionName,
                            versionCode = remoteCode,
                            updateDate = obj.optString("updateDate", ""),
                            updateLog = obj.optString("updateLog", ""),
                            apkDownloadUrl = apkUrl
                        ))
                    }
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
