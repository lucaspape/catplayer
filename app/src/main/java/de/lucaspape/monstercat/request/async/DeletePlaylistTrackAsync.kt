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

class DeletePlaylistTrackAsync(
    private val contextReference: WeakReference<Context>,
    private val songId: String,
    private val playlistId: String,
    private val songDeleteIndex: Int,
    private val finishedCallback: (songId: String, playlistId: String, songDeleteIndex: Int) -> Unit,
    private val errorCallback: (songId: String, playlistId: String, songDeleteIndex: Int) -> Unit
) : AsyncTask<Void, Void, Boolean>() {

    override fun onPostExecute(result: Boolean) {
        if (result) {
            finishedCallback(songId, playlistId, songDeleteIndex)
        } else {
            errorCallback(songId, playlistId, songDeleteIndex)
        }
    }

    override fun doInBackground(vararg params: Void?): Boolean {
        contextReference.get()?.let { context ->
            SongDatabaseHelper(context).getSong(context, songId)?.let { song ->
                val deleteSongObject = JSONObject()
                deleteSongObject.put("trackId", song.songId)
                deleteSongObject.put("releaseId", song.albumId)
                deleteSongObject.put("sort", songDeleteIndex)

                val deleteSongVolleyQueue = newRequestQueue(context)

                var success = true
                val syncObject = Object()

                deleteSongVolleyQueue.addRequestFinishedListener<Any?> {
                    synchronized(syncObject) {
                        syncObject.notify()
                    }
                }

                val deleteTrackFromPlaylistUrl =
                    context.getString(R.string.playlistUrl) + playlistId + "/record"

                val deleteSongRequest = AuthorizedJsonObjectRequest(
                    Request.Method.DELETE,
                    deleteTrackFromPlaylistUrl,
                    deleteSongObject,
                    Response.Listener {
                    },
                    Response.ErrorListener {
                        success = false
                    })

                deleteSongVolleyQueue.add(deleteSongRequest)

                synchronized(syncObject) {
                    syncObject.wait()

                    return success
                }
            }
        }

        return false
    }
}