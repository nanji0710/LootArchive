package com.nanji.lootarchive.ui.backup

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nanji.lootarchive.data.local.entity.BackupRecordEntity
import com.nanji.lootarchive.data.local.entity.ItemEntity
import com.nanji.lootarchive.data.repository.BackupRepository
import com.nanji.lootarchive.data.repository.BackupRepository.Companion.DOWNLOAD_SUBDIR
import com.nanji.lootarchive.data.repository.CategoryRepository
import com.nanji.lootarchive.data.repository.ItemRepository
import com.nanji.lootarchive.util.BackupUtil
import com.nanji.lootarchive.util.Quad
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
                val (items, photos, categories, exportFile) = withContext(Dispatchers.IO) {
                    val items = itemRepository.getAllItems().first()
                    val photos = itemRepository.getAllPhotos()
                    val categories = categoryRepository.getAllCategories().first()
                    val fileName = BackupUtil.buildZipFileName()
                    // 默认落到手机 Download（下载）目录；存储策略不允许时自动回落私有目录
                    val exportFile = backupRepository.exportToDownloads(fileName) { out ->
                        BackupUtil.writeZip(items, photos, categories, context.cacheDir, out)
                    }
                    Quad(items, photos, categories, exportFile)
                }
                backupRepository.saveExcelExportRecord(exportFile.displayName, exportFile.path, items.size)
                val location = if (exportFile.inDownloads) "\n位置: Download/$DOWNLOAD_SUBDIR" else "\n位置: 应用私有目录"
                _uiState.update {
                    it.copy(isLoading = false, isSuccess = true,
                        message = "导出成功\n物品: ${items.size} 件\n照片: ${photos.size} 张\n分类: ${categories.size} 个\n文件: ${exportFile.displayName}$location")
                }
            } catch (e: Throwable) {
                android.util.Log.e("BackupVM", "导出失败", e)
                _uiState.update {
                    it.copy(isLoading = false, isSuccess = false, message = "导出失败: ${e.readableMessage()}")
                }
            }
        }
    }

    fun fullImport(uriString: String) {
        runImport { BackupUtil.fullImport(context, Uri.parse(uriString)) }
    }

    /** 点击备份记录直接恢复：文件已在记录里，无需用户再去文件选择器里找。 */
    fun restoreRecord(record: BackupRecordEntity) {
        runImport { BackupUtil.fullImportFromRecord(context, record.filePath) }
    }

    private fun runImport(load: () -> BackupUtil.ImportResult) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = null) }
            try {
                val importResult = withContext(Dispatchers.IO) { load() }
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

                // 合并覆盖：现有物品按 id / 名称建索引，备份里的同一条走更新而不是再插一条，
                // 否则同一个备份恢复两次数据就翻倍。
                // 用含回收站的查询：正躺在回收站里的物品也要能被匹配到，不能被当成新物品再插一条。
                val existingItems = itemRepository.getAllItemsIncludingDeleted()
                val byName = existingItems.groupBy { it.name }
                // 已经配出去的现有物品不能再被下一条备份记录认领，否则两条同名备份会覆盖到同一行，
                // 另一行永远是旧的。同名多条时按顺序一一配对。
                val claimed = mutableSetOf<Long>()

                /**
                 * 给备份里的一条物品找它在库里的"同一条"。
                 * 优先主键相同的那条（同一次导出的备份最可靠）；同名多条时退化为按顺序配对。
                 * 不单凭主键匹配：主键是自增的，库重建或换过备份后可能指向另一个物品。
                 */
                fun matchExisting(item: ItemEntity): ItemEntity? {
                    val candidates = byName[item.name]?.filter { it.id !in claimed } ?: return null
                    if (candidates.isEmpty()) return null
                    return candidates.firstOrNull { item.id != 0L && it.id == item.id }
                        ?: candidates.first()
                }

                var itemCount = 0
                var addedCount = 0
                var updatedCount = 0
                var photoCount = 0
                for (ii in importResult.items) {
                    // Find new category ID by looking up the old category ID's name
                    val oldCat = importResult.categories.firstOrNull { it.id == ii.item.categoryId }
                    val newCatId = oldCat?.name?.let { nameToId[it] } ?: ii.item.categoryId

                    val matched = matchExisting(ii.item)

                    if (matched != null) {
                        claimed += matched.id
                        // 覆盖更新：保留原主键与创建时间，其余字段用备份内容覆盖。
                        // 备份里只会有未删除的物品，所以顺带把回收站里的同名物品复活。
                        val merged = ii.item.copy(
                            id = matched.id,
                            categoryId = newCatId,
                            createdAt = matched.createdAt,
                            isDeleted = false,
                            deletedAt = null
                        )
                        itemRepository.updateItem(merged)
                        if (ii.photoFiles.isNotEmpty()) {
                            itemRepository.replacePhotosForItem(matched.id, ii.photoFiles)
                            photoCount += ii.photoFiles.size
                        }
                        updatedCount++
                    } else {
                        val itemToSave = ii.item.copy(id = 0L, categoryId = newCatId)
                        val itemId = itemRepository.insertItem(itemToSave)
                        if (ii.photoFiles.isNotEmpty()) {
                            itemRepository.addPhotosForItem(itemId, ii.photoFiles)
                            photoCount += ii.photoFiles.size
                        }
                        addedCount++
                    }
                    itemCount++
                }
                // 导入完成后一次性重算
                expService.recalculateProfile()
                val summary = buildString {
                    append("恢复成功\n")
                    append("分类: $catCount 个\n")
                    append("物品: $itemCount 件（新增 $addedCount / 覆盖 $updatedCount）\n")
                    append("照片: $photoCount 张\n")
                    append("请退出重进以刷新数据")
                }
                _uiState.update {
                    it.copy(isLoading = false, isSuccess = true, message = summary)
                }
            } catch (e: Throwable) {
                android.util.Log.e("BackupVM", "导入失败", e)
                _uiState.update {
                    it.copy(isLoading = false, isSuccess = false, message = "恢复失败: ${e.readableMessage()}")
                }
            }
        }
    }

    private fun Throwable.readableMessage(): String = when {
        this is OutOfMemoryError -> "内存不足"
        !message.isNullOrBlank() -> message!!
        else -> javaClass.simpleName
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
