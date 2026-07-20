package com.nanji.lootarchive.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nanji.lootarchive.ui.theme.*

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    tier: GlassTier = GlassTier.CARD,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    // 用 Box 替代 Surface，避免 Material3 的 elevation 渲染白色底层
    Box(
        modifier = modifier
            .glassEffect(tier = tier)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        Column(modifier = Modifier.padding(12.dp)) { content() }
    }
}

@Composable
fun GlassStatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = Primary(),
    onClick: (() -> Unit)? = null
) {
    GlassCard(modifier = modifier, onClick = onClick) {
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = valueColor,
            maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        Spacer(modifier = Modifier.height(6.dp))
        Text(title, style = MaterialTheme.typography.bodySmall, color = TextAuxiliary(),
            maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
    }
}

@Composable
fun EmptyState(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String = "",
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        icon()
        Spacer(modifier = Modifier.height(24.dp))
        Text(title, fontSize = 20.sp, color = TextSecondary())
        if (subtitle.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextAuxiliary())
        }
    }
}

@Composable
fun GlassAlertDialog(
    title: String,
    message: String,
    confirmText: String = "确认",
    dismissText: String = "取消",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.Transparent,
        modifier = Modifier.glassEffect(tier = GlassTier.DIALOG),
        title = { Text(title, fontWeight = FontWeight.SemiBold) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmText) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(dismissText) } }
    )
}
