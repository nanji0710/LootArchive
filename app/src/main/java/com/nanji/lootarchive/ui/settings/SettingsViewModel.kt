package com.nanji.lootarchive.ui.settings

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nanji.lootarchive.data.repository.ItemRepository
import com.nanji.lootarchive.data.repository.SettingsRepository
import com.nanji.lootarchive.util.FormatUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class SettingsUiState(
    val currency: String = "CNY",
    val warrantyReminderDays: Int = 7,
    val backupReminderEnabled: Boolean = false,
    val backupReminderDay: Int = 1,
    val themeMode: String = "system",
    val primaryColor: Int = 0xFFFFA500.toInt(),
    val dynamicColor: Boolean = false,
    val avatarUri: String = "",
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
        // 监听设置变更，使用 update{} 避免覆盖缓存/消息等独立状态
        viewModelScope.launch {
            settingsRepository.themeMode.collect { mode ->
                _uiState.update { it.copy(themeMode = mode) }
            }
        }
        viewModelScope.launch {
            settingsRepository.warrantyReminderDays.collect { days ->
                _uiState.update { it.copy(warrantyReminderDays = days) }
            }
        }
        viewModelScope.launch {
            settingsRepository.currency.collect { currency ->
                _uiState.update { it.copy(currency = currency) }
            }
        }
        viewModelScope.launch {
            settingsRepository.backupReminderEnabled.collect { enabled ->
                _uiState.update { it.copy(backupReminderEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            settingsRepository.backupReminderDay.collect { day ->
                _uiState.update { it.copy(backupReminderDay = day) }
            }
        }
        viewModelScope.launch {
            settingsRepository.appName.collect { name ->
                _uiState.update { it.copy(appName = name) }
            }
        }
        viewModelScope.launch {
            settingsRepository.primaryColor.collect { color ->
                _uiState.update { it.copy(primaryColor = color) }
            }
        }
        viewModelScope.launch {
            settingsRepository.dynamicColor.collect { enabled ->
                _uiState.update { it.copy(dynamicColor = enabled) }
            }
        }
        viewModelScope.launch {
            settingsRepository.avatarUri.collect { uri ->
                _uiState.update { it.copy(avatarUri = uri) }
            }
        }
        viewModelScope.launch {
            itemRepository.getDeletedItems().collect { deleted ->
                _uiState.update { it.copy(trashItemCount = deleted.size) }
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

    fun setPrimaryColor(color: Int) {
        _uiState.update { it.copy(primaryColor = color) }
        viewModelScope.launch { settingsRepository.setPrimaryColor(color) }
    }

    fun setDynamicColor(enabled: Boolean) {
        _uiState.update { it.copy(dynamicColor = enabled) }
        viewModelScope.launch { settingsRepository.setDynamicColor(enabled) }
    }

    fun setAvatarUri(uri: String) {
        _uiState.update { it.copy(avatarUri = uri) }
        viewModelScope.launch { settingsRepository.setAvatarUri(uri) }
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
                    dirSizeSafe(app.cacheDir) + dirSizeSafe(app.codeCacheDir)
                }
                _uiState.update { it.copy(cacheSize = size, cacheSizeFormatted = FormatUtil.formatSize(size), isCalculatingCache = false) }
            } catch (e: Exception) {
                android.util.Log.e("SettingsVM", "计算缓存失败", e)
                _uiState.update { it.copy(cacheSizeFormatted = "无法获取", isCalculatingCache = false) }
            }
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            _uiState.update { it.copy(isClearing = true) }
            try {
                withContext(Dispatchers.IO) {
                    listOf(app.cacheDir, app.codeCacheDir).forEach { dir ->
                        dir.listFiles()?.forEach { f ->
                            if (f.isDirectory) f.deleteRecursively() else f.delete()
                        }
                    }
                }
                val size = withContext(Dispatchers.IO) {
                    dirSizeSafe(app.cacheDir) + dirSizeSafe(app.codeCacheDir)
                }
                _uiState.update { it.copy(cacheSize = size, cacheSizeFormatted = FormatUtil.formatSize(size), isClearing = false, message = "缓存已清除") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isClearing = false, message = "清除失败: ${e.message}") }
            }
        }
    }

    private fun dirSizeSafe(dir: java.io.File): Long {
        if (!dir.exists()) return 0L
        var total = 0L
        val stack = ArrayDeque<java.io.File>()
        stack.add(dir)
        while (stack.isNotEmpty()) {
            val current = stack.removeLast()
            val children = try { current.listFiles() } catch (e: Exception) {
                android.util.Log.e("SettingsVM", "listFiles failed dir=${current.absolutePath}", e); null
            } ?: continue
            for (child in children) {
                try {
                    if (child.isFile) total += child.length()
                    else if (child.isDirectory) stack.add(child)
                } catch (e: Exception) {
                    android.util.Log.e("SettingsVM", "File length failed", e)
                }
            }
        }
        return total
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    // v6.3 新手引导重置
    fun resetOnboarding() {
        viewModelScope.launch { settingsRepository.setOnboardingCompleted(false) }
    }
}
