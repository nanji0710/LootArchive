# 新手引导（Onboarding） 实施计划

> **For agentic workers:** 使用 superpowers:executing-plans 或 superpowers:subagent-driven-development 来逐任务实施。步骤使用 checkbox (`- [ ]`) 语法追踪。

**目标:** 为新用户在首次启动时展示 5 页全屏玻璃卡片轮播引导，介绍核心功能，完成后进入首页。

**方案:** 方案 B — 全屏玻璃卡片轮播。DataStore 持久化 "已完成引导" 标记，设置页可重置重新查看。

**技术栈:** Kotlin + Jetpack Compose + Material3 + HorizontalPager (Compose Foundation) + DataStore Preferences

## 全局约束

- 纯 Compose，不引入任何第三方库
- Warm Glassmorphism 风格：28dp 圆角、玻璃模糊、琥珀主色 #E8782A
- 字体：Fredoka 标题 + Nunito 正文
- 版本号：v6.2.1 / versionCode=621
- 总计新增/修改约 300 行代码

---

### Task 1: SettingsRepository — 新增引导完成标记

**文件:**
- 修改: `app/src/main/java/com/nanji/lootarchive/data/repository/SettingsRepository.kt`

**接口:**
- 产出: `val onboardingCompleted: Flow<Boolean>` (默认 false)
- 产出: `suspend fun setOnboardingCompleted(completed: Boolean)`

- [ ] **Step 1: 添加 KEY 和 flow/setter**

在 `companion object` 的 KEY 声明区末尾（第 24 行后）追加：

```kotlin
val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
```

在 `searchHistory` 区块之后（第 41 行后）追加：

```kotlin
// ── v6.2 新手引导 ──
val onboardingCompleted: Flow<Boolean> = dataStore.data.map { it[KEY_ONBOARDING_COMPLETED] ?: false }
suspend fun setOnboardingCompleted(completed: Boolean) {
    dataStore.edit { it[KEY_ONBOARDING_COMPLETED] = completed }
}
```

- [ ] **Step 2: 验证编译**

```bash
cd d:/GitHub/LootArchive && ./gradlew compileReleaseKotlin 2>&1 | tail -5
```

预期：`BUILD SUCCESSFUL`

---

### Task 2: OnboardingScreen — 引导卡片轮播组件

**文件:**
- 创建: `app/src/main/java/com/nanji/lootarchive/ui/onboarding/OnboardingScreen.kt`

**接口:**
- 产出: `@Composable fun OnboardingScreen(onComplete: () -> Unit)` — 用户点"开始使用"或"跳过"时回调

- [ ] **Step 1: 创建完整组件**

```kotlin
package com.nanji.lootarchive.ui.onboarding

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nanji.lootarchive.ui.theme.*

private data class OnboardingPage(
    val icon: String,
    val iconBg: Long,
    val step: String,
    val title: String,
    val description: String,
    val tags: List<Pair<String, Long>>
)

private val pages = listOf(
    OnboardingPage("📦", 0x1AE8782A, "1 / 5", "欢迎来到拾物集",
        "你的私人物品资产管理工具\n一件一档，精细管理\n所有数据纯本地存储，无需联网",
        listOf("离线安全" to 0x1AE8782A, "隐私优先" to 0x1A10B981)),
    OnboardingPage("📸", 0x1A10B981, "2 / 5", "记录你的物品",
        "拍照或从相册选择照片\n填写名称、价格、存放位置\n标签分类 + 5种状态随心标记",
        listOf("拍照录入" to 0x1A10B981, "状态追踪" to 0x1AE8782A)),
    OnboardingPage("📊", 0x1A3B82F6, "3 / 5", "资产一目了然",
        "环形图看分类分布\n雷达图多维度对比 + 趋势线\n月度购入 + CSV数据导出",
        listOf("多维图表" to 0x1A7C3AED, "CSV导出" to 0x1A10B981)),
    OnboardingPage("🏆", 0x1A7C3AED, "4 / 5", "收藏家成长体系",
        "EXP经验值 + 10级阶梯\n13枚成就徽章等你解锁\n数量/价值双维度评级",
        listOf("EXP等级" to 0x1A7C3AED, "成就徽章" to 0x1AE8782A)),
    OnboardingPage("🚀", 0x1AE8782A, "5 / 5", "开始你的收藏之旅",
        "先添加第一件物品试试吧\n点击首页底部"新增物品"\n记录你的第一件宝贝 ✨",
        listOf("现在开始" to 0x1A10B981, "随时回看" to 0x1AE8782A))
)

@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val dark = LocalDarkTheme.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (dark) _BackgroundDark else _BackgroundLight)
    ) {
        // 装饰光斑
        Box(Modifier.size(200.dp).align(Alignment.TopEnd).offset(x = 50.dp, y = (-40).dp)
            .background(brush = androidx.compose.ui.graphics.Brush.radialGradient(
                colors = listOf(Primary().copy(alpha = 0.10f), Primary().copy(alpha = 0f)), radius = 100f)))
        Box(Modifier.size(160.dp).align(Alignment.BottomStart).offset(x = (-30).dp, y = 160.dp)
            .background(brush = androidx.compose.ui.graphics.Brush.radialGradient(
                colors = listOf(Secondary().copy(alpha = 0.06f), Secondary().copy(alpha = 0f)), radius = 90f)))

        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(80.dp))

            // 卡片轮播区
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                beyondViewportPageCount = 1
            ) { pageIdx ->
                val page = pages[pageIdx]
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    // 玻璃卡片
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
                            // 图标
                            Box(
                                Modifier.size(76.dp)
                                    .clip(RoundedCornerShape(22.dp))
                                    .background(Color(page.iconBg)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(page.icon, fontSize = 38.sp)
                            }
                            Spacer(Modifier.height(22.dp))
                            // 步骤
                            Text(page.step, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                                color = Primary(), fontFamily = FredokaFont,
                                letterSpacing = 0.6.sp)
                            Spacer(Modifier.height(6.dp))
                            // 标题
                            Text(page.title, fontSize = 23.sp, fontWeight = FontWeight.Bold,
                                color = TextPrimary(), fontFamily = FredokaFont,
                                letterSpacing = (-0.2).sp)
                            Spacer(Modifier.height(12.dp))
                            // 描述
                            Text(page.description, fontSize = 14.sp, color = TextSecondary(),
                                lineHeight = 23.sp, textAlign = TextAlign.Center)
                            // 标签
                            if (page.tags.isNotEmpty()) {
                                Spacer(Modifier.height(18.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    page.tags.forEach { (label, bg) ->
                                        Box(Modifier.clip(RoundedCornerShape(20.dp))
                                            .background(Color(bg)).padding(horizontal = 12.dp, vertical = 5.dp)) {
                                            Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFFE8782A))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 缩略图导航条
            Row(
                Modifier.padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                pages.forEachIndexed { i, p ->
                    val isCurrent = i == pagerState.currentPage
                    Box(
                        Modifier.size(if (isCurrent) 56.dp else 48.dp)
                            .clip(RoundedCornerShape(15.dp))
                            .background(
                                if (isCurrent) Primary().copy(alpha = 0.10f)
                                else (if (dark) Color.White else Color.Black).copy(alpha = 0.03f)
                            )
                            .then(
                                if (isCurrent) Modifier.offset(y = (-4).dp) else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(p.icon, fontSize = if (isCurrent) 26.sp else 22.sp)
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

            // 按钮
            val isLast = pagerState.currentPage == pages.lastIndex
            Box(
                Modifier.clip(RoundedCornerShape(24.dp)).background(Primary())
                    .padding(horizontal = 52.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (isLast) "开始使用 🎉" else "下一步 →",
                    fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                    color = androidx.compose.ui.graphics.Color.White
                )
            }

            // 跳过
            Box(
                Modifier.padding(top = 14.dp, bottom = 44.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("跳过引导", fontSize = 13.sp, color = TextAuxiliary())
            }
        }
    }
}
```

> 注：上面为伪代码结构示意。实际实施时需要确保 Compose Foundation `HorizontalPager` 和 `androidx.compose.foundation.pager.*` 可用（BOM 2025.06.00 已包含），并在绘制时使用 `Box` 替代 `Surface` 避免 `LocalContentColor` 继承问题。每个 `Box` 可点区域用 `.clickable { }` 包裹。`Spacer` 替代 `Modifier.weight(1f)` 不需要 `RowScope`。完整可运行代码约 200 行。

- [ ] **Step 2: 验证编译**

```bash
cd d:/GitHub/LootArchive && ./gradlew compileReleaseKotlin 2>&1 | tail -5
```

预期：`BUILD SUCCESSFUL`

---

### Task 3: MainActivity — 接入引导覆盖层

**文件:**
- 修改: `app/src/main/java/com/nanji/lootarchive/MainActivity.kt`

**接口:**
- 消费: `settingsRepository.onboardingCompleted: Flow<Boolean>`

- [ ] **Step 1: 在 MainActivity 中添加条件引导**

在 `setContent` 块中，`val primaryColor` 之后追加：

```kotlin
val onboardingCompleted by settingsRepository.onboardingCompleted.collectAsState(initial = false)
```

将 `Box(Modifier.fillMaxSize().background(...))` 中的内容改为：

```kotlin
Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
    if (!onboardingCompleted) {
        com.nanji.lootarchive.ui.onboarding.OnboardingScreen(
            onComplete = {
                kotlinx.coroutines.MainScope().launch {
                    settingsRepository.setOnboardingCompleted(true)
                }
            }
        )
    } else {
        key("main") { MainScreen() }
    }
}
```

需要追加 import：
```kotlin
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
```

- [ ] **Step 2: 验证编译**

```bash
cd d:/GitHub/LootArchive && ./gradlew compileReleaseKotlin 2>&1 | tail -5
```

预期：`BUILD SUCCESSFUL`

---

### Task 4: MainActivity — 修复协程作用域

**文件:**
- 修改: `app/src/main/java/com/nanji/lootarchive/MainActivity.kt`

`MainScope().launch` 不是最佳实践。改为使用 `rememberCoroutineScope`：

- [ ] **Step 1: 改用 rememberCoroutineScope**

将 Task 3 中 `MainActivity.kt` 的修改替换为：

```kotlin
val onboardingCompleted by settingsRepository.onboardingCompleted.collectAsState(initial = false)
val scope = rememberCoroutineScope()

// ... 在 Box 中:
if (!onboardingCompleted) {
    OnboardingScreen(
        onComplete = {
            scope.launch {
                settingsRepository.setOnboardingCompleted(true)
            }
        }
    )
}
```

需要追加 import：
```kotlin
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
```

- [ ] **Step 2: 验证编译**

```bash
cd d:/GitHub/LootArchive && ./gradlew compileReleaseKotlin 2>&1 | tail -5
```

---

### Task 5: SettingsViewModel — 添加重置引导方法

**文件:**
- 修改: `app/src/main/java/com/nanji/lootarchive/ui/settings/SettingsViewModel.kt`

**接口:**
- 产出: `fun resetOnboarding()` — 将 `onboarding_completed` 设为 false

- [ ] **Step 1: 添加方法**

在 `clearMessage()` 方法之前追加：

```kotlin
fun resetOnboarding() {
    viewModelScope.launch { settingsRepository.setOnboardingCompleted(false) }
}
```

- [ ] **Step 2: 验证编译**

```bash
cd d:/GitHub/LootArchive && ./gradlew compileReleaseKotlin 2>&1 | tail -5
```

---

### Task 6: SettingsScreen — 添加"重新查看引导"入口

**文件:**
- 修改: `app/src/main/java/com/nanji/lootarchive/ui/settings/SettingsScreen.kt`

**接口:**
- 消费: `viewModel.resetOnboarding()`

- [ ] **Step 1: 在"个性化"Card 中添加菜单项**

在 `SettingsScreen.kt` 中找到显示模式/自定义头像的 Card，在"自定义头像"行之后（第 113 行，`}`闭合缩进前）、`HorizontalDivider` 之前插入：

```kotlin
HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = TextAuxiliary().copy(alpha = 0.10f))
// v6.2 新手引导
Row(
    Modifier.fillMaxWidth().clickable { viewModel.resetOnboarding() }.padding(horizontal = 16.dp, vertical = 14.dp),
    verticalAlignment = Alignment.CenterVertically
) {
    Text("重新查看引导", fontSize = 15.sp, color = TextPrimary(), modifier = Modifier.weight(1f))
    Icon(Icons.Rounded.ChevronRight, null, tint = TextAuxiliary(), modifier = Modifier.size(18.dp))
}
```

- [ ] **Step 2: 验证编译**

```bash
cd d:/GitHub/LootArchive && ./gradlew compileReleaseKotlin 2>&1 | tail -5
```

---

### Task 7: 版本号更新至 v6.2.1

**文件:**
- 修改: `app/build.gradle.kts`
- 修改: `version.json`
- 修改: `app/src/main/java/com/nanji/lootarchive/ui/MyLandingScreen.kt`
- 修改: `app/src/main/java/com/nanji/lootarchive/ui/settings/SettingsScreen.kt`

- [ ] **Step 1: 更新 build.gradle.kts**

```kotlin
versionCode = 621
versionName = "6.2.1"
```

- [ ] **Step 2: 更新 version.json**

```json
{
  "versionName": "6.2.1",
  "versionCode": 621,
  "updateDate": "2026-07-30",
  "updateLog": "v6.2.1\n• 新用户首次启动新手引导：5张玻璃卡片轮播\n• 设置页新增\"重新查看引导\"入口\n• 首页非拥有物品卡片去除价格背景方块",
  "apkDownloadUrl": "https://github.com/nanji0710/LootArchive/releases/download/V6.2.1/LootArchive-release-v6.2.1.apk"
}
```

- [ ] **Step 3: 更新所有 UI 中的版本字符串**

`MyLandingScreen.kt` (2处):
- `"当前版本 v6.2.1"`
- `"当前已是最新版本 v6.2.1"`

`SettingsScreen.kt` (1处):
- `"当前版本 v6.2.1"`

---

### Task 8: 构建 Release APK + 推送

- [ ] **Step 1: 构建**

```bash
cd d:/GitHub/LootArchive && ./gradlew assembleRelease
```

- [ ] **Step 2: 复制 APK + 提交**

```bash
cp -f app/build/outputs/apk/release/LootArchive-release-v6.2.1.apk ./
git add -A
git commit -m "v6.2.1: 新手引导全屏玻璃卡片轮播+设置重置入口"
```

- [ ] **Step 3: 标签 + 推送**

```bash
git tag -f V6.2.1
git push origin main --tags
```

---

## 验收清单

- [ ] 首次安装打开 APP → 显示 5 页引导卡片
- [ ] 卡片支持左右滑动切换
- [ ] 点击"跳过引导" → 直接进入首页
- [ ] 最后一张点"开始使用" → 进入首页
- [ ] 再次打开 APP → 不显示引导（直接进首页）
- [ ] 设置页 → "重新查看引导" → 下次打开 APP 重新显示
- [ ] 卸载重装 → 引导重新出现
- [ ] 引导页风格与 APP Warm Glassmorphism 一致
- [ ] 深色模式下引导页正常显示
