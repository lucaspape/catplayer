package de.lucaspape.monstercat.request.async

import android.content.Context
import android.os.AsyncTask
import com.android.volley.Request
import com.android.volley.Response
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.database.helper.SongDatabaseHelper
import de.lucaspape.monstercat.request.AuthorizedJsonObjectRequest
import de.lucaspape.monstercat.util.newRequestQueue
import org.json.JSONObject
import java.lang.ref.WeakReference

class AddToPlaylistAsync(
    private val contextReference: WeakReference<Context>,
    private val playlistId: String,
    private val songId: String,
    private val finishedCallback: (playlistId: String, songId: String) -> Unit,
    private val errorCallback: (playlistId: String, songId: String) -> Unit
) : AsyncTask<Void, Void, Boolean>() {

    override fun onPostExecute(result: Boolean) {
        if (result) {
            finishedCallback(playlistId, songId)
        } else {
            errorCallback(playlistId, songId)
        }
    }

    override fun doInBackground(vararg params: Void?): Boolean {
        contextReference.get()?.let { context ->
            SongDatabaseHelper(context).getSong(context, songId)?.let { song ->
                val addToPlaylistUrl =
                    context.getString(R.string.playlistUrl) + playlistId + "/record"

                val addToPlaylistQueue = newRequestQueue(context)

                val songJsonObject = JSONObject()
                songJsonObject.put("trackId", song.songId)
                songJsonObject.put("releaseId", song.albumId)

                var success = true
                val syncObject = Object()

                addToPlaylistQueue.addRequestFinishedListener<Any?> {
                    synchronized(syncObject) {
                        syncObject.notify()
                    }
                }

                val addToPlaylistRequest = AuthorizedJsonObjectRequest(
                    Request.Method.PATCH,
                    addToPlaylistUrl,
                    songJsonObject,
                    Response.Listener {

                    },
                    Response.ErrorListener {
                        success = false
                    })

                addToPlaylistQueue.add(addToPlaylistRequest)

                synchronized(syncObject) {
                    syncObject.wait()

                    return success
                }
            }
        }

        return false
    }

}