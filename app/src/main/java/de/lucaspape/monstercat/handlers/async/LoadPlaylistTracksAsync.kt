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

class LoadPlaylistTracksAsync(
    private val viewReference: WeakReference<View>,
    private val contextReference: WeakReference<Context>,
    private val forceReload: Boolean,
    private val playlistId: String
) : AsyncTask<Void, Void, String>() {
    override fun onPreExecute() {
        val swipeRefreshLayout =
            viewReference.get()!!.findViewById<SwipeRefreshLayout>(R.id.playlistSwipeRefresh)
        swipeRefreshLayout.isRefreshing = true
    }

    override fun onPostExecute(result: String?) {
        val playlistItemDatabaseHelper =
            PlaylistItemDatabaseHelper(
                contextReference.get()!!,
                playlistId
            )

        val sortedList = ArrayList<HashMap<String, Any?>>()

        val playlistItems = playlistItemDatabaseHelper.getAllData()

        val songDatabaseHelper =
            SongDatabaseHelper(contextReference.get()!!)

        for (playlistItem in playlistItems) {
            val hashMap = parseSongToHashMap(
                contextReference.get()!!,
                songDatabaseHelper.getSong(playlistItem.songId)
            )
            sortedList.add(hashMap)
        }

        //display list
        PlaylistHandler.currentListViewData = sortedList
        PlaylistHandler.listViewDataIsPlaylistView = false

        PlaylistHandler.updateListView(viewReference.get()!!)
        PlaylistHandler.redrawListView(viewReference.get()!!)

        //download cover art
        addDownloadCoverArray(PlaylistHandler.currentListViewData)

        val swipeRefreshLayout =
            viewReference.get()!!.findViewById<SwipeRefreshLayout>(R.id.playlistSwipeRefresh)
        swipeRefreshLayout.isRefreshing = false
    }

    override fun doInBackground(vararg param: Void?): String? {
        val playlistItemDatabaseHelper =
            PlaylistItemDatabaseHelper(
                contextReference.get()!!,
                playlistId
            )
        val playlistItems = playlistItemDatabaseHelper.getAllData()

        if (!forceReload && playlistItems.isNotEmpty()) {
            return null
        } else {

            val syncObject = Object()

            val trackCountRequestQueue = Volley.newRequestQueue(contextReference.get()!!)

            var trackCount = 0

            val trackCountRequest = AuthorizedRequest(Request.Method.GET,
                contextReference.get()!!.getString(R.string.playlistsUrl),
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

                val playlistTrackRequestQueue = Volley.newRequestQueue(contextReference.get()!!)

                playlistTrackRequestQueue.addRequestFinishedListener<Any> {
                    finishedRequests++

                    if (finishedRequests >= totalRequestsCount) {

                        playlistItemDatabaseHelper.reCreateTable()

                        for (playlistObject in tempList) {
                            if (playlistObject != null) {
                                parsePlaylistTrackToDB(
                                    playlistId,
                                    playlistObject,
                                    contextReference.get()!!
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
                        contextReference.get()!!.getString(R.string.loadSongsUrl) + "?playlistId=" + playlistId + "&skip=" + (i * 50).toString() + "&limit=50"

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

            return null
        }
    }

}