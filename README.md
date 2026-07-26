# 🏠 拾物集 ItemGlow (LootArchive)

> 纯本地私人物品资产管理工具 —— 你的每一件宝贝都值得被记录

[![Android](https://img.shields.io/badge/Android-12%2B-brightgreen)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-blue)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material3-purple)](https://developer.android.com/compose)
[![License](https://img.shields.io/badge/License-MIT-yellow)](LICENSE)

---

## ✨ 功能特性

### 📦 物品管理
- **双列网格**展示所有物品，卡片式布局，照片占比大更直观
- **增删改查**完整 CRUD，支持名称、分类、价格、存放位置、购入日期、物品描述
- **分类筛选**：底部弹出式分类面板，按分类快速定位物品
- **全文搜索**：按名称/存放位置/备注/保修信息搜索，支持排序
- **回收站**：软删除机制，可恢复或彻底清空

### 📸 照片管理
- **CameraX 拍照**：应用内相机，支持闪光灯切换，连续拍摄多张
- **相册选择**：从系统相册批量选择照片
- **多图浏览**：详情页水平滚动查看所有照片
- **照片编辑**：编辑物品时可新增/删除照片

### 📊 资产统计
- **分类资产概览**：合并展示各分类的价值占比和数量分布
- **月度购入趋势**：柱状图展示每月购入物品总价值
- **时间筛选**：全部时间 / 近三月 / 近半年 / 近一年
- **数字滚动动画**：首页资产总值和物品数量的动画效果

### 🔔 保修追踪
- **保修到期提醒**：首页显示即将到期物品数量
- **自动计算**：输入购入日期 + 保修天数，自动算出到期日
- **状态标签**：保修中 / 即将到期 / 已过期 三种状态

### 💾 数据备份
- **一键导出 ZIP**：物品数据 + 分类 + 全部照片打包为一个 ZIP 文件
- **一键导入恢复**：选择 ZIP 文件恢复全部数据
- **备份记录**：每次备份操作自动记录，可查看和删除

### ⚙️ 个性化
- **深色/浅色/跟随系统**三种显示模式
- **自定义头像**
- **缓存管理**：查看和清除图片缓存
- **检查更新**：连接 GitHub Releases 检测新版本，支持下载进度显示

---

## 🏗 技术架构

### 技术栈

| 领域 | 技术 | 版本 |
|------|------|------|
| 语言 | Kotlin | 2.0 |
| UI | Jetpack Compose + Material3 | BOM 2025.06.00 |
| 数据库 | Room | 2.6.1 |
| DI | Hilt | 2.53.1 |
| 图片加载 | Coil | 2.7.0 |
| 毛玻璃效果 | Haze | 1.5.2 |
| 相机 | CameraX | 1.3.4 |
| 数据存储 | DataStore Preferences | 1.1.1 |
| 后台任务 | WorkManager | 2.10.0 |
| Excel | Apache POI | 5.2.5 |
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
- **POI 资源排除**：排除 PPT/Word/Visio 等无用模块
- APK 体积：约 6.2MB

---

## 📝 更新日志

### v3.1.4 (2026-07-26)
- 修复搜索页分类筛选（物品名称/存放位置/备注/保修）不生效
- 修复浅色模式备份按钮白底
- 修复编辑照片后旧照片变空白占位符

### v3.1.3 (2026-07-26)
- 修复底部导航栏毛玻璃透明问题
- 修复编辑物品删除照片后反而增多
- 搜索页缩略图正常显示
- 搜索页排序功能生效

### v3.1.2 (2026-07-26)
- 主题色橙色全面生效（修复 4 处残留暗金色）
- 相机拍照安全防护
- 物品详情页照片铺满显示

### v3.1.1 (2026-07-26)
- 主题色从金色改为橙色(#FFA500)
- 导航栏毛玻璃透明度降低
- 修复拍照闪退
- 合并分类价值占比和数量对比图表

### v3.1.0 (2026-07-26)
- 毛玻璃悬浮导航栏（Haze 库实现实时模糊）
- 详情页重设计（300dp 主图 + 偏移卡片重叠效果）
- CameraX 应用内拍照

### v3.0.0 (2026-07-26)
- 全新 Material3 UI 风格
- 物品卡片重设计
- 统计图表优化
- 备份一键导出/导入
- 清理大量冗余代码

---

## 📄 开源协议

MIT License

---

## 👤 作者

**nanji0710** — [GitHub](https://github.com/nanji0710)

---

*拾物集 —— 记录你的每一件宝贝，管理你的私人物品资产。*
