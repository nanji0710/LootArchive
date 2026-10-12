package com.nanji.lootarchive.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nanji.lootarchive.data.local.entity.ItemEntity
import com.nanji.lootarchive.data.repository.ItemRepository
import com.nanji.lootarchive.data.repository.SettingsRepository
import com.nanji.lootarchive.domain.model.ItemStatus
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
    /** 保修待提醒明细 —— 与 [warrantyExpiringCount] 同源（DAO 已剔除已出、按到期日升序） */
    val warrantyExpiringItems: List<ItemEntity> = emptyList(),
    val warrantyReminderDays: Int = 7,
    val currency: String = "CNY",
    val saleRevenue: Double = 0.0
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
                    itemRepository.getAllItems(),
                    itemRepository.getOwnedCount(),
                    itemRepository.getOwnedValue(),
                    // 直接取明细列表：计数 = 列表尺寸，指标卡与点开明细
                    // 由同一个查询驱动，不会再出现"卡片说 3 个、明细里 5 个"的分裂。
                    itemRepository.getWarrantyExpiringItems(threshold),
                    settingsRepository.currency
                ) { items, count, value, expiringItems, currency ->
                    // 批量取首图，避免逐物品 N+1 查询
                    val paths = try { itemRepository.getAllFirstPhotos() } catch (e: Exception) { android.util.Log.e("HomeVM", "Load photos failed", e); emptyMap() }
                    val saleRev = items.filter { ItemStatus.fromCode(it.status) == ItemStatus.SOLD && !it.isDeleted }.sumOf { it.salePrice ?: 0.0 }
                    HomeUiState(
                        isLoading = false, items = items, photoPaths = paths,
                        totalCount = count, totalValue = value,
                        warrantyExpiringCount = expiringItems.size, warrantyExpiringItems = expiringItems,
                        warrantyReminderDays = reminderDays.toInt(),
                        currency = currency, saleRevenue = saleRev
                    )
                }.catch { e ->
                    android.util.Log.e("HomeVM", "loadData failed", e)
                    _uiState.update { it.copy(isLoading = false) }
                }
                 .collect { state -> _uiState.value = state }
            }
        }
    }

    fun deleteItem(itemId: Long) { viewModelScope.launch { itemRepository.softDeleteItem(itemId) } }

    /** 下拉刷新：本轮起为空实现（原先只重算 EXP/成就），待 A-5 S2 摘除刷新控件后删除本方法 */
    fun refresh() { }
}
