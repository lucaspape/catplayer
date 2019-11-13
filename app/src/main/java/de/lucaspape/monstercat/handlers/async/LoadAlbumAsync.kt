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
import de.lucaspape.monstercat.database.helper.AlbumItemDatabaseHelper
import de.lucaspape.monstercat.database.helper.SongDatabaseHelper
import de.lucaspape.monstercat.download.addDownloadCoverArray
import de.lucaspape.monstercat.handlers.HomeHandler
import de.lucaspape.monstercat.request.AuthorizedRequest
import de.lucaspape.monstercat.util.parsAlbumSongToDB
import de.lucaspape.monstercat.util.parseSongToHashMap
import org.json.JSONObject
import java.lang.ref.WeakReference

class LoadAlbumAsync(
    private val viewReference: WeakReference<View>,
    private val contextReference: WeakReference<Context>,
    private val forceReload: Boolean,
    private val itemValue: HashMap<*, *>
) : AsyncTask<Void, Void, String>() {
    override fun onPreExecute() {
        val swipeRefreshLayout =
            viewReference.get()!!.findViewById<SwipeRefreshLayout>(R.id.pullToRefresh)
        swipeRefreshLayout.isRefreshing = true
    }

    override fun onPostExecute(result: String?) {
        val albumId = itemValue["id"] as String

        val albumItemDatabaseHelper =
            AlbumItemDatabaseHelper(contextReference.get()!!, albumId)
        val albumItemList = albumItemDatabaseHelper.getAllData()

        val dbSongs = ArrayList<HashMap<String, Any?>>()

        val songDatabaseHelper = SongDatabaseHelper(contextReference.get()!!)

        for (albumItem in albumItemList) {
            dbSongs.add(
                parseSongToHashMap(
                    contextReference.get()!!,
                    songDatabaseHelper.getSong(albumItem.songId)
                )
            )
        }

        HomeHandler.currentListViewData = dbSongs

        HomeHandler.albumView = false

        HomeHandler.updateListView(viewReference.get()!!)
        HomeHandler.redrawListView(viewReference.get()!!)

        //download cover art
        addDownloadCoverArray(HomeHandler.currentListViewData)

        val swipeRefreshLayout =
            viewReference.get()!!.findViewById<SwipeRefreshLayout>(R.id.pullToRefresh)
        swipeRefreshLayout.isRefreshing = false

        HomeHandler.albumContentsDisplayed = true
        HomeHandler.currentAlbumId = itemValue["id"] as String
    }

    override fun doInBackground(vararg param: Void?): String? {
        val albumId = itemValue["id"] as String

        val requestQueue = Volley.newRequestQueue(contextReference.get()!!)

        val albumItemDatabaseHelper =
            AlbumItemDatabaseHelper(contextReference.get()!!, albumId)
        val albumItems = albumItemDatabaseHelper.getAllData()

        if (!forceReload && albumItems.isNotEmpty()) {
            return null
        } else {
            val syncObject = Object()

            requestQueue.addRequestFinishedListener<Any> {
                synchronized(syncObject) {
                    syncObject.notify()
                }
            }

            val requestUrl =
                contextReference.get()!!.getString(R.string.loadSongsUrl) + "?albumId=" + albumId

            val listRequest = AuthorizedRequest(
                Request.Method.GET, requestUrl,
                getSid(), Response.Listener { response ->
                    val json = JSONObject(response)
                    val jsonArray = json.getJSONArray("results")

                    albumItemDatabaseHelper.reCreateTable()

                    //parse every single song into list
                    for (k in (0 until jsonArray.length())) {
                        parsAlbumSongToDB(
                            jsonArray.getJSONObject(k),
                            albumId,
                            contextReference.get()!!
                        )
                    }

                }, Response.ErrorListener { }
            )

            requestQueue.add(listRequest)
            synchronized(syncObject) {
                syncObject.wait()
            }

            return null
        }
    }

}