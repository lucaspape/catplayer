package de.lucaspape.monstercat.handlers.async

import android.content.Context
import android.os.AsyncTask
import android.view.View
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.android.volley.Response
import com.android.volley.Request
import com.android.volley.toolbox.Volley
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.auth.getSid
import de.lucaspape.monstercat.database.CatalogSongsDatabaseHelper
import de.lucaspape.monstercat.database.Song
import de.lucaspape.monstercat.database.SongDatabaseHelper
import de.lucaspape.monstercat.download.addDownloadCoverArray
import de.lucaspape.monstercat.handlers.HomeHandler
import de.lucaspape.monstercat.json.JSONParser
import de.lucaspape.monstercat.request.MonstercatRequest
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
        val catalogSongsDatabaseHelper = CatalogSongsDatabaseHelper(contextReference.get()!!)
        val songIdList = catalogSongsDatabaseHelper.getAllSongs()

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
        val songIdList = catalogSongsDatabaseHelper.getAllSongs()

        if (!forceReload && songIdList.isNotEmpty()) {
            return null
        } else {
            val requestQueue = Volley.newRequestQueue(contextReference.get()!!)

            val sortedList = arrayOfNulls<JSONObject>(HomeHandler.loadMax)

            val syncObject = Object()

            requestQueue.addRequestFinishedListener<Any> {
                synchronized(syncObject) {
                    syncObject.notify()
                }
            }

            for (i in (0 until HomeHandler.loadMax / 50)) {
                val requestUrl =
                    contextReference.get()!!.getString(R.string.loadSongsUrl) + "?limit=50&skip=" + i * 50

                val listRequest = MonstercatRequest(
                    Request.Method.GET, requestUrl, getSid(),
                    Response.Listener { response ->
                        val json = JSONObject(response)
                        val jsonArray = json.getJSONArray("results")

                        //parse every single song into list
                        for (k in (0 until jsonArray.length())) {
                            sortedList[i * 50 + k] = jsonArray.getJSONObject(k)
                        }

                    }, Response.ErrorListener { }
                )

                requestQueue.add(listRequest)

                synchronized(syncObject) {
                    syncObject.wait()
                }
            }

            sortedList.reverse()

            val jsonParser = JSONParser()

            for (jsonObject in sortedList) {
                if (jsonObject != null) {

                    jsonParser.parseCatalogSongToDB(
                        jsonObject,
                        contextReference.get()!!
                    )
                }
            }

            return null
        }
    }
}