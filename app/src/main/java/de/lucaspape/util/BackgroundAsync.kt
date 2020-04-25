package de.lucaspape.util

import android.os.AsyncTask

/**
 * General purpose background task using higher order functions
 */

class BackgroundAsync(private val background: () -> Unit, private val finished: () -> Unit) :
    AsyncTask<Void, Void, String>() {
    override fun onPostExecute(result: String?) {
        finished()
    }

    override fun doInBackground(vararg params: Void?): String? {
        background()

        return null
    }

}