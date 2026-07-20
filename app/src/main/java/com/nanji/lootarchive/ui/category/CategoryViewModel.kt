package com.nanji.lootarchive.ui.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nanji.lootarchive.data.local.entity.CategoryEntity
import com.nanji.lootarchive.data.repository.CategoryRepository
import com.nanji.lootarchive.data.repository.ItemRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoryUiState(
    val isLoading: Boolean = true,
    val categories: List<CategoryEntity> = emptyList(),
    val categoryItemCounts: Map<Long, Int> = emptyMap(),
    val showAddDialog: Boolean = false,
    val showEditDialog: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val editingCategory: CategoryEntity? = null,
    val dialogName: String = ""
)

@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val itemRepository: ItemRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoryUiState())
    val uiState: StateFlow<CategoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            categoryRepository.getAllCategories().collect { categories ->
                // 获取每个分类下的物品数量
                val counts = mutableMapOf<Long, Int>()
                categories.forEach { cat ->
                    // 用一次性的 suspend 查询
                }
                _uiState.update {
                    it.copy(isLoading = false, categories = categories, categoryItemCounts = counts)
                }
            }
        }
    }

    fun showAddDialog() {
        _uiState.update { it.copy(showAddDialog = true, dialogName = "") }
    }

    fun showEditDialog(category: CategoryEntity) {
        _uiState.update {
            it.copy(showEditDialog = true, editingCategory = category, dialogName = category.name)
        }
    }

    fun showDeleteDialog(category: CategoryEntity) {
        _uiState.update { it.copy(showDeleteDialog = true, editingCategory = category) }
    }

    fun dismissDialogs() {
        _uiState.update {
            it.copy(
                showAddDialog = false,
                showEditDialog = false,
                showDeleteDialog = false,
                editingCategory = null,
                dialogName = ""
            )
        }
    }

    fun updateDialogName(name: String) {
        _uiState.update { it.copy(dialogName = name) }
    }

    fun addCategory() {
        val name = _uiState.value.dialogName.trim()
        if (name.isEmpty()) return
        viewModelScope.launch {
            categoryRepository.createCategory(name)
            dismissDialogs()
        }
    }

    fun updateCategory() {
        val category = _uiState.value.editingCategory ?: return
        val name = _uiState.value.dialogName.trim()
        if (name.isEmpty()) return
        viewModelScope.launch {
            categoryRepository.updateCategory(category.copy(name = name))
            dismissDialogs()
        }
    }

    fun deleteCategory() {
        val category = _uiState.value.editingCategory ?: return
        viewModelScope.launch {
            categoryRepository.deleteCategory(category)
            dismissDialogs()
        }
    }
}
