package de.lucaspape.monstercat.handlers.async

import android.os.AsyncTask

class BackgroundAsync(private val background: () -> Unit, private val finished: () -> Unit) : AsyncTask<Void, Void, String>(){
    override fun onPostExecute(result: String?) {
        finished()
    }

    override fun doInBackground(vararg params: Void?): String? {
        background()

        return null
    }

}