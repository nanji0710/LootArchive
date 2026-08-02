package com.nanji.lootarchive.data.repository

import android.content.Context
import android.os.Environment
import com.nanji.lootarchive.data.local.dao.BackupRecordDao
import com.nanji.lootarchive.data.local.entity.BackupRecordEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backupRecordDao: BackupRecordDao
) {
    companion object {
        const val BACKUP_DIR = "备份"
        const val EXPORT_DIR = "导出"
    }

    val exportDir: File
        get() = File(context.getExternalFilesDir(null), EXPORT_DIR).also { it.mkdirs() }

    private val backupDir: File
        get() = File(context.getExternalFilesDir(null), BACKUP_DIR).also { it.mkdirs() }

    private val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

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
        File(record.filePath).delete()
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
