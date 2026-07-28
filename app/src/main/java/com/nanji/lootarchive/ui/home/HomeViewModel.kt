package com.nanji.lootarchive.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nanji.lootarchive.data.local.entity.ItemEntity
import com.nanji.lootarchive.data.repository.ItemRepository
import com.nanji.lootarchive.data.repository.SettingsRepository
import com.nanji.lootarchive.domain.model.ItemWithPhotos
import com.nanji.lootarchive.util.Quintet
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

    init { loadData() }

    private fun loadData() {
        viewModelScope.launch {
            // 监听设置中的保修提醒天数变化，动态重算阈值
            settingsRepository.warrantyReminderDays.collectLatest { reminderDays ->
                val threshold = System.currentTimeMillis() + reminderDays * 24L * 60 * 60 * 1000
                combine(
                    combine(
                        itemRepository.getAllItems(),
                        itemRepository.getTotalCount(),
                        itemRepository.getTotalValue(),
                        itemRepository.getWarrantyExpiringCount(threshold),
                        settingsRepository.currency
                    ) { items, count, value, expiringCount, currency ->
                        Quintet(items, count, value, expiringCount, currency)
                    },
                    settingsRepository.appName
                ) { quintet, appName ->
                    val paths = mutableMapOf<Long, String>()
                    quintet.first.forEach { item ->
                        try {
                            val photos = itemRepository.getItemPhotos(item.id).first()
                            if (photos.isNotEmpty()) paths[item.id] = photos.first().photoPath
                        } catch (_: Exception) {}
                    }
                    HomeUiState(
                        isLoading = false, items = quintet.first, photoPaths = paths,
                        totalCount = quintet.second, totalValue = quintet.third,
                        warrantyExpiringCount = quintet.fourth, currency = quintet.fifth, appName = appName
                    )
                }.catch { _uiState.value = _uiState.value.copy(isLoading = false) }
                 .collect { state -> _uiState.value = state }
            }
        }
    }

    fun deleteItem(itemId: Long) { viewModelScope.launch { itemRepository.softDeleteItem(itemId) } }
}
