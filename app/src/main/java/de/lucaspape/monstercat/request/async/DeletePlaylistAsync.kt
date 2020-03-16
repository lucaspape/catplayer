package de.lucaspape.monstercat.request.async

import android.content.Context
import android.os.AsyncTask
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.Volley
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.request.AuthorizedRequest
import de.lucaspape.monstercat.util.displayInfo
import de.lucaspape.monstercat.util.sid
import java.lang.ref.WeakReference

class DeletePlaylistAsync(
    private val contextReference: WeakReference<Context>,
    private val playlistId: String,
    private val finishedCallback: (playlistId: String) -> Unit,
    private val errorCallback: (playlistId: String) -> Unit
) : AsyncTask<Void, Void, Boolean>() {

    override fun onPostExecute(result: Boolean) {
        if (result) {
            finishedCallback(playlistId)
        } else {
            errorCallback(playlistId)
        }
    }

    override fun doInBackground(vararg params: Void?): Boolean {
        contextReference.get()?.let { context ->
            val deletePlaylistVolleyQueue = Volley.newRequestQueue(context)

            val deletePlaylistUrl = context.getString(R.string.playlistUrl) + playlistId

            var success = true
            val syncObject = Object()

            deletePlaylistVolleyQueue.addRequestFinishedListener<Any?> {
                synchronized(syncObject) {
                    syncObject.notify()
                }
            }

            val deletePlaylistRequest = AuthorizedRequest(
                Request.Method.DELETE, deletePlaylistUrl, sid,
                Response.Listener {
                },
                Response.ErrorListener {
                    success = false
                })

            deletePlaylistVolleyQueue.add(deletePlaylistRequest)

            synchronized(syncObject) {
                syncObject.wait()

                return success
            }
        }

        return false
    }

}