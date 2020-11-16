package de.lucaspape.util

import kotlinx.coroutines.*

/**
 * Run functions in background
 */

class BackgroundAsync<T>(
    private val preBackground: () -> Boolean,
    private val background: () -> T?,
    private val finished: (result: T?) -> Unit
) {

    private val scope = CoroutineScope(Dispatchers.Main)

    constructor(background: () -> T?, finished: (result: T?) -> Unit) : this(
        { true },
        background,
        finished
    )

    constructor(background: () -> T?) : this(background, {})

    fun execute() {
        if (preBackground()) {
            scope.launch {
                withContext(Dispatchers.Default) {
                    val result = background()

                    withContext(Dispatchers.Main) {
                        finished(result)
                    }
                }
            }
        }
    }
}