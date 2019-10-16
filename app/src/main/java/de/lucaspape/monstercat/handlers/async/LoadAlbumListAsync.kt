package de.lucaspape.monstercat.handlers.async

import android.content.Context
import android.os.AsyncTask
import android.view.View
import android.widget.ListView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.auth.getSid
import de.lucaspape.monstercat.database.AlbumDatabaseHelper
import de.lucaspape.monstercat.download.addDownloadCoverArray
import de.lucaspape.monstercat.handlers.HomeHandler
import de.lucaspape.monstercat.json.JSONParser
import de.lucaspape.monstercat.request.MonstercatRequest
import de.lucaspape.monstercat.settings.Settings
import org.json.JSONObject
import java.lang.ref.WeakReference

class LoadAlbumListAsync(
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
        val albumDatabaseHelper = AlbumDatabaseHelper(contextReference.get()!!)
        val albumList = albumDatabaseHelper.getAllAlbums()

        val sortedList = ArrayList<HashMap<String, Any?>>()

        for (album in albumList) {
            val jsonParser = JSONParser()
            sortedList.add(jsonParser.parseAlbumToHashMap(contextReference.get()!!, album))
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

        if(top != null && lastScroll != null){
            listView.setSelectionFromTop(lastScroll.toInt(), top.toInt())
        }

        settings.saveSetting("currentListAlbumViewLastScrollIndex", 0.toString())
        settings.saveSetting("currentListAlbumViewTop", 0.toString())

        val swipeRefreshLayout =
            viewReference.get()!!.findViewById<SwipeRefreshLayout>(R.id.pullToRefresh)
        swipeRefreshLayout.isRefreshing = false
    }

    override fun doInBackground(vararg param: Void?): String? {
        //TODO download correct album list

        val requestQueue = Volley.newRequestQueue(contextReference.get()!!)

        val tempList = arrayOfNulls<JSONObject>(HomeHandler.loadMax)

        val albumDatabaseHelper = AlbumDatabaseHelper(contextReference.get()!!)
        val albumList = albumDatabaseHelper.getAllAlbums()

        if (!forceReload && albumList.isNotEmpty()) {
            return null
        } else {
            //if all finished continue
            var finishedRequests = 0
            var totalRequestsCount = 0

            val requests = ArrayList<StringRequest>()

            val syncObject = Object()

            requestQueue.addRequestFinishedListener<Any?> {
                finishedRequests++

                //check if all done
                if (finishedRequests >= totalRequestsCount) {

                    tempList.reverse()

                    val jsonParser = JSONParser()
                    for (jsonObject in tempList) {
                        if (jsonObject != null) {
                            jsonParser.parseAlbumToDB(jsonObject, contextReference.get()!!)
                        }
                    }

                    synchronized(syncObject) {
                        syncObject.notify()
                    }

                } else {
                    requestQueue.add(requests[finishedRequests])
                }

            }

            for (i in (0 until HomeHandler.loadMax / 50)) {
                val requestUrl =
                    contextReference.get()!!.getString(R.string.loadAlbumsUrl) + "?limit=50&skip=" + i * 50
                val albumsRequest = MonstercatRequest(
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

                totalRequestsCount++

                requests.add(albumsRequest)
            }

            requestQueue.add(requests[finishedRequests])

            synchronized(syncObject) {
                syncObject.wait()
                return null
            }
        }


    }

}