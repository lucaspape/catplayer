package de.lucaspape.util

import kotlinx.coroutines.*

/**
 * Run single task in background
 */
abstract class BackgroundTask {
    private val scope = CoroutineScope(Dispatchers.Main)

    abstract fun background()
    abstract fun publishProgress(values: Array<String>?)

    fun execute() {
        scope.launch {
            withContext(Dispatchers.Default) {
                background()
            }
        }
    }

    fun updateProgress(values: Array<String>?) {
        scope.launch {
            withContext(Dispatchers.Main) {
                publishProgress(values)
            }
        }
    }
}