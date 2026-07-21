package com.nanji.lootarchive.ui.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nanji.lootarchive.data.local.entity.BackupRecordEntity
import com.nanji.lootarchive.data.repository.BackupRepository
import com.nanji.lootarchive.data.repository.ItemRepository
import com.nanji.lootarchive.util.ExcelUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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
                    it.copy(isLoading = false, isSuccess = true, message = "数据库备份成功: ${record.fileName}")
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
                    it.copy(isLoading = false, isSuccess = true, message = "照片备份成功: ${zipFile.name}")
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
                    it.copy(isLoading = false, isSuccess = true, message = "Excel导出成功: ${file.name}")
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, isSuccess = false, message = "导出失败: ${e.message}")
                }
            }
        }
    }

    fun importFromExcel(filePath: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = null) }
            try {
                val items = ExcelUtil.importItemsFromExcel(File(filePath))
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

    fun restoreDatabase(filePath: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = null) }
            try {
                backupRepository.restoreDatabase(filePath)
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

    fun deleteRecord(record: BackupRecordEntity) {
        viewModelScope.launch {
            backupRepository.deleteRecord(record)
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
