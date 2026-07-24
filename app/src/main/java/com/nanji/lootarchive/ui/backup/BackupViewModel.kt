package com.nanji.lootarchive.ui.backup

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nanji.lootarchive.data.local.entity.BackupRecordEntity
import com.nanji.lootarchive.data.repository.BackupRepository
import com.nanji.lootarchive.data.repository.ItemRepository
import com.nanji.lootarchive.util.BackupUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

data class BackupUiState(
    val isLoading: Boolean = false,
    val backupRecords: List<BackupRecordEntity> = emptyList(),
    val message: String? = null,
    val isSuccess: Boolean = false
)

@HiltViewModel
class BackupViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backupRepository: BackupRepository,
    private val itemRepository: ItemRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            backupRepository.getAllRecords().collect { records ->
                _uiState.update { it.copy(backupRecords = records) }
            }
        }
    }

    fun backupDatabase() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = null) }
            try {
                val record = backupRepository.backupDatabase()
                _uiState.update {
                    it.copy(isLoading = false, isSuccess = true,
                        message = "数据库备份成功\n文件: ${record.fileName}\n位置: ${backupRepository.exportDir.absolutePath}")
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, isSuccess = false, message = "备份失败: ${e.message}")
                }
            }
        }
    }

    fun backupPhotos() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = null) }
            try {
                val photoPaths = itemRepository.getAllPhotoPaths()
                if (photoPaths.isEmpty()) {
                    _uiState.update {
                        it.copy(isLoading = false, message = "没有照片需要备份")
                    }
                    return@launch
                }
                val zipFile = backupRepository.backupPhotos(photoPaths)
                _uiState.update {
                    it.copy(isLoading = false, isSuccess = true,
                        message = "照片备份成功\n文件: ${zipFile.name}\n位置: ${backupRepository.exportDir.absolutePath}")
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, isSuccess = false, message = "照片备份失败: ${e.message}")
                }
            }
        }
    }

    fun fullExport() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = null) }
            try {
                val (items, photos, dir, file) = withContext(Dispatchers.IO) {
                    val items = itemRepository.getAllItems().first()
                    val photos = itemRepository.getAllPhotos()
                    val dir = backupRepository.exportDir
                    if (!dir.exists()) dir.mkdirs()
                    val file = BackupUtil.fullExport(context, items, photos, dir)
                    Quad(items, photos, dir, file)
                }
                backupRepository.saveExcelExportRecord(file.name, file.absolutePath, items.size)
                _uiState.update {
                    it.copy(isLoading = false, isSuccess = true,
                        message = "导出成功\n物品: ${items.size} 件\n照片: ${photos.size} 张\n文件: ${file.name}")
                }
            } catch (e: Throwable) {
                android.util.Log.e("BackupVM", "导出失败", e)
                val msg = when {
                    e.message != null -> e.message!!
                    e is OutOfMemoryError -> "内存不足"
                    else -> "${e.javaClass.simpleName}: ${e.message}"
                }
                _uiState.update {
                    it.copy(isLoading = false, isSuccess = false, message = "导出失败: $msg")
                }
            }
        }
    }

    fun fullImport(uriString: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = null) }
            try {
                val uri = Uri.parse(uriString)
                val importItems = withContext(Dispatchers.IO) {
                    BackupUtil.fullImport(context, uri)
                }
                var itemCount = 0
                var photoCount = 0
                for (ii in importItems) {
                    val itemId = itemRepository.insertItem(ii.item)
                    if (ii.photoFiles.isNotEmpty()) {
                        itemRepository.addPhotosForItem(itemId, ii.photoFiles)
                        photoCount += ii.photoFiles.size
                    }
                    itemCount++
                }
                _uiState.update {
                    it.copy(isLoading = false, isSuccess = true,
                        message = "导入成功\n物品: $itemCount 件\n照片: $photoCount 张\n请退出重进以刷新数据")
                }
            } catch (e: Throwable) {
                android.util.Log.e("BackupVM", "导入失败", e)
                _uiState.update {
                    it.copy(isLoading = false, isSuccess = false, message = "导入失败: ${e.message}")
                }
            }
        }
    }

    // 用于 withContext 返回多值的临时数据类
    private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

    fun restoreDatabase(uriString: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = null) }
            try {
                val uri = Uri.parse(uriString)
                val fileName = getFileName(uri) ?: ""
                if (!fileName.endsWith(".db", ignoreCase = true)) {
                    _uiState.update { it.copy(isLoading = false, isSuccess = false, message = "文件格式不符合，请选择 .db 数据库文件") }
                    return@launch
                }
                val tempFile = copyUriToTemp(uri, "restore_${System.currentTimeMillis()}.db")
                backupRepository.restoreDatabase(tempFile.absolutePath)
                tempFile.delete()
                _uiState.update {
                    it.copy(isLoading = false, isSuccess = true, message = "数据库恢复成功，请重启APP")
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, isSuccess = false, message = "恢复失败: ${e.message}")
                }
            }
        }
    }

    // 从 content URI 获取文件名
    private fun getFileName(uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) cursor.getString(idx) else null
                } else null
            }
        } catch (_: Exception) { null }
    }

    // 将 content URI 复制到临时文件
    private suspend fun copyUriToTemp(uri: Uri, tempName: String): File = withContext(Dispatchers.IO) {
        val tempFile = File(context.cacheDir, tempName)
        context.contentResolver.openInputStream(uri)?.use { input ->
            java.io.FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        } ?: throw Exception("无法读取文件")
        tempFile
    }

    fun deleteRecord(record: BackupRecordEntity) {
        viewModelScope.launch {
            backupRepository.deleteRecord(record)
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
