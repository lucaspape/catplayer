package de.lucaspape.monstercat.request.async

import android.content.Context
import android.os.AsyncTask
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.request.newLoadPlaylistRequest
import de.lucaspape.monstercat.util.newAuthorizedRequestQueue
import de.lucaspape.monstercat.util.parsePlaylistToDB
import java.lang.ref.WeakReference

/**
 * Load playlists into database
 */
class LoadManualPlaylist(
    private val contextReference: WeakReference<Context>,
    private val playlistId: String,
    private val finishedCallback: () -> Unit,
    private val errorCallback: () -> Unit
) : AsyncTask<Void, Void, Boolean>() {

    override fun onPostExecute(result: Boolean) {
        if (result) {
            finishedCallback()
        } else {
            errorCallback()
        }
    }

    override fun doInBackground(vararg param: Void?): Boolean {
        contextReference.get()?.let { context ->
            var success = true
            val syncObject = Object()

            val getManualPlaylistsRequestQueue = newAuthorizedRequestQueue(context, context.getString(R.string.connectApiHost))

            getManualPlaylistsRequestQueue.addRequestFinishedListener<Any?> {
                synchronized(syncObject) {
                    syncObject.notify()
                }
            }

            getManualPlaylistsRequestQueue.add(newLoadPlaylistRequest(context, playlistId, {
                parsePlaylistToDB(
                    context,
                    it,
                    false
                )
            }, {
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