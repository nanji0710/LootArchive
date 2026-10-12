# -*- coding: utf-8 -*-
"""MIGRATION_6_7 回归校验（无三方依赖，纯标准库 sqlite3）。

背景：item_photos.itemId → items.id 是 ON DELETE CASCADE。MIGRATION_6_7 采用
「建新表 → 拷贝 → DROP TABLE items → 改名」的表重建流程，而 `DROP TABLE items`
会执行隐式 DELETE 并**触发级联动作**，把 item_photos 里的照片记录一并删掉。
`PRAGMA defer_foreign_keys = ON` 只推迟外键约束的「校验」，不推迟级联「动作」，
因此不能靠它保护照片行 —— 必须在 DROP 之前把照片行备份到临时表，重建后写回。

本脚本从 AppDatabase.kt 中**直接抽取** MIGRATION_6_7 里按顺序执行的 db.execSQL(...)
语句，在一个模拟的 v6 库上执行，然后断言数据完整性，用来防止该缺陷回归。

用法（任意 Python 3）：  python tools/verify_migration_6_7.py [AppDatabase.kt 路径]
不带参数时校验仓库里的 app/src/main/.../database/AppDatabase.kt；
额外传入路径可用于验证「改造过的副本」（例如故意去掉照片备份语句，检查本脚本能否报红）。
退出码 0 = 全部通过；非 0 = 有断言失败。
"""

import os
import re
import sqlite3
import sys
import tempfile

DEFAULT_DB = os.path.join(
    os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
    "app", "src", "main", "java", "com", "nanji", "lootarchive",
    "data", "local", "database", "AppDatabase.kt",
)

V6_SCHEMA = """
CREATE TABLE categories (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    name TEXT NOT NULL
);
CREATE TABLE items (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    name TEXT NOT NULL,
    categoryId INTEGER NOT NULL,
    purchasePrice REAL NOT NULL,
    currency TEXT NOT NULL,
    storageLocation TEXT NOT NULL,
    purchaseDate INTEGER,
    warrantyExpiryDate INTEGER,
    warrantyPeriodDays INTEGER,
    description TEXT NOT NULL,
    isDeleted INTEGER NOT NULL,
    deletedAt INTEGER,
    createdAt INTEGER NOT NULL,
    updatedAt INTEGER NOT NULL,
    status TEXT NOT NULL DEFAULT 'active',
    tags TEXT NOT NULL DEFAULT '',
    lastStatusChangedAt INTEGER DEFAULT NULL,
    salePrice REAL DEFAULT NULL,
    saleDate INTEGER DEFAULT NULL
);
CREATE INDEX index_items_categoryId ON items (categoryId);
CREATE INDEX index_items_name ON items (name);
CREATE INDEX index_items_isDeleted ON items (isDeleted);
CREATE INDEX index_items_status ON items (status);
CREATE TABLE item_photos (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    itemId INTEGER NOT NULL,
    photoPath TEXT NOT NULL,
    sortOrder INTEGER NOT NULL DEFAULT 0,
    createdAt INTEGER NOT NULL,
    FOREIGN KEY(itemId) REFERENCES items(id) ON UPDATE NO ACTION ON DELETE CASCADE
);
CREATE INDEX index_item_photos_itemId ON item_photos (itemId);
"""

V6_DATA = """
INSERT INTO categories (id, name) VALUES (1, '数码电子');
INSERT INTO items (id, name, categoryId, purchasePrice, currency, storageLocation, purchaseDate,
                   warrantyExpiryDate, warrantyPeriodDays, description, isDeleted, deletedAt,
                   createdAt, updatedAt, status, tags, lastStatusChangedAt, salePrice, saleDate)
VALUES (1, '在用物品', 1, 1315.0, 'CNY', '卧室', 1600000000000, 1610000000000, 1048, '正常',
        0, NULL, 1, 10, 'active', '徒步,背包', NULL, NULL, NULL),
       (2, '已出物品', 1, 500.0, 'CNY', '书房', NULL, NULL, NULL, '', 0, NULL, 2, 20,
        'sold', '', NULL, 19038.0, 1700000000000),
       (3, '回收站物品', 1, 30.0, 'CNY', '', NULL, NULL, NULL, '', 1, 1700000000001, 3, 30,
        'lost', 'x', 1700000000002, NULL, NULL);
INSERT INTO item_photos (itemId, photoPath, sortOrder, createdAt)
VALUES (1, '/data/user/0/com.nanji.lootarchive/files/LootArchive/photos/IMG_1_aaa.jpg', 0, 1),
       (1, '/data/user/0/com.nanji.lootarchive/files/LootArchive/photos/IMG_2_bbb.jpg', 1, 2),
       (2, '/data/user/0/com.nanji.lootarchive/files/LootArchive/photos/IMG_3_ccc.jpg', 0, 3);
PRAGMA user_version = 6;
"""

EXPECTED_COLUMNS = {
    "id", "name", "categoryId", "purchasePrice", "currency", "storageLocation",
    "purchaseDate", "warrantyExpiryDate", "warrantyPeriodDays", "description",
    "isDeleted", "deletedAt", "createdAt", "updatedAt", "status", "tags",
    "lastStatusChangedAt",
}


def extract_migration_sql(path):
    """从 AppDatabase.kt 抽取 MIGRATION_6_7 中按顺序出现的 db.execSQL 语句。"""
    src = open(path, encoding="utf-8").read()
    start = src.find("val MIGRATION_6_7")
    if start < 0:
        raise SystemExit("AppDatabase.kt 中找不到 MIGRATION_6_7")
    nxt = src.find("val MIGRATION_7_8", start)
    block = src[start:nxt if nxt > 0 else len(src)]

    stmts, pos = [], 0
    while True:
        m = re.compile(r"db\.execSQL\(").search(block, pos)
        if not m:
            break
        i = m.end()
        if block.startswith('"""', i):
            j = block.find('"""', i + 3)
            stmts.append(block[i + 3:j])
            pos = j + 3
        else:
            j = block.find('"', i + 1)
            stmts.append(block[i + 1:j])
            pos = j + 1
    if not stmts:
        raise SystemExit("MIGRATION_6_7 中没有解析到任何 db.execSQL 语句")
    return stmts


def main():
    db_path = os.path.join(tempfile.gettempdir(), "lootarchive_migration_6_7_check.db")
    if os.path.exists(db_path):
        os.remove(db_path)

    con = sqlite3.connect(db_path, isolation_level=None)
    con.execute("PRAGMA foreign_keys = ON")
    con.executescript(V6_SCHEMA)
    con.executescript(V6_DATA)

    before_items = con.execute("SELECT COUNT(*) FROM items").fetchone()[0]
    before_photos = con.execute("SELECT COUNT(*) FROM item_photos").fetchone()[0]
    before_rows = con.execute(
        "SELECT id, status, isDeleted, purchasePrice, tags FROM items ORDER BY id").fetchall()

    source_path = sys.argv[1] if len(sys.argv) > 1 else DEFAULT_DB
    print("source: %s" % source_path)
    stmts = extract_migration_sql(source_path)
    con.execute("BEGIN")
    for sql in stmts:
        con.execute(sql)
    con.execute("PRAGMA user_version = 7")
    con.execute("COMMIT")

    after_items = con.execute("SELECT COUNT(*) FROM items").fetchone()[0]
    after_photos = con.execute("SELECT COUNT(*) FROM item_photos").fetchone()[0]
    after_rows = con.execute(
        "SELECT id, status, isDeleted, purchasePrice, tags FROM items ORDER BY id").fetchall()
    columns = {r[1] for r in con.execute("PRAGMA table_info(items)").fetchall()}
    index_names = {r[1] for r in con.execute("PRAGMA index_list(items)").fetchall()}
    fk_violations = con.execute("PRAGMA foreign_key_check").fetchall()
    orphan_photos = con.execute(
        "SELECT COUNT(*) FROM item_photos p "
        "WHERE NOT EXISTS (SELECT 1 FROM items i WHERE i.id = p.itemId)").fetchone()[0]
    photo_sort = con.execute(
        "SELECT itemId, sortOrder FROM item_photos ORDER BY itemId, sortOrder").fetchall()
    version = con.execute("PRAGMA user_version").fetchone()[0]
    con.close()

    checks = [
        ("抽取到的迁移语句 >= 8 条", len(stmts) >= 8, "%d 条" % len(stmts)),
        ("物品行数不变", after_items == before_items, "%d -> %d" % (before_items, after_items)),
        ("照片行数不变（CASCADE 回归点）", after_photos == before_photos,
         "%d -> %d" % (before_photos, after_photos)),
        ("物品字段内容不变", after_rows == before_rows, "%s" % (after_rows,)),
        ("照片归属与排序不变",
         photo_sort == [(1, 0), (1, 1), (2, 0)], "%s" % (photo_sort,)),
        ("items 列集合 = 17 列且无售出列", columns == EXPECTED_COLUMNS,
         "%d 列" % len(columns)),
        ("salePrice/saleDate 已移除",
         "salePrice" not in columns and "saleDate" not in columns, ""),
        ("4 个 items 索引已重建",
         {"index_items_categoryId", "index_items_name",
          "index_items_isDeleted", "index_items_status"} <= index_names,
         "%s" % sorted(index_names)),
        ("无外键违规", not fk_violations, "%s" % (fk_violations,)),
        ("无孤儿照片行", orphan_photos == 0, "orphan=%d" % orphan_photos),
        ("user_version = 7", version == 7, "version=%d" % version),
    ]

    failed = 0
    for label, ok, detail in checks:
        print("[%s] %s%s" % ("PASS" if ok else "FAIL", label,
                             (" (%s)" % detail) if detail and not ok else ""))
        if not ok:
            failed += 1
    print("\n%d/%d checks passed" % (len(checks) - failed, len(checks)))
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
