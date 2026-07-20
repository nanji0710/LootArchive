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
    val activeFilter: String? = null,
    val sort: String = "date_new",
    val isLoading: Boolean = false,
    val recentSearches: List<String> = emptyList()
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val itemRepository: ItemRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private val history = mutableListOf<String>()

    init {
        viewModelScope.launch {
            categoryRepository.getAllCategories().collect { categories ->
                _uiState.update { it.copy(categories = categories) }
            }
        }
    }

    fun updateQuery(query: String) {
        _uiState.update { it.copy(query = query) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300)
            if (query.isBlank()) {
                _uiState.update { it.copy(results = emptyList(), isLoading = false) }
                return@launch
            }
            executeSearch(query)
        }
    }

    fun submitSearch() {
        val q = _uiState.value.query.trim()
        if (q.isBlank()) return
        if (!history.contains(q)) { history.add(0, q); if (history.size > 20) history.removeLast() }
        _uiState.update { it.copy(recentSearches = history.toList()) }
        executeSearch(q)
    }

    fun clearHistory() { history.clear(); _uiState.update { it.copy(recentSearches = emptyList()) } }

    fun setActiveFilter(filter: String?) { _uiState.update { it.copy(activeFilter = filter) }; doSearch() }
    fun setSort(sort: String) { _uiState.update { it.copy(sort = sort) }; doSearch() }

    private fun doSearch() {
        val q = _uiState.value.query
        if (q.isBlank() && _uiState.value.activeFilter == null) { _uiState.update { it.copy(results = emptyList()) }; return }
        executeSearch(q)
    }

    private fun executeSearch(q: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            itemRepository.searchItems(q.ifBlank { "" }).catch {}.collect { items ->
                _uiState.update { it.copy(results = items, isLoading = false) }
            }
        }
    }
}
