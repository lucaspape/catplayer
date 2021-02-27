package de.lucaspape.monstercat.core.music.util

import kotlinx.coroutines.*

/**
 * Run task in background looped until stopped
 */
abstract class BackgroundService<T>(private val delay:Long) {
    private val scope = CoroutineScope(Dispatchers.Main)

    private var lastActive: Long = 0

    val active: Boolean
        get() {
            return System.currentTimeMillis() - lastActive <= 1000
        }

    private var stopped = false

    abstract fun background(): Boolean
    abstract fun publishProgress(value: T)

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
    
    fun updateProgress(value: T) {
        if(!stopped){
            scope.launch {
                withContext(Dispatchers.Main) {
                    publishProgress(value)
                }
            }
        }
    }

    fun cancel() {
        stopped = true
    }
}