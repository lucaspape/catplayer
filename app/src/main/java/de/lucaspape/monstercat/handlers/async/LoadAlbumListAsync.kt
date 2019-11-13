package de.lucaspape.monstercat.handlers.async

import android.content.Context
import android.os.AsyncTask
import android.view.View
import android.widget.ListView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.Volley
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.util.getSid
import de.lucaspape.monstercat.database.helper.AlbumDatabaseHelper
import de.lucaspape.monstercat.download.addDownloadCoverArray
import de.lucaspape.monstercat.handlers.HomeHandler
import de.lucaspape.monstercat.request.AuthorizedRequest
import de.lucaspape.monstercat.util.Settings
import de.lucaspape.monstercat.util.parseAlbumToDB
import de.lucaspape.monstercat.util.parseAlbumToHashMap
import org.json.JSONObject
import java.lang.ref.WeakReference

class LoadAlbumListAsync(
    private val viewReference: WeakReference<View>,
    private val contextReference: WeakReference<Context>,
    private val forceReload: Boolean,
    private val loadMax: Int
) : AsyncTask<Void, Void, String>() {
    override fun onPreExecute() {
        val swipeRefreshLayout =
            viewReference.get()!!.findViewById<SwipeRefreshLayout>(R.id.pullToRefresh)
        swipeRefreshLayout.isRefreshing = true
    }

    override fun onPostExecute(result: String?) {
        val albumDatabaseHelper =
            AlbumDatabaseHelper(contextReference.get()!!)
        val albumList = albumDatabaseHelper.getAllAlbums()

        val sortedList = ArrayList<HashMap<String, Any?>>()

        for (album in albumList) {
            sortedList.add(parseAlbumToHashMap(contextReference.get()!!, album))
        }

        HomeHandler.currentListViewData = sortedList

        HomeHandler.updateListView(viewReference.get()!!)
        HomeHandler.redrawListView(viewReference.get()!!)

        //download cover art
        addDownloadCoverArray(HomeHandler.currentListViewData)

        val listView = viewReference.get()!!.findViewById<ListView>(R.id.musiclistview)
        val settings = Settings(viewReference.get()!!.context)
        val lastScroll = settings.getSetting("currentListAlbumViewLastScrollIndex")
        val top = settings.getSetting("currentListAlbumViewTop")

        if (top != null && lastScroll != null) {
            listView.setSelectionFromTop(lastScroll.toInt(), top.toInt())
        }

        settings.saveSetting("currentListAlbumViewLastScrollIndex", 0.toString())
        settings.saveSetting("currentListAlbumViewTop", 0.toString())

        val swipeRefreshLayout =
            viewReference.get()!!.findViewById<SwipeRefreshLayout>(R.id.pullToRefresh)
        swipeRefreshLayout.isRefreshing = false

        HomeHandler.albumContentsDisplayed = false
    }

    override fun doInBackground(vararg param: Void?): String? {
        val requestQueue = Volley.newRequestQueue(contextReference.get()!!)

        val tempList = arrayOfNulls<JSONObject>(loadMax)

        val albumDatabaseHelper =
            AlbumDatabaseHelper(contextReference.get()!!)
        val albumList = albumDatabaseHelper.getAllAlbums()

        if (!forceReload && albumList.isNotEmpty()) {
            return null
        } else {
            val syncObject = Object()

            requestQueue.addRequestFinishedListener<Any> {

                synchronized(syncObject) {
                    syncObject.notify()
                }
            }

            for (i in (0 until loadMax / 50)) {
                val requestUrl =
                    contextReference.get()!!.getString(R.string.loadAlbumsUrl) + "?limit=50&skip=" + i * 50
                val albumsRequest = AuthorizedRequest(
                    Request.Method.GET, requestUrl, getSid(),
                    Response.Listener { response ->
                        val json = JSONObject(response)
                        val jsonArray = json.getJSONArray("results")

                        for (k in (0 until jsonArray.length())) {
                            val jsonObject = jsonArray.getJSONObject(k)

                            tempList[i * 50 + k] = jsonObject
                        }
                    },
                    Response.ErrorListener { }
                )

                requestQueue.add(albumsRequest)

                synchronized(syncObject) {
                    syncObject.wait()
                }
            }

            tempList.reverse()

            albumDatabaseHelper.reCreateTable()

            for (jsonObject in tempList) {
                if (jsonObject != null) {
                    parseAlbumToDB(jsonObject, contextReference.get()!!)
                }
            }

            return null
        }


    }

}