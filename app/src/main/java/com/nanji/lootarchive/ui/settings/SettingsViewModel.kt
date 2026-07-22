package com.nanji.lootarchive.ui.settings

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nanji.lootarchive.data.repository.ItemRepository
import com.nanji.lootarchive.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

data class SettingsUiState(
    val currency: String = "CNY",
    val warrantyReminderDays: Int = 7,
    val backupReminderEnabled: Boolean = false,
    val backupReminderDay: Int = 1,
    val themeMode: String = "system",
    val appName: String = "拾物集",
    val trashItemCount: Int = 0,
    val cacheSize: Long = 0L,
    val cacheSizeFormatted: String = "计算中...",
    val isClearing: Boolean = false,
    val isCalculatingCache: Boolean = true,
    val message: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val app: Application,
    private val settingsRepository: SettingsRepository,
    private val itemRepository: ItemRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        calculateCacheSize()
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

    fun calculateCacheSize() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCalculatingCache = true) }
            try {
                val size = withContext(Dispatchers.IO) {
                    var total = 0L
                    // Coil image cache
                    val coilCache = File(app.cacheDir, "coil_cache")
                    if (coilCache.exists()) total += coilCache.walkTopDown().filter { it.isFile }.sumOf { it.length() }
                    // App internal cache (exclude coil_cache to avoid double count)
                    app.cacheDir.listFiles()?.filter { it.name != "coil_cache" }?.forEach { f ->
                        if (f.isDirectory) total += f.walkTopDown().filter { it.isFile }.sumOf { it.length() }
                        else total += f.length()
                    }
                    total
                }
                _uiState.update { it.copy(cacheSize = size, cacheSizeFormatted = formatSize(size), isCalculatingCache = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(cacheSizeFormatted = "无法获取", isCalculatingCache = false) }
            }
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            _uiState.update { it.copy(isClearing = true) }
            try {
                withContext(Dispatchers.IO) {
                    // 仅清除缓存目录，不清除 app 数据（数据库/preferences 在 filesDir）
                    app.cacheDir.listFiles()?.forEach { f ->
                        if (f.isDirectory) f.deleteRecursively() else f.delete()
                    }
                }
                _uiState.update { it.copy(cacheSize = 0L, cacheSizeFormatted = "0 B", isClearing = false, message = "缓存已清除") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isClearing = false, message = "清除失败: ${e.message}") }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private fun formatSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${"%.1f".format(bytes.toDouble() / (1024 * 1024))} MB"
            else -> "${"%.2f".format(bytes.toDouble() / (1024 * 1024 * 1024))} GB"
        }
    }

    private data class Quintet(
        val currency: String,
        val reminderDays: Int,
        val backupEnabled: Boolean,
        val backupDay: Int,
        val theme: String
    )
}
