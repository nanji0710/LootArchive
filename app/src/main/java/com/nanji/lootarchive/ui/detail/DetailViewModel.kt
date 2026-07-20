package com.nanji.lootarchive.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nanji.lootarchive.data.repository.ItemRepository
import com.nanji.lootarchive.data.repository.SettingsRepository
import com.nanji.lootarchive.domain.model.ItemWithPhotos
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DetailUiState(
    val isLoading: Boolean = true,
    val itemWithPhotos: ItemWithPhotos? = null,
    val currency: String = "CNY",
    val showDeleteDialog: Boolean = false,
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

    fun loadItem(itemId: Long) {
        currentItemId = itemId
        viewModelScope.launch {
            combine(
                settingsRepository.currency,
                itemRepository.getItemPhotos(itemId)
            ) { currency, photos ->
                Pair(currency, photos)
            }.collect { (currency, photos) ->
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
}
