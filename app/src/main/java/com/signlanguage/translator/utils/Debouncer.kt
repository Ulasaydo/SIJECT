package com.signlanguage.translator.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Cancels any pending job and schedules a new one to run after [delayMillis].
 * Calling [submit] again before the delay elapses cancels and reschedules.
 * Calling [cancel] aborts any pending job without running the action.
 */
class Debouncer(
    private val scope: CoroutineScope,
    private val delayMillis: Long
) {
    private var job: Job? = null

    fun submit(action: suspend () -> Unit) {
        job?.cancel()
        job = scope.launch {
            delay(delayMillis)
            action()
        }
    }

    fun cancel() {
        job?.cancel()
        job = null
    }

    fun isPending(): Boolean = job?.isActive == true
}
