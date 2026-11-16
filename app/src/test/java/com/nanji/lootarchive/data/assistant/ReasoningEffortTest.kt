package com.nanji.lootarchive.data.assistant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReasoningEffortTest {

    @Test
    fun `deepseek models expose three levels`() {
        assertEquals(listOf("low", "medium", "high"), ReasoningEffort.inferForModel("deepseek-v4.1-flash-1017"))
    }

    @Test
    fun `gpt5 has minimal level`() {
        assertEquals(listOf("minimal", "low", "medium", "high"), ReasoningEffort.inferForModel("gpt-5.2"))
    }

    @Test
    fun `claude has max level`() {
        assertEquals(listOf("low", "medium", "high", "max"), ReasoningEffort.inferForModel("claude-opus-4-6"))
    }

    @Test
    fun `non chat models have no reasoning levels`() {
        assertTrue(ReasoningEffort.inferForModel("text-embedding-3-large").isEmpty())
        assertTrue(ReasoningEffort.inferForModel("qwen-image-edit").isEmpty())
    }
}
