package com.nanji.lootarchive.ui.backup

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nanji.lootarchive.data.local.entity.BackupRecordEntity
import com.nanji.lootarchive.data.repository.BackupRepository
import com.nanji.lootarchive.data.repository.CategoryRepository
import com.nanji.lootarchive.data.repository.ItemRepository
import com.nanji.lootarchive.util.BackupUtil
import com.nanji.lootarchive.util.Quintet
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
    private val itemRepository: ItemRepository,
    private val categoryRepository: CategoryRepository,
    private val expService: com.nanji.lootarchive.data.ExpService
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

    fun fullExport() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = null) }
            try {
                val (items, photos, categories, dir, file) = withContext(Dispatchers.IO) {
                    val items = itemRepository.getAllItems().first()
                    val photos = itemRepository.getAllPhotos()
                    val categories = categoryRepository.getAllCategories().first()
                    val dir = backupRepository.exportDir
                    if (!dir.exists()) dir.mkdirs()
                    val file = BackupUtil.fullExport(context, items, photos, categories, dir)
                    Quintet(items, photos, categories, dir, file)
                }
                backupRepository.saveExcelExportRecord(file.name, file.absolutePath, items.size)
                _uiState.update {
                    it.copy(isLoading = false, isSuccess = true,
                        message = "导出成功\n物品: ${items.size} 件\n照片: ${photos.size} 张\n分类: ${categories.size} 个\n文件: ${file.name}")
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
                val importResult = withContext(Dispatchers.IO) {
                    BackupUtil.fullImport(context, uri)
                }
                // Build name→ID map from current DB categories
                val existingCats = categoryRepository.getAllCategories().first()
                val nameToId = mutableMapOf<String, Long>()
                existingCats.forEach { nameToId[it.name] = it.id }

                // Import missing categories, track their new IDs
                var catCount = 0
                for (cat in importResult.categories) {
                    if (!nameToId.containsKey(cat.name)) {
                        val newId = categoryRepository.createCategory(cat.name)
                        nameToId[cat.name] = newId
                        catCount++
                    }
                }

                // Import items — remap categoryId by name lookup
                var itemCount = 0
                var photoCount = 0
                for (ii in importResult.items) {
                    // Find new category ID by looking up the old category ID's name
                    val oldCat = importResult.categories.firstOrNull { it.id == ii.item.categoryId }
                    val newCatId = if (oldCat != null && nameToId.containsKey(oldCat.name)) {
                        nameToId[oldCat.name]!!
                    } else {
                        // Fallback: try to use the categoryId directly
                        ii.item.categoryId
                    }
                    val itemToSave = ii.item.copy(categoryId = newCatId)
                    val itemId = itemRepository.insertItem(itemToSave)
                    if (ii.photoFiles.isNotEmpty()) {
                        itemRepository.addPhotosForItem(itemId, ii.photoFiles)
                        photoCount += ii.photoFiles.size
                    }
                    expService.recordAddItem(itemId, itemToSave.purchasePrice, itemToSave.description.isNotBlank(), ii.photoFiles.size)
                    itemCount++
                }
                _uiState.update {
                    it.copy(isLoading = false, isSuccess = true,
                        message = "导入成功\n分类: $catCount 个\n物品: $itemCount 件\n照片: $photoCount 张\n请退出重进以刷新数据")
                }
            } catch (e: Throwable) {
                android.util.Log.e("BackupVM", "导入失败", e)
                _uiState.update {
                    it.copy(isLoading = false, isSuccess = false, message = "导入失败: ${e.message}")
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
