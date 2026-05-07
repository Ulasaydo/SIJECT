package com.signlanguage.translator.utils

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DebouncerTest {

    @Test
    fun submit_runsActionAfterDelay() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = TestScope(dispatcher)
        val debouncer = Debouncer(scope, delayMillis = 800L)
        var calls = 0

        debouncer.submit { calls += 1 }
        scope.advanceTimeBy(799)
        assertEquals(0, calls)
        scope.advanceTimeBy(2)
        assertEquals(1, calls)
    }

    @Test
    fun submit_rescheduleCancelsPrevious() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = TestScope(dispatcher)
        val debouncer = Debouncer(scope, delayMillis = 800L)
        var calls = 0

        repeat(5) {
            debouncer.submit { calls += 1 }
            scope.advanceTimeBy(100)
        }
        // total elapsed since first submit = 500ms, but every submit reset the timer
        scope.advanceUntilIdle()
        assertEquals("only the last submit should fire", 1, calls)
    }

    @Test
    fun cancel_preventsAction() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = TestScope(dispatcher)
        val debouncer = Debouncer(scope, delayMillis = 800L)
        var calls = 0

        debouncer.submit { calls += 1 }
        scope.advanceTimeBy(400)
        debouncer.cancel()
        scope.advanceUntilIdle()
        assertEquals(0, calls)
    }

    @Test
    fun isPending_reflectsState() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = TestScope(dispatcher)
        val debouncer = Debouncer(scope, delayMillis = 800L)

        assertFalse(debouncer.isPending())
        debouncer.submit { /* no-op */ }
        assertTrue(debouncer.isPending())
        scope.advanceUntilIdle()
        assertFalse(debouncer.isPending())
    }
}
