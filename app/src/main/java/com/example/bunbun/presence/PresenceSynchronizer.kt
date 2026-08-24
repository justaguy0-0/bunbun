package com.example.bunbun.presence

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class PresenceSynchronizer(
    private val scope: CoroutineScope,
    private val touch: suspend () -> Unit,
    private val intervalMillis: Long = 60_000L,
) {
    private val wakeUp = Channel<Unit>(Channel.CONFLATED)
    private var loopJob: Job? = null

    @Synchronized
    fun start() {
        if (loopJob?.isActive == true) return
        loopJob = scope.launch {
            while (isActive) {
                runCatching { touch() }
                withTimeoutOrNull(intervalMillis) { wakeUp.receive() }
            }
        }
    }

    @Synchronized
    fun stop() {
        loopJob?.cancel()
        loopJob = null
        while (wakeUp.tryReceive().isSuccess) Unit
    }

    /** Restarts and immediately touches after login/registration in the active UI. */
    fun onAuthenticated() {
        val wasRunning = isRunning
        start()
        if (wasRunning) wakeUp.trySend(Unit)
    }

    val isRunning: Boolean
        @Synchronized get() = loopJob?.isActive == true
}
