package com.nanji.lootarchive.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nanji.lootarchive.data.local.entity.ItemEntity
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
    val sort: String = "date_new",
    val isLoading: Boolean = false,
    val recentSearches: List<String> = emptyList()
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val itemRepository: ItemRepository,
    private val settingsRepository: com.nanji.lootarchive.data.repository.SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private val history = mutableListOf<String>()

    init {
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

    private fun executeSearch(q: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            itemRepository.searchItemsWithTags(q.ifBlank { "" }).catch { e ->
                android.util.Log.e("SearchVM", "Search failed query=$q", e)
                _uiState.update { it.copy(isLoading = false, results = emptyList()) }
            }.collect { items ->
                // 排序固定为「购入时间最新」（原排序菜单已移除，保留既有默认行为）
                val sorted = items.sortedByDescending { it.purchaseDate ?: 0L }
                val allFirstPhotos = itemRepository.getAllFirstPhotos()
                val results = sorted.map { item ->
                    SearchResultItem(item = item, firstPhotoPath = allFirstPhotos[item.id])
                }
                _uiState.update { it.copy(results = results, isLoading = false) }
            }
        }
    }
}
