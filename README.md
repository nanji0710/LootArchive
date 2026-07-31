# 拾物集 ItemGlow

> 纯本地私人物品资产管理工具 — 每件宝贝都值得被记录

[![Android](https://img.shields.io/badge/Android-12%2B-brightgreen)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-blue)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material3-orange)](https://developer.android.com/compose)
[![Version](https://img.shields.io/badge/Version-6.5.0-orange)]()
[![License](https://img.shields.io/badge/License-MIT-yellow)](LICENSE)

---

## 功能

### 物品管理

- 便当盒网格首页，首项双列宽 Hero 卡片，半透明状态区分
- 增删改查，字段覆盖名称 / 分类 / 价格 / 存放位置 / 购入日期 / 描述
- 5 种物品状态：在用 / 闲置 / 已出 / 待修 / 丢失；非在用卡片自动半透明
- 自定义标签系统，搜索页可筛选
- 分类横向滚动筛选
- 全文搜索：名称 / 位置 / 备注 / 保修，三级行内展开式 Chip 筛选 + 排序 + 历史持久化
- 回收站：14 天倒计时 + 自动清理 + 还原 / 彻底删除

### 照片

- CameraX 应用内拍照，支持闪光灯 + 连拍
- 系统相册批量选择
- 详情页全幅水平滚动 + 圆点指示器
- 导入照片 UUID 文件名防冲突

### 资产统计

- 分类资产环形图 (Canvas Donut)
- 分类多维雷达图 (Canvas + TextMeasurer 字体渲染)
- 累计净值趋势线 (Canvas)
- 月度购入柱状图，≤6 列居中，>6 列横向滚动
- 分类排名（金/银/铜）+ 标签资产分布
- 时间筛选：全部 / 近三月 / 近半年 / 近一年
- 仅统计拥有物品（在用+闲置+待修）
- CSV 数据导出

### 收藏家成长体系

双维度：数量线（广度）+ 价值线（深度），取最高星级。

**EXP 系统**（10 级阶梯，从 DB 实时重算）：

| 等级 | 称号 | 所需 EXP |
|:--|:--|--:|
| Lv.1 | 入门 | 0 |
| Lv.2 | 新手 | 50 |
| Lv.3 | 爱好者 | 150 |
| Lv.4 | 达人 | 350 |
| Lv.5 | 收藏家 | 600 |
| Lv.6 | 专家 | 1,000 |
| Lv.7 | 大师 | 2,000 |
| Lv.8 | 藏家 | 5,000 |
| Lv.9 | 鉴赏家 | 10,000 |
| Lv.10 | 传奇 | 20,000 |

| 来源 | EXP |
|:--|--:|
| 拥有物品 | 每件 +10 |
| 资产价值 | 每 ¥100 +1 |
| 完善描述 | 每件 +3 |
| 拍摄照片 | 每张 +2 |

### 成就徽章 (13 枚)

| 类别 | 成就 | 目标 |
|:--|:--|--:|
| 收藏 | 初级收藏 / 中级收藏家 / 高级收藏家 / 百物之主 | 5 / 20 / 50 / 100 件 |
| 价值 | 万元户 / 小富翁 / 财富自由 | ¥1万 / ¥10万 / ¥50万 |
| 细节 | 细节控 / 文字家 | 10 / 50 件描述 |
| 摄影 | 随手拍 / 摄影师 | 10 / 50 张 |
| 活跃 | 坚持一周 / 月常打卡 | 7 / 30 天 |

实时进度追踪 + 解锁弹窗 (2.5s 自动消失) + 去重 + LazyRow 横向滚动展示

### 数据备份

- 一键导出 ZIP（物品 / 分类 / 照片 / 成就 / EXP / 备份记录）
- 一键导入恢复，自动 ID 映射 + EXP 重算
- 备份提醒开关

### 个性化与设置

- 深色 / 浅色 / 跟随系统
- 自定义头像 + 首页背景
- 保修到期提醒（阈值可调）
- 缓存清理 + 数据看板
- 新手引导 5 页玻璃卡片轮播（首次启动，可重置）
- GitHub Releases 版本更新检测

---

## 技术栈

| 领域 | 方案 | 版本 |
|:--|:--|:--|
| 语言 | Kotlin | 2.0 |
| UI | Jetpack Compose + Material 3 | BOM 2025.06 |
| 数据库 | Room | 2.6.1 |
| DI | Hilt | 2.53.1 |
| 图片 | Coil | 2.7.0 |
| 模糊 | Haze (Glassmorphism) | 1.5.2 |
| 相机 | CameraX | 1.3.4 |
| 存储 | DataStore Preferences | 1.1.1 |
| 后台 | WorkManager | 2.10.0 |
| 图表 | Canvas 自绘 | — |
| 系统 | Android 12+ (API 31) → 16 (API 36) | |

---

## 架构

```
app/src/main/java/com/nanji/lootarchive/
├── data/
│   ├── local/
│   │   ├── dao/          7 个 DAO
│   │   ├── entity/       7 个实体
│   │   └── database/     AppDatabase (v4, destructive fallback)
│   ├── repository/       4 个仓库 (Item/Category/Settings/Backup)
│   └── ExpService.kt     EXP + 成就引擎
├── di/                   Hilt 模块
├── ui/
│   ├── additem/          新增/编辑 (三步骤向导)
│   ├── backup/           备份与恢复
│   ├── camera/           CameraX
│   ├── category/         分类管理
│   ├── component/        GlassPanel / ClayCard / GlassAlertDialog / RadarChart / TrendLineChart / WheelDatePicker
│   ├── detail/           物品详情 (照片轮播 + 状态切换)
│   ├── home/             首页 (便当网格 + 分类筛选 + Hero 统计)
│   ├── onboarding/       新手引导 (HorizontalPager 5 页)
│   ├── recyclebin/       回收站 (倒计时 + 还原)
│   ├── search/           搜索 (三级筛选 + 历史)
│   ├── settings/         设置
│   ├── statistics/       统计图表
│   ├── theme/            主题 (Warm Glassmorphism + Fredoka/Nunito/Mono 字体)
│   ├── MainScreen.kt     主导航 (3 Tab 底部胶囊)
│   └── MyLandingScreen.kt 我的页面
├── util/                 工具类
└── MainActivity.kt       入口 (引导判断)
```

**导航**：自定义状态路由 (`currentRoute` + `backStack`)，`AnimatedContent` 过渡动画。底部 3 Tab（首页 / 统计 / 我的），子页面压栈返回。

---

## 设计系统

**Warm Glassmorphism** — Haze 实时模糊 + 半透明玻璃 + 柔和阴影。

| Token | 浅色 | 深色 |
|:--|:--|:--|
| 主色 | `#E8782A` 琥珀 | 自适应提亮 |
| 辅色 | `#7C3AED` 紫罗兰 | — |
| 背景 | `#FBF9F6` 暖象牙 | `#0C0C10` 深暖黑 |
| 卡片 | `#FEFDFB` 微暖白 | `#1C1A18` 暖深棕 |
| 文字主 | `#1C1917` | `#F0ECE6` |
| 文字辅 | `#78716C` | `#A8A29E` |

**字体**：Fredoka (标题) + Nunito (正文) + Monospace (数字/价格)。  
**图标**：Material Icons Rounded (`Icons.Rounded.*`)，返回键 `AutoMirrored`。  
**间距**：外层 16dp / 卡片间 12dp / 内部 16dp。  
**阴影**：卡片 1dp / 悬浮面板 4dp / FAB 6dp / 导航 8dp。

---

## 数据库

### items
| 字段 | 类型 | 说明 |
|:--|:--|:--|
| id | Long PK | 自增 |
| name | String | 名称 |
| categoryId | Long FK | 分类 |
| purchasePrice | Double | 价格 |
| storageLocation | String | 位置 |
| purchaseDate | Long? | 购入日期 |
| warrantyExpiryDate | Long? | 保修到期 |
| warrantyPeriodDays | Int? | 保修天数 |
| description | String | 描述 |
| isDeleted | Boolean | 软删除 |
| deletedAt | Long? | 删除时间 |
| status | String | 状态 (active/idle/sold/repair/lost) |
| tags | String | 标签 (逗号分隔) |
| lastStatusChangedAt | Long? | 状态变更时间 |

索引: `categoryId` / `name` / `isDeleted` / `status`

### item_photos
| 字段 | 类型 |
|:--|:--|
| id | Long PK |
| itemId | Long FK |
| photoPath | String |
| sortOrder | Int |

### categories
| 字段 | 类型 |
|:--|:--|
| id | Long PK |
| name | String |
| iconName | String |
| sortOrder | Int |

内置 10 类：食品饮料 / 药品保健 / 日用百货 / 数码电子 / 服饰鞋包 / 书籍文具 / 工具器材 / 藏品摆件 / 家居家具 / 其他

### achievements
| 字段 | 类型 |
|:--|:--|
| key | String PK |
| title | String |
| description | String |
| category | String |
| target | Int |
| progress | Int |
| isUnlocked | Boolean |
| unlockedAt | Long? |

### user_profile
| 字段 | 类型 |
|:--|:--|
| id | Int PK (固定 1) |
| exp | Int |
| level | Int |
| totalItemsAdded | Int |
| totalPhotosAdded | Int |
| totalDescriptionsFilled | Int |
| streakDays | Int |
| lastActiveDate | Long? |

另有 `backup_records` 和 `experience_logs` 表。

---

## 构建

```bash
./gradlew assembleRelease
```

APK 输出：`LootArchive-release-v6.5.0.apk`（约 7.4 MB，arm64-v8a + armeabi-v7a，R8 压缩 + 资源缩减）

---

## 更新日志

### v6.5.0 (2026-07-31) 视觉优化
- 全局 Emoji → Material Icons (15 处)
- 导航栏尺寸规范、全页面间距统一、卡片 elevation 统一
- 暗黑模式卡片改暖深棕、首页价格改等宽数字

### v6.3.0 (2026-07-30) 新手引导
- 5 页玻璃卡片轮播，首次启动或设置页重置触发

### v6.2.0 (2026-07-30)
- 物品状态横向滚动；首页价格背景方块修复；回收站去双重提示；徽章改主题色

### v6.1.0 (2026-07-30)
- 搜索页重构；成就入口徽章 + 去重 + 详情弹窗；回收站倒计时修复

### v6.0.0 (2026-07-30)
- 成就详情/通知；搜索历史持久化；WorkManager 回收站清理；设置页备份提醒+数据看板

### v5.5.0 (2026-07-29)
- Canvas 雷达图 + 趋势线；CSV 导出；备份兼容新字段 (status/tags)

### v5.3.0 (2026-07-28)
- EXP 10 级 + 13 成就徽章系统

### v5.2.0 (2026-07-28)
- 5 种物品状态 + 标签系统

### v5.0 (2026-07-27) Warm Glassmorphism
- Haze 模糊 + 暖琥珀色板 + Fredoka/Nunito/Mono 字体 + Canvas 饼图 + 便当网格

---

MIT License · [nanji0710](https://github.com/nanji0710)
