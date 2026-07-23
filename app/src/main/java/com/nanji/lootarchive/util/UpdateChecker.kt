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
                    Result.success(UpdateInfo(
                        versionName = obj.getString("versionName"),
                        versionCode = remoteCode,
                        updateDate = obj.optString("updateDate", ""),
                        updateLog = obj.optString("updateLog", ""),
                        apkDownloadUrl = obj.optString("apkDownloadUrl", "")
                    ))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
