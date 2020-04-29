package de.lucaspape.monstercat.request.async

import android.content.Context
import android.os.AsyncTask
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.request.newCreatePlaylistRequest
import de.lucaspape.monstercat.util.newAuthorizedRequestQueue
import java.lang.ref.WeakReference

class CreatePlaylistAsync(
    private val contextReference: WeakReference<Context>,
    private val playlistName: String,
    private val finishedCallback: (playlistName: String) -> Unit,
    private val errorCallback: (playlistName: String) -> Unit
) : AsyncTask<Void, Void, Boolean>() {

    override fun onPostExecute(result: Boolean) {
        if (result) {
            finishedCallback(playlistName)
        } else {
            errorCallback(playlistName)
        }
    }

    override fun doInBackground(vararg params: Void?): Boolean {
        contextReference.get()?.let { context ->
            val newPlaylistVolleyQueue = newAuthorizedRequestQueue(context, context.getString(R.string.connectApiHost))

            var success = true
            val syncObject = Object()

            newPlaylistVolleyQueue.addRequestFinishedListener<Any?> {
                synchronized(syncObject) {
                    syncObject.notify()
                }
            }

            newPlaylistVolleyQueue.add(newCreatePlaylistRequest(context, playlistName, {}, {
                success = false
            }))

            synchronized(syncObject) {
                syncObject.wait()

                return success
            }
        }

        return false
    }

}