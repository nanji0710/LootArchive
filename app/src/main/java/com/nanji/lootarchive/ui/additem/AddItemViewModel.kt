package com.nanji.lootarchive.ui.additem

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nanji.lootarchive.data.local.entity.CategoryEntity
import com.nanji.lootarchive.data.local.entity.ItemEntity
import com.nanji.lootarchive.data.repository.CategoryRepository
import com.nanji.lootarchive.data.repository.ItemRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar
import javax.inject.Inject

data class AddItemUiState(
    val isLoading: Boolean = false,
    val isEditMode: Boolean = false,
    val isSaved: Boolean = false,
    val categories: List<CategoryEntity> = emptyList(),
    // 表单字段
    val name: String = "",
    val categoryId: Long = 0,
    val purchasePrice: String = "",
    val storageLocation: String = "",
    val purchaseDate: Long? = null,
    val warrantyExpiryDate: Long? = null,
    val warrantyPeriodDays: String = "",
    val description: String = "",
    val photoPaths: List<String> = emptyList(),
    val status: String = "active",
    val tags: String = "",
    // v6.12 单屏改造：折叠区、最近位置、保存后动作条
    val recentLocations: List<String> = emptyList(),
    val moreExpanded: Boolean = false,
    val savedItemId: Long? = null,
    val savedItemName: String = "",
    val infoMessage: String? = null,
    // 表单校验
    val nameError: String? = null,
    val priceError: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class AddItemViewModel @Inject constructor(
    private val itemRepository: ItemRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddItemUiState())
    val uiState: StateFlow<AddItemUiState> = _uiState.asStateFlow()

    private var editingItemId: Long? = null
    private var warrantyManuallySet = false

    init {
        viewModelScope.launch {
            categoryRepository.getAllCategories().collect { categories ->
                _uiState.update { state ->
                    // 分类是异步加载的：会话早于分类数据到达时先留 0，数据到位后自动补默认分类
                    val fixedId = if (state.categoryId == 0L) defaultCategoryId(categories) else state.categoryId
                    state.copy(categories = categories, categoryId = fixedId)
                }
            }
        }
        viewModelScope.launch {
            val recent = runCatching { itemRepository.getRecentLocations(3) }.getOrDefault(emptyList())
            _uiState.update { it.copy(recentLocations = recent) }
        }
    }

    private fun defaultCategoryId(categories: List<CategoryEntity>): Long =
        categories.find { it.name == "其他" }?.id ?: categories.firstOrNull()?.id ?: 1L

    private fun todayMillis(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    /**
     * 进入表单会话：新增页每次进入、编辑页进入、以及保存后点「再记一件」都会调用。
     * 用会话号驱动（而不是 onDispose 时序），避免"同一次进入里 LaunchedEffect 重复触发"和"离开后状态残留"两类问题。
     */
    fun startSession(itemId: Long?) {
        val current = _uiState.value
        editingItemId = itemId
        warrantyManuallySet = false
        _uiState.value = AddItemUiState(
            categories = current.categories,
            recentLocations = current.recentLocations,
            categoryId = if (current.categories.isEmpty()) 0L else defaultCategoryId(current.categories),
            purchaseDate = todayMillis()
        )
        if (itemId != null) loadForEdit(itemId)
    }

    private fun loadForEdit(itemId: Long) {
        viewModelScope.launch {
            val itemWithPhotos = itemRepository.getItemWithPhotos(itemId) ?: return@launch
            val item = itemWithPhotos.item
            _uiState.update {
                it.copy(
                    isEditMode = true,
                    name = item.name,
                    categoryId = item.categoryId,
                    purchasePrice = item.purchasePrice.toString(),
                    storageLocation = item.storageLocation,
                    purchaseDate = item.purchaseDate,
                    warrantyExpiryDate = item.warrantyExpiryDate,
                    warrantyPeriodDays = item.warrantyPeriodDays?.toString() ?: "",
                    description = item.description,
                    photoPaths = itemWithPhotos.photos.map { it.photoPath },
                    status = item.status,
                    tags = item.tags
                )
            }
        }
    }

    fun updateName(name: String) {
        _uiState.update { it.copy(name = name, nameError = null) }
    }

    fun updateCategoryId(categoryId: Long) {
        _uiState.update { it.copy(categoryId = categoryId) }
    }

    fun updatePurchasePrice(price: String) {
        // 只接受数字与一个小数点，省掉一次系统键盘报错
        val filtered = price.filter { it.isDigit() || it == '.' }
        if (filtered.count { it == '.' } > 1) return
        _uiState.update { it.copy(purchasePrice = filtered, priceError = null) }
    }

    fun updateStorageLocation(location: String) {
        _uiState.update { it.copy(storageLocation = location) }
    }

    fun updatePurchaseDate(date: Long?) {
        _uiState.update { it.copy(purchaseDate = date) }
        autoCalcWarranty()
    }

    /** 购入日期快捷：今天 / 昨天 */
    fun setPurchaseDateToday() = updatePurchaseDate(todayMillis())

    fun setPurchaseDateYesterday() = updatePurchaseDate(todayMillis() - 24L * 60 * 60 * 1000)

    fun updateWarrantyExpiryDate(date: Long?) {
        warrantyManuallySet = true
        _uiState.update { it.copy(warrantyExpiryDate = date) }
    }

    fun updateWarrantyPeriodDays(days: String) {
        val filtered = days.filter { it.isDigit() }
        _uiState.update { it.copy(warrantyPeriodDays = filtered) }
        autoCalcWarranty()
    }

    /** 保修快捷档位：点一下即按购入日期算出到期日（手动设过到期日则以手动值为准）。 */
    fun setWarrantyPreset(days: Int) {
        warrantyManuallySet = false
        _uiState.update { it.copy(warrantyPeriodDays = days.toString()) }
        autoCalcWarranty()
    }

    /** 关闭保修：清空天数与到期日。 */
    fun clearWarranty() {
        warrantyManuallySet = false
        _uiState.update { it.copy(warrantyPeriodDays = "", warrantyExpiryDate = null) }
    }

    private fun autoCalcWarranty() {
        if (warrantyManuallySet) return  // 用户手动设过到期日，不覆盖
        val state = _uiState.value
        val periodDays = state.warrantyPeriodDays.toIntOrNull()
        if (periodDays != null && periodDays > 0 && state.purchaseDate != null) {
            val expiry = state.purchaseDate + periodDays * 24L * 60 * 60 * 1000
            _uiState.update { it.copy(warrantyExpiryDate = expiry) }
        }
    }

    fun updateDescription(desc: String) {
        _uiState.update { it.copy(description = desc) }
    }

    fun updateStatus(status: String) {
        _uiState.update { it.copy(status = status) }
    }

    fun setMoreExpanded(expanded: Boolean) {
        _uiState.update { it.copy(moreExpanded = expanded) }
    }

    fun addPhotoPath(path: String) {
        _uiState.update { it.copy(photoPaths = it.photoPaths + path) }
    }

    fun removePhotoPath(path: String) {
        _uiState.update { it.copy(photoPaths = it.photoPaths - path) }
    }

    fun clearInfoMessage() {
        _uiState.update { it.copy(infoMessage = null) }
    }

    /**
     * 拍照/相册回传的照片路由：
     * - 已保存过（动作条在）→ 直接追加到刚保存的物品，不必重进详情页；
     * - 未保存 → 进表单照片条。
     * 追加的照片不写入表单 photoPaths，避免"撤销后重存"造成同一文件被两条记录引用。
     */
    fun onPhotosPicked(paths: List<String>) {
        if (paths.isEmpty()) return
        if (_uiState.value.savedItemId != null) appendPhotosToSavedItem(paths)
        else _uiState.update { it.copy(photoPaths = it.photoPaths + paths) }
    }

    private fun appendPhotosToSavedItem(paths: List<String>) {
        val id = _uiState.value.savedItemId ?: return
        viewModelScope.launch {
            try {
                itemRepository.addPhotosForItem(id, paths.map { File(it) })
                _uiState.update { it.copy(infoMessage = "已为「${it.savedItemName}」添加 ${paths.size} 张照片") }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "添加照片失败: ${e.message}") }
            }
        }
    }

    fun saveItem() {
        val state = _uiState.value

        // 校验：名称与价格都必填（价格是价值统计的基准，不允许为空）
        if (state.name.isBlank()) {
            _uiState.update { it.copy(nameError = "请输入物品名称") }
            return
        }
        val price = state.purchasePrice.toDoubleOrNull()
        if (price == null || price < 0) {
            _uiState.update { it.copy(priceError = "请输入有效价格") }
            return
        }

        // 分类未选时默认归入"其他"
        val actualCategoryId = if (state.categoryId == 0L) {
            defaultCategoryId(state.categories)
        } else {
            state.categoryId
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                // 计算保修到期日
                val periodDays = state.warrantyPeriodDays.toIntOrNull()
                val expiryDate = when {
                    state.warrantyExpiryDate != null -> state.warrantyExpiryDate
                    periodDays != null && state.purchaseDate != null ->
                        state.purchaseDate + periodDays * 24 * 60 * 60 * 1000L
                    periodDays != null -> System.currentTimeMillis() + periodDays * 24 * 60 * 60 * 1000L
                    else -> null
                }

                val item = ItemEntity(
                    id = editingItemId ?: 0,
                    name = state.name.trim(),
                    categoryId = actualCategoryId,
                    purchasePrice = price,
                    storageLocation = state.storageLocation.trim(),
                    purchaseDate = state.purchaseDate,
                    warrantyExpiryDate = expiryDate,
                    warrantyPeriodDays = periodDays,
                    description = state.description.trim(),
                    status = state.status,
                    tags = state.tags,
                    updatedAt = System.currentTimeMillis()
                )

                // 原子保存（文件 IO 在 IO 线程，DB 写入在事务内；编辑/新建自动分流）
                val savedId = itemRepository.saveItemWithPhotos(item, state.photoPaths)

                if (state.isEditMode) {
                    // 编辑：保存即返回（由页面 LaunchedEffect 处理导航）
                    _uiState.update { it.copy(isLoading = false, isSaved = true, savedItemId = savedId, savedItemName = item.name) }
                } else {
                    // 新增：留在页面，露出保存后动作条（再记一件 / 补照片 / 撤销）
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isSaved = true,
                            savedItemId = savedId,
                            savedItemName = item.name,
                            infoMessage = null
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "保存失败: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * 保存后点「再记一件」：沿用分类/位置/购入日期（同一批采购常见共性），
     * 清空名称/价格/照片/描述/状态/保修，动作条收起。
     */
    fun startNewEntry() {
        val current = _uiState.value
        editingItemId = null
        warrantyManuallySet = false
        _uiState.value = AddItemUiState(
            categories = current.categories,
            recentLocations = current.recentLocations,
            categoryId = current.categoryId,
            storageLocation = current.storageLocation,
            purchaseDate = current.purchaseDate ?: todayMillis()
        )
    }

    /**
     * 保存后点「撤销」：软删除刚保存的物品（进回收站，可还原），
     * 表单内容保留，用户可以直接改完再存。
     */
    fun undoLastSave() {
        val id = _uiState.value.savedItemId ?: return
        viewModelScope.launch {
            try {
                itemRepository.softDeleteItem(id)
                editingItemId = null
                _uiState.update {
                    it.copy(
                        isSaved = false,
                        savedItemId = null,
                        infoMessage = "已撤销并移入回收站，可修改后重新保存"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "撤销失败: ${e.message}") }
            }
        }
    }
}
