package com.nanji.lootarchive.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nanji.lootarchive.ui.theme.*
import kotlinx.coroutines.launch

private data class OnboardingPage(
    val icon: ImageVector, val iconBgColor: Color,
    val step: String, val title: String, val description: String,
    val tags: List<Pair<String, Color>>
)

private val pages = listOf(
    OnboardingPage(Icons.Rounded.Inventory2, _Primary, "1 / 5", "欢迎来到拾物集",
        "你的私人物品资产管理工具\n一件一档，精细管理\n所有数据纯本地存储，无需联网",
        listOf("离线安全" to Color(0xFF10B981), "隐私优先" to Color(0xFF10B981))),
    OnboardingPage(Icons.Rounded.CameraAlt, Color(0xFF10B981), "2 / 5", "记录你的物品",
        "拍照或从相册选择照片\n填写名称、价格、存放位置\n标签分类 + 5种状态随心标记",
        listOf("拍照录入" to Color(0xFF10B981), "状态追踪" to _Primary)),
    OnboardingPage(Icons.Rounded.BarChart, Color(0xFF3B82F6), "3 / 5", "资产一目了然",
        "环形图看分类分布\n雷达图多维度对比 + 趋势线\n月度购入 + CSV数据导出",
        listOf("多维图表" to _Secondary, "CSV导出" to Color(0xFF10B981))),
    OnboardingPage(Icons.Rounded.EmojiEvents, _Secondary, "4 / 5", "收藏家成长体系",
        "EXP经验值 + 10级阶梯\n13枚成就徽章等你解锁\n数量/价值双维度评级",
        listOf("EXP等级" to _Secondary, "成就徽章" to _Primary)),
    OnboardingPage(Icons.Rounded.RocketLaunch, _Primary, "5 / 5", "开始你的收藏之旅",
        "先添加第一件物品试试吧\n点击首页底部\"新增物品\"\n记录你的第一件宝贝",
        listOf("现在开始" to Color(0xFF10B981), "随时回看" to _Primary))
)

@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val dark = LocalDarkTheme.current

    suspend fun animateTo(page: Int) {
        pagerState.animateScrollToPage(page)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (dark) _BackgroundDark else _BackgroundLight)
    ) {
        // 装饰光斑 — 右上角
        Box(
            Modifier.size(200.dp).align(Alignment.TopEnd).offset(x = 50.dp, y = (-40).dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Primary().copy(alpha = 0.10f), Color.Transparent),
                        radius = 100f
                    )
                )
        )
        // 装饰光斑 — 左下角
        Box(
            Modifier.size(160.dp).align(Alignment.BottomStart).offset(x = (-30).dp, y = 160.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(_Secondary.copy(alpha = 0.06f), Color.Transparent),
                        radius = 90f
                    )
                )
        )

        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(80.dp))

            // 卡片轮播区
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                beyondViewportPageCount = 1
            ) { pageIdx ->
                val page = pages[pageIdx]
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .widthIn(max = 350.dp).fillMaxWidth(0.82f)
                            .clip(RoundedCornerShape(28.dp))
                            .background(
                                if (dark) _CardDark.copy(alpha = 0.82f) else Color(0xBDFFFFFF),
                                RoundedCornerShape(28.dp)
                            )
                            .padding(36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                Modifier.size(76.dp)
                                    .clip(RoundedCornerShape(22.dp))
                                    .background(page.iconBgColor.copy(alpha = 0.10f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    page.icon, null,
                                    modifier = Modifier.size(40.dp),
                                    tint = page.iconBgColor
                                )
                            }
                            Spacer(Modifier.height(22.dp))
                            Text(
                                page.step, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                                color = Primary(), fontFamily = FredokaFont,
                                letterSpacing = 0.6.sp
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                page.title, fontSize = 23.sp, fontWeight = FontWeight.Bold,
                                color = TextPrimary(), fontFamily = FredokaFont,
                                letterSpacing = (-0.2).sp
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                page.description, fontSize = 14.sp, color = TextSecondary(),
                                lineHeight = 23.sp, textAlign = TextAlign.Center
                            )
                            if (page.tags.isNotEmpty()) {
                                Spacer(Modifier.height(18.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    page.tags.forEach { (label, bgColor) ->
                                        Box(
                                            Modifier.clip(RoundedCornerShape(20.dp))
                                                .background(bgColor.copy(alpha = 0.10f))
                                                .padding(horizontal = 12.dp, vertical = 5.dp)
                                        ) {
                                            Text(
                                                label, fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = bgColor
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            // 缩略图导航条 — 用圆圈简点替代emoji
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                pages.forEachIndexed { i, _ ->
                    val isCurrent = i == pagerState.currentPage
                    Box(
                        Modifier
                            .size(if (isCurrent) 56.dp else 48.dp)
                            .clip(RoundedCornerShape(15.dp))
                            .background(
                                if (isCurrent) Primary().copy(alpha = 0.10f)
                                else (if (dark) Color.White else Color.Black).copy(alpha = 0.03f)
                            )
                            .then(if (isCurrent) Modifier.offset(y = (-4).dp) else Modifier)
                            .clickable { scope.launch { animateTo(i) } },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            pages[i].icon, null,
                            modifier = Modifier.size(if (isCurrent) 28.dp else 22.dp),
                            tint = if (isCurrent) Primary() else TextAuxiliary()
                        )
                    }
                }
            }

            // 底部：点状指示器 + 按钮
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(pages.size) { i ->
                    Box(
                        Modifier
                            .width(if (i == pagerState.currentPage) 24.dp else 8.dp)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (i == pagerState.currentPage) Primary()
                                else Color(0xFFE0D8D0)
                            )
                    )
                }
            }
            Spacer(Modifier.height(22.dp))

            // 下一步 / 开始使用 按钮
            val isLast = pagerState.currentPage == pages.lastIndex
            Box(
                Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(Primary())
                    .clickable {
                        if (isLast) onComplete()
                        else scope.launch { animateTo(pagerState.currentPage + 1) }
                    }
                    .padding(horizontal = 40.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (isLast) "开始使用" else "下一步",
                        fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        if (isLast) Icons.Rounded.Check else Icons.AutoMirrored.Rounded.ArrowForward,
                        null, Modifier.size(18.dp), tint = Color.White
                    )
                }
            }

            // 跳过引导
            Box(
                Modifier
                    .padding(top = 14.dp, bottom = 44.dp)
                    .clickable { onComplete() },
                contentAlignment = Alignment.Center
            ) {
                Text("跳过引导", fontSize = 13.sp, color = TextAuxiliary())
            }
        }
    }
}
