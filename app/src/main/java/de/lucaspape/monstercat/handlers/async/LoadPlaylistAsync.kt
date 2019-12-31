package de.lucaspape.monstercat.handlers.async

import android.content.Context
import android.os.AsyncTask
import com.android.volley.Response
import com.android.volley.Request
import com.android.volley.toolbox.Volley
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.util.getSid
import de.lucaspape.monstercat.database.helper.PlaylistDatabaseHelper
import de.lucaspape.monstercat.request.AuthorizedRequest
import de.lucaspape.monstercat.util.parsePlaylistToDB
import org.json.JSONObject
import java.lang.ref.WeakReference

/**
 * Load playlists into database
 */
class LoadPlaylistAsync(
    private val contextReference: WeakReference<Context>,
    private val forceReload: Boolean,
    private val requestFinished: () -> Unit
) : AsyncTask<Void, Void, String>() {

    override fun onPostExecute(result: String?) {
        requestFinished()
    }

    override fun doInBackground(vararg param: Void?): String? {
        val playlistDatabaseHelper =
            PlaylistDatabaseHelper(contextReference.get()!!)
        val playlists = playlistDatabaseHelper.getAllPlaylists()

        if (!forceReload && playlists.isNotEmpty()) {
            return null
        } else {
            contextReference.get()?.let { context ->
                val playlistRequestQueue = Volley.newRequestQueue(context)

                val playlistUrl = context.getString(R.string.playlistsUrl)

                val syncObject = Object()

                playlistRequestQueue.addRequestFinishedListener<Any> {
                    synchronized(syncObject) {
                        syncObject.notify()
                    }
                }

                val playlistRequest = AuthorizedRequest(
                    Request.Method.GET, playlistUrl, getSid(),
                    Response.Listener { response ->
                        val jsonObject = JSONObject(response)
                        val jsonArray = jsonObject.getJSONArray("results")

                        playlistDatabaseHelper.reCreateTable()

                        for (i in (0 until jsonArray.length())) {
                            parsePlaylistToDB(
                                context,
                                jsonArray.getJSONObject(i)
                            )
                        }

                    },
                    Response.ErrorListener { })

                playlistRequestQueue.add(playlistRequest)

                synchronized(syncObject) {
                    syncObject.wait()
                }
            }

            return null
        }
    }

}