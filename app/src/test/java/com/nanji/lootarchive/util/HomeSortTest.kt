package com.nanji.lootarchive.util

import com.nanji.lootarchive.data.local.entity.ItemEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeSortTest {

    private fun item(
        id: Long,
        status: String,
        warrantyExpiryDate: Long? = null,
        updatedAt: Long = id,
    ) = ItemEntity(
        id = id,
        name = "item$id",
        status = status,
        warrantyExpiryDate = warrantyExpiryDate,
        updatedAt = updatedAt,
    )

    @Test
    fun `groups by status in home order active-repair-idle-sold-lost`() {
        val items = listOf(
            item(1, "sold"),
            item(2, "active"),
            item(3, "lost"),
            item(4, "repair"),
            item(5, "idle"),
        )
        val ids = items.sortedForHome().map { it.id }
        assertEquals(listOf(2L, 4L, 5L, 1L, 3L), ids)
    }

    @Test
    fun `active items sorted by remaining warranty descending`() {
        val now = 1_000_000_000_000L
        val items = listOf(
            item(1, "active", warrantyExpiryDate = now + 30_000L), // 剩余最长 → 最前
            item(2, "active", warrantyExpiryDate = now + 10_000L),
            item(3, "active", warrantyExpiryDate = now + 5_000L),  // 剩余最短 → 最后
        )
        val ids = items.sortedForHome().map { it.id }
        assertEquals(listOf(1L, 2L, 3L), ids)
    }

    @Test
    fun `active item without warranty goes last in its group`() {
        val now = 1_000_000_000_000L
        val items = listOf(
            item(1, "active", warrantyExpiryDate = null),          // 无保修 → 组内最后
            item(2, "active", warrantyExpiryDate = now + 20_000L),
        )
        val ids = items.sortedForHome().map { it.id }
        assertEquals(listOf(2L, 1L), ids)
    }

    @Test
    fun `expired warranty sorts before no-warranty but after later expiries`() {
        val now = 1_000_000_000_000L
        val items = listOf(
            item(1, "active", warrantyExpiryDate = now - 1_000L), // 已过期
            item(2, "active", warrantyExpiryDate = null),
            item(3, "active", warrantyExpiryDate = now + 10_000L),
        )
        val ids = items.sortedForHome().map { it.id }
        assertEquals(listOf(3L, 1L, 2L), ids)
    }

    @Test
    fun `sold items ranked above lost within pushed to end`() {
        val items = listOf(
            item(1, "lost"),
            item(2, "sold"),
            item(3, "active"),
        )
        val ids = items.sortedForHome().map { it.id }
        assertEquals(listOf(3L, 2L, 1L), ids)
    }

    @Test
    fun `tie broken by updatedAt descending`() {
        val items = listOf(
            item(1, "active", warrantyExpiryDate = null, updatedAt = 100),
            item(2, "active", warrantyExpiryDate = null, updatedAt = 300),
        )
        val ids = items.sortedForHome().map { it.id }
        assertEquals(listOf(2L, 1L), ids)
    }
}