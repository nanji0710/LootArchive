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
        val KEY_PRIMARY_COLOR = intPreferencesKey("primary_color") // ARGB int
        val KEY_SEARCH_HISTORY = stringPreferencesKey("search_history")
        val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")

        // ── v7.6 外观 ──
        /** 视效模式：immersive | balanced | lite（见 ui/theme/Appearance.kt）。 */
        val KEY_VISUAL_EFFECT_MODE = stringPreferencesKey("visual_effect_mode")
        /** 图标风格：rounded | outlined | duotone。 */
        val KEY_ICON_STYLE = stringPreferencesKey("icon_style")
        /** 启动时静默检查更新（发现新版本只在「检查更新」页提示）。 */
        val KEY_AUTO_CHECK_UPDATE = booleanPreferencesKey("auto_check_update")

        // ── v7.6 AI 助理 ──
        /** AI 助理总开关。关闭后助理页只做纯本地问答。 */
        val KEY_ASSISTANT_ENABLED = booleanPreferencesKey("assistant_enabled")
        /** OpenAI 兼容接口地址，例如 https://api.openai.com/v1 。留空 = 仅本地问答。 */
        val KEY_ASSISTANT_ENDPOINT = stringPreferencesKey("assistant_endpoint")
        /** 接口密钥。仅存本机 DataStore，不随备份导出、不上传。 */
        val KEY_ASSISTANT_API_KEY = stringPreferencesKey("assistant_api_key")
        /** 模型名，例如 gpt-4o-mini。 */
        val KEY_ASSISTANT_MODEL = stringPreferencesKey("assistant_model")
        /** 思考强度：off | low | medium | high（仅对支持推理的模型生效）。 */
        val KEY_ASSISTANT_REASONING = stringPreferencesKey("assistant_reasoning_effort")
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

    // ========== 主色调 ==========
    val primaryColor: Flow<Int> = dataStore.data.map { it[KEY_PRIMARY_COLOR] ?: 0xFFFFA500.toInt() }
    suspend fun setPrimaryColor(color: Int) {
        dataStore.edit { it[KEY_PRIMARY_COLOR] = color }
    }

    // ========== v7.6 外观（视效模式 / 图标风格 / 自动检查更新）==========
    val visualEffectMode: Flow<String> = dataStore.data.map { it[KEY_VISUAL_EFFECT_MODE] ?: "balanced" }
    suspend fun setVisualEffectMode(mode: String) {
        dataStore.edit { it[KEY_VISUAL_EFFECT_MODE] = mode }
    }

    val iconStyle: Flow<String> = dataStore.data.map { it[KEY_ICON_STYLE] ?: "rounded" }
    suspend fun setIconStyle(style: String) {
        dataStore.edit { it[KEY_ICON_STYLE] = style }
    }

    val autoCheckUpdate: Flow<Boolean> = dataStore.data.map { it[KEY_AUTO_CHECK_UPDATE] ?: true }
    suspend fun setAutoCheckUpdate(enabled: Boolean) {
        dataStore.edit { it[KEY_AUTO_CHECK_UPDATE] = enabled }
    }

    // ========== v7.6 AI 助理 ==========
    val assistantEnabled: Flow<Boolean> = dataStore.data.map { it[KEY_ASSISTANT_ENABLED] ?: true }
    suspend fun setAssistantEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_ASSISTANT_ENABLED] = enabled }
    }

    val assistantEndpoint: Flow<String> = dataStore.data.map { it[KEY_ASSISTANT_ENDPOINT] ?: "" }
    suspend fun setAssistantEndpoint(endpoint: String) {
        dataStore.edit { it[KEY_ASSISTANT_ENDPOINT] = endpoint.trim() }
    }

    val assistantApiKey: Flow<String> = dataStore.data.map { it[KEY_ASSISTANT_API_KEY] ?: "" }
    suspend fun setAssistantApiKey(key: String) {
        dataStore.edit { it[KEY_ASSISTANT_API_KEY] = key.trim() }
    }

    val assistantModel: Flow<String> = dataStore.data.map { it[KEY_ASSISTANT_MODEL] ?: "" }
    suspend fun setAssistantModel(model: String) {
        dataStore.edit { it[KEY_ASSISTANT_MODEL] = model.trim() }
    }

    val assistantReasoningEffort: Flow<String> = dataStore.data.map { it[KEY_ASSISTANT_REASONING] ?: "medium" }
    suspend fun setAssistantReasoningEffort(effort: String) {
        dataStore.edit { it[KEY_ASSISTANT_REASONING] = effort }
    }
}
