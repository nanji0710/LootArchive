package com.nanji.lootarchive.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        val KEY_CURRENCY = stringPreferencesKey("currency")
        val KEY_WARRANTY_REMINDER_DAYS = intPreferencesKey("warranty_reminder_days")
        val KEY_BACKUP_REMINDER_ENABLED = booleanPreferencesKey("backup_reminder_enabled")
        val KEY_BACKUP_REMINDER_DAY = intPreferencesKey("backup_reminder_day")
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode") // "system" | "light" | "dark"
        val KEY_CUSTOM_BACKGROUND_URI = stringPreferencesKey("custom_background_uri")
        val KEY_AVATAR_URI = stringPreferencesKey("avatar_uri")
        val KEY_APP_NAME = stringPreferencesKey("app_name")
        val KEY_PRIMARY_COLOR = intPreferencesKey("primary_color") // ARGB int
        val KEY_SEARCH_HISTORY = stringPreferencesKey("search_history")
        val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    }

    // ── v6.0 搜索历史 ──
    val searchHistory: Flow<List<String>> = dataStore.data.map { prefs ->
        (prefs[KEY_SEARCH_HISTORY] ?: "").split(",").filter { it.isNotBlank() }
    }
    suspend fun addSearchHistory(query: String) {
        dataStore.edit { prefs ->
            val list = (prefs[KEY_SEARCH_HISTORY] ?: "").split(",").filter { it.isNotBlank() }.toMutableList()
            list.remove(query); list.add(0, query)
            if (list.size > 20) list.removeAt(list.lastIndex)
            prefs[KEY_SEARCH_HISTORY] = list.joinToString(",")
        }
    }
    suspend fun clearSearchHistory() {
        dataStore.edit { it[KEY_SEARCH_HISTORY] = "" }
    }

    // ── v6.2 新手引导 ──
    val onboardingCompleted: Flow<Boolean> = dataStore.data.map { it[KEY_ONBOARDING_COMPLETED] ?: false }
    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { it[KEY_ONBOARDING_COMPLETED] = completed }
    }

    // ========== 价格单位 ==========
    val currency: Flow<String> = dataStore.data.map { it[KEY_CURRENCY] ?: "CNY" }
    suspend fun setCurrency(currency: String) {
        dataStore.edit { it[KEY_CURRENCY] = currency }
    }

    // ========== 保修提醒 ==========
    val warrantyReminderDays: Flow<Int> = dataStore.data.map { it[KEY_WARRANTY_REMINDER_DAYS] ?: 7 }
    suspend fun setWarrantyReminderDays(days: Int) {
        dataStore.edit { it[KEY_WARRANTY_REMINDER_DAYS] = days }
    }

    // ========== 备份提醒 ==========
    val backupReminderEnabled: Flow<Boolean> = dataStore.data.map { it[KEY_BACKUP_REMINDER_ENABLED] ?: false }
    suspend fun setBackupReminderEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_BACKUP_REMINDER_ENABLED] = enabled }
    }

    val backupReminderDay: Flow<Int> = dataStore.data.map { it[KEY_BACKUP_REMINDER_DAY] ?: 1 }
    suspend fun setBackupReminderDay(day: Int) {
        dataStore.edit { it[KEY_BACKUP_REMINDER_DAY] = day }
    }

    // ========== 主题 ==========
    val themeMode: Flow<String> = dataStore.data.map { it[KEY_THEME_MODE] ?: "system" }
    suspend fun setThemeMode(mode: String) {
        dataStore.edit { it[KEY_THEME_MODE] = mode }
    }

    // ========== 背景图 ==========
    val customBackgroundUri: Flow<String> = dataStore.data.map { it[KEY_CUSTOM_BACKGROUND_URI] ?: "" }
    suspend fun setCustomBackgroundUri(uri: String) {
        dataStore.edit { it[KEY_CUSTOM_BACKGROUND_URI] = uri }
    }

    // ========== 头像 ==========
    val avatarUri: Flow<String> = dataStore.data.map { it[KEY_AVATAR_URI] ?: "" }
    suspend fun setAvatarUri(uri: String) {
        dataStore.edit { it[KEY_AVATAR_URI] = uri }
    }

    // ========== 主色调 ==========
    val primaryColor: Flow<Int> = dataStore.data.map { it[KEY_PRIMARY_COLOR] ?: 0xFFFFA500.toInt() }
    suspend fun setPrimaryColor(color: Int) {
        dataStore.edit { it[KEY_PRIMARY_COLOR] = color }
    }

    // ========== APP名称 ==========
    val appName: Flow<String> = dataStore.data.map { it[KEY_APP_NAME] ?: "拾物集" }
    suspend fun setAppName(name: String) {
        dataStore.edit { it[KEY_APP_NAME] = name }
    }
}
