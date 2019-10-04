package de.lucaspape.monstercat.handlers

import android.content.Context
import android.os.AsyncTask
import android.view.View
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.android.volley.AuthFailureError
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.auth.loggedIn
import de.lucaspape.monstercat.auth.sid
import de.lucaspape.monstercat.database.PlaylistDatabaseHelper
import de.lucaspape.monstercat.json.JSONParser
import org.json.JSONObject
import java.lang.ref.WeakReference

class LoadPlaylistAsync(private val viewReference: WeakReference<View>, private val contextReference: WeakReference<Context>, private val forceReload: Boolean, private val showAfter:Boolean) : AsyncTask<Void, Void, String>(){
    override fun onPreExecute() {
        if(showAfter){
            val swipeRefreshLayout = viewReference.get()!!.findViewById<SwipeRefreshLayout>(R.id.playlistSwipeRefresh)
            swipeRefreshLayout.isRefreshing = true
        }
    }

    override fun onPostExecute(result: String?) {
        if(showAfter){
            PlaylistHandler.updateListView(viewReference.get()!!)
            PlaylistHandler.redrawListView(viewReference.get()!!)

            val swipeRefreshLayout = viewReference.get()!!.findViewById<SwipeRefreshLayout>(R.id.playlistSwipeRefresh)
            swipeRefreshLayout.isRefreshing = false
        }
    }

    override fun doInBackground(vararg params: Void?): String? {
        val playlistDatabaseHelper = PlaylistDatabaseHelper(contextReference.get()!!)
        var playlists = playlistDatabaseHelper.getAllPlaylists()

        if (!forceReload && playlists.isNotEmpty()) {
            val jsonParser = JSONParser()

            val playlistHashMaps = ArrayList<HashMap<String, Any?>>()

            for (playlist in playlists) {
                playlistHashMaps.add(jsonParser.parsePlaylistToHashMap(playlist))
            }

            if(showAfter) {
                PlaylistHandler.currentListViewData = playlistHashMaps
            }

            return null
        } else {
            val playlistRequestQueue = Volley.newRequestQueue(contextReference.get()!!)

            val playlistUrl = contextReference.get()!!.getString(R.string.playlistsUrl)

            val playlistRequest = object : StringRequest(
                Method.GET, playlistUrl,
                Response.Listener { response ->
                    val jsonObject = JSONObject(response)
                    val jsonArray = jsonObject.getJSONArray("results")

                    val list = ArrayList<Long>()

                    val jsonParser = JSONParser()

                    for (i in (0 until jsonArray.length())) {
                        list.add(
                            jsonParser.parsePlaylistToDB(
                                contextReference.get()!!,
                                jsonArray.getJSONObject(i)
                            )
                        )
                    }

                    playlists = playlistDatabaseHelper.getAllPlaylists()

                    val playlistHashMaps = ArrayList<HashMap<String, Any?>>()

                    for (playlist in playlists) {
                        playlistHashMaps.add(jsonParser.parsePlaylistToHashMap(playlist))
                    }

                    if(showAfter){
                        PlaylistHandler.currentListViewData = playlistHashMaps
                    }

                },
                Response.ErrorListener { }) {
                @Throws(AuthFailureError::class)
                override fun getHeaders(): Map<String, String> {
                    val params = HashMap<String, String>()
                    if (loggedIn) {
                        params["Cookie"] = "connect.sid=$sid"
                    }

                    return params
                }
            }

            playlistRequestQueue.add(playlistRequest)
        }

        return null
    }

}