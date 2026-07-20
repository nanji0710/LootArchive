package com.nanji.lootarchive.ui.additem

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nanji.lootarchive.data.local.entity.CategoryEntity
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

    fun initEditMode(itemId: Long?) {
        if (itemId == null) return
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
                    photoPaths = itemWithPhotos.photos.map { it.photoPath }
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
    }

    fun updateWarrantyExpiryDate(date: Long?) {
        _uiState.update { it.copy(warrantyExpiryDate = date) }
    }

    fun updateWarrantyPeriodDays(days: String) {
        _uiState.update { it.copy(warrantyPeriodDays = days) }
    }

    fun updateDescription(desc: String) {
        _uiState.update { it.copy(description = desc) }
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
                    updatedAt = System.currentTimeMillis()
                )

                val savedId = if (editingItemId != null) {
                    itemRepository.updateItem(item)
                    editingItemId!!
                } else {
                    itemRepository.insertItem(item)
                }

                // 保存照片引用
                state.photoPaths.forEachIndexed { index, path ->
                    itemRepository.addPhoto(
                        ItemPhotoEntity(
                            itemId = savedId,
                            photoPath = path,
                            sortOrder = index
                        )
                    )
                }

                _uiState.update { it.copy(isLoading = false, isSaved = true) }
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
