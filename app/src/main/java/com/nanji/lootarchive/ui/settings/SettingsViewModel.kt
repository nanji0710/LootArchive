package com.nanji.lootarchive.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nanji.lootarchive.data.repository.ItemRepository
import com.nanji.lootarchive.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val currency: String = "CNY",
    val warrantyReminderDays: Int = 7,
    val backupReminderEnabled: Boolean = false,
    val backupReminderDay: Int = 1,
    val themeMode: String = "system",
    val appName: String = "拾物集",
    val trashItemCount: Int = 0,
    val isClearing: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val itemRepository: ItemRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                combine(
                    settingsRepository.currency,
                    settingsRepository.warrantyReminderDays,
                    settingsRepository.backupReminderEnabled,
                    settingsRepository.backupReminderDay,
                    settingsRepository.themeMode
                ) { currency, reminderDays, backupEnabled, backupDay, theme ->
                    Quintet(currency, reminderDays, backupEnabled, backupDay, theme)
                },
                combine(
                    settingsRepository.appName,
                    itemRepository.getDeletedItems()
                ) { appName, deletedItems ->
                    Pair(appName, deletedItems.size)
                }
            ) { quintet, (appName, trashCount) ->
                SettingsUiState(
                    currency = quintet.currency,
                    warrantyReminderDays = quintet.reminderDays,
                    backupReminderEnabled = quintet.backupEnabled,
                    backupReminderDay = quintet.backupDay,
                    themeMode = quintet.theme,
                    appName = appName,
                    trashItemCount = trashCount
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun setCurrency(currency: String) {
        viewModelScope.launch { settingsRepository.setCurrency(currency) }
    }

    fun setWarrantyReminderDays(days: Int) {
        viewModelScope.launch { settingsRepository.setWarrantyReminderDays(days) }
    }

    fun setBackupReminder(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setBackupReminderEnabled(enabled) }
    }

    fun setBackupReminderDay(day: Int) {
        viewModelScope.launch { settingsRepository.setBackupReminderDay(day) }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun setBackgroundUri(uri: String) {
        viewModelScope.launch { settingsRepository.setCustomBackgroundUri(uri) }
    }

    fun clearBackground() {
        viewModelScope.launch { settingsRepository.setCustomBackgroundUri("") }
    }

    fun setAppName(name: String) {
        viewModelScope.launch { settingsRepository.setAppName(name) }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            _uiState.update { it.copy(isClearing = true) }
            try {
                itemRepository.emptyTrash()
                _uiState.update { it.copy(isClearing = false, message = "回收站已清空") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isClearing = false, message = "清空失败: ${e.message}") }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private data class Quintet(
        val currency: String,
        val reminderDays: Int,
        val backupEnabled: Boolean,
        val backupDay: Int,
        val theme: String
    )
}
