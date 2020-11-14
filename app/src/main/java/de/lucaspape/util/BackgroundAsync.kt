package de.lucaspape.util

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.*

/**
 * General purpose background task using higher order functions
 */

class BackgroundAsync<T>(
    private val preBackground: () -> Boolean,
    private val background: () -> T?,
    private val finished: (result: T?) -> Unit
) : ViewModel() {

    private val viewModelJob = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + viewModelJob)

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