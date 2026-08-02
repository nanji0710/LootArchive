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
    val warrantyReminderDays: Int = 7,
    val currency: String = "CNY",
    val saleRevenue: Double = 0.0,
    val appName: String = "拾物集"
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val itemRepository: ItemRepository,
    private val settingsRepository: SettingsRepository,
    private val expService: com.nanji.lootarchive.data.ExpService
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init { loadData(); viewModelScope.launch { expService.recalculateProfile() } }

    private fun loadData() {
        viewModelScope.launch {
            // 监听设置中的保修提醒天数变化，动态重算阈值
            settingsRepository.warrantyReminderDays.collectLatest { reminderDays ->
                val threshold = System.currentTimeMillis() + reminderDays * 24L * 60 * 60 * 1000
                combine(
                    combine(
                        itemRepository.getAllItems(),
                        itemRepository.getOwnedCount(),
                        itemRepository.getOwnedValue(),
                        itemRepository.getWarrantyExpiringCount(threshold),
                        settingsRepository.currency
                    ) { items, count, value, expiringCount, currency ->
                        Quintet(items, count, value, expiringCount, currency)
                    },
                    settingsRepository.appName
                ) { quintet, appName ->
                    // 批量取首图，避免逐物品 N+1 查询
                    val paths = try { itemRepository.getAllFirstPhotos() } catch (e: Exception) { android.util.Log.e("HomeVM", "Load photos failed", e); emptyMap() }
                    val saleRev = quintet.first.filter { it.status == "sold" && !it.isDeleted }.sumOf { it.salePrice ?: 0.0 }
                    HomeUiState(
                        isLoading = false, items = quintet.first, photoPaths = paths,
                        totalCount = quintet.second, totalValue = quintet.third,
                        warrantyExpiringCount = quintet.fourth, warrantyReminderDays = reminderDays.toInt(),
                        currency = quintet.fifth, saleRevenue = saleRev, appName = appName
                    )
                }.catch { e ->
                    android.util.Log.e("HomeVM", "loadData failed", e)
                    _uiState.update { it.copy(isLoading = false) }
                }
                 .collect { state -> _uiState.value = state }
            }
        }
    }

    fun deleteItem(itemId: Long) { viewModelScope.launch { itemRepository.softDeleteItem(itemId); expService.recalculateProfile() } }

    /** 下拉刷新：重新计算 EXP/成就（数据本身由 Room Flow 响应式驱动，无需重复查询） */
    fun refresh() { viewModelScope.launch { expService.recalculateProfile() } }
}
