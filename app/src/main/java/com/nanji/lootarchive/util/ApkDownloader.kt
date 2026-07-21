package com.nanji.lootarchive.util

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast

object ApkDownloader {

    private var lastDownloadId = -1L

    fun download(context: Context, url: String, fileName: String) {
        try {
            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

            // 清理旧下载的APK文件
            try {
                val dir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS)
                dir?.listFiles()?.filter { it.name.startsWith("LootArchive-v") && it.name.endsWith(".apk") }
                    ?.forEach { it.delete() }
            } catch (_: Exception) {}

            // 防重复点击
            if (lastDownloadId > 0) {
                try { dm.remove(lastDownloadId) } catch (_: Exception) {}
            }

            val request = DownloadManager.Request(Uri.parse(url))
                .setTitle("拾物集 更新")
                .setDescription("下载完成后点击通知安装")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            lastDownloadId = dm.enqueue(request)
            Toast.makeText(context, "已开始下载，请查看通知栏", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "下载失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
