package de.lucaspape.monstercat.request.async

import android.content.Context
import android.os.AsyncTask
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.database.helper.ManualPlaylistDatabaseHelper
import de.lucaspape.monstercat.database.helper.PlaylistDatabaseHelper
import de.lucaspape.monstercat.util.newRequestQueue
import java.lang.ref.WeakReference

class DeletePlaylistAsync(
    private val contextReference: WeakReference<Context>,
    private val playlistId: String,
    private val deleteRemote: Boolean,
    private val deleteLocal: Boolean,
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
            var success = true

            if(deleteLocal){
                val playlistDatabaseHelper = PlaylistDatabaseHelper(context)
                playlistDatabaseHelper.removePlaylist(playlistId)

                val manualPlaylistDatabaseHelper = ManualPlaylistDatabaseHelper(context)
                manualPlaylistDatabaseHelper.removePlaylist(playlistId)
            }

            if(deleteRemote){
                val deletePlaylistVolleyQueue = newRequestQueue(context)

                val deletePlaylistUrl = context.getString(R.string.playlistUrl) + playlistId

                val syncObject = Object()

                deletePlaylistVolleyQueue.addRequestFinishedListener<Any?> {
                    synchronized(syncObject) {
                        syncObject.notify()
                    }
                }

                val deletePlaylistRequest = StringRequest(
                    Request.Method.DELETE, deletePlaylistUrl,
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

            return success
        }

        return false
    }

}