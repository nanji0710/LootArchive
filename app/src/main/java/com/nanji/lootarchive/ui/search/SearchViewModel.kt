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
    val activeFilter: String? = null,   // null=全部, "name", "location", "desc", "warranty"
    val sort: String = "date_new",      // "price_desc", "date_new", "warranty"
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

    fun setActiveFilter(filter: String?) {
        _uiState.update { it.copy(activeFilter = filter) }
        doSearch()
    }

    fun setSort(sort: String) {
        _uiState.update { it.copy(sort = sort) }
        doSearch()
    }

    private fun doSearch() {
        val state = _uiState.value
        if (state.query.isBlank() && state.activeFilter == null) {
            _uiState.update { it.copy(results = emptyList()) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            itemRepository.searchItems(state.query.ifBlank { "" }).catch {}.collect { items ->
                _uiState.update { it.copy(results = items, isLoading = false) }
            }
        }
    }
}
