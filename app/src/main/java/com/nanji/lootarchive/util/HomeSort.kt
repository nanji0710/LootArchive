package com.nanji.lootarchive.util

import com.nanji.lootarchive.data.local.entity.ItemEntity
import com.nanji.lootarchive.domain.model.ItemStatus

/**
 * 首页卡片排序。
 *
 * 需求口径（2026-09-30）：
 *   1. 状态分组顺序：在用 → 待修 → 闲置 → 已出 → 丢失
 *      （"已出"整体推到拥有物品之后，只在"丢失"前面）。
 *   2. 组内按剩余保修期从高到低：到期日越晚、剩余越多，越靠前；
 *      无保修期的物品排在本组末尾（按剩余保修期视为 0 / 缺失）。
 *   3. 其余按更新时间降序，保证排序稳定（不会同组内乱跳）。
 */
fun Iterable<ItemEntity>.sortedForHome(): List<ItemEntity> =
    sortedWith(
        compareBy<ItemEntity> { ItemStatus.fromCode(it.status).homeSortRank }
            .thenByDescending { it.warrantyExpiryDate ?: Long.MIN_VALUE }
            .thenByDescending { it.updatedAt }
    )