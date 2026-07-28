package com.nanji.lootarchive.ui.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nanji.lootarchive.data.local.entity.CategoryEntity
import com.nanji.lootarchive.data.local.entity.ItemEntity
import com.nanji.lootarchive.data.repository.CategoryRepository
import com.nanji.lootarchive.data.repository.ItemRepository
import com.nanji.lootarchive.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategorySummary(
    val category: CategoryEntity,
    val itemCount: Int,
    val totalValue: Double
)

data class StatisticsUiState(
    val isLoading: Boolean = true,
    val totalCount: Int = 0,
    val totalValue: Double = 0.0,
    val currency: String = "CNY",
    val categorySummaries: List<CategorySummary> = emptyList(),
    val items: List<ItemEntity> = emptyList(),
    val timeFilter: String = "all",  // "all" | "3months" | "6months" | "1year"
    val selectedCategorySummary: CategorySummary? = null,  // 图表点击弹窗
    val selectedCategoryItems: List<ItemEntity> = emptyList()
)

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val itemRepository: ItemRepository,
    private val categoryRepository: CategoryRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                combine(
                    itemRepository.getAllItems(),
                    itemRepository.getTotalCount(),
                    itemRepository.getTotalValue()
                ) { items, count, value -> Triple(items, count, value) },
                categoryRepository.getAllCategories(),
                settingsRepository.currency
            ) { (items, count, value), categories, currency ->
                val filter = _uiState.value.timeFilter
                val now = System.currentTimeMillis()
                val cutoff = when (filter) {
                    "3months" -> now - 90L * 24 * 60 * 60 * 1000
                    "6months" -> now - 180L * 24 * 60 * 60 * 1000
                    "1year" -> now - 365L * 24 * 60 * 60 * 1000
                    else -> 0L
                }
                val filteredItems = if (cutoff == 0L) items else items.filter { (it.purchaseDate ?: 0) >= cutoff }
                val summaries = categories.map { cat ->
                    val catItems = filteredItems.filter { it.categoryId == cat.id }
                    CategorySummary(category = cat, itemCount = catItems.size, totalValue = catItems.sumOf { it.purchasePrice })
                }
                StatisticsUiState(
                    isLoading = false,
                    totalCount = filteredItems.size,
                    totalValue = filteredItems.sumOf { it.purchasePrice },
                    currency = currency,
                    categorySummaries = summaries,
                    items = filteredItems,
                    timeFilter = filter
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun refresh() {
        // Room Flows are reactive — data auto-updates via combine above.
        // Just trigger a brief loading state to signal UI refresh.
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            kotlinx.coroutines.delay(100)
            // Flow will auto-emit fresh data
        }
    }

    fun setTimeFilter(filter: String) {
        _uiState.update { it.copy(timeFilter = filter, isLoading = true) }
    }

    fun selectCategorySummary(summary: CategorySummary) {
        val catItems = _uiState.value.items.filter { it.categoryId == summary.category.id }
        _uiState.update {
            it.copy(
                selectedCategorySummary = summary,
                selectedCategoryItems = catItems
            )
        }
    }

    fun dismissCategoryDetail() {
        _uiState.update {
            it.copy(selectedCategorySummary = null, selectedCategoryItems = emptyList())
        }
    }
}
