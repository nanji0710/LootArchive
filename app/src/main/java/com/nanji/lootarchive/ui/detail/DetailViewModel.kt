package com.nanji.lootarchive.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nanji.lootarchive.data.repository.ItemRepository
import com.nanji.lootarchive.data.repository.SettingsRepository
import com.nanji.lootarchive.domain.model.ItemWithPhotos
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DetailUiState(
    val isLoading: Boolean = true,
    val itemWithPhotos: ItemWithPhotos? = null,
    val currency: String = "CNY",
    val showDeleteDialog: Boolean = false,
    val showStatusSheet: Boolean = false,
    val isDeleted: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val itemRepository: ItemRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    private var currentItemId: Long = 0
    private var loadJob: Job? = null

    fun loadItem(itemId: Long) {
        currentItemId = itemId
        // 同一件物品可能被两个地方加载（点击时预取 + 详情页 LaunchedEffect）。
        // 旧协程不取消的话，两个 collector 会抢着写 uiState，出现「上一件物品的数据盖掉新的」。
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            combine(
                itemRepository.getItemByIdFlow(itemId),
                itemRepository.getItemPhotos(itemId),
                settingsRepository.currency
            ) { item, photos, currency ->
                Triple(item, photos, currency)
            }.collect { (item, photos, currency) ->
                if (item == null) return@collect
                val itemWithPhotos = itemRepository.getItemWithPhotos(itemId)
                _uiState.value = DetailUiState(
                    isLoading = false,
                    itemWithPhotos = itemWithPhotos?.copy(photos = photos),
                    currency = currency
                )
            }
        }
    }

    fun showDeleteConfirm() {
        _uiState.update { it.copy(showDeleteDialog = true) }
    }

    fun dismissDeleteDialog() {
        _uiState.update { it.copy(showDeleteDialog = false) }
    }

    fun deleteItem() {
        viewModelScope.launch {
            try {
                itemRepository.softDeleteItem(currentItemId)
                _uiState.update { it.copy(showDeleteDialog = false, isDeleted = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "删除失败: ${e.message}") }
            }
        }
    }

    fun showStatusSheet() {
        _uiState.update { it.copy(showStatusSheet = true) }
    }

    fun dismissStatusSheet() {
        _uiState.update { it.copy(showStatusSheet = false) }
    }

    fun updateItemStatus(status: String) {
        viewModelScope.launch {
            try {
                itemRepository.updateItemStatus(currentItemId, status)
                loadItem(currentItemId)
                dismissStatusSheet()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "状态更新失败: ${e.message}") }
            }
        }
    }
}
