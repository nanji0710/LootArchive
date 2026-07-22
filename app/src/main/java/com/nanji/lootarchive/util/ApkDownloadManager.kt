package com.nanji.lootarchive.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class ApkDownloadManager(private val context: Context) {

    data class Progress(
        val downloaded: Long = 0L,
        val total: Long = 0L,
        val percentage: Int = 0,
        val speedKBps: Double = 0.0
    ) {
        val sizeText: String
            get() {
                if (total <= 0) return "${formatSize(downloaded)} / ???"
                return "${formatSize(downloaded)} / ${formatSize(total)}"
            }
        val speedText: String
            get() = "${"%.1f".format(speedKBps)} KB/s"
        val percentText: String
            get() = "${percentage}%"
    }

    suspend fun download(
        url: String,
        fileName: String,
        onProgress: (Progress) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            // 清理旧 APK
            val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!
            if (!dir.exists()) dir.mkdirs()
            dir.listFiles()?.filter { it.name.startsWith("LootArchive-v") && it.name.endsWith(".apk") }
                ?.forEach { it.delete() }

            val file = File(dir, fileName)

            connection = URL(url).openConnection() as HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 30000
            connection.requestMethod = "GET"

            val totalSize = connection.contentLength.toLong()
            var downloaded = 0L
            val buffer = ByteArray(8192)
            var lastUpdateTime = System.currentTimeMillis()
            var lastUpdateBytes = 0L

            connection.inputStream.use { input ->
                FileOutputStream(file).use { output ->
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloaded += bytesRead

                        val now = System.currentTimeMillis()
                        val elapsed = now - lastUpdateTime
                        if (elapsed >= 200) { // 每 200ms 更新一次进度
                            val speed = if (elapsed > 0) {
                                ((downloaded - lastUpdateBytes).toDouble() / 1024) / (elapsed / 1000.0)
                            } else 0.0
                            val pct = if (totalSize > 0) ((downloaded * 100) / totalSize).toInt() else 0
                            onProgress(Progress(downloaded, totalSize, pct, speed))
                            lastUpdateTime = now
                            lastUpdateBytes = downloaded
                        }
                    }
                }
            }
            connection.disconnect()

            // 最终进度
            val total = if (totalSize > 0) totalSize else file.length()
            onProgress(Progress(file.length(), total, 100, 0.0))
            Result.success(file)
        } catch (e: Exception) {
            connection?.disconnect()
            Result.failure(e)
        }
    }

    fun install(file: File) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    companion object {
        private fun formatSize(bytes: Long): String = when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> "${"%.1f".format(bytes.toDouble() / (1024 * 1024))} MB"
        }
    }
}
