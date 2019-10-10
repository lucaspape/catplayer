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
import de.lucaspape.monstercat.database.Album
import de.lucaspape.monstercat.database.AlbumDatabaseHelper
import de.lucaspape.monstercat.download.addDownloadCoverArray
import de.lucaspape.monstercat.json.JSONParser
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
        HomeHandler.updateListView(viewReference.get()!!)
        HomeHandler.redrawListView(viewReference.get()!!)

        //download cover art
        addDownloadCoverArray(HomeHandler.currentListViewData)

        val swipeRefreshLayout =
            viewReference.get()!!.findViewById<SwipeRefreshLayout>(R.id.pullToRefresh)
        swipeRefreshLayout.isRefreshing = false
    }

    override fun doInBackground(vararg param: Void?): String? {
        val requestQueue = Volley.newRequestQueue(contextReference.get()!!)

        val tempList = arrayOfNulls<JSONObject>(HomeHandler.loadMax)

        val albumDatabaseHelper = AlbumDatabaseHelper(contextReference.get()!!)
        var albumList = albumDatabaseHelper.getAllAlbums()

        if (!forceReload && albumList.isNotEmpty()) {
            val sortedList = ArrayList<HashMap<String, Any?>>()

            for (album in albumList) {
                val jsonParser = JSONParser()
                sortedList.add(jsonParser.parseAlbumToHashMap(contextReference.get()!!, album))
            }

            HomeHandler.currentListViewData = sortedList

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

                    val albums = ArrayList<Album>()

                    tempList.reverse()

                    val jsonParser = JSONParser()
                    for (jsonObject in tempList) {
                        if(jsonObject != null){
                            val id = jsonParser.parseAlbumToDB(jsonObject, contextReference.get()!!)

                            albums.add(albumDatabaseHelper.getAlbum(id))
                        }
                    }

                    for (album in albums) {
                        jsonParser.parseAlbumToHashMap(
                            contextReference.get()!!,
                            album
                        )
                    }

                    albumList = albumDatabaseHelper.getAllAlbums()

                    val sortedList = ArrayList<HashMap<String, Any?>>()

                    for (album in albumList) {
                        sortedList.add(
                            jsonParser.parseAlbumToHashMap(
                                contextReference.get()!!,
                                album
                            )
                        )
                    }

                    HomeHandler.currentListViewData = sortedList

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
                val albumsRequest = object : StringRequest(
                    Method.GET, requestUrl,
                    Response.Listener { response ->
                        val json = JSONObject(response)
                        val jsonArray = json.getJSONArray("results")

                        for (k in (0 until jsonArray.length())) {
                            val jsonObject = jsonArray.getJSONObject(k)

                            tempList[i * 50 + k] = jsonObject
                                //jsonParser.parseAlbumToDB(jsonObject, contextReference.get()!!)
                        }
                    },
                    Response.ErrorListener { }
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