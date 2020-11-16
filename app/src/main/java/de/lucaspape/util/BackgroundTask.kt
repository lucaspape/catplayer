package de.lucaspape.util

import kotlinx.coroutines.*

/**
 * Run single task in background
 */
abstract class BackgroundTask<T> {
    private val scope = CoroutineScope(Dispatchers.Main)

    abstract suspend fun background()
    abstract suspend fun publishProgress(value: T)

    fun execute() {
        scope.launch {
            withContext(Dispatchers.Default) {
                background()
            }
        }
    }

    fun updateProgress(value: T) {
        scope.launch {
            withContext(Dispatchers.Main) {
                publishProgress(value)
            }
        }
    }
}