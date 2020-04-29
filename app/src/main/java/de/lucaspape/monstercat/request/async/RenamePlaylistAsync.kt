package de.lucaspape.monstercat.request.async

import android.content.Context
import android.os.AsyncTask
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.request.newRenamePlaylistRequest
import de.lucaspape.monstercat.util.getAuthorizedRequestQueue
import java.lang.ref.WeakReference

class RenamePlaylistAsync(
    private val contextReference: WeakReference<Context>,
    private val playlistId: String,
    private val playlistName: String,
    private val finishedCallback: (playlistName: String, playlistId: String) -> Unit,
    private val errorCallback: (playlistName: String, playlistId: String) -> Unit
) : AsyncTask<Void, Void, Boolean>() {

    override fun onPostExecute(result: Boolean) {
        if (result) {
            finishedCallback(playlistName, playlistId)
        } else {
            errorCallback(playlistName, playlistId)
        }
    }

    override fun doInBackground(vararg params: Void?): Boolean {
        contextReference.get()?.let { context ->
            val newPlaylistVolleyQueue =
                getAuthorizedRequestQueue(context, context.getString(R.string.connectApiHost))

            var success = true
            val syncObject = Object()

            newPlaylistVolleyQueue.add(
                newRenamePlaylistRequest(
                    context,
                    playlistId,
                    playlistName,
                    {
                        synchronized(syncObject) {
                            syncObject.notify()
                        }
                    },
                    {
                        success = false
                        synchronized(syncObject) {
                            syncObject.notify()
                        }
                    })
            )

            synchronized(syncObject) {
                syncObject.wait()

                return success
            }
        }

        return false
    }

}