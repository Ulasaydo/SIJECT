package com.signlanguage.translator.ui.fragments

import com.signlanguage.translator.data.model.WordHistory
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WordHistoryDiffCallbackTest {
    private val callback = WordHistoryAdapter.DIFF_CALLBACK

    @Test
    fun areItemsTheSame_sameTimestampAndWord() {
        val a = WordHistory("hello", 0.9f, 1000L)
        val b = WordHistory("hello", 0.5f, 1000L)
        assertTrue(callback.areItemsTheSame(a, b))
    }

    @Test
    fun areItemsTheSame_differentTimestamp() {
        val a = WordHistory("hello", 0.9f, 1000L)
        val b = WordHistory("hello", 0.9f, 2000L)
        assertFalse(callback.areItemsTheSame(a, b))
    }

    @Test
    fun areItemsTheSame_differentWord() {
        val a = WordHistory("hello", 0.9f, 1000L)
        val b = WordHistory("drink", 0.9f, 1000L)
        assertFalse(callback.areItemsTheSame(a, b))
    }

    @Test
    fun areContentsTheSame_identical() {
        val a = WordHistory("hello", 0.9f, 1000L)
        val b = WordHistory("hello", 0.9f, 1000L)
        assertTrue(callback.areContentsTheSame(a, b))
    }

    @Test
    fun areContentsTheSame_differentConfidence() {
        val a = WordHistory("hello", 0.9f, 1000L)
        val b = WordHistory("hello", 0.7f, 1000L)
        assertFalse(callback.areContentsTheSame(a, b))
    }
}
