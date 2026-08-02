package com.nanji.lootarchive.domain.model

/**
 * 物品状态枚举 — 统一"拥有/售出"判定与中文标签，消除散落字符串清单。
 * DB 层 items.status 仍存 code 字符串（保持 schema 兼容），在代码边界用 [fromCode] 转换。
 */
enum class ItemStatus(val code: String, val label: String) {
    ACTIVE("active", "在用"),
    IDLE("idle", "闲置"),
    SOLD("sold", "已出"),
    REPAIR("repair", "待修"),
    LOST("lost", "丢失");

    /** 实际持有（资产/成就统计口径：在用+闲置+待修） */
    val isOwned: Boolean get() = this == ACTIVE || this == IDLE || this == REPAIR

    companion object {
        /** 未知/空 code 回退为在用（保持旧数据兼容） */
        fun fromCode(code: String?): ItemStatus =
            entries.firstOrNull { it.code == code } ?: ACTIVE
    }
}
