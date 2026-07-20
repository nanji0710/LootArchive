package com.nanji.lootarchive.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nanji.lootarchive.data.local.entity.CategoryEntity
import com.nanji.lootarchive.data.local.entity.ItemEntity
import com.nanji.lootarchive.data.repository.CategoryRepository
import com.nanji.lootarchive.data.repository.ItemRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val results: List<ItemEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val selectedCategoryId: Long? = null,
    val warrantyFilter: String? = null,  // "active" | "expiring" | "expired" | null
    val isLoading: Boolean = false
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val itemRepository: ItemRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            categoryRepository.getAllCategories().collect { categories ->
                _uiState.update { it.copy(categories = categories) }
            }
        }
    }

    fun updateQuery(query: String) {
        _uiState.update { it.copy(query = query) }
        // 防抖搜索（300ms）
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300)
            if (query.isBlank()) {
                _uiState.update { it.copy(results = emptyList(), isLoading = false) }
                return@launch
            }
            _uiState.update { it.copy(isLoading = true) }
            itemRepository.searchItems(query).catch {}.collect { items ->
                _uiState.update { it.copy(results = items, isLoading = false) }
            }
        }
    }

    fun setCategoryFilter(categoryId: Long?) {
        val newFilter = if (_uiState.value.selectedCategoryId == categoryId) null else categoryId
        _uiState.update { it.copy(selectedCategoryId = newFilter) }
        applyFilters()
    }

    fun setWarrantyFilter(filter: String?) {
        val newFilter = if (_uiState.value.warrantyFilter == filter) null else filter
        _uiState.update { it.copy(warrantyFilter = newFilter) }
        applyFilters()
    }

    private fun applyFilters() {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            itemRepository.filterItems(
                categoryId = state.selectedCategoryId
            ).catch {}.collect { items ->
                val filtered = when (state.warrantyFilter) {
                    "expired" -> items.filter { (it.warrantyExpiryDate ?: Long.MAX_VALUE) < System.currentTimeMillis() }
                    "expiring" -> {
                        val threshold = System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L
                        items.filter {
                            val expiry = it.warrantyExpiryDate ?: return@filter false
                            expiry in (System.currentTimeMillis() + 1)..threshold
                        }
                    }
                    else -> items
                }
                _uiState.update { it.copy(results = filtered, isLoading = false) }
            }
        }
    }
}
