# 视觉优化实施计划 v6.4.0

> P0+P1 一次性完成：Emoji→图标、导航栏尺寸、间距统一、阴影层次、M3排版

## Task 1: P0 — Emoji 替换为 Material Icons

修改文件：OnboardingScreen.kt, MyLandingScreen.kt, HomeScreen.kt
每个 emoji 替换为对应的 `Icons.Rounded.*`

## Task 2: P0 — 导航栏图标尺寸规范
修改文件：MainScreen.kt
图标 20→24dp, 背景 30→36dp, 标签 10→11sp

## Task 3: P1 — 全页面间距统一为 16dp/12dp/16dp
修改文件：MyLandingScreen.kt, AddItemScreen.kt, HomeScreen.kt, StatisticsScreen.kt, SearchScreen.kt

## Task 4: P1 — 卡片 elevation 统一
修改文件：MyLandingScreen.kt, HomeScreen.kt, StatisticsScreen.kt
卡片 1dp, 悬浮面板 4dp, FAB 6dp, 导航/弹窗 8dp

## Task 5: 版本号 v6.4.0 + 构建推送
