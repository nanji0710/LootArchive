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
        const val BACKUP_DIR = "LootArchive/备份"
        const val EXPORT_DIR = "LootArchive/导出"
    }

    val backupDir: File
        get() = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), BACKUP_DIR).also { it.mkdirs() }

    val exportDir: File
        get() = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), EXPORT_DIR).also { it.mkdirs() }

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

    // ========== 照片打包备份 ==========

    suspend fun backupPhotos(photoPaths: List<String>): File {
        val timestamp = dateFormat.format(Date())
        val zipFileName = "LootArchive_photos_$timestamp.zip"
        val zipFile = File(backupDir, zipFileName)

        java.util.zip.ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            photoPaths.forEach { path ->
                val file = File(path)
                if (file.exists()) {
                    val entry = java.util.zip.ZipEntry(file.name)
                    zos.putNextEntry(entry)
                    FileInputStream(file).use { it.copyTo(zos) }
                    zos.closeEntry()
                }
            }
        }

        val record = BackupRecordEntity(
            fileName = zipFileName,
            backupType = "photos",
            filePath = zipFile.absolutePath,
            itemCount = photoPaths.size
        )
        backupRecordDao.insertRecord(record)

        return zipFile
    }

    suspend fun restorePhotos(zipFilePath: String, targetDir: File) {
        java.util.zip.ZipInputStream(FileInputStream(File(zipFilePath))).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val file = File(targetDir, entry.name)
                FileOutputStream(file).use { fos ->
                    zis.copyTo(fos)
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }

    // ========== 记录管理 ==========

    fun getAllRecords(): Flow<List<BackupRecordEntity>> = backupRecordDao.getAllRecords()

    suspend fun deleteRecord(record: BackupRecordEntity) {
        File(record.filePath).delete()
        backupRecordDao.deleteRecord(record)
    }
}
