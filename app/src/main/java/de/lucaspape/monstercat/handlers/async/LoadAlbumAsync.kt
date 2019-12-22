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
        viewReference.get()?.let {view ->
            val swipeRefreshLayout =
                view.findViewById<SwipeRefreshLayout>(R.id.pullToRefresh)
            swipeRefreshLayout.isRefreshing = true
        }
    }

    override fun onPostExecute(result: String?) {
        val albumId = itemValue["id"] as String

        val albumItemDatabaseHelper =
            AlbumItemDatabaseHelper(contextReference.get()!!, albumId)
        val albumItemList = albumItemDatabaseHelper.getAllData()

        val dbSongs = ArrayList<HashMap<String, Any?>>()

        val songDatabaseHelper = SongDatabaseHelper(contextReference.get()!!)

        for (albumItem in albumItemList) {
            contextReference.get()?.let {context ->
                dbSongs.add(
                    parseSongToHashMap(
                        context,
                        songDatabaseHelper.getSong(albumItem.songId)
                    )
                )
            }

        }

        HomeHandler.currentListViewData = dbSongs

        HomeHandler.albumView = false

        viewReference.get()?.let { view ->
            HomeHandler.updateListView(view)
            HomeHandler.redrawListView(view)
        }

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
        val mcID = itemValue["mcID"] as String

        contextReference.get()?.let {context ->
            val requestQueue = Volley.newRequestQueue(context)

            val albumItemDatabaseHelper =
                AlbumItemDatabaseHelper(context, albumId)
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
                    context.getString(R.string.loadAlbumSongsUrl) + "/" + mcID

                val listRequest = AuthorizedRequest(
                    Request.Method.GET, requestUrl,
                    getSid(), Response.Listener { response ->
                        val json = JSONObject(response)
                        val jsonArray = json.getJSONArray("tracks")

                        albumItemDatabaseHelper.reCreateTable()

                        //parse every single song into list
                        for (k in (0 until jsonArray.length())) {
                            parsAlbumSongToDB(
                                jsonArray.getJSONObject(k),
                                albumId,
                                context
                            )
                        }

                    }, Response.ErrorListener { }
                )

                requestQueue.add(listRequest)
                synchronized(syncObject) {
                    syncObject.wait()
                }


            }
        }

        return null
    }

}