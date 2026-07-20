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
        loadStatistics()
    }

    private fun loadStatistics() {
        viewModelScope.launch {
            combine(
                itemRepository.getAllItems(),
                itemRepository.getTotalCount(),
                itemRepository.getTotalValue(),
                categoryRepository.getAllCategories(),
                settingsRepository.currency
            ) { items, count, value, categories, currency ->
                val summaries = categories.map { cat ->
                    val catItems = items.filter { it.categoryId == cat.id }
                    CategorySummary(
                        category = cat,
                        itemCount = catItems.size,
                        totalValue = catItems.sumOf { it.purchasePrice }
                    )
                }
                StatisticsUiState(
                    isLoading = false,
                    totalCount = count,
                    totalValue = value,
                    currency = currency,
                    categorySummaries = summaries,
                    items = items
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun setTimeFilter(filter: String) {
        _uiState.update { it.copy(timeFilter = filter) }
        // TODO: 根据筛选条件重新加载数据
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
