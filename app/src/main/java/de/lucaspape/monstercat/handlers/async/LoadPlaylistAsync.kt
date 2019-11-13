package de.lucaspape.monstercat.handlers.async

import android.content.Context
import android.os.AsyncTask
import android.view.View
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.android.volley.Response
import com.android.volley.Request
import com.android.volley.toolbox.Volley
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.util.getSid
import de.lucaspape.monstercat.database.helper.PlaylistDatabaseHelper
import de.lucaspape.monstercat.handlers.PlaylistHandler
import de.lucaspape.monstercat.request.AuthorizedRequest
import de.lucaspape.monstercat.util.parsePlaylistToDB
import de.lucaspape.monstercat.util.parsePlaylistToHashMap
import org.json.JSONObject
import java.lang.ref.WeakReference

class LoadPlaylistAsync(
    private val viewReference: WeakReference<View>,
    private val contextReference: WeakReference<Context>,
    private val forceReload: Boolean,
    private val showAfter: Boolean
) : AsyncTask<Void, Void, String>() {
    override fun onPreExecute() {
        if (showAfter) {
            val swipeRefreshLayout =
                viewReference.get()!!.findViewById<SwipeRefreshLayout>(R.id.playlistSwipeRefresh)
            swipeRefreshLayout.isRefreshing = true
        }
    }

    override fun onPostExecute(result: String?) {
        val playlistDatabaseHelper =
            PlaylistDatabaseHelper(contextReference.get()!!)
        val playlists = playlistDatabaseHelper.getAllPlaylists()

        val playlistHashMaps = ArrayList<HashMap<String, Any?>>()

        for (playlist in playlists) {
            playlistHashMaps.add(parsePlaylistToHashMap(playlist))
        }

        if (showAfter) {
            PlaylistHandler.currentListViewData = playlistHashMaps
            PlaylistHandler.listViewDataIsPlaylistView = true

            PlaylistHandler.updateListView(viewReference.get()!!)
            PlaylistHandler.redrawListView(viewReference.get()!!)

            val swipeRefreshLayout =
                viewReference.get()!!.findViewById<SwipeRefreshLayout>(R.id.playlistSwipeRefresh)
            swipeRefreshLayout.isRefreshing = false
        }
    }

    override fun doInBackground(vararg param: Void?): String? {
        val playlistDatabaseHelper =
            PlaylistDatabaseHelper(contextReference.get()!!)
        val playlists = playlistDatabaseHelper.getAllPlaylists()

        if (!forceReload && playlists.isNotEmpty()) {
            return null
        } else {
            val playlistRequestQueue = Volley.newRequestQueue(contextReference.get()!!)

            val playlistUrl = contextReference.get()!!.getString(R.string.playlistsUrl)

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

                    for (i in (0 until jsonArray.length())) {
                        parsePlaylistToDB(
                            contextReference.get()!!,
                            jsonArray.getJSONObject(i)
                        )
                    }

                },
                Response.ErrorListener { })

            playlistRequestQueue.add(playlistRequest)

            synchronized(syncObject) {
                syncObject.wait()
            }

            return null
        }
    }

}