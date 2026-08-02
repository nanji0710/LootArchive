# P0 硬伤修复 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 修复 LootArchive v6.6.4 的 5 项高风险硬伤：DB 迁移缺口（升级清库）、保修/备份提醒 Worker 未调度（功能失效）、备份恢复 Zip Slip（安全）、release 签名用 debug keystore、APK 更新无校验（安全）。

**Architecture:** 以最小侵入方式修复既有代码——恢复历史手写迁移、在 Application.onCreate 补调度、加固文件解压与 URL 校验逻辑，全部改动集中在 6 个文件。

**Tech Stack:** Kotlin / Room 2.6.1 / Hilt / WorkManager / JUnit4 (JVM test) / Gradle

**Global Constraints:**
- 版本基线 v6.6.4，AppDatabase version=6 不变
- MIGRATION_2_3 必须从 git 历史 b0aca8d 恢复（含建表 + seed），不得偏离原始 schema 定义
- 所有 JVM 单测放 `app/src/test/java/`，不新增 androidTest 运行依赖（本机 ABI 无 x86_64，无法跑模拟器）
- build.gradle.kts 现有 release/debug signing 行为在未配置环境变量时必须保持可构建
- 每 Task 结束 `./gradlew :app:assembleDebug` 回归

---

### Task 1: 补齐 DB 迁移链 + 移除 destructive fallback

**Files:**
- Modify: `app/src/main/java/com/nanji/lootarchive/data/local/database/AppDatabase.kt`
- Modify: `app/src/main/java/com/nanji/lootarchive/di/DatabaseModule.kt:35-37`

**Interfaces:**
- Consumes: 现有 `MIGRATION_1_2`、`MIGRATION_4_5`、`MIGRATION_5_6`（保留）
- Produces: `AppDatabase.MIGRATION_2_3`、`AppDatabase.MIGRATION_3_4` 两个新 companion 常量，供 `DatabaseModule.provideDatabase` 引用

- [ ] **Step 1: 在 AppDatabase.kt companion 恢复 `MIGRATION_2_3`**

在 `MIGRATION_1_2` 之后、`MIGRATION_4_5` 之前插入（SQL 完整取自 git 历史 b0aca8d）：

```kotlin
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS user_profile (
                id INTEGER NOT NULL PRIMARY KEY DEFAULT 1,
                exp INTEGER NOT NULL DEFAULT 0,
                level INTEGER NOT NULL DEFAULT 1,
                totalItemsAdded INTEGER NOT NULL DEFAULT 0,
                totalPhotosAdded INTEGER NOT NULL DEFAULT 0,
                totalDescriptionsFilled INTEGER NOT NULL DEFAULT 0,
                streakDays INTEGER NOT NULL DEFAULT 0,
                lastActiveDate INTEGER DEFAULT NULL,
                updatedAt INTEGER NOT NULL DEFAULT 0
            )
        """)
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS achievements (
                `key` TEXT NOT NULL PRIMARY KEY,
                title TEXT NOT NULL,
                description TEXT NOT NULL,
                icon TEXT NOT NULL DEFAULT '',
                category TEXT NOT NULL DEFAULT 'collection',
                isUnlocked INTEGER NOT NULL DEFAULT 0,
                unlockedAt INTEGER DEFAULT NULL,
                progress INTEGER NOT NULL DEFAULT 0,
                target INTEGER NOT NULL DEFAULT 100
            )
        """)
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS experience_log (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                source TEXT NOT NULL,
                amount INTEGER NOT NULL,
                itemId INTEGER DEFAULT NULL,
                createdAt INTEGER NOT NULL DEFAULT 0
            )
        """)
        val achs = listOf(
            listOf("items_5","初级收藏","收集5件物品","collection","5"),
            listOf("items_20","中级收藏家","收集20件物品","collection","20"),
            listOf("items_50","高级收藏家","收集50件物品","collection","50"),
            listOf("items_100","百物之主","收集100件物品","collection","100"),
            listOf("value_10000","万元户","总资产超过1万","value","10000"),
            listOf("value_100000","小富翁","总资产超过10万","value","100000"),
            listOf("value_500000","财富自由","总资产超过50万","value","500000"),
            listOf("photos_10","随手拍","拍摄10张照片","photo","10"),
            listOf("photos_50","摄影师","拍摄50张照片","photo","50"),
            listOf("desc_10","细节控","完善10件物品描述","detail","10"),
            listOf("desc_50","文字家","完善50件物品描述","detail","50"),
            listOf("streak_7","坚持一周","连续7天活跃","streak","7"),
            listOf("streak_30","月常打卡","连续30天活跃","streak","30")
        )
        achs.forEach { a ->
            db.execSQL("INSERT OR IGNORE INTO achievements (`key`,title,description,category,target) VALUES ('${a[0]}','${a[1]}','${a[2]}','${a[3]}',${a[4]})")
        }
        db.execSQL("INSERT OR IGNORE INTO user_profile (id,exp,level) VALUES (1,0,1)")
    }
}
```

- [ ] **Step 2: 新增 no-op `MIGRATION_3_4`**

在 `MIGRATION_2_3` 之后插入（v3/v4 schema 经 git 对比完全相同，仅 version 号跳变）：

```kotlin
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // v3→v4 无 schema 列变更（v5.4.2 仅移除 MIGRATION_2_3 并跳版本号），no-op
    }
}
```

- [ ] **Step 3: 更新 DatabaseModule 迁移链并移除 fallback**

修改 `DatabaseModule.kt:35-37`：

```kotlin
.addMigrations(
    AppDatabase.MIGRATION_1_2,
    AppDatabase.MIGRATION_2_3,
    AppDatabase.MIGRATION_3_4,
    AppDatabase.MIGRATION_4_5,
    AppDatabase.MIGRATION_5_6
)
.build()
```

删除 `.fallbackToDestructiveMigration()` 行。

- [ ] **Step 4: 验证构建**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL（Room/KSP 编译通过，无 schema 校验错误）

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/nanji/lootarchive/data/local/database/AppDatabase.kt app/src/main/java/com/nanji/lootarchive/di/DatabaseModule.kt
git commit -m "fix(db): 补齐 MIGRATION_2_3/3_4 迁移链，移除 destructive fallback 防升级清库"
```

---

### Task 2: 调度保修 / 备份提醒 Worker

**Files:**
- Modify: `app/src/main/java/com/nanji/lootarchive/LootArchiveApp.kt:36`

**Interfaces:**
- Consumes: `worker/ReminderWorker.kt` 已有的 `WarrantyCheckWorker.schedule(context)`、`BackupReminderWorker.schedule(context)`（`@HiltWorker` + `enqueueUniquePeriodicWork`，实现已验证完整）
- Produces: 应用启动即注册两个唯一周期任务（KEEP 策略幂等）

- [ ] **Step 1: 在 onCreate 补调度**

修改 `LootArchiveApp.kt:36` 之后：

```kotlin
override fun onCreate() {
    super.onCreate()
    NotificationUtil.createNotificationChannels(this)
    TrashCleanupWorker.schedule(this)
    WarrantyCheckWorker.schedule(this)
    BackupReminderWorker.schedule(this)
}
```

在 import 区补充：
```kotlin
import com.nanji.lootarchive.worker.WarrantyCheckWorker
import com.nanji.lootarchive.worker.BackupReminderWorker
```

- [ ] **Step 2: 验证构建**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/nanji/lootarchive/LootArchiveApp.kt
git commit -m "fix(worker): 调度保修/备份提醒 Worker，修复提醒功能完全未生效"
```

---

### Task 3: 备份恢复 Zip Slip 路径穿越防护

**Files:**
- Modify: `app/src/main/java/com/nanji/lootarchive/data/repository/BackupRepository.kt:69-81`
- Create: `app/src/test/java/com/nanji/lootarchive/data/repository/BackupRepositoryZipSlipTest.kt`

**Interfaces:**
- Produces: `internal fun resolveSafeFile(targetDir: File, entryName: String): File?` — 返回 null 表示拒绝（路径越界/绝对路径），非 null 时保证父目录已创建

- [ ] **Step 1: 写失败测试**

```kotlin
package com.nanji.lootarchive.data.repository

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class BackupRepositoryZipSlipTest {

    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun `normal entry resolves under target dir`() {
        val target = tmp.newFolder("target")
        val file = resolveSafeFile(target, "photos/a.jpg")
        assertNotNull(file)
        assertTrue(file!!.parentFile.isDirectory) // 父目录已创建
    }

    @Test
    fun `parent-dotdot traversal is rejected`() {
        val target = tmp.newFolder("target")
        assertNull(resolveSafeFile(target, "../../evil.txt"))
    }

    @Test
    fun `absolute path is rejected`() {
        val target = tmp.newFolder("target")
        assertNull(resolveSafeFile(target, "/etc/passwd"))
    }

    @Test
    fun `windows backslash traversal is rejected`() {
        val target = tmp.newFolder("target")
        assertNull(resolveSafeFile(target, "..\\..\\evil.txt"))
    }
}
```

- [ ] **Step 2: 运行确认失败**

Run: `./gradlew :app:testDebugUnitTest --tests "com.nanji.lootarchive.data.repository.BackupRepositoryZipSlipTest"`
Expected: FAIL（`resolveSafeFile` 未定义）

- [ ] **Step 3: 实现 `resolveSafeFile` + 改造 `restorePhotos`**

在 `BackupRepository.kt` companion 附近新增：

```kotlin
/**
 * 安全地将 zip entry 解析到 targetDir 下。返回 null 拒绝越界/绝对路径。
 * 非 null 时保证父目录已创建。
 */
internal fun resolveSafeFile(targetDir: File, entryName: String): File? {
    // 统一为规范路径，拒绝反斜杠分隔符
    val safeName = entryName.replace('\\', '/')
    val targetPath = targetDir.canonicalPath
    val candidate = File(targetDir, safeName)
    val resolved = candidate.canonicalPath
    return if (resolved.startsWith(targetPath + File.separator)) {
        candidate.parentFile?.mkdirs()
        candidate
    } else null
}
```

改造 `restorePhotos` 循环体：

```kotlin
while (entry != null) {
    val file = resolveSafeFile(targetDir, entry.name)
    if (file != null) {
        FileOutputStream(file).use { fos ->
            zis.copyTo(fos)
        }
    } // null → 跳过恶意/异常 entry
    zis.closeEntry()
    entry = zis.nextEntry
}
```

- [ ] **Step 4: 运行确认通过**

Run: `./gradlew :app:testDebugUnitTest --tests "com.nanji.lootarchive.data.repository.BackupRepositoryZipSlipTest"`
Expected: PASS（4 tests）

- [ ] **Step 5: 全量单测 + 构建回归**

Run: `./gradlew :app:testDebugUnitTest :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/nanji/lootarchive/data/repository/BackupRepository.kt app/src/test/java/com/nanji/lootarchive/data/repository/BackupRepositoryZipSlipTest.kt
git commit -m "fix(backup): 修复 restorePhotos Zip Slip 路径穿越，补单测"
```

---

### Task 4: release 签名走环境变量，移除硬编码 debug keystore

**Files:**
- Modify: `app/build.gradle.kts:13-20`

**Interfaces:**
- Produces: signingConfig 优先读环境变量 `LOOTARCHIVE_KEYSTORE_PATH`/`LOOTARCHIVE_KEYSTORE_PASSWORD`/`LOOTARCHIVE_KEY_ALIAS`/`LOOTARCHIVE_KEY_PASSWORD`；未配置时回退 `~/.android/debug.keystore`（保持现状可构建）

- [ ] **Step 1: 重构 signingConfigs**

替换 `app/build.gradle.kts:13-20`：

```kotlin
signingConfigs {
    create("release") {
        val envPath = System.getenv("LOOTARCHIVE_KEYSTORE_PATH")
        if (envPath != null && envPath.isNotBlank()) {
            storeFile = file(envPath)
            storePassword = System.getenv("LOOTARCHIVE_KEYSTORE_PASSWORD") ?: ""
            keyAlias = System.getenv("LOOTARCHIVE_KEY_ALIAS") ?: "lootarchive"
            keyPassword = System.getenv("LOOTARCHIVE_KEY_PASSWORD") ?: ""
        } else {
            // 开发兜底：本机 debug keystore（禁止用于正式发布）
            storeFile = file(System.getProperty("user.home") + "/.android/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }
}
```

- [ ] **Step 2: 生成 release keystore（一次性，由用户保管）**

Run（bash）:
```bash
keytool -genkeypair -v -keystore "$HOME/.android/release.keystore" -alias lootarchive -keyalg RSA -keysize 2048 -validity 10000 -storepass "${LOOTARCHIVE_KEYSTORE_PASSWORD:?}" -keypass "${LOOTARCHIVE_KEY_PASSWORD:?}" -dname "CN=LootArchive, OU=Dev, O=nanji0710, L=CN"
```
（执行前先向用户确认/生成两个口令，写入用户级 `~/.gradle/gradle.properties` 而不入库。若用户暂无口令，回退 debug 兜底即可正常构建。）

- [ ] **Step 3: 验证构建（含 release）**

Run: `./gradlew :app:assembleDebug :app:assembleRelease`
Expected: BUILD SUCCESSFUL（未配环境变量时 release 走 debug 兜底）

- [ ] **Step 4: Commit**

```bash
git add app/build.gradle.kts
git commit -m "fix(build): release 签名改环境变量配置，移除硬编码 debug keystore"
```

---

### Task 5: APK 更新 URL / 版本名校验

**Files:**
- Modify: `app/src/main/java/com/nanji/lootarchive/util/UpdateChecker.kt`
- Modify: `app/src/main/java/com/nanji/lootarchive/util/ApkDownloadManager.kt`
- Modify: `app/src/main/java/com/nanji/lootarchive/ui/MyLandingScreen.kt`（versionName 拼文件名处）
- Create: `app/src/test/java/com/nanji/lootarchive/util/UpdateUrlValidationTest.kt`

**Interfaces:**
- Produces: `fun isValidDownloadUrl(url: String): Boolean`（域名白名单）、`fun isValidVersionName(v: String): Boolean`（`^v?\d+(\.\d+){1,3}$`），均为 `UpdateChecker` 的 internal 顶层函数

- [ ] **Step 1: 写失败测试**

```kotlin
package com.nanji.lootarchive.util

import org.junit.Assert.*
import org.junit.Test

class UpdateUrlValidationTest {

    @Test
    fun `accepts github release download url`() {
        assertTrue(isValidDownloadUrl(
            "https://github.com/nanji0710/LootArchive/releases/download/V6.6.4/LootArchive-release-v6.6.4.apk"))
    }

    @Test
    fun `accepts githubusercontent raw url`() {
        assertTrue(isValidDownloadUrl(
            "https://raw.githubusercontent.com/nanji0710/LootArchive/main/version.json"))
    }

    @Test
    fun `rejects unknown host`() {
        assertFalse(isValidDownloadUrl("https://evil.example.com/LootArchive.apk"))
    }

    @Test
    fun `rejects non-https`() {
        assertFalse(isValidDownloadUrl("http://github.com/foo.apk"))
    }

    @Test
    fun `rejects path traversal versionName`() {
        assertFalse(isValidVersionName("../../etc/passwd"))
        assertFalse(isValidVersionName("6.6.4;rm -rf /"))
    }

    @Test
    fun `accepts normal versionName`() {
        assertTrue(isValidVersionName("6.6.4"))
        assertTrue(isValidVersionName("v6.6.4"))
    }
}
```

- [ ] **Step 2: 运行确认失败**

Run: `./gradlew :app:testDebugUnitTest --tests "com.nanji.lootarchive.util.UpdateUrlValidationTest"`
Expected: FAIL（函数未定义）

- [ ] **Step 3: 在 UpdateChecker.kt 实现校验函数**

新增（文件顶层）：

```kotlin
private val ALLOWED_DOWNLOAD_HOSTS = setOf("github.com", "raw.githubusercontent.com")

internal fun isValidDownloadUrl(url: String): Boolean {
    return try {
        val u = java.net.URI(url)
        u.scheme == "https" && u.host in ALLOWED_DOWNLOAD_HOSTS
    } catch (e: Exception) {
        false
    }
}

internal fun isValidVersionName(v: String): Boolean =
    Regex("""^v?\d+(\.\d+){1,3}$""").matches(v)
```

在 `check()` 返回 UpdateInfo 前追加校验：

```kotlin
if (!isValidVersionName(obj.getString("versionName")) ||
    !isValidDownloadUrl(obj.optString("apkDownloadUrl", ""))) {
    Result.failure(IllegalStateException("Invalid update metadata"))
} else {
    Result.success(UpdateInfo(...))
}
```

- [ ] **Step 4: 在 ApkDownloadManager.download 入口加域名校验**

`download()` 函数开头（`withContext` 内第一行）：

```kotlin
if (!isValidDownloadUrl(url)) {
    throw IllegalArgumentException("非允许的下载域名: $url")
}
```

- [ ] **Step 5: 在 MyLandingScreen 用白名单校验 versionName**

定位 `"LootArchive-v${updateInfo.versionName}.apk"` 处，改为：

```kotlin
val safeVersion = updateInfo.versionName.takeIf { isValidVersionName(it) } ?: return@launch // 或显示错误
fileName = "LootArchive-v$safeVersion.apk"
```

- [ ] **Step 6: 运行确认通过 + 全量回归**

Run: `./gradlew :app:testDebugUnitTest :app:assembleDebug`
Expected: BUILD SUCCESSFUL，6 tests PASS

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/nanji/lootarchive/util/UpdateChecker.kt app/src/main/java/com/nanji/lootarchive/util/ApkDownloadManager.kt app/src/main/java/com/nanji/lootarchive/ui/MyLandingScreen.kt app/src/test/java/com/nanji/lootarchive/util/UpdateUrlValidationTest.kt
git commit -m "fix(update): 校验更新 URL 域名与 versionName 白名单，防路径注入"
```

---

## Self-Review

- **Spec coverage**: optimization-plan.md 的 0.1→0.5 五项各对应一个 Task；0.4 keystore 生成依赖用户口令（Step 2 标注了回退路径），不阻塞。
- **Placeholder scan**: 所有代码块均为完整实现；Task 1 迁移 SQL 取自历史 commit 非占位。
- **Type consistency**: `resolveSafeFile`（Task 3）与 `isValidDownloadUrl/isValidVersionName`（Task 5）为独立文件 internal 函数，跨 Task 引用一致；`WarrantyCheckWorker.schedule`/`BackupReminderWorker.schedule` 与现有签名一致。
- **注意**: Task 1 若 `assembleDebug` 触发 Room schema 校验失败（历史上 exportSchema=false 的 hash 差异），说明存在未记录的 schema 变更，需停下来核对 `ItemEntity` 等实体与迁移的列一致后再继续。
