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
import de.lucaspape.monstercat.database.PlaylistSongsDatabaseHelper
import de.lucaspape.monstercat.database.SongDatabaseHelper
import de.lucaspape.monstercat.download.addDownloadCoverArray
import de.lucaspape.monstercat.json.JSONParser
import org.json.JSONObject
import java.lang.ref.WeakReference

class LoadPlaylistTracksAsync(
    private val viewReference: WeakReference<View>,
    private val contextReference: WeakReference<Context>,
    private val forceReload: Boolean,
    private val playlistId: String,
    private val trackCount: Int
) : AsyncTask<Void, Void, String>() {
    override fun onPreExecute() {
        val swipeRefreshLayout =
            viewReference.get()!!.findViewById<SwipeRefreshLayout>(R.id.playlistSwipeRefresh)
        swipeRefreshLayout.isRefreshing = true
    }

    override fun onPostExecute(result: String?) {
        val playlistSongsDatabaseHelper =
            PlaylistSongsDatabaseHelper(contextReference.get()!!, playlistId)

        val sortedList = ArrayList<HashMap<String, Any?>>()

        val playlistSongs = playlistSongsDatabaseHelper.getAllData()

        val songDatabaseHelper = SongDatabaseHelper(contextReference.get()!!)

        val jsonParser = JSONParser()

        for (playlistSong in playlistSongs) {
            val song = songDatabaseHelper.getSong(playlistSong.songId)

            val hashMap = jsonParser.parseSongToHashMap(contextReference.get()!!, song)
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
        val playlistSongsDatabaseHelper =
            PlaylistSongsDatabaseHelper(contextReference.get()!!, playlistId)
        val playlistSongs = playlistSongsDatabaseHelper.getAllData()

        if (!forceReload && playlistSongs.isNotEmpty()) {
            return null
        } else {
            var finishedRequests = 0
            var totalRequestsCount = 0

            val requests = ArrayList<StringRequest>()

            val playlistTrackRequestQueue = Volley.newRequestQueue(contextReference.get()!!)

            val tempList = arrayOfNulls<Long>(trackCount)

            val syncObject = Object()

            playlistTrackRequestQueue.addRequestFinishedListener<Any> {
                finishedRequests++

                if (finishedRequests >= totalRequestsCount) {
                    synchronized(syncObject){
                        syncObject.notify()
                    }
                } else {
                    playlistTrackRequestQueue.add(requests[finishedRequests])
                }
            }

            for (i in (0..(trackCount / 50))) {
                val playlistTrackUrl =
                    contextReference.get()!!.getString(R.string.loadSongsUrl) + "?playlistId=" + playlistId + "&skip=" + (i * 50).toString() + "&limit=50"

                val playlistTrackRequest = object : StringRequest(
                    Method.GET, playlistTrackUrl,
                    Response.Listener { response ->
                        val jsonObject = JSONObject(response)
                        val jsonArray = jsonObject.getJSONArray("results")

                        for (k in (0 until jsonArray.length())) {
                            val playlistObject = jsonArray.getJSONObject(k)

                            val jsonParser = JSONParser()
                            val id = jsonParser.parsePlaylistTrackToDB(
                                playlistId,
                                playlistObject,
                                contextReference.get()!!
                            )
                            if (id != null) {
                                tempList[i * 50 + k] = id
                            }
                        }
                    },
                    Response.ErrorListener { }) {
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
                requests.add(playlistTrackRequest)
            }
            playlistTrackRequestQueue.add(requests[finishedRequests])

            synchronized(syncObject){
                syncObject.wait()
                return null
            }
        }
    }

}