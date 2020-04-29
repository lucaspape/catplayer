package de.lucaspape.monstercat.request.async

import android.content.Context
import android.os.AsyncTask
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.request.newChangePlaylistPublicStateRequest
import de.lucaspape.monstercat.util.newAuthorizedRequestQueue
import java.lang.ref.WeakReference

class ChangePlaylistPublicStateAsync(
    private val contextReference: WeakReference<Context>,
    private val playlistId: String,
    private val public: Boolean,
    private val finishedCallback: (playlistId: String, public: Boolean) -> Unit,
    private val errorCallback: (playlistId: String, public: Boolean) -> Unit
) : AsyncTask<Void, Void, Boolean>() {

    override fun onPostExecute(result: Boolean) {
        if (result) {
            finishedCallback(playlistId, public)
        } else {
            errorCallback(playlistId, public)
        }
    }

    override fun doInBackground(vararg params: Void?): Boolean {
        contextReference.get()?.let { context ->
            val newPlaylistVolleyQueue =
                newAuthorizedRequestQueue(context, context.getString(R.string.connectApiHost))

            var success = true
            val syncObject = Object()

            newPlaylistVolleyQueue.addRequestFinishedListener<Any?> {
                synchronized(syncObject) {
                    syncObject.notify()
                }
            }

            newPlaylistVolleyQueue.add(
                newChangePlaylistPublicStateRequest(
                    context,
                    playlistId,
                    public,
                    {

                    },
                    {
                        success = false
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