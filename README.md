# 🏠 拾物集 ItemGlow (LootArchive)

> 纯本地私人物品资产管理工具 —— 你的每一件宝贝都值得被记录

[![Android](https://img.shields.io/badge/Android-12%2B-brightgreen)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-blue)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-Warm%20Glassmorphism-orange)](https://developer.android.com/compose)
[![Version](https://img.shields.io/badge/Version-5.1.3-orange)]()
[![License](https://img.shields.io/badge/License-MIT-yellow)](LICENSE)

---

## ✨ 功能特性

### 📦 物品管理
- **便当盒网格**主页布局，首项双列宽卡片，照片占比大更直观
- **增删改查**完整 CRUD，支持名称、分类、价格、存放位置、购入日期、物品描述
- **分类筛选**：横向滚动分类标签，按分类快速定位物品
- **全文搜索**：按名称/存放位置/备注/保修信息搜索，支持排序和历史记录
- **回收站**：软删除机制，14 天自动清空，可还原或彻底删除

### 📸 照片管理
- **CameraX 拍照**：应用内相机，支持闪光灯切换，连续拍摄多张
- **相册选择**：从系统相册批量选择照片
- **多图浏览**：详情页全幅照片水平滚动，圆点指示器
- **照片编辑**：编辑物品时可新增/删除照片

### 📊 资产统计
- **环形图**展示分类资产分布，Canvas 自绘 Donut Chart
- **月度购入趋势**：柱状图展示每月购入物品总价值，智能居中/滚动
- **分类排名**：金/银/铜徽章标注前三名
- **时间筛选**：全部时间 / 近三月 / 近半年 / 近一年，饼图和总资产实时联动

### 🏆 收藏家等级
- **双维度体系**：数量线（收藏广度）+ 价值线（藏品深度）独立计算
- **动态星级**：取两条线最高一档，价值达人加 ✨ 珍品标记
- **等级规则面板**：点击等级徽章查看完整规则和当前进度

### 🔔 保修追踪
- **保修到期提醒**：首页显示即将到期物品数量
- **自动计算**：输入购入日期 + 保修天数，自动算出到期日
- **状态标签**：保修中 / 即将到期 / 已过期 三种状态，进度条可视化

### 💾 数据备份
- **一键导出 ZIP**：物品数据 + 分类 + 全部照片打包为一个 ZIP 文件
- **一键导入恢复**：选择 ZIP 文件恢复全部数据，自动处理分类 ID 映射
- **备份记录**：每次备份操作自动记录，可查看和删除

### ⚙️ 个性化
- **深色/浅色/跟随系统**三种显示模式
- **自定义头像**：收藏家卡片集成头像
- **检查更新**：连接 GitHub Releases 检测新版本，下载进度条实时显示

---

## 🏗 技术架构

### 技术栈

| 领域 | 技术 | 版本 |
|------|------|------|
| 语言 | Kotlin | 2.0 |
| UI | Jetpack Compose + Material3（Warm Glassmorphism） | BOM 2025.06.00 |
| 数据库 | Room | 2.6.1 |
| DI | Hilt | 2.53.1 |
| 图片加载 | Coil | 2.7.0 |
| 毛玻璃效果 | Haze | 1.5.2 |
| 相机 | CameraX | 1.3.4 |
| 数据存储 | DataStore Preferences | 1.1.1 |
| 后台任务 | WorkManager | 2.10.0 |
| 图表 | Canvas 自绘（Donut / Sparkline） | — |
| 最低 SDK | Android 12 (API 31) | |
| 目标 SDK | Android 16 (API 36) | |

### 架构分层

```
app/src/main/java/com/nanji/lootarchive/
├── data/                    # 数据层
│   ├── local/
│   │   ├── dao/            # Room DAO 接口
│   │   ├── entity/         # 数据库实体
│   │   └── database/       # AppDatabase
│   └── repository/         # 数据仓库（单一数据源）
├── di/                     # Hilt 依赖注入模块
├── domain/model/           # 领域模型（ItemWithPhotos 等）
├── ui/                     # UI 层（按功能分包）
│   ├── additem/            # 新增/编辑物品
│   ├── backup/             # 备份与恢复
│   ├── camera/             # CameraX 拍照
│   ├── category/           # 分类管理
│   ├── component/          # 通用组件（GlassPanel, GlassCard 等）
│   ├── detail/             # 物品详情
│   ├── home/               # 首页（物品网格）
│   ├── search/             # 搜索
│   ├── settings/           # 设置
│   ├── statistics/         # 统计图表
│   └── theme/              # 主题系统（Material3 定制）
├── util/                   # 工具类
│   ├── ApkDownloadManager  # 自定义下载器（含进度回调）
│   ├── BackupUtil          # ZIP 打包/解包
│   ├── FormatUtil          # 价格/文件大小格式化
│   ├── LocationSuggestions # 存放位置建议
│   ├── PhotoUtil           # 照片保存工具
│   ├── Tuples              # 通用元组（Quad/Quintet/Sextet）
│   └── UpdateChecker       # GitHub 版本检查
├── MainActivity.kt         # 入口 Activity
└── LootArchiveApp.kt       # Application（Hilt）
```

### 导航设计

使用**自定义状态路由**（非 Navigation Compose NavHost），通过 `currentRoute` 状态管理页面切换，配合 `AnimatedContent` 实现过渡动画。子页面使用 `backStack` 管理返回栈。

### 主题设计

- **Warm Glassmorphism** 风格：Haze 实时模糊 + 半透明玻璃质感 + 柔和单阴影
- **暖色调体系**：琥珀色主色 #E8782A、紫罗兰辅色 #7C3AED、暖象牙背景 #FBF9F6
- **字体系统**：Fredoka 标题 + Nunito 正文 + Monospace 数字
- **图标统一**：全局使用 `Icons.Rounded.*`，返回按钮使用 `Icons.AutoMirrored.Rounded.ArrowBack`
- **语义色板**：保修色、玻璃色、深浅模式独立色调

### 数据库设计

**items** — 物品主表
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long (PK) | 自增主键 |
| name | String | 物品名称 |
| categoryId | Long (FK) | 所属分类 |
| purchasePrice | Double | 购入价格 |
| storageLocation | String | 存放位置 |
| purchaseDate | Long? | 购入日期（时间戳） |
| warrantyExpiryDate | Long? | 保修到期日 |
| warrantyPeriodDays | Int? | 保修天数 |
| description | String | 物品描述 |
| isDeleted | Boolean | 软删除标记 |
| createdAt | Long | 创建时间 |
| updatedAt | Long | 最后修改时间 |
| deletedAt | Long? | 删除时间 |

**item_photos** — 物品照片表
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long (PK) | 自增主键 |
| itemId | Long (FK) | 所属物品 |
| photoPath | String | 照片文件路径 |
| sortOrder | Int | 排序 |

**categories** — 分类表
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long (PK) | 自增主键 |
| name | String | 分类名称 |
| sortOrder | Int | 排序 |

**backup_records** — 备份记录表
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long (PK) | 自增主键 |
| fileName | String | 备份文件名 |
| backupType | String | 备份类型 |
| createdAt | Long | 创建时间 |

内置 10 大分类：食品饮料、药品保健、日用百货、数码电子、服饰鞋包、书籍文具、工具器材、藏品摆件、家居家具、其他

---

## 🚀 构建

### 前置条件
- Android Studio Hedgehog (2024.1+) 或更高版本
- JDK 17
- Android SDK 36

### 构建 Release APK

```bash
./gradlew assembleRelease
```

APK 输出路径：`app/build/outputs/apk/release/LootArchive-release-vX.Y.Z.apk`

### 签名

Release 使用 debug keystore 签名（`~/.android/debug.keystore`）。如需正式发布，请替换为正式签名配置。

### APK 优化

- **R8 代码压缩 + 资源缩减**已启用
- **ABI 过滤**：仅保留 arm64-v8a 和 armeabi-v7a
- APK 体积：约 7.1MB

---

## 📝 更新日志

### v5.1.3 (2026-07-29)
- 首页总资产显示精确金额（不再缩写为"万"）
- 统计页饼图和总资产值根据时间区间动态变化
- 我的页面设置备注移除多余"数据备份"

### v5.1.2 (2026-07-29)
- 全新双维度收藏家等级系统
- 数量线（收藏广度）和价值线（藏品深度）独立计算
- 星级取两条线最高一档，价值达人加 ✨ 珍品标记
- 点击等级可查看完整规则和当前进度

### v5.1.1 (2026-07-28)
- 回收站布局优化：物品名称和金额同行显示
- 删除时间单行完整显示不换行

### v5.1 (2026-07-28) 🎈 逢九进一
- 月度趋势横轴修复 + 导航栏变矮
- 月度趋势智能居中、横向滚动显示全部月份
- 统计页 6 项修复（空分类/标题折叠/字体统一/柱图空间/双¥/内边距）
- FAB 不被导航栏遮挡

### v5.0 (2026-07-27) 🎨 Warm Glassmorphism 全新设计
- **全新 Warm Glassmorphism 风格**：Haze 实时模糊 + 半透明玻璃质感 + 便当盒网格布局
- **新字体组合**：Fredoka 圆润标题 + Nunito 柔和正文 + Monospace 数字
- **全新暖色配色**：琥珀主色 #E8782A + 紫罗兰辅色 + 暖象牙背景
- **底部导航栏重构**：等宽布局，图标统一 Rounded 风格
- **统计页全新设计**：Canvas 自绘 Donut 饼图 + 柱状趋势图 + 分类排名
- **物品卡片重设计**：首项双列宽 Hero 卡，圆角 20dp，照片 135dp
- **详情页 BottomSheet**：全幅照片浏览 + 圆点指示器 + 保修进度条
- **新增物品三步骤向导**：照片 → 信息 → 详情
- **全局毛玻璃效果**：LocalHazeState 贯穿组件树
- 分类管理、备份恢复、回收站全面同步新风格

### v4.0 (2026-07-27)
- 全新现代拟物风格（Neumorphism）
- 双列网格 + 卡片式布局
- 统计图表优化
- 备份一键导出/导入

### v3.x (2026-07-26)
- Material3 UI 风格
- CameraX 应用内拍照
- 毛玻璃悬浮导航栏
- 主题色从金色改为橙色

---

## 📄 开源协议

MIT License

---

## 👤 作者

**nanji0710** — [GitHub](https://github.com/nanji0710)

---

*拾物集 —— 记录你的每一件宝贝，管理你的私人物品资产。*
