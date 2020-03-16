package de.lucaspape.monstercat.request.async

import android.content.Context
import android.os.AsyncTask
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.database.Song
import de.lucaspape.monstercat.database.helper.SongDatabaseHelper
import de.lucaspape.monstercat.util.displayInfo
import de.lucaspape.monstercat.util.sid
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
                deleteSongObject.put("songDeleteIndex", songDeleteIndex)
                deleteSongObject.put("releaseId", song.albumId)
                deleteSongObject.put("trackId", song.songId)

                val deleteObject = JSONObject()
                deleteObject.put("songDelete", deleteSongObject)
                deleteObject.put("sid", sid)
                deleteObject.put("playlistId", playlistId)

                val deleteSongVolleyQueue = Volley.newRequestQueue(context)

                var success = true
                val syncObject = Object()

                deleteSongVolleyQueue.addRequestFinishedListener<Any?> {
                    synchronized(syncObject) {
                        syncObject.notify()
                    }
                }

                val deleteSongRequest = JsonObjectRequest(
                    Request.Method.POST,
                    context.getString(R.string.removeFromPlaylistUrl),
                    deleteObject,
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