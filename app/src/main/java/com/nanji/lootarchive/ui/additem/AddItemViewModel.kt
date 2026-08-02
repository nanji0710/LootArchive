package com.nanji.lootarchive.ui.additem

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nanji.lootarchive.data.local.entity.CategoryEntity
import com.nanji.lootarchive.domain.model.ItemStatus
import com.nanji.lootarchive.data.local.entity.ItemEntity
import com.nanji.lootarchive.data.local.entity.ItemPhotoEntity
import com.nanji.lootarchive.data.repository.CategoryRepository
import com.nanji.lootarchive.data.repository.ItemRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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
    val tagInput: String = "",
    val salePriceText: String = "",
    val saleDate: Long? = null,
    // 表单校验
    val nameError: String? = null,
    val priceError: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class AddItemViewModel @Inject constructor(
    private val itemRepository: ItemRepository,
    private val categoryRepository: CategoryRepository,
    private val expService: com.nanji.lootarchive.data.ExpService
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddItemUiState())
    val uiState: StateFlow<AddItemUiState> = _uiState.asStateFlow()

    private var editingItemId: Long? = null
    private var formInitialized = false

    fun onScreenDisposed() {
        formInitialized = false
    }

    fun resetForm() {
        editingItemId = null
        warrantyManuallySet = false
        formInitialized = false
        _uiState.value = AddItemUiState(categories = _uiState.value.categories)
    }

    fun initEditMode(itemId: Long?) {
        // 关键修复：每次进入页面都重置加载状态，不再依赖 onScreenDisposed
        // formInitialized 仅用于防止同一次进入中 LaunchedEffect 重复触发
        if (formInitialized) return
        formInitialized = true
        if (itemId == null) { resetForm(); return }
        editingItemId = itemId
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
                    tags = item.tags,
                    salePriceText = item.salePrice?.toString() ?: "",
                    saleDate = item.saleDate
                )
            }
        }
    }

    init {
        viewModelScope.launch {
            categoryRepository.getAllCategories().collect { categories ->
                _uiState.update { it.copy(categories = categories) }
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
        _uiState.update { it.copy(purchasePrice = price, priceError = null) }
    }

    fun updateStorageLocation(location: String) {
        _uiState.update { it.copy(storageLocation = location) }
    }

    fun updatePurchaseDate(date: Long?) {
        _uiState.update { it.copy(purchaseDate = date) }
        autoCalcWarranty()
    }

    fun updateWarrantyExpiryDate(date: Long?) {
        warrantyManuallySet = true
        _uiState.update { it.copy(warrantyExpiryDate = date) }
    }

    fun updateWarrantyPeriodDays(days: String) {
        _uiState.update { it.copy(warrantyPeriodDays = days) }
        autoCalcWarranty()
    }

    private var warrantyManuallySet = false

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
        _uiState.update { it.copy(status = status, salePriceText = if (ItemStatus.fromCode(status) == ItemStatus.SOLD) it.salePriceText else "", saleDate = if (ItemStatus.fromCode(status) == ItemStatus.SOLD) it.saleDate else null) }
    }

    fun updateSalePrice(text: String) {
        _uiState.update { it.copy(salePriceText = text) }
    }

    fun updateSaleDate(date: Long?) {
        _uiState.update { it.copy(saleDate = date) }
    }

    fun updateTagInput(input: String) {
        _uiState.update { it.copy(tagInput = input) }
    }

    fun addTag(tag: String) {
        val trimmed = tag.trim()
        if (trimmed.isEmpty()) return
        val current = _uiState.value.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
        if (trimmed !in current) {
            current.add(trimmed)
            _uiState.update { it.copy(tags = current.joinToString(","), tagInput = "") }
        } else {
            _uiState.update { it.copy(tagInput = "") }
        }
    }

    fun removeTag(tag: String) {
        val current = _uiState.value.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() && it != tag }
        _uiState.update { it.copy(tags = current.joinToString(",")) }
    }

    fun addPhotoPath(path: String) {
        _uiState.update { it.copy(photoPaths = it.photoPaths + path) }
    }

    fun removePhotoPath(path: String) {
        _uiState.update { it.copy(photoPaths = it.photoPaths - path) }
    }

    fun saveItem() {
        val state = _uiState.value

        // 校验
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
            state.categories.find { it.name == "其他" }?.id
                ?: state.categories.firstOrNull()?.id
                ?: 1L
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
                    updatedAt = System.currentTimeMillis(),
                    salePrice = state.salePriceText.toDoubleOrNull(),
                    saleDate = state.saleDate
                )

                // 原子保存（文件 IO 在 IO 线程，DB 写入在事务内；编辑/新建自动分流）
                itemRepository.saveItemWithPhotos(item, state.photoPaths)

                _uiState.update { it.copy(isLoading = false, isSaved = true) }
                expService.recalculateProfile()
                formInitialized = false  // 允许下次新增物品时重新初始化表单
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
}
