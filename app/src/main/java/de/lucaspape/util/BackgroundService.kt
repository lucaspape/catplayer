package de.lucaspape.util

import kotlinx.coroutines.*

/**
 * Run task in background looped until stopped
 */
abstract class BackgroundService(private val delay:Long) {
    private val scope = CoroutineScope(Dispatchers.Main)

    private var lastActive: Long = 0

    val active: Boolean
        get() {
            return System.currentTimeMillis() - lastActive <= 1000
        }

    private var stopped = false

    abstract fun background(): Boolean
    abstract fun publishProgress(values: Array<String>?)

    fun execute() {
        scope.launch {
            withContext(Dispatchers.Default) {
                while (!stopped) {
                    lastActive = System.currentTimeMillis()

                    if(!background()){
                        stopped = true
                    }

                    delay(delay)
                }
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

    fun cancel() {
        stopped = true
    }
}