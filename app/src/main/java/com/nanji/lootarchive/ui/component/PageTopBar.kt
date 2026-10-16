package com.nanji.lootarchive.ui.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.nanji.lootarchive.ui.liquidglass.LiquidIconButton
import com.nanji.lootarchive.ui.theme.FredokaFont
import com.nanji.lootarchive.ui.theme.TextPrimary

/**
 * 二级页统一顶栏：玻璃返回按钮 + 页面标题。
 *
 * 按钮与标题在同一个 [Row] 里，由 `verticalAlignment` 天然对齐 —— 不再像 AboutScreen
 * 那样把两者拆到层内/层外分别手写 padding（那正是「按钮与标题没对齐」的成因）。
 *
 * 结构约束：调用点必须把它放在页面 `layerBackdrop` **之外** 作为悬浮兄弟，
 * 否则玻璃按钮会采样到自己所在的图层，触发 v7.2.1 那类渲染线程递归崩溃。
 */
@Composable
fun PageTopBar(
    title: String,
    onNavigateBack: () -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier
) {
    Row(
        modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 20.dp, top = 8.dp, end = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LiquidIconButton(onClick = onNavigateBack, backdrop = backdrop) {
            Icon(
                Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "返回",
                tint = TextPrimary(),
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = TextPrimary(),
            fontFamily = FredokaFont,
            maxLines = 1
        )
    }
}

/** 顶栏占位高度：内容区顶部留白，避免首项被悬浮顶栏压住。 */
val PageTopBarHeight = 48.dp
