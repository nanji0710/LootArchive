package com.nanji.lootarchive.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nanji.lootarchive.ui.component.*
import com.nanji.lootarchive.ui.liquidglass.LiquidAlertDialog
import com.nanji.lootarchive.ui.liquidglass.LiquidGlassButton
import com.nanji.lootarchive.ui.theme.*

/**
 * 通用设置页 —— 保持应用原有的暖色玻璃版式，只把「视效模式 / 图标风格」
 * 改成 SleepDown 那种点击后就地弹出玻璃菜单的交互（不跳页）。
 */
@Composable
fun GeneralSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val effect = VisualEffectMode.fromKey(uiState.visualEffectMode)
    val iconStyle = IconStyle.fromKey(uiState.iconStyle)

    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showWarrantyDialog by remember { mutableStateOf(false) }
    var showBackupDayDialog by remember { mutableStateOf(false) }
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var editWarrantyDays by remember { mutableStateOf("") }
    var editBackupDay by remember { mutableStateOf("") }
    val context = androidx.compose.ui.platform.LocalContext.current

    SettingsPageScaffold(title = "通用设置", onNavigateBack = onNavigateBack) {

        // ── 外观 ──
        SettingsGroupHeader(title = "外观", icon = AppIcons.palette)
        SettingsCard {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                ThemePreview(
                    modifier = Modifier.weight(1f),
                    dark = false,
                    selected = uiState.themeMode == "light",
                    onClick = { viewModel.setThemeMode("light") },
                )
                ThemePreview(
                    modifier = Modifier.weight(1f),
                    dark = true,
                    selected = uiState.themeMode == "dark",
                    onClick = { viewModel.setThemeMode("dark") },
                )
            }
            PreferenceDivider()
            TogglePreferenceRow(
                title = "跟随系统",
                subtitle = "根据系统深浅色自动切换",
                icon = AppIcons.lightMode,
                checked = uiState.themeMode == "system",
                onCheckedChange = { follow ->
                    viewModel.setThemeMode(if (follow) "system" else "light")
                },
            )
            PreferenceDivider()
            // SleepDown 同款：点击行后在行下方弹出玻璃菜单，选中即应用，不跳页。
            DropdownPreferenceRow(
                title = "视效模式",
                subtitle = "使用液态玻璃材质",
                value = effect.label,
                icon = AppIcons.visualEffect,
                options = VisualEffectMode.entries.map { it.label },
                selectedIndex = effect.ordinal,
                onSelected = { index -> viewModel.setVisualEffectMode(VisualEffectMode.entries[index].key) },
            )
            PreferenceDivider()
            DropdownPreferenceRow(
                title = "图标风格",
                subtitle = "底部导航与设置图标",
                value = iconStyle.label,
                icon = AppIcons.iconStyle,
                options = IconStyle.entries.map { it.label },
                selectedIndex = iconStyle.ordinal,
                onSelected = { index -> viewModel.setIconStyle(IconStyle.entries[index].key) },
            )
        }

        // ── 金额与单位 ──
        SettingsGroupHeader(title = "金额与单位", icon = AppIcons.currency)
        SettingsCard {
            PreferenceRow(
                title = "价格单位",
                icon = AppIcons.currency,
                subtitle = "首页总额、统计与卡片价格同步切换",
                value = currencyLabel(uiState.currency),
                onClick = { showCurrencyDialog = true },
            )
        }

        // ── 提醒 ──
        SettingsGroupHeader(title = "提醒", icon = AppIcons.notifications)
        SettingsCard {
            PreferenceRow(
                title = "保修到期提醒",
                icon = AppIcons.shield,
                subtitle = "到期前在首页「保修待提醒」中计数",
                value = "提前 ${uiState.warrantyReminderDays} 天",
                onClick = {
                    editWarrantyDays = uiState.warrantyReminderDays.toString()
                    showWarrantyDialog = true
                },
            )
            PreferenceDivider()
            TogglePreferenceRow(
                title = "备份提醒",
                subtitle = "按设定日期提醒导出一次备份",
                icon = AppIcons.backup,
                checked = uiState.backupReminderEnabled,
                onCheckedChange = viewModel::setBackupReminder,
            )
            if (uiState.backupReminderEnabled) {
                PreferenceDivider()
                PreferenceRow(
                    title = "每月提醒日",
                    icon = AppIcons.history,
                    subtitle = "每月这一天的上午提醒你备份",
                    value = "${uiState.backupReminderDay} 日",
                    onClick = {
                        editBackupDay = uiState.backupReminderDay.toString()
                        showBackupDayDialog = true
                    },
                )
            }
        }

        // ── 存储 ──
        SettingsGroupHeader(title = "存储", icon = AppIcons.storage)
        SettingsCard {
            PreferenceRow(
                title = "缓存",
                icon = AppIcons.storage,
                subtitle = if (uiState.isCalculatingCache) "正在统计…" else "图片缓存与临时文件",
                value = uiState.cacheSizeFormatted,
                trailing = {
                    LiquidGlassButton(
                        onClick = { showClearCacheDialog = true },
                        backdrop = rememberRowBackdrop(),
                        height = 36.dp,
                        horizontalPadding = 12.dp,
                        enabled = !uiState.isClearing,
                    ) {
                        if (uiState.isClearing) {
                            CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp, color = Primary())
                        } else {
                            Text("清除", style = MaterialTheme.typography.labelMedium, color = Primary())
                        }
                    }
                },
            )
        }

        // ── 其他 ──
        SettingsGroupHeader(title = "其他", icon = AppIcons.tune)
        SettingsCard {
            PreferenceRow(
                title = "重新查看新手引导",
                icon = AppIcons.school,
                subtitle = "下次启动时重新展示引导页",
                onClick = {
                    viewModel.resetOnboarding()
                    android.widget.Toast.makeText(context, "下次启动将重新展示引导", android.widget.Toast.LENGTH_SHORT).show()
                },
            )
        }

        Spacer(Modifier.height(24.dp))
    }

    if (showCurrencyDialog) {
        ChoiceDialog(
            title = "选择价格单位",
            options = CurrencyOptions.map { it.first },
            selected = CurrencyOptions.firstOrNull { it.second == uiState.currency }?.first ?: CurrencyOptions.first().first,
            onSelect = { label ->
                CurrencyOptions.firstOrNull { it.first == label }?.let { viewModel.setCurrency(it.second) }
                showCurrencyDialog = false
            },
            onDismiss = { showCurrencyDialog = false },
        )
    }

    if (showWarrantyDialog) {
        NumberInputDialog(
            title = "保修提醒天数",
            hint = "到期前多少天开始提醒（1–365）",
            value = editWarrantyDays,
            onValueChange = { editWarrantyDays = it },
            onConfirm = {
                editWarrantyDays.toIntOrNull()?.takeIf { it in 1..365 }?.let(viewModel::setWarrantyReminderDays)
                showWarrantyDialog = false
            },
            onDismiss = { showWarrantyDialog = false },
        )
    }

    if (showBackupDayDialog) {
        NumberInputDialog(
            title = "每月备份提醒日",
            hint = "每月哪一天提醒（1–28）",
            value = editBackupDay,
            onValueChange = { editBackupDay = it },
            onConfirm = {
                editBackupDay.toIntOrNull()?.takeIf { it in 1..28 }?.let(viewModel::setBackupReminderDay)
                showBackupDayDialog = false
            },
            onDismiss = { showBackupDayDialog = false },
        )
    }

    if (showClearCacheDialog) {
        LiquidAlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            title = { Text("清除缓存", color = TextPrimary(), fontWeight = FontWeight.SemiBold) },
            text = {
                Text(
                    "将清除图片缓存等临时数据（约 ${uiState.cacheSizeFormatted}），不会影响物品数据与设置。",
                    color = TextSecondary(),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearCache()
                    showClearCacheDialog = false
                }) { Text("清除", color = Primary()) }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheDialog = false }) { Text("取消") }
            },
        )
    }
}

/**
 * 深浅色预览：用一块缩小的"手机屏"示意两种主题，点卡片即切换。
 */
@Composable
private fun ThemePreview(
    modifier: Modifier,
    dark: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(148.dp)
                .clip(AppShape.panel)
                .background(if (dark) Color(0xFF141418) else Color(0xFFFBF9F6))
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) Primary() else TextAuxiliary().copy(alpha = 0.25f),
                    shape = AppShape.panel,
                ),
        ) {
            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    Modifier.fillMaxWidth(0.62f).height(7.dp).clip(AppShape.chip)
                        .background(if (dark) Color.White.copy(alpha = 0.25f) else Color(0xFF1C1917).copy(alpha = 0.25f))
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    repeat(3) {
                        Box(
                            Modifier.weight(1f).height(46.dp).clip(AppShape.chip)
                                .background(Primary().copy(alpha = if (dark) 0.28f else 0.18f))
                        )
                    }
                }
                repeat(2) {
                    Box(
                        Modifier.fillMaxWidth().height(12.dp).clip(AppShape.chip)
                            .background(if (dark) Color.White.copy(alpha = 0.10f) else Color(0xFF1C1917).copy(alpha = 0.08f))
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            if (dark) "暗色模式" else "亮色模式",
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) Primary() else TextSecondary(),
        )
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier
                .size(20.dp)
                .clip(CircleShape)
                .border(2.dp, if (selected) Primary() else TextAuxiliary().copy(alpha = 0.45f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(Primary()))
            }
        }
    }
}

@Composable
private fun rememberRowBackdrop(): com.kyant.backdrop.Backdrop {
    val color = LocalGlassColors.current.cardBg
    return com.kyant.backdrop.backdrops.rememberLayerBackdrop {
        drawRect(color)
        drawContent()
    }
}

/** 货币选项：值与符号分离，符号交给显示层。 */
private val CurrencyOptions = listOf(
    "人民币 ¥" to "CNY",
    "美元 $" to "USD",
    "欧元 €" to "EUR",
    "日元 ¥" to "JPY",
    "港币 HK$" to "HKD",
    "新台币 NT$" to "TWD",
)

private fun currencyLabel(code: String): String =
    CurrencyOptions.firstOrNull { it.second == code }?.first ?: code

@Composable
private fun ChoiceDialog(
    title: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    LiquidAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = TextPrimary(), fontWeight = FontWeight.SemiBold) },
        text = {
            Column {
                options.forEach { option ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = option == selected,
                            onClick = { onSelect(option) },
                            colors = RadioButtonDefaults.colors(selectedColor = Primary()),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            option,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (option == selected) Primary() else TextPrimary(),
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun NumberInputDialog(
    title: String,
    hint: String,
    value: String,
    onValueChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    LiquidAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = TextPrimary(), fontWeight = FontWeight.SemiBold) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { input -> onValueChange(input.filter { it.isDigit() }.take(3)) },
                label = { Text(hint) },
                singleLine = true,
                shape = AppShape.panel,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text("确认", color = Primary()) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}
