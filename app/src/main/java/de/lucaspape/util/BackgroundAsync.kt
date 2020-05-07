package de.lucaspape.util

import android.os.AsyncTask

/**
 * General purpose background task using higher order functions
 */

class BackgroundAsync<T>(
    private val preBackground: () -> Boolean,
    private val background: () -> T,
    private val finished: (result: T) -> Unit
) :
    AsyncTask<Void, Void, T>() {

    constructor(background: () -> T, finished: (result: T) -> Unit) : this(
        { true },
        background,
        finished
    )

    override fun onPreExecute() {
        if (!preBackground()) {
            cancel(true)
        }
    }

    override fun onPostExecute(result: T) {
        finished(result)
    }

    override fun doInBackground(vararg params: Void?): T? {
        background()

        return null
    }

}