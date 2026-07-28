package com.nanji.lootarchive.ui.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nanji.lootarchive.ui.theme.*

// ═══════════════════════════════════════════════════════════════
//  现代拟物风 (Neumorphism) 通用组件
//  冷灰底 + 双层光影 → 浮凸深度感
// ═══════════════════════════════════════════════════════════════

private val CardShape = RoundedCornerShape(18.dp)

/**
 * 拟物卡片 — 与背景同色系，靠阴影浮起
 *
 * Light: 亮面在上/左，暗影在下/右 → 凸起感
 * Dark:  微弱亮边 + 暗影 → 从深色背景中浮出
 */
@Composable
fun NeoCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val dark = LocalDarkTheme.current
    val cardBg = if (dark) _CardDark else _CardLight

    // 拟物双层阴影：亮面（左上）+ 暗面（右下）= 凸起深度
    val shadowColor = if (dark)
        Color.Black.copy(alpha = 0.40f)
    else
        Color.Black.copy(alpha = 0.07f)

    val highlightColor = if (dark)
        Color.White.copy(alpha = 0.04f)
    else
        Color.White.copy(alpha = 0.90f)

    Card(
        modifier = modifier
            .shadow(
                elevation = 4.dp,
                shape = CardShape,
                ambientColor = highlightColor,
                spotColor = shadowColor
            )
            .shadow(
                elevation = 1.dp,
                shape = CardShape,
                ambientColor = highlightColor.copy(alpha = 0.5f),
                spotColor = shadowColor.copy(alpha = 0.5f)
            )
            .clip(CardShape),
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        onClick = onClick ?: {}
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content
        )
    }
}

/**
 * 拟物统计卡片 — 大数字 + 标签
 */
@Composable
fun NeoStatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = Primary(),
    onClick: (() -> Unit)? = null
) {
    NeoCard(
        modifier = modifier,
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Text(
            value,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontFamily = FredokaFont
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            title,
            fontSize = 13.sp,
            color = TextAuxiliary(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * 空状态 — 浮动图标 + 引导文字
 */
@Composable
fun NeoEmptyState(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String = "",
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    val infinite = rememberInfiniteTransition(label = "float")
    val floatOffset by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatOffset"
    )

    Column(
        modifier = modifier.fillMaxWidth().padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.offset(y = floatOffset.dp)) { icon() }
        Spacer(modifier = Modifier.height(24.dp))
        Text(title, fontSize = 18.sp, color = TextSecondary(), textAlign = TextAlign.Center)
        if (subtitle.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(subtitle, fontSize = 14.sp, color = TextAuxiliary(), textAlign = TextAlign.Center)
        }
        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(20.dp))
            OutlinedButton(
                onClick = onAction,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary())
            ) {
                Text(actionLabel, fontSize = 14.sp)
            }
        }
    }
}

/**
 * 拟物对话框
 */
@Composable
fun NeoAlertDialog(
    title: String,
    message: String,
    confirmText: String = "确认",
    dismissText: String = "取消",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val dark = LocalDarkTheme.current
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = if (dark) _CardDark else _CardLight,
        title = { Text(title, fontWeight = FontWeight.SemiBold) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmText, color = Primary())
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissText)
            }
        }
    )
}

// ── 向后兼容别名 ──

@Composable
fun ClayCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) = NeoCard(modifier, onClick, contentPadding, content)

@Composable
fun StatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = Primary(),
    onClick: (() -> Unit)? = null
) = NeoStatCard(title, value, modifier, valueColor, onClick)

@Composable
fun EmptyState(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String = "",
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) = NeoEmptyState(icon, title, subtitle, modifier, actionLabel, onAction)

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    tier: GlassTier = GlassTier.CARD,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) = NeoCard(modifier, onClick, content = content)

@Composable
fun GlassStatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = Primary(),
    onClick: (() -> Unit)? = null
) = NeoStatCard(title, value, modifier, valueColor, onClick)

@Composable
fun GlassAlertDialog(
    title: String, message: String,
    confirmText: String = "确认", dismissText: String = "取消",
    onConfirm: () -> Unit, onDismiss: () -> Unit
) = NeoAlertDialog(title, message, confirmText, dismissText, onConfirm, onDismiss)
