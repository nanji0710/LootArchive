package com.nanji.lootarchive.data.repository

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import com.nanji.lootarchive.data.local.dao.BackupRecordDao
import com.nanji.lootarchive.data.local.entity.BackupRecordEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/** 一次导出的落盘结果。 */
data class ExportFile(
    /** 记录进备份记录的定位符：Download 走 content:// URI，兜底走绝对路径。 */
    val path: String,
    /** 实际写入的文件名（MediaStore 遇到重名会自动加序号，这里回读真实值）。 */
    val displayName: String,
    /** true = 落在系统 Download（下载）目录；false = 落在应用私有目录。 */
    val inDownloads: Boolean
)

@Singleton
class BackupRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backupRecordDao: BackupRecordDao
) {
    companion object {
        private const val TAG = "BackupRepository"
        const val BACKUP_DIR = "备份"
        const val EXPORT_DIR = "导出"

        /** Download 内的子目录，避免把文件直接堆在下载根目录。 */
        const val DOWNLOAD_SUBDIR = "LootArchive"
    }

    /** 兜底目录：MediaStore 写入失败时才用（Android/data/.../files/导出）。 */
    val exportDir: File
        get() = File(context.getExternalFilesDir(null), EXPORT_DIR).also { it.mkdirs() }

    private val backupDir: File
        get() = File(context.getExternalFilesDir(null), BACKUP_DIR).also { it.mkdirs() }

    private val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

    // ========== 导出落盘 ==========

    /**
     * 把备份 ZIP 写进手机本地文件系统的 **Download（下载）目录**，用户可在系统
     * 文件管理器里直接找到。
     *
     * 走 MediaStore 而不是直接 File 写 /storage/emulated/0/Download：
     * Android 10（API 29）起分区存储，应用对公共目录只能通过 MediaStore 写入，
     * 直接 File 写会 EACCES。MediaStore.Downloads 由系统代建文件，不需要任何
     * 存储权限（WRITE_EXTERNAL_STORAGE 在清单里已被 maxSdkVersion=29 限制）。
     *
     * 用 IS_PENDING 标记：写完之前对别的应用不可见，避免用户或扫描器读到半截 ZIP。
     * 任何一步失败都回落到应用私有目录，保证"导出"不会因为存储策略差异直接失败。
     */
    suspend fun exportToDownloads(fileName: String, write: (OutputStream) -> Unit): ExportFile {
        val uri = runCatching { createDownloadEntry(fileName) }.getOrNull()
        if (uri != null) {
            try {
                val out = context.contentResolver.openOutputStream(uri)
                    ?: throw java.io.IOException("无法打开下载目录输出流")
                out.use { write(it) }
                publishPending(uri)
                return ExportFile(
                    path = uri.toString(),
                    displayName = queryDisplayName(uri) ?: fileName,
                    inDownloads = true
                )
            } catch (e: Exception) {
                // 半成品要从下载目录里清掉，别留一个坏 ZIP 让用户以为是好的
                android.util.Log.w(TAG, "写入下载目录失败，回落到私有目录", e)
                runCatching { context.contentResolver.delete(uri, null, null) }
            }
        }
        return exportToPrivateDir(fileName, write)
    }

    private fun exportToPrivateDir(fileName: String, write: (OutputStream) -> Unit): ExportFile {
        val file = File(exportDir, fileName)
        try {
            FileOutputStream(file).use { write(it) }
        } catch (e: Exception) {
            file.delete() // 别留半截 ZIP
            throw e
        }
        return ExportFile(path = file.absolutePath, displayName = file.name, inDownloads = false)
    }

    private fun createDownloadEntry(fileName: String): Uri {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/zip")
            put(
                MediaStore.MediaColumns.RELATIVE_PATH,
                Environment.DIRECTORY_DOWNLOADS + File.separator + DOWNLOAD_SUBDIR
            )
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        return context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: throw java.io.IOException("下载目录不可写")
    }

    private fun publishPending(uri: Uri) {
        val values = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
        context.contentResolver.update(uri, values, null, null)
    }

    private fun queryDisplayName(uri: Uri): String? = runCatching {
        context.contentResolver
            .query(uri, arrayOf(MediaStore.MediaColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
    }.getOrNull()

    // ========== 数据库备份 ==========

    suspend fun backupDatabase(): BackupRecordEntity {
        val dbFile = context.getDatabasePath("lootarchive.db")
        val timestamp = dateFormat.format(Date())
        val backupFileName = "LootArchive_backup_$timestamp.db"
        val backupFile = File(backupDir, backupFileName)

        FileInputStream(dbFile).use { input ->
            FileOutputStream(backupFile).use { output ->
                input.copyTo(output)
            }
        }

        val record = BackupRecordEntity(
            fileName = backupFileName,
            backupType = "database",
            filePath = backupFile.absolutePath
        )
        val id = backupRecordDao.insertRecord(record)
        return record.copy(id = id)
    }

    suspend fun restoreDatabase(filePath: String) {
        val dbFile = context.getDatabasePath("lootarchive.db")
        val sourceFile = File(filePath)

        FileInputStream(sourceFile).use { input ->
            FileOutputStream(dbFile).use { output ->
                input.copyTo(output)
            }
        }
    }

    suspend fun restorePhotos(zipFilePath: String, targetDir: File) {
        java.util.zip.ZipInputStream(FileInputStream(File(zipFilePath))).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val file = resolveSafeFile(targetDir, entry.name)
                if (file != null) {
                    FileOutputStream(file).use { fos ->
                        zis.copyTo(fos)
                    }
                } // null → 跳过越界/绝对路径的恶意 entry
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }

    // ========== 记录管理 ==========

    fun getAllRecords(): Flow<List<BackupRecordEntity>> = backupRecordDao.getAllRecords()

    suspend fun saveExcelExportRecord(fileName: String, filePath: String, itemCount: Int): BackupRecordEntity {
        val record = BackupRecordEntity(
            fileName = fileName,
            backupType = "excel",
            filePath = filePath,
            itemCount = itemCount
        )
        val id = backupRecordDao.insertRecord(record)
        return record.copy(id = id)
    }

    suspend fun deleteRecord(record: BackupRecordEntity) {
        // 导出到 Download 的记录存的是 content:// URI，不能当文件路径删
        if (record.filePath.startsWith("content://")) {
            runCatching { context.contentResolver.delete(Uri.parse(record.filePath), null, null) }
                .onFailure { android.util.Log.w(TAG, "删除备份文件失败: ${record.filePath}", it) }
        } else {
            File(record.filePath).delete()
        }
        backupRecordDao.deleteRecord(record)
    }
}

/**
 * 安全地将 zip entry 解析到 targetDir 下。返回 null 拒绝越界/绝对路径。
 * 非 null 时保证父目录已创建。
 */
internal fun resolveSafeFile(targetDir: File, entryName: String): File? {
    // 统一为规范路径，拒绝反斜杠分隔符
    val safeName = entryName.replace('\\', '/')
    // zip entry 名不允许为绝对路径（Windows/Linux 平台行为不一致，主动拦截更严谨）
    if (safeName.startsWith("/")) return null
    val targetPath = targetDir.canonicalPath
    val candidate = File(targetDir, safeName)
    val resolved = candidate.canonicalPath
    return if (resolved.startsWith(targetPath + File.separator)) {
        candidate.parentFile?.mkdirs()
        candidate
    } else null
}
