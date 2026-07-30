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

data class SearchResultItem(
    val item: ItemEntity,
    val firstPhotoPath: String?
)

data class SearchUiState(
    val query: String = "",
    val results: List<SearchResultItem> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val activeFilter: String? = null,
    val sort: String = "date_new",
    val isLoading: Boolean = false,
    val recentSearches: List<String> = emptyList(),
    val statusFilter: String? = null,
    val tagFilter: String? = null,
    val allTags: List<String> = emptyList()
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val itemRepository: ItemRepository,
    private val categoryRepository: CategoryRepository,
    private val settingsRepository: com.nanji.lootarchive.data.repository.SettingsRepository
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
        viewModelScope.launch {
            itemRepository.getAllTags().collect { tags ->
                _uiState.update { it.copy(allTags = tags) }
            }
        }
        viewModelScope.launch {
            settingsRepository.searchHistory.collect { saved ->
                history.clear(); history.addAll(saved)
                _uiState.update { it.copy(recentSearches = saved) }
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
        viewModelScope.launch { settingsRepository.addSearchHistory(q) }
        executeSearch(q)
    }

    fun clearHistory() {
        history.clear()
        _uiState.update { it.copy(recentSearches = emptyList()) }
        viewModelScope.launch { settingsRepository.clearSearchHistory() }
    }

    fun setActiveFilter(filter: String?) { _uiState.update { it.copy(activeFilter = filter) }; doSearch() }
    fun setSort(sort: String) { _uiState.update { it.copy(sort = sort) }; doSearch() }
    fun setStatusFilter(status: String?) { _uiState.update { it.copy(statusFilter = status) }; doSearch() }
    fun setTagFilter(tag: String?) { _uiState.update { it.copy(tagFilter = tag) }; doSearch() }

    private fun doSearch() {
        val q = _uiState.value.query
        if (q.isBlank() && _uiState.value.activeFilter == null) { _uiState.update { it.copy(results = emptyList()) }; return }
        executeSearch(q)
    }

    private fun executeSearch(q: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            itemRepository.searchItemsWithTags(q.ifBlank { "" }).catch {}.collect { items ->
                val filter = _uiState.value.activeFilter
                // 按筛选字段过滤
                val filtered = when (filter) {
                    "name" -> items.filter { it.name.contains(q, ignoreCase = true) }
                    "location" -> items.filter { it.storageLocation.contains(q, ignoreCase = true) }
                    "desc" -> items.filter { it.description.contains(q, ignoreCase = true) }
                    "warranty" -> items.filter {
                        (it.warrantyPeriodDays?.toString()?.contains(q) == true) ||
                        (it.warrantyExpiryDate?.let { d ->
                            java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date(d))
                        }?.contains(q) == true)
                    }
                    else -> items // "全部"
                }
                // v5.2 状态筛选
                val statusF = _uiState.value.statusFilter
                val statusFiltered = if (statusF != null) filtered.filter { it.status == statusF } else filtered
                // v5.2 标签筛选
                val tagF = _uiState.value.tagFilter
                val tagFiltered = if (tagF != null) statusFiltered.filter { it.tags.split(",").any { t -> t.trim() == tagF } } else statusFiltered
                val sort = _uiState.value.sort
                val sorted = when (sort) {
                    "price_desc" -> tagFiltered.sortedByDescending { it.purchasePrice }
                    "date_new" -> tagFiltered.sortedByDescending { it.purchaseDate ?: 0L }
                    "warranty" -> tagFiltered.sortedBy { it.warrantyExpiryDate ?: Long.MAX_VALUE }
                    else -> tagFiltered
                }
                val results = sorted.map { item ->
                    SearchResultItem(item = item, firstPhotoPath = itemRepository.getFirstPhotoPath(item.id))
                }
                _uiState.update { it.copy(results = results, isLoading = false) }
            }
        }
    }
}
