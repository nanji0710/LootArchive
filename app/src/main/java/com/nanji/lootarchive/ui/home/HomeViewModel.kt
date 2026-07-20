package com.nanji.lootarchive.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nanji.lootarchive.data.local.entity.ItemEntity
import com.nanji.lootarchive.data.repository.ItemRepository
import com.nanji.lootarchive.data.repository.SettingsRepository
import com.nanji.lootarchive.domain.model.ItemWithPhotos
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = true,
    val items: List<ItemEntity> = emptyList(),
    val photoPaths: Map<Long, String> = emptyMap(),
    val totalCount: Int = 0,
    val totalValue: Double = 0.0,
    val warrantyExpiringCount: Int = 0,
    val currency: String = "CNY",
    val appName: String = "拾物集"
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val itemRepository: ItemRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val warrantyThresholdMs = 7L * 24 * 60 * 60 * 1000
    private val warrantyThreshold = System.currentTimeMillis() + warrantyThresholdMs

    init { loadData() }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                combine(
                    itemRepository.getAllItems(),
                    itemRepository.getTotalCount(),
                    itemRepository.getTotalValue(),
                    itemRepository.getWarrantyExpiringCount(warrantyThreshold),
                    settingsRepository.currency
                ) { items, count, value, expiringCount, currency ->
                    Quintet(items, count, value, expiringCount, currency)
                },
                settingsRepository.appName
            ) { quintet, appName ->
                // 加载每件物品的第一张照片
                val paths = mutableMapOf<Long, String>()
                quintet.items.forEach { item ->
                    try {
                        val photos = itemRepository.getItemPhotos(item.id).first()
                        if (photos.isNotEmpty()) paths[item.id] = photos.first().photoPath
                    } catch (_: Exception) {}
                }
                HomeUiState(
                    isLoading = false, items = quintet.items, photoPaths = paths,
                    totalCount = quintet.count, totalValue = quintet.value,
                    warrantyExpiringCount = quintet.expiringCount, currency = quintet.currency, appName = appName
                )
            }.catch { e -> _uiState.value = _uiState.value.copy(isLoading = false) }
             .collect { state -> _uiState.value = state }
        }
    }

    private data class Quintet(val items: List<ItemEntity>, val count: Int, val value: Double, val expiringCount: Int, val currency: String)

    fun deleteItem(itemId: Long) { viewModelScope.launch { itemRepository.softDeleteItem(itemId) } }
}
