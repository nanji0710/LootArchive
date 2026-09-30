package com.nanji.lootarchive.ui.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nanji.lootarchive.ui.liquidglass.LiquidAlertDialog
import com.nanji.lootarchive.ui.theme.*

// ═══════════════════════════════════════════════════════════════
//  v5.0 Warm Glassmorphism — 对标 HTML .glass-card
//  background: rgba(255,255,255,0.75) + blur(20px) + border + shadow
// ═══════════════════════════════════════════════════════════════

private val CardShape = AppShape.card

/**
 * v6.7 标准玻璃卡片 — 统一 Card 样板（玻璃底 + 20dp 圆角 + 0 海拔，NeoCard 同款柔和阴影）。
 * 消除各页面重复的 CardDefaults 样板；0 海拔避免半透明玻璃底透出灰圈。
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = CardShape,
    content: @Composable () -> Unit
) {
    val glass = LocalGlassColors.current
    Card(
        modifier = modifier.fillMaxWidth().shadow(4.dp, shape, ambientColor = Color.White.copy(alpha = 0.3f), spotColor = glass.shadow),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = glass.glassBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        content = { content() }
    )
}

/**
 * v6.8 LiquidCard — 半透明 + 高光边框（RiseCard 模式）
 * 依赖 Task 3 渐变背景，半透明玻璃底即可读作毛玻璃
 */
@Composable
fun NeoCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val glass = LocalGlassColors.current
    val dark = LocalDarkTheme.current
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (dark) Modifier.border(0.5.dp, Color.White.copy(alpha = 0.07f), CardShape)
                else Modifier.border(0.5.dp, glass.glassBorder, CardShape)
            )
            .shadow(4.dp, CardShape, ambientColor = Color.White.copy(alpha = 0.3f), spotColor = glass.shadow),
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = glass.glassBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        onClick = onClick ?: {}
    ) {
        Column(Modifier.padding(contentPadding), content = content)
    }
}

/**
 * v5.0 统计卡片 — 大数字 + 标签
 */
@Composable
fun NeoStatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = Primary(),
    onClick: (() -> Unit)? = null
) {
    NeoCard(modifier = modifier, onClick = onClick, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)) {
        Text(value, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = valueColor, maxLines = 1, overflow = TextOverflow.Ellipsis, fontFamily = FredokaFont)
        Spacer(Modifier.height(4.dp))
        Text(title, style = MaterialTheme.typography.bodySmall, color = TextAuxiliary(), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/**
 * v5.0 Hero 统计卡片
 */
@Composable
fun HeroStatCard(
    title: String, value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = Primary(),
    accentBg: Color? = null,
    onClick: (() -> Unit)? = null
) {
    val bg = accentBg ?: Primary().copy(alpha = 0.06f)
    Card(modifier = modifier, shape = AppShape.panel, colors = CardDefaults.cardColors(containerColor = bg), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), onClick = onClick ?: {}) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = valueColor, fontFamily = FredokaFont, maxLines = 1)
            Spacer(Modifier.height(2.dp))
            Text(title, style = MaterialTheme.typography.labelSmall, color = TextAuxiliary(), maxLines = 1)
        }
    }
}

/**
 * v5.0 空状态
 */
@Composable
fun NeoEmptyState(
    icon: @Composable () -> Unit, title: String, subtitle: String = "",
    modifier: Modifier = Modifier, actionLabel: String? = null, onAction: (() -> Unit)? = null
) {
    val infinite = rememberInfiniteTransition(label = "float")
    val floatOffset by infinite.animateFloat(initialValue = 0f, targetValue = 6f, animationSpec = infiniteRepeatable(animation = tween(MotionDuration.Ambient.Float, easing = MotionCurve.EaseInOut), repeatMode = RepeatMode.Reverse), label = "floatOffset")
    Column(modifier = modifier.fillMaxWidth().padding(48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.offset(y = floatOffset.dp)) { icon() }
        Spacer(Modifier.height(24.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall, color = TextSecondary(), textAlign = TextAlign.Center, fontWeight = FontWeight.Medium)
        if (subtitle.isNotEmpty()) { Spacer(Modifier.height(8.dp)); Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = TextAuxiliary(), textAlign = TextAlign.Center) }
        if (actionLabel != null && onAction != null) { Spacer(Modifier.height(20.dp)); Button(onClick = onAction, shape = AppShape.panel, colors = ButtonDefaults.buttonColors(containerColor = Primary(), contentColor = Color.White)) { Text(actionLabel, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium) } }
    }
}

/**
 * v5.0 玻璃对话框
 */
@Composable
fun NeoAlertDialog(title: String, message: String, confirmText: String = "确认", dismissText: String = "取消", onConfirm: () -> Unit, onDismiss: () -> Unit) {
    LiquidAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.SemiBold, color = TextPrimary()) },
        text = { Text(message, color = TextSecondary(), style = MaterialTheme.typography.bodyMedium) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmText, color = Primary(), fontWeight = FontWeight.SemiBold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(dismissText, color = TextSecondary()) } }
    )
}

// ══════════════════════════════════════════════════════════
// 别名
//
// 这些是 v5.0 改名留下的兼容层（NeoXxx → 旧名）。实测仍在被调用的保留：
//   ClayCard 16 处、EmptyState 14 处、GlassAlertDialog 7 处
// 已删除无人调用的：
//   GlassCard（且它收下 tier 参数却直接忽略，是误导）、
//   StatCard / GlassStatCard
// ══════════════════════════════════════════════════════════
@Composable fun ClayCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, contentPadding: PaddingValues = PaddingValues(16.dp), content: @Composable ColumnScope.() -> Unit) = NeoCard(modifier, onClick, contentPadding, content)
@Composable fun EmptyState(icon: @Composable () -> Unit, title: String, subtitle: String = "", modifier: Modifier = Modifier, actionLabel: String? = null, onAction: (() -> Unit)? = null) = NeoEmptyState(icon, title, subtitle, modifier, actionLabel, onAction)
@Composable fun GlassAlertDialog(title: String, message: String, confirmText: String = "确认", dismissText: String = "取消", onConfirm: () -> Unit, onDismiss: () -> Unit) = NeoAlertDialog(title, message, confirmText, dismissText, onConfirm, onDismiss)
