package com.nanji.lootarchive.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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

private data class OnboardingPage(
    val icon: ImageVector, val iconBgColor: Color,
    val step: String, val title: String, val description: String,
    val tags: List<Pair<String, Color>>
)

private val pages = listOf(
    OnboardingPage(Icons.Rounded.Inventory2, _Primary, "1 / 1", "欢迎来到拾物集",
        "你的私人物品资产管理工具\n一件一档，精细管理\n所有数据纯本地存储，无需联网",
        listOf("离线安全" to Color(0xFF10B981), "隐私优先" to Color(0xFF10B981)))
)

@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    val dark = LocalDarkTheme.current
    val page = pages.first()

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

            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .widthIn(max = 350.dp).fillMaxWidth(0.82f)
                        .clip(AppShape.sheet)
                        .background(
                            if (dark) _CardDark.copy(alpha = 0.82f) else Color(0xBDFFFFFF),
                            AppShape.sheet
                        )
                        .padding(36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier.size(76.dp)
                                .clip(AppShape.sheet)
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
                            page.step, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold,
                            color = Primary(), fontFamily = FredokaFont,
                            letterSpacing = 0.6.sp
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            page.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold,
                            color = TextPrimary(), fontFamily = FredokaFont,
                            letterSpacing = (-0.2).sp
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            page.description, style = MaterialTheme.typography.bodyMedium, color = TextSecondary(),
                            lineHeight = 23.sp, textAlign = TextAlign.Center
                        )
                        if (page.tags.isNotEmpty()) {
                            Spacer(Modifier.height(18.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                page.tags.forEach { (label, bgColor) ->
                                    Box(
                                        Modifier.clip(AppShape.card)
                                            .background(bgColor.copy(alpha = 0.10f))
                                            .padding(horizontal = 12.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            label, style = MaterialTheme.typography.labelSmall,
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

            Spacer(Modifier.height(6.dp))

            Box(
                Modifier
                    .clip(AppShape.sheet)
                    .background(Primary())
                    .clickable { onComplete() }
                    .padding(horizontal = 40.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "开始使用",
                        style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.Rounded.Check, null, Modifier.size(18.dp), tint = Color.White)
                }
            }

            Spacer(Modifier.height(44.dp))
        }
    }
}
