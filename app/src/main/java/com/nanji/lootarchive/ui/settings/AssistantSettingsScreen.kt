package com.nanji.lootarchive.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nanji.lootarchive.data.assistant.ModelCatalog
import com.nanji.lootarchive.data.assistant.ModelOption
import com.nanji.lootarchive.data.assistant.ReasoningEffort
import com.nanji.lootarchive.ui.component.*
import com.nanji.lootarchive.ui.liquidglass.LiquidAlertDialog
import com.nanji.lootarchive.ui.liquidglass.LiquidGlassButton
import com.nanji.lootarchive.ui.theme.*
import kotlinx.coroutines.launch

/**
 * AI 助理设置页。
 *
 * 设计原则：**默认不联网**。这里配的接口是可选的增强项 —— 不填就只跑本机规则问答，
 * 填了才把「问题 + 不含照片的库存摘要」发给你指定的服务商。所以页面把
 * 「当前模式」放在最上面，并把隐私边界写在最后一段正文里。
 */
@Composable
fun AssistantSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    var showEndpointDialog by remember { mutableStateOf(false) }
    var showKeyDialog by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }

    // 模型目录：接口地址 + Key 都填好后自动拉一次，也可以手动刷新
    var models by remember { mutableStateOf<List<ModelOption>>(emptyList()) }
    var modelLoading by remember { mutableStateOf(false) }
    var modelError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    fun loadModels() {
        if (uiState.assistantEndpoint.isBlank() || uiState.assistantApiKey.isBlank()) {
            modelError = "请先填写接口地址和 API Key"
            return
        }
        modelLoading = true
        modelError = null
        scope.launch {
            ModelCatalog.fetch(uiState.assistantEndpoint, uiState.assistantApiKey)
                .onSuccess { list ->
                    models = list
                    // 当前模型不在列表里时自动选第一个，避免"已配置但不可用"
                    val selected = list.firstOrNull { it.id == uiState.assistantModel } ?: list.first()
                    if (uiState.assistantModel.isBlank() || list.none { it.id == uiState.assistantModel }) {
                        viewModel.setAssistantModel(selected.id)
                    }
                    // 当前思考强度不在该模型支持档位里时，换成该模型的默认档
                    if (uiState.assistantReasoningEffort !in selected.reasoningLevels) {
                        viewModel.setAssistantReasoningEffort(ReasoningEffort.defaultLevel(selected.reasoningLevels))
                    }
                }
                .onFailure { modelError = it.message ?: "拉取失败" }
            modelLoading = false
        }
    }
    LaunchedEffect(uiState.assistantEndpoint, uiState.assistantApiKey) {
        if (uiState.assistantEndpoint.isNotBlank() && uiState.assistantApiKey.isNotBlank() && models.isEmpty()) {
            loadModels()
        }
    }

    SettingsPageScaffold(title = "AI 助理", onNavigateBack = onNavigateBack) {

        // ── 状态卡：一句话说清现在用的是哪种模式 ──
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = AppShape.sheet,
            colors = CardDefaults.cardColors(containerColor = CardBg()),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Column(Modifier.padding(18.dp)) {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    IconTile(AppIcons.assistant, Primary(), size = 44.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (uiState.assistantRemoteReady) "已接入模型服务" else "仅本地分析",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary(),
                            fontFamily = FredokaFont,
                        )
                        Text(
                            if (uiState.assistantRemoteReady) {
                                uiState.assistantModel.ifBlank { "未命名模型" }
                            } else {
                                "不联网 · 用本机物品数据回答"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = TextAuxiliary(),
                        )
                    }
                    Surface(shape = AppShape.pill, color = Primary().copy(alpha = 0.12f)) {
                        Text(
                            if (uiState.assistantEnabled) "已启用" else "已关闭",
                            style = MaterialTheme.typography.labelSmall,
                            color = Primary(),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }
            }
        }

        SettingsGroupHeader(title = "助理", icon = AppIcons.settings)
        SettingsCard {
            TogglePreferenceRow(
                title = "启用 AI 助理",
                subtitle = "关闭后底部导航的助理入口会隐藏",
                icon = AppIcons.assistant,
                checked = uiState.assistantEnabled,
                onCheckedChange = viewModel::setAssistantEnabled,
            )
        }

        SettingsGroupHeader(
            title = "模型服务（可选）",
            icon = AppIcons.cloud,
            subtitle = "留空则只使用本地问答，不产生任何网络请求",
        )
        SettingsCard {
            PreferenceRow(
                title = "接口地址",
                icon = AppIcons.link,
                subtitle = "OpenAI 兼容接口，如 https://api.openai.com/v1",
                value = if (uiState.assistantEndpoint.isBlank()) "未配置" else "已填写",
                onClick = { showEndpointDialog = true },
            )
            PreferenceDivider()
            PreferenceRow(
                title = "API Key",
                icon = AppIcons.key,
                subtitle = "仅保存在本机，不随备份导出",
                value = if (uiState.assistantApiKey.isBlank()) "未配置" else maskKey(uiState.assistantApiKey),
                onClick = { showKeyDialog = true },
            )
            PreferenceDivider()
            PreferenceRow(
                title = "拉取模型列表",
                icon = AppIcons.refresh,
                subtitle = modelError ?: "按接口地址与 Key 读取全部可用模型",
                value = when {
                    modelLoading -> "拉取中…"
                    models.isNotEmpty() -> "${models.size} 个"
                    else -> "未拉取"
                },
                onClick = { loadModels() },
            )
            PreferenceDivider()
            if (models.isNotEmpty()) {
                val selectedModel = models.firstOrNull { it.id == uiState.assistantModel } ?: models.first()
                DropdownPreferenceRow(
                    title = "模型",
                    subtitle = "从接口返回的 ${models.size} 个模型中任选",
                    value = uiState.assistantModel.ifBlank { "未选择" },
                    icon = AppIcons.memory,
                    options = models.map { it.id },
                    selectedIndex = models.indexOfFirst { it.id == uiState.assistantModel }.coerceAtLeast(0),
                    onSelected = { index ->
                        val picked = models[index]
                        viewModel.setAssistantModel(picked.id)
                        if (uiState.assistantReasoningEffort !in picked.reasoningLevels) {
                            viewModel.setAssistantReasoningEffort(ReasoningEffort.defaultLevel(picked.reasoningLevels))
                        }
                    },
                )
            } else {
                PreferenceRow(
                    title = "模型",
                    icon = AppIcons.memory,
                    subtitle = "先拉取模型列表，再选择要使用的模型",
                    value = uiState.assistantModel.ifBlank { "未配置" },
                    onClick = { loadModels() },
                )
            }
            PreferenceDivider()
            val effortLevels = if (models.isEmpty()) {
                emptyList()
            } else {
                models.firstOrNull { it.id == uiState.assistantModel }?.reasoningLevels.orEmpty()
            }
            if (effortLevels.isEmpty()) {
                PreferenceRow(
                    title = "思考强度",
                    subtitle = "当前模型不支持思考档位",
                    value = "不支持",
                    icon = AppIcons.bolt,
                    onClick = {},
                )
            } else {
                val effortIndex = effortLevels.indexOf(uiState.assistantReasoningEffort).coerceAtLeast(0)
                DropdownPreferenceRow(
                    title = "思考强度",
                    subtitle = if (models.isEmpty()) {
                        "选择模型后显示接口返回的档位"
                    } else {
                        "接口返回 ${effortLevels.size} 档"
                    },
                    value = effortLevels[effortIndex],
                    icon = AppIcons.bolt,
                    // 直接用接口返回的原始档位（low / high / max …），不再自己映射
                    options = effortLevels,
                    selectedIndex = effortIndex,
                    onSelected = { index -> viewModel.setAssistantReasoningEffort(effortLevels[index]) },
                )
            }
            if (uiState.assistantRemoteReady) {
                PreferenceDivider()
                Box(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    LiquidGlassButton(
                        onClick = { showClearDialog = true },
                        backdrop = rememberPanelBackdrop(),
                        height = 40.dp,
                        horizontalPadding = 16.dp,
                    ) {
                        Text("清除模型配置", style = MaterialTheme.typography.labelMedium, color = WarrantyExpired)
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }

    if (showEndpointDialog) {
        TextInputDialog(
            title = "接口地址",
            hint = "https://api.openai.com/v1",
            initial = uiState.assistantEndpoint,
            keyboardType = KeyboardType.Uri,
            onConfirm = viewModel::setAssistantEndpoint,
            onDismiss = { showEndpointDialog = false },
        )
    }

    if (showKeyDialog) {
        TextInputDialog(
            title = "API Key",
            hint = "sk-...",
            initial = uiState.assistantApiKey,
            keyboardType = KeyboardType.Password,
            masked = true,
            onConfirm = viewModel::setAssistantApiKey,
            onDismiss = { showKeyDialog = false },
        )
    }

    if (showClearDialog) {
        LiquidAlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("清除模型配置", color = TextPrimary(), fontWeight = FontWeight.SemiBold) },
            text = { Text("将清空接口地址、模型名称与 API Key，助理会回到仅本地分析模式。", color = TextSecondary()) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setAssistantEndpoint("")
                    viewModel.setAssistantModel("")
                    viewModel.setAssistantApiKey("")
                    showClearDialog = false
                }) { Text("清除", color = WarrantyExpired) }
            },
            dismissButton = { TextButton(onClick = { showClearDialog = false }) { Text("取消") } },
        )
    }
}

/** Key 的展示形态：只留首尾各 4 个字符，中间打点。 */
private fun maskKey(key: String): String {
    if (key.length <= 10) return "已配置"
    return key.take(4) + "••••" + key.takeLast(4)
}

@Composable
private fun rememberPanelBackdrop(): com.kyant.backdrop.Backdrop {
    val color = LocalGlassColors.current.cardBg
    return com.kyant.backdrop.backdrops.rememberLayerBackdrop {
        drawRect(color)
        drawContent()
    }
}

@Composable
private fun TextInputDialog(
    title: String,
    hint: String,
    initial: String,
    keyboardType: KeyboardType,
    masked: Boolean = false,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var value by remember { mutableStateOf(initial) }
    LiquidAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = TextPrimary(), fontWeight = FontWeight.SemiBold) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text(hint) },
                singleLine = true,
                shape = AppShape.panel,
                visualTransformation = if (masked) PasswordVisualTransformation() else VisualTransformation.None,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            )
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(value)
                onDismiss()
            }) { Text("保存", color = Primary()) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}
