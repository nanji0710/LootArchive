package com.nanji.lootarchive.ui.backup

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nanji.lootarchive.data.local.entity.BackupRecordEntity
import com.nanji.lootarchive.data.repository.BackupRepository
import com.nanji.lootarchive.data.repository.ItemRepository
import com.nanji.lootarchive.util.ExcelUtil
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

    fun exportToExcel() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = null) }
            try {
                val items = itemRepository.getAllItems().first()
                val dir = backupRepository.exportDir
                if (!dir.exists()) dir.mkdirs()
                val file = ExcelUtil.exportItemsToExcel(items, dir)
                _uiState.update {
                    it.copy(isLoading = false, isSuccess = true,
                        message = "Excel导出成功\n文件: ${file.name}\n位置: ${dir.absolutePath}")
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, isSuccess = false, message = "导出失败: ${e.message}")
                }
            }
        }
    }

    fun importFromExcel(uriString: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = null) }
            try {
                val uri = Uri.parse(uriString)
                val fileName = getFileName(uri) ?: ""
                if (!fileName.endsWith(".xlsx", ignoreCase = true) && !fileName.endsWith(".xls", ignoreCase = true)) {
                    _uiState.update { it.copy(isLoading = false, isSuccess = false, message = "文件格式不符合，请选择 .xlsx 或 .xls 文件") }
                    return@launch
                }
                val tempFile = copyUriToTemp(uri, "import_${System.currentTimeMillis()}.xlsx")
                val items = ExcelUtil.importItemsFromExcel(tempFile)
                tempFile.delete()
                items.forEach { itemRepository.insertItem(it) }
                _uiState.update {
                    it.copy(isLoading = false, isSuccess = true, message = "成功导入 ${items.size} 件物品")
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, isSuccess = false, message = "导入失败: ${e.message}")
                }
            }
        }
    }

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
