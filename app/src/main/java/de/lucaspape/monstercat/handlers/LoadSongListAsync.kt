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
import de.lucaspape.monstercat.database.CatalogSongsDatabaseHelper
import de.lucaspape.monstercat.database.Song
import de.lucaspape.monstercat.database.SongDatabaseHelper
import de.lucaspape.monstercat.download.addDownloadCoverArray
import de.lucaspape.monstercat.json.JSONParser
import org.json.JSONObject
import java.lang.ref.WeakReference

class LoadSongListAsync(
    private val viewReference: WeakReference<View>,
    private val contextReference: WeakReference<Context>,
    private val forceReload: Boolean
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

        val catalogSongsDatabaseHelper = CatalogSongsDatabaseHelper(contextReference.get()!!)
        var songIdList = catalogSongsDatabaseHelper.getAllSongs()

        if (!forceReload && songIdList.isNotEmpty()) {
            val dbSongs = ArrayList<HashMap<String, Any?>>()

            val songDatabaseHelper = SongDatabaseHelper(contextReference.get()!!)
            val songList = ArrayList<Song>()

            for (song in songIdList) {
                songList.add(songDatabaseHelper.getSong(song.songId))
            }

            for (song in songList) {
                val jsonParser = JSONParser()
                dbSongs.add(jsonParser.parseSongToHashMap(contextReference.get()!!, song))
            }

            //display list
            HomeHandler.currentListViewData = dbSongs
            return null
        } else {
            val requestQueue = Volley.newRequestQueue(contextReference.get()!!)

            val dbIds = ArrayList<Long>()

            //if all finished continue
            var finishedRequests = 0
            var totalRequestsCount = 0

            val sortedList = arrayOfNulls<Long>(HomeHandler.loadMax)

            val requests = ArrayList<StringRequest>()

            requestQueue.addRequestFinishedListener<Any> {
                finishedRequests++

                //check if all done
                if (finishedRequests >= totalRequestsCount) {
                    val dbSongs = ArrayList<HashMap<String, Any?>>()

                    for (i in sortedList) {
                        if (i != null) {
                            if (catalogSongsDatabaseHelper.getCatalogSong(i) == null) {
                                catalogSongsDatabaseHelper.insertSong(i)
                            }
                        }
                    }

                    songIdList = catalogSongsDatabaseHelper.getAllSongs()
                    val songDatabaseHelper = SongDatabaseHelper(contextReference.get()!!)
                    val songList = ArrayList<Song>()

                    for (song in songIdList) {
                        songList.add(songDatabaseHelper.getSong(song.songId))
                    }

                    for (song in songList) {
                        val jsonParser = JSONParser()
                        dbSongs.add(jsonParser.parseSongToHashMap(contextReference.get()!!, song))
                    }

                    //display list
                    HomeHandler.currentListViewData = dbSongs
                } else {
                    requestQueue.add(requests[finishedRequests])
                }
            }

            for (i in (0 until HomeHandler.loadMax / 50)) {
                val requestUrl =
                    contextReference.get()!!.getString(R.string.loadSongsUrl) + "?limit=50&skip=" + i * 50

                val listRequest = object : StringRequest(
                    Method.GET, requestUrl, Response.Listener { response ->
                        val json = JSONObject(response)
                        val jsonArray = json.getJSONArray("results")

                        //parse every single song into list
                        for (k in (0 until jsonArray.length())) {
                            val jsonParser = JSONParser()

                            val dbId = jsonParser.parseCatalogSongToDB(
                                jsonArray.getJSONObject(k),
                                contextReference.get()!!
                            )
                            dbIds.add(dbId)

                            sortedList[i * 50 + k] = dbId
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

                totalRequestsCount++
                requests.add(listRequest)
            }

            requestQueue.add(requests[finishedRequests])
        }

        return null
    }
}