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
import de.lucaspape.monstercat.database.helper.PlaylistItemDatabaseHelper
import de.lucaspape.monstercat.database.helper.SongDatabaseHelper
import de.lucaspape.monstercat.download.addDownloadCoverArray
import de.lucaspape.monstercat.handlers.PlaylistHandler
import de.lucaspape.monstercat.request.AuthorizedRequest
import de.lucaspape.monstercat.util.parsePlaylistTrackToDB
import de.lucaspape.monstercat.util.parseSongToHashMap
import org.json.JSONObject
import java.lang.ref.WeakReference
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class LoadPlaylistTracksAsync(
    private val viewReference: WeakReference<View>,
    private val contextReference: WeakReference<Context>,
    private val forceReload: Boolean,
    private val playlistId: String
) : AsyncTask<Void, Void, String>() {
    override fun onPreExecute() {
        viewReference.get()?.let { view ->
            val swipeRefreshLayout =
                view.findViewById<SwipeRefreshLayout>(R.id.playlistSwipeRefresh)
            swipeRefreshLayout.isRefreshing = true
        }
    }

    override fun onPostExecute(result: String?) {
        contextReference.get()?.let { context ->
            val playlistItemDatabaseHelper =
                PlaylistItemDatabaseHelper(
                    context,
                    playlistId
                )

            val sortedList = ArrayList<HashMap<String, Any?>>()

            val playlistItems = playlistItemDatabaseHelper.getAllData()

            val songDatabaseHelper =
                SongDatabaseHelper(context)

            for (playlistItem in playlistItems) {
                val hashMap = parseSongToHashMap(
                    context,
                    songDatabaseHelper.getSong(context, playlistItem.songId)
                )
                sortedList.add(hashMap)
            }

            //display list
            PlaylistHandler.currentListViewData = ArrayList()

            for (i in (sortedList.size - 1) downTo 0) {
                PlaylistHandler.currentListViewData.add(sortedList[i])
            }

            PlaylistHandler.listViewDataIsPlaylistView = false

            viewReference.get()?.let { view ->
                PlaylistHandler.updateListView(view)
                PlaylistHandler.redrawListView(view)

                //download cover art
                addDownloadCoverArray(PlaylistHandler.currentListViewData)

                val swipeRefreshLayout =
                    view.findViewById<SwipeRefreshLayout>(R.id.playlistSwipeRefresh)
                swipeRefreshLayout.isRefreshing = false
            }


        }
    }

    override fun doInBackground(vararg param: Void?): String? {
        contextReference.get()?.let { context ->
            val playlistItemDatabaseHelper =
                PlaylistItemDatabaseHelper(
                    context,
                    playlistId
                )
            val playlistItems = playlistItemDatabaseHelper.getAllData()

            if (!forceReload && playlistItems.isNotEmpty()) {
                return null
            } else {

                val syncObject = Object()

                val trackCountRequestQueue = Volley.newRequestQueue(context)

                var trackCount = 0

                val trackCountRequest = AuthorizedRequest(Request.Method.GET,
                    context.getString(R.string.playlistsUrl),
                    getSid(),
                    Response.Listener { response ->
                        val jsonObject = JSONObject(response)
                        val jsonArray = jsonObject.getJSONArray("results")

                        for (i in (0 until jsonArray.length())) {
                            if (jsonArray.getJSONObject(i).getString("_id") == playlistId) {
                                val tracks = jsonArray.getJSONObject(i).getJSONArray("tracks")
                                trackCount = tracks.length()
                                break
                            }
                        }
                    },
                    Response.ErrorListener { })


                trackCountRequestQueue.addRequestFinishedListener<Any> {
                    val tempList = arrayOfNulls<JSONObject>(trackCount)

                    var finishedRequests = 0
                    var totalRequestsCount = 0

                    val requests = ArrayList<AuthorizedRequest>()

                    val playlistTrackRequestQueue = Volley.newRequestQueue(context)

                    playlistTrackRequestQueue.addRequestFinishedListener<Any> {
                        finishedRequests++

                        if (finishedRequests >= totalRequestsCount) {

                            playlistItemDatabaseHelper.reCreateTable()

                            for (playlistObject in tempList) {
                                if (playlistObject != null) {
                                    parsePlaylistTrackToDB(
                                        playlistId,
                                        playlistObject,
                                        context
                                    )
                                }

                            }

                            synchronized(syncObject) {
                                syncObject.notify()
                            }
                        } else {
                            playlistTrackRequestQueue.add(requests[finishedRequests])
                        }
                    }

                    for (i in (0..(trackCount / 50))) {
                        val playlistTrackUrl =
                            context.getString(R.string.playlistTrackUrl) + playlistId + "/catalog?skip=" + (i * 50).toString() + "&limit=50"

                        val playlistTrackRequest = AuthorizedRequest(
                            Request.Method.GET, playlistTrackUrl, getSid(),
                            Response.Listener { response ->
                                val jsonObject = JSONObject(response)
                                val jsonArray = jsonObject.getJSONArray("results")

                                for (k in (0 until jsonArray.length())) {
                                    tempList[i * 50 + k] = jsonArray.getJSONObject(k)
                                }
                            },
                            Response.ErrorListener { })

                        totalRequestsCount++
                        requests.add(playlistTrackRequest)
                    }

                    playlistTrackRequestQueue.add(requests[finishedRequests])
                }

                trackCountRequestQueue.add(trackCountRequest)

                synchronized(syncObject) {
                    syncObject.wait()
                }

            }
        }

        return null
    }

}