# 数据库迁移注意事项（LootArchive）

记录 Room 迁移中踩过的坑与可复用的检查手段。每次新增迁移前请过一遍文末清单。

## 1. 为什么 v6 → v7 必须做「表重建」

Android 12 自带的 SQLite 是 3.32，**不支持 `ALTER TABLE ... DROP COLUMN`**（3.35 才有）。
`MIGRATION_6_7` 要删掉 `items.salePrice` / `items.saleDate` 两列，只能走官方表重建流程：

```
建 items_new（目标结构） → INSERT ... SELECT 拷贝数据 → DROP TABLE items
→ ALTER TABLE items_new RENAME TO items → 重建 4 个索引
```

## 2. 坑：`DROP TABLE items` 会级联删掉照片记录

`item_photos.itemId → items.id` 带 `ON DELETE CASCADE`（`ItemPhotoEntity.kt:11-15`，全库**唯一**外键）。
SQLite 在执行 `DROP TABLE` 时，会对被删表做一次隐式 `DELETE`，并**执行外键级联动作**，
于是 `item_photos` 里的照片记录被一并删除 —— 表结构迁移成功、物品数据完好，
但「物品 ⇄ 照片」的关联全部消失（照片文件仍留在 `filesDir/LootArchive/photos/`，成为孤儿文件）。

**`PRAGMA defer_foreign_keys = ON` 挡不住这个动作**：该 pragma 只把外键约束的「校验」推迟到事务提交，
不推迟级联「动作」本身。实测（`tools/verify_migration_6_7.py` 的判别力验证）：

| 迁移写法 | items | item_photos |
| --- | --- | --- |
| 仅 `PRAGMA defer_foreign_keys = ON` | 3 | **0** |
| DROP 前把照片行存入临时表、重建后写回 | 3 | 3 |

正确写法（现行 `MIGRATION_6_7`）：

```kotlin
db.execSQL("CREATE TEMP TABLE IF NOT EXISTS item_photos_backup AS SELECT * FROM item_photos")
// …建表 / 拷贝 / DROP TABLE items / 改名 / 重建索引…
db.execSQL("DELETE FROM item_photos")
db.execSQL("""INSERT INTO item_photos (id, itemId, photoPath, sortOrder, createdAt)
              SELECT id, itemId, photoPath, sortOrder, createdAt FROM item_photos_backup""")
db.execSQL("DROP TABLE temp.item_photos_backup")
```

写回发生在 `items` 重建完成之后，外键可正常解析；实测无外键违规、无孤儿照片行、
`sortOrder` 与归属保持不变。

## 3. 回归校验脚本

`tools/verify_migration_6_7.py`（纯标准库，不需要 Android/Room）：

```pwsh
python tools/verify_migration_6_7.py                  # 校验仓库里的 AppDatabase.kt
python tools/verify_migration_6_7.py <某份副本.kt>     # 校验改造过的副本
```

脚本从 `AppDatabase.kt` 中**直接抽取** `MIGRATION_6_7` 里按顺序执行的 `db.execSQL(...)` 语句，
在内存里造一个 v6 库（含 3 件物品、3 张照片、软删除与已出状态）执行迁移，然后断言 11 项：
物品行数/字段内容不变、**照片行数不变**、照片归属与排序不变、列集合恰为 17 列且无售出列、
4 个索引重建、无外键违规、无孤儿照片、`user_version = 7`。

判别力已实测：把照片备份语句从副本里去掉后，脚本报 `[FAIL] 照片行数不变（CASCADE 回归点）(3 -> 0)`，
退出码 1。

## 4. 已经跑过缺陷版迁移的库怎么补救

缺陷版迁移只删了 `item_photos` 行，**照片文件还在**，物品数据也完整；但「谁的照片」这一映射
只存在于备份 ZIP 的 manifest（每个物品的 `photos` 数组，见 `util/BackupUtil.kt:70-74、274-284`）。
补救步骤：用**迁移前**生成的完整备份恢复一次（我的页面 → 备份与恢复 → 点该条记录 → 恢复），
照片会被重新登记到对应物品；随后核对详情页照片与实际文件数。

## 5. 新增迁移前的检查清单

1. 目标 schema 变更是否需要表重建（SQLite < 3.35 无法删列/改列类型）？
2. 被重建的表**是否被别的表用外键引用**？有没有 `ON DELETE CASCADE`（本库仅 `item_photos → items`）？
   - 有 → 先把引用方数据备份到临时表，重建完成后再写回；不要依赖 `defer_foreign_keys`。
3. 迁移里创建的索引名、列默认值、`NOT NULL` 是否与实体注解完全一致（Room 会在打开时校验 schema，不一致直接抛 `Migration didn't properly handle`）。
4. 迁移是否幂等（`IF NOT EXISTS`）、是否保留软删除行与回收站数据（`isDeleted = 1` 的行不能被过滤掉）。
5. 跑 `tools/verify_migration_6_7.py`（改一下脚本里的版本号与断言即可复用），并真机验证：
   升级后物品数、金额、照片、保修日期、标签、回收站条目都与升级前一致。
