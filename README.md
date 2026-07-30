# 🏠 拾物集 ItemGlow (LootArchive)

> 纯本地私人物品资产管理工具 —— 你的每一件宝贝都值得被记录

[![Android](https://img.shields.io/badge/Android-12%2B-brightgreen)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-blue)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-Warm%20Glassmorphism-orange)](https://developer.android.com/compose)
[![Version](https://img.shields.io/badge/Version-6.3.0-orange)]()
[![License](https://img.shields.io/badge/License-MIT-yellow)](LICENSE)

---

## ✨ 功能特性

### 📦 物品管理
- **便当盒网格**主页布局，首项双列宽卡片，照片占比大更直观
- **增删改查**完整 CRUD，支持名称、分类、价格、存放位置、购入日期、物品描述
- **物品状态**：在用 / 闲置 / 已出 / 待修 / 丢失，5 种状态切换，非在用物品卡片半透明显示
- **标签系统**：自定义标签，逗号分隔，搜索页按标签筛选
- **分类筛选**：横向滚动分类标签，按分类快速定位物品
- **全文搜索**：按名称/存放位置/备注/保修信息搜索，支持搜索范围+状态+标签三级筛选（行内展开式 Chip），排序和历史记录持久化
- **回收站**：软删除机制，14 天自动清空（WorkManager 后台任务），可还原或彻底删除，倒计时显示

### 📸 照片管理
- **CameraX 拍照**：应用内相机，支持闪光灯切换，连续拍摄多张
- **相册选择**：从系统相册批量选择照片
- **多图浏览**：详情页全幅照片水平滚动，圆点指示器
- **照片编辑**：编辑物品时可新增/删除照片
- **UUID 防覆盖**：导入照片文件名含 UUID 后缀，避免同名冲突

### 📊 资产统计
- **环形图**展示分类资产分布，Canvas 自绘 Donut Chart
- **雷达图**：多维度可视化（物品数/价值/照片/描述/标签/活动），Canvas + TextMeasurer 渲染
- **月度购入趋势**：柱状图展示每月购入物品总价值，智能居中/滚动
- **趋势线图**：累计净值曲线，Canvas 自绘带 Y 轴标签
- **分类排名**：金/银/铜徽章标注前三名
- **时间筛选**：全部时间 / 近三月 / 近半年 / 近一年，饼图和总资产实时联动
- **统计口径**：仅统计拥有物品（在用+闲置+待修），排除已出和丢失
- **CSV 导出**：物品数据导出为 CSV 文件

### 🏆 收藏家等级（双体系）
- **双维度体系**：数量线（收藏广度）+ 价值线（藏品深度）独立计算，取最高星级
- **EXP 体系**：基于实际数据计算（物品数量+价值+照片+描述+连续活跃），10 级阶梯（入门→传奇）
- **动态星级**：价值达人加 ✨ 珍品标记
- **等级规则面板**：点击等级徽章查看完整规则和当前进度
- **EXP 详情弹窗**：等级阶梯 + 当前进度条 + 距离下一级 EXP

### 🎖️ 成就系统
- **13 枚成就徽章**按 4 个类别：collection（收藏）/ value（价值）/ detail（细节）+ photo（摄影）/ streak（活跃）
- **进度追踪**：每项成就实时显示进度（如 "5/20"），解锁时弹窗通知（2.5 秒自动消失）
- **成就详情弹窗**：点击查看标题+描述+进度条+解锁状态
- **去重机制**：已解锁成就不会重复弹窗
- **成就入口**：我的页面顶部收藏家卡片显示 🏅 已解锁/总数 徽章

### 🔔 保修追踪
- **保修到期提醒**：首页显示即将到期物品数量
- **自动计算**：输入购入日期 + 保修天数，自动算出到期日
- **状态标签**：保修中 / 即将到期 / 已过期 三种状态，进度条可视化
- **提醒阈值可调**：设置页自定义提前提醒天数

### 💾 数据备份
- **一键导出 ZIP**：物品数据（含状态+标签）+ 分类 + 照片 + 成就 + EXP 全部打包
- **一键导入恢复**：选择 ZIP 文件恢复全部数据，自动处理分类 ID 映射，导入后自动重算 EXP
- **备份记录**：每次备份操作自动记录
- **备份提醒**：设置页可开启定期备份提醒

### ⚙️ 个性化
- **深色/浅色/跟随系统**三种显示模式
- **自定义头像**：收藏家卡片集成头像，支持从相册选择
- **自定义背景**：首页支持自定义背景图片
- **检查更新**：连接 GitHub Releases 检测新版本，下载进度条实时显示
- **数据看板**：设置页查看回收站物品数量等统计信息
- **缓存管理**：一键清除图片缓存等临时数据
- **新手引导**：首次启动 5 页全屏玻璃卡片轮播，介绍核心功能，设置页可重置重新查看

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
| 图表 | Canvas 自绘（Donut / Radar / TrendLine / Sparkline） | — |
| 最低 SDK | Android 12 (API 31) | |
| 目标 SDK | Android 16 (API 36) | |

### 架构分层

```
app/src/main/java/com/nanji/lootarchive/
├── data/                    # 数据层
│   ├── local/
│   │   ├── dao/            # Room DAO 接口（7个）
│   │   │   ├── ItemDao          # 物品 CRUD + 搜索/状态/标签/统计
│   │   │   ├── ItemPhotoDao     # 照片管理
│   │   │   ├── CategoryDao      # 分类管理
│   │   │   ├── AchievementDao   # 成就查询+解锁+进度更新
│   │   │   ├── UserProfileDao   # 用户档案+EXP
│   │   │   ├── ExperienceLogDao # EXP 日志
│   │   │   └── BackupRecordDao  # 备份记录
│   │   ├── entity/         # 数据库实体（7个）
│   │   │   ├── ItemEntity       # 物品（含 status/tags/lastStatusChangedAt）
│   │   │   ├── ItemPhotoEntity  # 物品照片
│   │   │   ├── CategoryEntity   # 分类
│   │   │   ├── AchievementEntity # 成就（key/title/desc/category/target/progress）
│   │   │   ├── UserProfileEntity # 用户档案（exp/level/streakDays）
│   │   │   ├── ExperienceLogEntity # EXP 变更日志
│   │   │   └── BackupRecordEntity # 备份记录
│   │   └── database/       # AppDatabase（version=4, fallbackToDestructiveMigration）
│   ├── repository/         # 数据仓库（4个：Item/Category/Settings/Backup）
│   └── ExpService.kt       # EXP + 成就引擎（从DB数据重算，SharedFlow弹窗通知）
├── di/                     # Hilt 依赖注入模块
├── ui/                     # UI 层（按功能分包）
│   ├── additem/            # 新增/编辑物品（状态选择+标签输入）
│   ├── backup/             # 备份与恢复
│   ├── camera/             # CameraX 拍照
│   ├── category/           # 分类管理
│   ├── component/          # 通用组件
│   │   ├── RadarChart          # Canvas 雷达图（TextMeasurer 标签）
│   │   ├── TrendLineChart      # Canvas 趋势线图（累计净值）
│   │   ├── GlassComponents     # GlassAlertDialog / ClayCard
│   │   ├── GlassPanel          # 毛玻璃面板
│   │   └── WheelDatePickerDialog # 滚轮日期选择器
│   ├── detail/             # 物品详情（照片轮播+保修进度+状态切换+标签显示）
│   ├── home/               # 首页（便当盒网格+分类筛选+状态筛选+保修提醒）
│   ├── onboarding/          # v6.3 新手引导（5页玻璃卡片轮播+HorizontalPager）
│   ├── search/             # 搜索（三级行内展开筛选+历史持久化+排序）
│   ├── settings/           # 设置（主题+头像+提醒+缓存+数据看板）
│   ├── statistics/         # 统计图表（Donut+Radar+TrendLine+Sparkline+排名+CSV）
│   ├── recyclebin/         # 回收站（倒计时+还原+彻底删除+清空）
│   ├── theme/              # 主题系统（Material3 定制+Warm Glassmorphism）
│   └── MainScreen.kt       # 主导航（底部Tab：首页/统计/我的，自定义状态路由）
├── util/                   # 工具类
│   ├── ApkDownloadManager  # 自定义下载器（含进度回调）
│   ├── BackupUtil          # ZIP 打包/解包（manifest v3）
│   ├── ExpCalculator       # EXP 计算器（10级阶梯+进度+奖励常量）
│   ├── FormatUtil          # 价格/文件大小格式化
│   ├── LocationSuggestions # 存放位置建议
│   ├── PhotoUtil           # 照片保存工具（UUID 文件名）
│   ├── Tuples              # 通用元组（Quad/Quintet/Sextet）
│   └── UpdateChecker       # GitHub 版本检查
├── MainActivity.kt         # 入口 Activity
└── LootArchiveApp.kt       # Application（Hilt）
```

### 导航设计

使用**自定义状态路由**（非 Navigation Compose NavHost），通过 `currentRoute` 状态管理页面切换，配合 `AnimatedContent` 实现过渡动画。底部 3 Tab：首页 / 统计 / 我的。子页面使用 `backStack` 管理返回栈。

### 主题设计

- **Warm Glassmorphism** 风格：Haze 实时模糊 + 半透明玻璃质感 + 柔和单阴影
- **暖色调体系**：琥珀色主色 #E8782A、紫罗兰辅色 #7C3AED、暖象牙背景 #FBF9F6
- **字体系统**：Fredoka 标题 + Nunito 正文 + Monospace 数字
- **图标统一**：全局使用 `Icons.Rounded.*`，返回按钮使用 `Icons.AutoMirrored.Rounded.ArrowBack`
- **语义色板**：保修色（WarrantyExpired/WarrantyExpiring）、玻璃色、深浅模式独立色调
- **Canvas 图表字体**：使用 Compose TextMeasurer + drawText 替代 Paint.drawText，保持字体一致性

### 数据库设计

**items** — 物品主表
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long (PK) | 自增主键 |
| name | String | 物品名称 |
| categoryId | Long (FK) | 所属分类 |
| purchasePrice | Double | 购入价格 |
| currency | String | 货币代码（默认 CNY） |
| storageLocation | String | 存放位置 |
| purchaseDate | Long? | 购入日期（时间戳） |
| warrantyExpiryDate | Long? | 保修到期日 |
| warrantyPeriodDays | Int? | 保修天数 |
| description | String | 物品描述 |
| isDeleted | Boolean | 软删除标记 |
| deletedAt | Long? | 删除时间 |
| createdAt | Long | 创建时间 |
| updatedAt | Long | 最后修改时间 |
| status | String | 物品状态：active/idle/sold/repair/lost |
| tags | String | 标签（逗号分隔） |
| lastStatusChangedAt | Long? | 状态最后变更时间 |

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
| iconName | String | Material Icon 名称 |
| sortOrder | Int | 排序 |

**achievements** — 成就表
| 字段 | 类型 | 说明 |
|------|------|------|
| key | String (PK) | 成就标识（如 items_5） |
| title | String | 成就标题 |
| description | String | 成就描述 |
| icon | String | 图标（emoji） |
| category | String | 类别：collection/value/photo/detail/streak |
| isUnlocked | Boolean | 是否已解锁 |
| unlockedAt | Long? | 解锁时间 |
| progress | Int | 当前进度 |
| target | Int | 目标值 |

**user_profile** — 用户档案表
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Int (PK) | 固定 1（单用户） |
| exp | Int | 总经验值 |
| level | Int | 等级（1-10） |
| totalItemsAdded | Int | 累计添加物品数 |
| totalPhotosAdded | Int | 累计照片数 |
| totalDescriptionsFilled | Int | 累计完善描述数 |
| streakDays | Int | 连续活跃天数 |
| lastActiveDate | String? | 最后活跃日期 |

**backup_records** — 备份记录表
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long (PK) | 自增主键 |
| fileName | String | 备份文件名 |
| backupType | String | export / import |
| itemCount | Int | 物品数量 |
| createdAt | Long | 创建时间 |

**experience_logs** — EXP 变更日志表
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long (PK) | 自增主键 |
| exp | Int | 变更 EXP |
| reason | String | 变更原因 |
| createdAt | Long | 变更时间 |

内置 10 大分类：食品饮料、药品保健、日用百货、数码电子、服饰鞋包、书籍文具、工具器材、藏品摆件、家居家具、其他

### EXP 系统

**计算方式**：从数据库实际数据重算（非增量计数），所有 CRUD 操作后触发 `recalculateProfile()`。

| 行为 | EXP |
|------|-----|
| 添加物品 | +5 |
| 拍摄照片 | +2 |
| 完善描述 | +3 |
| 物品数量（拥有） | 每件 +10 |
| 资产价值 | 每 ¥1000 +1 |

**等级阶梯**：Lv.1 入门 (0) → Lv.2 新手 (50) → Lv.3 爱好者 (150) → Lv.4 达人 (350) → Lv.5 收藏家 (600) → Lv.6 专家 (1000) → Lv.7 大师 (2000) → Lv.8 藏家 (5000) → Lv.9 鉴赏家 (10000) → Lv.10 传奇 (20000)

### 13 枚成就

| 类别 | 成就 |
|------|------|
| 🏆 收藏 | 初级收藏 (5件) / 中级收藏家 (20件) / 高级收藏家 (50件) / 百物之主 (100件) |
| 💰 价值 | 万元户 (¥1万) / 小富翁 (¥10万) / 财富自由 (¥50万) |
| ✍️ 细节 | 细节控 (10件描述) / 文字家 (50件描述) |
| 📸 摄影 | 随手拍 (10张) / 摄影师 (50张) |
| 🔥 活跃 | 坚持一周 (7天) / 月常打卡 (30天) |

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

### v6.3.0 (2026-07-30) 🎉 新手引导
- 首次启动 5 页全屏玻璃卡片轮播（HorizontalPager + 28dp 圆角 + Warm Glassmorphism）
- 支持滑动切换 + 缩略图导航 + 点状指示器
- "跳过引导"直接进入首页，"开始使用"写入完成标记
- 设置页"个性化"→"重新查看引导"可重置

### v6.2.0 (2026-07-30)
- 物品状态选择改为横向滚动，防止"丢失"换行
- 首页非拥有物品卡片去除价格背景方块
- 回收站去除底部 Snackbar 重复提示，保留页面内提示
- 成就入口徽章紫色改为 Primary() 主题色

### v6.1.7 (2026-07-30)
- 成就网格改为 LazyRow 横向滚动
- "从相册选择"按钮缩小图标/间距/字体

### v6.1.6 (2026-07-30)
- 成就图标可见性修复（alpha 0.35→1.0）
- 成就网格去掉透明 Surface + weight

### v6.1.5 (2026-07-30)
- 修复顶部文字竖排：徽章独占一行

### v6.1.4 (2026-07-30)
- 成就种子 DAO 补种 + 状态变更触发 EXP 重算 + 全流程审计

### v6.1.2 (2026-07-30)
- 修复成就徽章不显示（移除外层条件门控，各徽章独立显隐）
- 搜索筛选改为行内展开式 Chip（范围/状态均与标签筛选一致）
- 回收站倒计时拆分双行（时间戳 + 倒计时）

### v6.1.1 (2026-07-30)
- 搜索筛选改为行内展开式 Chip
- 修复回收站倒计时单行截断
- 修复成就徽章新用户不显示

### v6.1.0 (2026-07-30)
- 搜索页重构：折叠式 Dropdown 筛选 + 可折叠标签行 + 纵向历史列表
- 成就入口徽章（顶部 🏅 已解锁/总数）
- 成就弹窗去重（emittedKeys Set）
- 回收站倒计时排版修复（单行合并）
- 成就详情弹窗（标题+描述+进度条+解锁状态）

### v6.0.0 (2026-07-30)
- 成就详情弹窗 + 解锁通知弹窗（2.5s 自动消失）
- 搜索历史 DataStore 持久化
- 回收站 14 天倒计时 WorkManager 后台清理
- 设置页：备份提醒开关 + 数据看板
- 统计口径修正：仅统计拥有物品（排除已出/丢失）

### v5.5.3 (2026-07-29)
- 照片文件名 UUID 防覆盖
- 我的页面方案 A 双行布局（身份+EXP）
- 等级阶梯高亮修正

### v5.5.2 (2026-07-29)
- 导入 EXP 刷新 + EXP 显示精简
- 物品详情弹窗 + 趋势图标尺标签

### v5.5.1 (2026-07-29)
- 雷达图颜色加深 + EXP 始终可见 + 成就种子数据修复

### v5.5.0 (2026-07-29)
- Canvas 雷达图（TextMeasurer 标签）+ 趋势累计净值线 + CSV 文件导出
- EXP 恢复 + 备份兼容新字段（status/tags/lastStatusChangedAt）

### v5.4.2 (2026-07-29)
- 修复启动崩溃，移除手动 DB migration

### v5.3.0 (2026-07-28)
- EXP 收藏家等级 v2（10级阶梯，从 DB 实际数据重算）
- 13 枚成就徽章系统（4 类别 + 进度追踪 + 解锁通知）

### v5.2.0 (2026-07-28)
- 物品状态管理：active/idle/sold/repair/lost 五种状态
- 标签系统：自定义标签 + 标签筛选
- 非拥有物品卡片 50% 透明度

### v5.1.3 (2026-07-29)
- 首页总资产显示精确金额（不再缩写为"万"）
- 统计页饼图和总资产值根据时间区间动态变化
- 我的页面设置备注移除多余"数据备份"

### v5.1 (2026-07-28) 🎈 逢九进一
- 月度趋势横轴修复 + 导航栏变矮
- 月度趋势智能居中、横向滚动显示全部月份
- 统计页 6 项修复

### v5.0 (2026-07-27) 🎨 Warm Glassmorphism 全新设计
- 全新 Warm Glassmorphism 风格：Haze 实时模糊 + 半透明玻璃质感
- 新字体组合：Fredoka + Nunito + Monospace
- 全新暖色配色：琥珀主色 #E8782A + 紫罗兰辅色
- Canvas 自绘 Donut 饼图 + 柱状趋势图 + 分类排名
- 首项双列宽 Hero 卡，三步骤新增物品向导

### v4.0 (2026-07-27)
- 全新现代拟物风格（Neumorphism）+ 双列网格卡片布局 + 备份一键导出/导入

### v3.x (2026-07-26)
- Material3 UI 风格 + CameraX 应用内拍照 + 毛玻璃悬浮导航栏

---

## 📄 开源协议

MIT License

---

## 👤 作者

**nanji0710** — [GitHub](https://github.com/nanji0710)

---

*拾物集 —— 记录你的每一件宝贝，管理你的私人物品资产。*
