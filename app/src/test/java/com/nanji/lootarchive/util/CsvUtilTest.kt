package com.nanji.lootarchive.util

import org.junit.Assert.assertEquals
import org.junit.Test

class CsvUtilTest {

    @Test
    fun `plain field unchanged`() {
        assertEquals("MacBook", csvEscape("MacBook"))
    }

    @Test
    fun `field with comma is quoted`() {
        assertEquals("\"Mac,Book\"", csvEscape("Mac,Book"))
    }

    @Test
    fun `field with embedded quote doubles it`() {
        assertEquals("\"say \"\"hi\"\"\"", csvEscape("say \"hi\""))
    }

    @Test
    fun `newline field is quoted`() {
        assertEquals("\"line1\nline2\"", csvEscape("line1\nline2"))
    }

    @Test
    fun `formula prefix gets neutralized`() {
        assertEquals("'=cmd()", csvEscape("=cmd()"))
        assertEquals("'@import", csvEscape("@import"))
        assertEquals("'-5", csvEscape("-5"))
    }

    @Test
    fun `formula prefix with comma is quoted after neutralize`() {
        assertEquals("\"'=cmd(a,b)\"", csvEscape("=cmd(a,b)"))
    }
}
