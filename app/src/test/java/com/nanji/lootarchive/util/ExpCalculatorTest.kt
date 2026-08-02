package com.nanji.lootarchive.util

import org.junit.Assert.assertEquals
import org.junit.Test

class ExpCalculatorTest {

    @Test
    fun `level boundaries`() {
        assertEquals(1, ExpCalculator.getLevel(0))
        assertEquals(1, ExpCalculator.getLevel(49))
        assertEquals(2, ExpCalculator.getLevel(50))
        assertEquals(2, ExpCalculator.getLevel(149))
        assertEquals(3, ExpCalculator.getLevel(150))
        assertEquals(9, ExpCalculator.getLevel(19999))
        assertEquals(10, ExpCalculator.getLevel(20000))
        assertEquals(10, ExpCalculator.getLevel(30000))
    }

    @Test
    fun `level titles`() {
        assertEquals("入门", ExpCalculator.getLevelTitle(1))
        assertEquals("传奇", ExpCalculator.getLevelTitle(10))
        // 越界回退到入门
        assertEquals("入门", ExpCalculator.getLevelTitle(0))
        assertEquals("入门", ExpCalculator.getLevelTitle(99))
    }

    @Test
    fun `next level exp`() {
        assertEquals(50, ExpCalculator.getNextLevelExp(0))
        assertEquals(150, ExpCalculator.getNextLevelExp(50))
        // 顶级返回 MAX_VALUE
        assertEquals(Int.MAX_VALUE, ExpCalculator.getNextLevelExp(20000))
        assertEquals(Int.MAX_VALUE, ExpCalculator.getNextLevelExp(30000))
    }

    @Test
    fun `level progress`() {
        assertEquals(0f, ExpCalculator.getLevelProgress(0), 0.001f)
        assertEquals(0.5f, ExpCalculator.getLevelProgress(25), 0.001f)
        // 恰好升到下一级 → 进度归零
        assertEquals(0f, ExpCalculator.getLevelProgress(50), 0.001f)
        // 顶级 → 满进度
        assertEquals(1f, ExpCalculator.getLevelProgress(20000), 0.001f)
    }

    @Test
    fun `value exp reward`() {
        assertEquals(1, ExpCalculator.Rewards.valueExp(100.0))
        assertEquals(0, ExpCalculator.Rewards.valueExp(99.0))
        assertEquals(10, ExpCalculator.Rewards.valueExp(1000.0))
    }
}
