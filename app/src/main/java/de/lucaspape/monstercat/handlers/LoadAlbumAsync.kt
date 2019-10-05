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
import de.lucaspape.monstercat.database.SongDatabaseHelper
import de.lucaspape.monstercat.download.addDownloadCoverArray
import de.lucaspape.monstercat.json.JSONParser
import org.json.JSONObject
import java.lang.ref.WeakReference

class LoadAlbumAsync(
    private val viewReference: WeakReference<View>,
    private val contextReference: WeakReference<Context>,
    private val forceReload: Boolean,
    private val itemValue: HashMap<String, Any?>
) : AsyncTask<Void, Void, String>() {
    override fun onPreExecute() {
        val swipeRefreshLayout =
            viewReference.get()!!.findViewById<SwipeRefreshLayout>(R.id.pullToRefresh)
        swipeRefreshLayout.isRefreshing = true
    }

    override fun onPostExecute(result: String?) {
        HomeHandler.updateListView(viewReference.get()!!)
        HomeHandler.redrawListView(viewReference.get()!!)

        //download cover art
        addDownloadCoverArray(HomeHandler.currentListViewData)

        val swipeRefreshLayout =
            viewReference.get()!!.findViewById<SwipeRefreshLayout>(R.id.pullToRefresh)
        swipeRefreshLayout.isRefreshing = false
    }

    override fun doInBackground(vararg param: Void?): String? {
        val albumId = itemValue["id"] as String

        val requestQueue = Volley.newRequestQueue(contextReference.get()!!)

        //used to sort list
        val tempList = ArrayList<Long>()

        val songDatabaseHelper = SongDatabaseHelper(contextReference.get()!!)
        var songList = songDatabaseHelper.getAlbumSongs(albumId)

        if (!forceReload && songList.isNotEmpty()) {
            // currentListViewData = albumCache as ArrayList<HashMap<String, Any?>>
            val dbSongs = ArrayList<HashMap<String, Any?>>()

            for (song in songList) {
                val jsonParser = JSONParser()
                dbSongs.add(jsonParser.parseSongToHashMap(contextReference.get()!!, song))
            }

            HomeHandler.currentListViewData = dbSongs

            HomeHandler.albumView = false

            return null
        } else {
            val syncObject = Object()

            requestQueue.addRequestFinishedListener<Any> {
                val dbSongs = ArrayList<HashMap<String, Any?>>()
                songList = songDatabaseHelper.getAlbumSongs(albumId)

                for (song in songList) {
                    val jsonParser = JSONParser()
                    dbSongs.add(jsonParser.parseSongToHashMap(contextReference.get()!!, song))
                }

                //display list
                HomeHandler.currentListViewData = dbSongs
                HomeHandler.albumView = false

                synchronized(syncObject){
                    syncObject.notify()
                }
            }

            val requestUrl =
                contextReference.get()!!.getString(R.string.loadSongsUrl) + "?albumId=" + albumId

            val listRequest = object : StringRequest(
                Method.GET, requestUrl, Response.Listener { response ->
                    val json = JSONObject(response)
                    val jsonArray = json.getJSONArray("results")

                    //parse every single song into list
                    for (k in (0 until jsonArray.length())) {
                        val jsonParser = JSONParser()
                        val songId =
                            jsonParser.parseCatalogSongToDB(
                                jsonArray.getJSONObject(k),
                                contextReference.get()!!
                            )

                        tempList.add(songId)
                    }

                }, Response.ErrorListener { }
            ) {
                //add authentication
                @Throws(AuthFailureError::class)
                override fun getHeaders(): Map<String, String> {
                    val params = HashMap<String, String>()
                    if (loggedIn) {
                        params["Cookie"] = "connect.sid=$sid"
                    }
                    return params
                }
            }

            requestQueue.add(listRequest)

            synchronized(syncObject){
                syncObject.wait()
                return null
            }
        }
    }

}