package com.nanji.lootarchive.ui.recyclebin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nanji.lootarchive.data.local.entity.ItemEntity
import com.nanji.lootarchive.data.repository.ItemRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecycleBinUiState(
    val deletedItems: List<ItemEntity> = emptyList(),
    val isLoading: Boolean = true,
    val showEmptyConfirm: Boolean = false,
    val showDeleteConfirm: Boolean = false,
    val targetItem: ItemEntity? = null,
    val message: String? = null
)

@HiltViewModel
class RecycleBinViewModel @Inject constructor(
    private val itemRepository: ItemRepository,
    private val expService: com.nanji.lootarchive.data.ExpService
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecycleBinUiState())
    val uiState: StateFlow<RecycleBinUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            itemRepository.getDeletedItems().collect { items ->
                _uiState.update { it.copy(deletedItems = items, isLoading = false) }
            }
        }
    }

    fun restoreItem(item: ItemEntity) {
        viewModelScope.launch {
            itemRepository.restoreItem(item.id)
            expService.recalculateProfile()
            _uiState.update { it.copy(message = "「${item.name}」已还原") }
        }
    }

    fun showDeleteConfirm(item: ItemEntity) {
        _uiState.update { it.copy(showDeleteConfirm = true, targetItem = item) }
    }

    fun dismissDeleteConfirm() {
        _uiState.update { it.copy(showDeleteConfirm = false, targetItem = null) }
    }

    fun hardDeleteItem(item: ItemEntity) {
        viewModelScope.launch {
            itemRepository.hardDeleteItem(item.id)
            expService.recalculateProfile()
            _uiState.update { it.copy(showDeleteConfirm = false, targetItem = null, message = "「${item.name}」已彻底删除") }
        }
    }

    fun showEmptyConfirm() {
        _uiState.update { it.copy(showEmptyConfirm = true) }
    }

    fun dismissEmptyConfirm() {
        _uiState.update { it.copy(showEmptyConfirm = false) }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            itemRepository.emptyTrash()
            expService.recalculateProfile()
            _uiState.update { it.copy(showEmptyConfirm = false, message = "回收站已清空") }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
