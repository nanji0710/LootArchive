package com.nanji.lootarchive.ui.component

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nanji.lootarchive.data.local.entity.CategoryEntity
import com.nanji.lootarchive.data.repository.CategoryRepository
import com.nanji.lootarchive.data.repository.ItemRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoryDrawerUiState(
    val totalItemCount: Int = 0,
    val categories: List<CategoryEntity> = emptyList(),
    val categoryCounts: Map<Long, Int> = emptyMap(),
    val showAddDialog: Boolean = false,
    val newCategoryName: String = ""
)

@HiltViewModel
class CategoryDrawerViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val itemRepository: ItemRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoryDrawerUiState())
    val uiState: StateFlow<CategoryDrawerUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                categoryRepository.getAllCategories(),
                itemRepository.getTotalCount()
            ) { categories, total ->
                val counts = mutableMapOf<Long, Int>()
                categories.forEach { cat ->
                    // 实际应该批量查询，简化处理
                }
                CategoryDrawerUiState(
                    totalItemCount = total,
                    categories = categories,
                    categoryCounts = counts
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun showAddDialog() {
        _uiState.update { it.copy(showAddDialog = true, newCategoryName = "") }
    }

    fun dismissDialog() {
        _uiState.update { it.copy(showAddDialog = false, newCategoryName = "") }
    }

    fun updateNewCategoryName(name: String) {
        _uiState.update { it.copy(newCategoryName = name) }
    }

    fun addCategory() {
        val name = _uiState.value.newCategoryName.trim()
        if (name.isEmpty()) return
        viewModelScope.launch {
            categoryRepository.createCategory(name)
            dismissDialog()
        }
    }
}
