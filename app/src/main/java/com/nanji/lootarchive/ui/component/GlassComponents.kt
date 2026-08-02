package com.nanji.lootarchive.ui.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nanji.lootarchive.ui.theme.*
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

// ═══════════════════════════════════════════════════════════════
//  v5.0 Warm Glassmorphism — 对标 HTML .glass-card
//  background: rgba(255,255,255,0.75) + blur(20px) + border + shadow
// ═══════════════════════════════════════════════════════════════

private val CardShape = RoundedCornerShape(20.dp)

/**
 * v6.7 标准玻璃卡片 — 统一 Card 样板（CardBg 底 + 20dp 圆角 + 1dp 海拔）。
 * 消除各页面重复的 CardDefaults 样板。
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = CardShape,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = CardBg()),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        content = { content() }
    )
}

/**
 * v5.0 玻璃卡片 — Haze 实时模糊 + 半透明背景 + 玻璃边框
 * 对标 HTML .glass-card: backdrop-blur(20px) + shadow 0 4px 24px rgba(0,0,0,0.05)
 */
@Composable
fun NeoCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val glass = LocalGlassColors.current
    val haze = LocalHazeState.current
    val cardBg = glass.glassBg
    val borderClr = glass.glassBorder
    val shadowClr = glass.shadow

    Box(
        modifier = modifier
            .then(
                if (haze != null) Modifier.hazeEffect(
                    state = haze,
                    style = HazeStyle(
                        backgroundColor = Color.Transparent,
                        tints = listOf(HazeTint(cardBg)),
                        blurRadius = 20.dp,
                        noiseFactor = 0f,
                        fallbackTint = HazeTint(glass.cardBg)
                    )
                ) else Modifier.background(glass.cardBg, CardShape)
            )
            .shadow(4.dp, CardShape, ambientColor = Color.White.copy(alpha = 0.3f), spotColor = shadowClr)
            .border(0.5.dp, borderClr, CardShape)
            .clip(CardShape)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = CardShape,
            colors = CardDefaults.cardColors(
                containerColor = if (haze != null) Color.Transparent else glass.cardBg
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            onClick = onClick ?: {}
        ) {
            Column(Modifier.padding(contentPadding), content = content)
        }
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
        Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = valueColor, maxLines = 1, overflow = TextOverflow.Ellipsis, fontFamily = FredokaFont)
        Spacer(Modifier.height(4.dp))
        Text(title, fontSize = 13.sp, color = TextAuxiliary(), maxLines = 1, overflow = TextOverflow.Ellipsis)
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
    Card(modifier = modifier, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = bg), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), onClick = onClick ?: {}) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = valueColor, fontFamily = FredokaFont, maxLines = 1)
            Spacer(Modifier.height(2.dp))
            Text(title, fontSize = 11.sp, color = TextAuxiliary(), maxLines = 1)
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
    val floatOffset by infinite.animateFloat(initialValue = 0f, targetValue = 6f, animationSpec = infiniteRepeatable(animation = tween(2000, easing = EaseInOutCubic), repeatMode = RepeatMode.Reverse), label = "floatOffset")
    Column(modifier = modifier.fillMaxWidth().padding(48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.offset(y = floatOffset.dp)) { icon() }
        Spacer(Modifier.height(24.dp))
        Text(title, fontSize = 18.sp, color = TextSecondary(), textAlign = TextAlign.Center, fontWeight = FontWeight.Medium)
        if (subtitle.isNotEmpty()) { Spacer(Modifier.height(8.dp)); Text(subtitle, fontSize = 14.sp, color = TextAuxiliary(), textAlign = TextAlign.Center) }
        if (actionLabel != null && onAction != null) { Spacer(Modifier.height(20.dp)); Button(onClick = onAction, shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = Primary(), contentColor = Color.White)) { Text(actionLabel, fontSize = 14.sp, fontWeight = FontWeight.Medium) } }
    }
}

/**
 * v5.0 玻璃对话框
 */
@Composable
fun NeoAlertDialog(title: String, message: String, confirmText: String = "确认", dismissText: String = "取消", onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, shape = RoundedCornerShape(28.dp), containerColor = CardBg(), tonalElevation = 0.dp,
        title = { Text(title, fontWeight = FontWeight.SemiBold, color = TextPrimary()) },
        text = { Text(message, color = TextSecondary(), fontSize = 14.sp) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmText, color = Primary(), fontWeight = FontWeight.SemiBold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(dismissText, color = TextSecondary()) } }
    )
}

// ══════════════════════════════════════════════════════════
// 别名
// ══════════════════════════════════════════════════════════
@Composable fun ClayCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, contentPadding: PaddingValues = PaddingValues(16.dp), content: @Composable ColumnScope.() -> Unit) = NeoCard(modifier, onClick, contentPadding, content)
@Composable fun StatCard(title: String, value: String, modifier: Modifier = Modifier, valueColor: Color = Primary(), onClick: (() -> Unit)? = null) = NeoStatCard(title, value, modifier, valueColor, onClick)
@Composable fun EmptyState(icon: @Composable () -> Unit, title: String, subtitle: String = "", modifier: Modifier = Modifier, actionLabel: String? = null, onAction: (() -> Unit)? = null) = NeoEmptyState(icon, title, subtitle, modifier, actionLabel, onAction)
@Composable fun GlassCard(modifier: Modifier = Modifier, tier: GlassTier = GlassTier.CARD, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) = NeoCard(modifier, onClick, content = content)
@Composable fun GlassStatCard(title: String, value: String, modifier: Modifier = Modifier, valueColor: Color = Primary(), onClick: (() -> Unit)? = null) = NeoStatCard(title, value, modifier, valueColor, onClick)
@Composable fun GlassAlertDialog(title: String, message: String, confirmText: String = "确认", dismissText: String = "取消", onConfirm: () -> Unit, onDismiss: () -> Unit) = NeoAlertDialog(title, message, confirmText, dismissText, onConfirm, onDismiss)
