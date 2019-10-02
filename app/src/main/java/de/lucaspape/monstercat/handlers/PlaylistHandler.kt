package de.lucaspape.monstercat.handlers

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.AdapterView
import android.widget.ListView
import android.widget.SimpleAdapter
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.android.volley.AuthFailureError
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import de.lucaspape.monstercat.MainActivity
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.auth.loggedIn
import de.lucaspape.monstercat.auth.sid
import de.lucaspape.monstercat.database.PlaylistDataDatabaseHelper
import de.lucaspape.monstercat.database.PlaylistDatabaseHelper
import de.lucaspape.monstercat.database.SongDatabaseHelper
import de.lucaspape.monstercat.json.JSONParser
import org.json.JSONObject

class PlaylistHandler {
    var currentListViewData = ArrayList<HashMap<String, Any?>>()
    var listViewDataIsPlaylistView = true

    private var simpleAdapter: SimpleAdapter? = null

    private var currentPlaylistId:String? = null
    private var currentPlaylistTrackCount:Int? = null

    fun setupListView(view: View) {
        updateListView(view)
        redrawListView(view)

        //setup auto reload
        Thread(Runnable {
            while (true) {
                Handler(Looper.getMainLooper()).post { redrawListView(view) }
                Thread.sleep(1000)
            }

        }).start()
    }

    fun registerListeners(view: View) {
        //click on list
        val playlistList = view.findViewById<ListView>(R.id.playlistView)

        //refresh
        val swipeRefreshLayout = view.findViewById<SwipeRefreshLayout>(R.id.playlistSwipeRefresh)
        swipeRefreshLayout.setOnRefreshListener {
            if (listViewDataIsPlaylistView) {
                loadPlaylist(view, true)
            } else {
                loadPlaylistTracks(view, true, currentPlaylistId!!, currentPlaylistTrackCount!!)
            }
        }

        playlistList.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val itemValue = playlistList.getItemAtPosition(position) as HashMap<String, Any?>

            if(listViewDataIsPlaylistView){
                currentPlaylistId = itemValue["playlistId"] as String
                currentPlaylistTrackCount = itemValue["trackCount"] as Int
                loadPlaylistTracks(view, false, currentPlaylistId!!, currentPlaylistTrackCount!!)
            }else{
                val songId = itemValue["id"] as String
                playSongFromId(view.context, songId, true)
            }
        }
    }

    fun updateListView(view: View) {
        val playlistList = view.findViewById<ListView>(R.id.playlistView)

        var from = arrayOf("shownTitle", "secondaryImage")
        var to = arrayOf(R.id.title, R.id.cover)

        if (listViewDataIsPlaylistView) {
            from = arrayOf("playlistName", "coverUrl")
            to = arrayOf(R.id.title, R.id.cover)
        }

        simpleAdapter = SimpleAdapter(
            view.context,
            currentListViewData,
            R.layout.list_single,
            from,
            to.toIntArray()
        )

        playlistList.adapter = simpleAdapter

    }

    private fun redrawListView(view: View) {
        val playlistList = view.findViewById<ListView>(R.id.playlistView)
        simpleAdapter!!.notifyDataSetChanged()
        playlistList.invalidateViews()
        playlistList.refreshDrawableState()
    }

    fun loadPlaylist(view: View, forceReload: Boolean) {
        val swipeRefreshLayout = view.findViewById<SwipeRefreshLayout>(R.id.playlistSwipeRefresh)
        swipeRefreshLayout.isRefreshing = true

        if (!forceReload) {
            swipeRefreshLayout.isRefreshing = false
        } else {
            val playlistRequestQueue = Volley.newRequestQueue(view.context)

            val playlistUrl = view.context.getString(R.string.playlistsUrl)

            val playlistRequest = object : StringRequest(Method.GET, playlistUrl,
                Response.Listener { response ->
                    val jsonObject = JSONObject(response)
                    val jsonArray = jsonObject.getJSONArray("results")

                    val list = ArrayList<Long>()

                    val jsonParser = JSONParser()

                    for (i in (0 until jsonArray.length())) {
                        list.add(jsonParser.parsePlaylistToDB(view.context, jsonArray.getJSONObject(i)))
                    }

                    val playlistDatabaseHelper = PlaylistDatabaseHelper(view.context)
                    val playlists = playlistDatabaseHelper.getAllPlaylists()

                    val playlistHashMaps = ArrayList<HashMap<String, Any?>>()

                    for(playlist in playlists){
                        playlistHashMaps.add(jsonParser.parsePlaylistToHashMap(playlist))
                    }

                    currentListViewData = playlistHashMaps
                    updateListView(view)
                    redrawListView(view)
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

            playlistRequestQueue.addRequestFinishedListener<Any> {
                swipeRefreshLayout.isRefreshing = false
                listViewDataIsPlaylistView = true
            }

            playlistRequestQueue.add(playlistRequest)
        }
    }

    fun loadPlaylistTracks(view: View, forceReload: Boolean, playlistId:String, trackCount:Int) {
        val swipeRefreshLayout = view.findViewById<SwipeRefreshLayout>(R.id.playlistSwipeRefresh)
        swipeRefreshLayout.isRefreshing = true

        if(!forceReload){
            val sortedList = ArrayList<HashMap<String, Any?>>()

            val playlistDataDatabaseHelper = PlaylistDataDatabaseHelper(view.context, playlistId)

            val playlistDatas = playlistDataDatabaseHelper.getAllData()

            val songDatabaseHelper = SongDatabaseHelper(view.context)

            val jsonParser = JSONParser()

            for(playlistData in playlistDatas){
                val song = songDatabaseHelper.getSong(playlistData.songId)
                if(song != null){
                    val hashMap = jsonParser.parseSongToHashMap(view.context, song)
                    sortedList.add(hashMap)
                }
            }

            //display list
            currentListViewData = sortedList
            listViewDataIsPlaylistView = false
            updateListView(view)
            redrawListView(view)

            swipeRefreshLayout.isRefreshing = false

            //download cover art
            MainActivity.downloadHandler!!.addCoverArray(currentListViewData)
        }else{
            var finishedRequests = 0
            var totalRequestsCount = 0

            val requests = ArrayList<StringRequest>()

            val playlistTrackRequestQueue = Volley.newRequestQueue(view.context)

            val tempList = arrayOfNulls<Long>(trackCount)

            playlistTrackRequestQueue.addRequestFinishedListener<Any> {
                finishedRequests++

                if (finishedRequests >= totalRequestsCount) {
                    val sortedList = ArrayList<HashMap<String, Any?>>()

                    val playlistDataDatabaseHelper = PlaylistDataDatabaseHelper(view.context, playlistId)

                    val playlistDatas = playlistDataDatabaseHelper.getAllData()

                    val songDatabaseHelper = SongDatabaseHelper(view.context)

                    val jsonParser = JSONParser()

                    for(playlistData in playlistDatas){
                        val song = songDatabaseHelper.getSong(playlistData.songId)
                        if(song != null){
                            val hashMap = jsonParser.parseSongToHashMap(view.context, song)
                            sortedList.add(hashMap)
                        }
                    }

                    //display list
                    currentListViewData = sortedList
                    listViewDataIsPlaylistView = false
                    updateListView(view)
                    redrawListView(view)

                    swipeRefreshLayout.isRefreshing = false

                    //download cover art
                    MainActivity.downloadHandler!!.addCoverArray(currentListViewData)
                }else{
                    playlistTrackRequestQueue.add(requests[finishedRequests])
                }
            }

            for (i in (0..(trackCount as Int / 50))) {
                val playlistTrackUrl =
                    view.context.getString(R.string.loadSongsUrl) + "?playlistId=" + playlistId + "&skip=" + (i * 50).toString() + "&limit=50"

                val playlistTrackRequest = object : StringRequest(Method.GET, playlistTrackUrl,
                    Response.Listener { response ->
                        val jsonObject = JSONObject(response)
                        val jsonArray = jsonObject.getJSONArray("results")

                        for (k in (0 until jsonArray.length())) {
                            val playlistObject = jsonArray.getJSONObject(k)

                            val jsonParser = JSONParser()

                            //  val trackHashMap = jsonParser.parsePlaylistTracksToHashMap(playlistObject, view.context)

                            val id = jsonParser.parsePlaylistTrackToDB(playlistId, playlistObject, view.context)
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
        }
    }
}