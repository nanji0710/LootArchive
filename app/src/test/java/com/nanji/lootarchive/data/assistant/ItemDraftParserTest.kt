package com.nanji.lootarchive.data.assistant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ItemDraftParserTest {

    @Test
    fun `parses full sentence with price date shelf life and location`() {
        val draft = ItemDraftParser.parse(
            "新增物品：牛奶，价格20元，购买时间2026年11月16日，保质期30天，放在冰箱"
        )
        assertNotNull(draft)
        draft!!
        assertEquals("牛奶", draft.name)
        assertEquals(20.0, draft.price!!, 0.001)
        assertEquals(30, draft.shelfLifeDays)
        assertEquals("冰箱", draft.location)
        val cal = Calendar.getInstance().apply { timeInMillis = draft.purchaseDate!! }
        assertEquals(2026, cal.get(Calendar.YEAR))
        assertEquals(Calendar.NOVEMBER, cal.get(Calendar.MONTH))
        assertEquals(16, cal.get(Calendar.DAY_OF_MONTH))
        assertNotNull(draft.expiryDate)
    }

    @Test
    fun `parses earphones with year shelf life and custom category`() {
        val draft = ItemDraftParser.parse(
            "添加物品：降噪耳机，价格399元，购买日期2026-11-01，保质期1年，分类：数码，放在书房"
        )
        assertNotNull(draft)
        draft!!
        assertEquals("降噪耳机", draft.name)
        assertEquals(399.0, draft.price!!, 0.001)
        assertEquals(365, draft.shelfLifeDays)
        assertEquals("数码", draft.categoryHint)
        assertEquals("书房", draft.location)
    }

    @Test
    fun `parses colloquial buy sentence with yuan price`() {
        val draft = ItemDraftParser.parse("买了牛奶20元")
        assertNotNull(draft)
        assertEquals("牛奶", draft!!.name)
        assertEquals(20.0, draft.price!!, 0.001)
    }

    @Test
    fun `non add intent returns null`() {
        assertNull(ItemDraftParser.parse("我的牛奶在哪？"))
        assertNull(ItemDraftParser.parse("我的东西一共值多少钱？"))
    }

    @Test
    fun `explicit expiry date is used`() {
        val draft = ItemDraftParser.parse("新增物品：面包，价格8元，购买日期2026-11-10，有效期至2026年11月20日")
        assertNotNull(draft)
        assertTrue(draft!!.expiryDate != null)
        val cal = Calendar.getInstance().apply { timeInMillis = draft.expiryDate!! }
        assertEquals(Calendar.NOVEMBER, cal.get(Calendar.MONTH))
        assertEquals(20, cal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `name correction reads name after the word name`() {
        val draft = ItemDraftParser.parseEdits("名称联想学习试用平板呀，你为什么改成了时间？")
        assertNotNull(draft)
        assertEquals("联想学习试用平板", draft!!.name)
    }

    @Test
    fun `date correction is not treated as a name change`() {
        val draft = ItemDraftParser.parseEdits("是今年11月，不是25年11月")
        assertNull(draft?.name?.takeIf { it.isNotBlank() })
    }
}
