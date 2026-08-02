package com.nanji.lootarchive.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ItemStatusTest {

    @Test
    fun `fromCode maps valid codes`() {
        assertEquals(ItemStatus.ACTIVE, ItemStatus.fromCode("active"))
        assertEquals(ItemStatus.SOLD, ItemStatus.fromCode("sold"))
        assertEquals(ItemStatus.LOST, ItemStatus.fromCode("lost"))
    }

    @Test
    fun `fromCode falls back to active on unknown or null`() {
        assertEquals(ItemStatus.ACTIVE, ItemStatus.fromCode(null))
        assertEquals(ItemStatus.ACTIVE, ItemStatus.fromCode(""))
        assertEquals(ItemStatus.ACTIVE, ItemStatus.fromCode("SOLD")) // 大小写/脏数据
    }

    @Test
    fun `owned states are active idle repair`() {
        assertTrue(ItemStatus.ACTIVE.isOwned)
        assertTrue(ItemStatus.IDLE.isOwned)
        assertTrue(ItemStatus.REPAIR.isOwned)
        assertFalse(ItemStatus.SOLD.isOwned)
        assertFalse(ItemStatus.LOST.isOwned)
    }

    @Test
    fun `labels are chinese`() {
        assertEquals("在用", ItemStatus.ACTIVE.label)
        assertEquals("已出", ItemStatus.SOLD.label)
    }
}
