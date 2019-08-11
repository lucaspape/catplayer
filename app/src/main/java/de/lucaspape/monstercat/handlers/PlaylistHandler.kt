package de.lucaspape.monstercat.handlers

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.AdapterView
import android.widget.ListView
import android.widget.SimpleAdapter
import android.widget.Toast
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.android.volley.*
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import de.lucaspape.monstercat.json.JSONParser
import de.lucaspape.monstercat.MainActivity
import de.lucaspape.monstercat.MainActivity.Companion.loggedIn
import de.lucaspape.monstercat.MainActivity.Companion.sid
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.cache.Cache
import de.lucaspape.monstercat.settings.Settings
import org.json.JSONObject
import java.io.*
import java.lang.Exception
import java.lang.IndexOutOfBoundsException

class PlaylistHandler {

    private var currentListData = ArrayList<HashMap<String, Any?>>()
    private var currentPlaylist = HashMap<String, Any?>()
    var simpleAdapter: SimpleAdapter? = null
    var isPlaylist = false

    fun setupListView(view: View) {
        updateListView(view, true)
        redrawListView(view)

        //setup auto reload
        Thread(Runnable {
            while (true) {
                Handler(Looper.getMainLooper()).post(Runnable { redrawListView(view) })
                Thread.sleep(1000)
            }

        }).start()
    }

    fun registerListeners(view: View) {
        val playlistView = view.findViewById<ListView>(R.id.listview)

        playlistView.onItemClickListener =
            AdapterView.OnItemClickListener { _, listViewView, position, _ ->
                val itemValue = playlistView.getItemAtPosition(position) as HashMap<String, Any?>

                if (itemValue["type"] == "playlist") {
                    currentPlaylist = itemValue
                    loadPlaylistTracks(view, itemValue, false)
                } else {
                    playSong(view.context, itemValue, false)
                }
            }

        val swipeRefreshLayout = view.findViewById<SwipeRefreshLayout>(R.id.swipeRefresh)
        swipeRefreshLayout.setOnRefreshListener {
            val listView = view.findViewById<ListView>(R.id.listview)
            val listViewItem: HashMap<String, Any?>

            try {
                listViewItem = listView.getItemAtPosition(0) as HashMap<String, Any?>
                if (listViewItem["type"] == "playlist") {
                    loadPlaylist(view, true)
                } else {
                    loadPlaylistTracks(view, listViewItem, true)
                }
            } catch (e: IndexOutOfBoundsException) {
                loadPlaylist(view, true)
            }

        }
    }

    fun redrawListView(view: View) {
        val playlistView = view.findViewById<ListView>(R.id.listview)
        simpleAdapter!!.notifyDataSetChanged()
        playlistView.invalidateViews()
        playlistView.refreshDrawableState()
    }

    fun updateListView(view: View, playlist: Boolean) {
        val playlistView = view.findViewById<ListView>(R.id.listview)

        var from = arrayOf("shownTitle", "secondaryImage")
        var to = arrayOf(R.id.title, R.id.cover)

        if (playlist) {
            from = arrayOf("playlistName", "coverUrl")
            to = arrayOf(R.id.title, R.id.cover)
        }

        simpleAdapter = SimpleAdapter(view.context, currentListData, R.layout.list_single, from, to.toIntArray())
        playlistView.adapter = simpleAdapter
    }

    fun loadPlaylist(view: View, force: Boolean) {
        val swipeRefreshLayout = view.findViewById<SwipeRefreshLayout>(R.id.swipeRefresh)

        swipeRefreshLayout.isRefreshing = true

        val cache = Cache("playlistCache", view.context)
        val playlistViewCache = cache.load("playlistView") as ArrayList<HashMap<String, Any?>>?

        if (playlistViewCache != null && !force) {
            currentListData = playlistViewCache
            updateListView(view, true)
            redrawListView(view)
            swipeRefreshLayout.isRefreshing = false
            isPlaylist = true
        } else {
            val playlistRequestQueue = Volley.newRequestQueue(view.context)

            val playlistUrl = view.context.getString(R.string.playlistsUrl)

            val playlistRequest = object : StringRequest(Method.GET, playlistUrl,
                Response.Listener { response ->
                    val jsonObject = JSONObject(response)
                    val jsonArray = jsonObject.getJSONArray("results")

                    val list = ArrayList<HashMap<String, Any?>>()

                    val jsonParser = JSONParser()

                    for (i in (0 until jsonArray.length())) {
                        list.add(jsonParser.parsePlaylistToHashMap(jsonArray.getJSONObject(i)))
                    }

                    currentListData = list
                    updateListView(view, true)
                    redrawListView(view)
                },
                Response.ErrorListener { }) {
                @Throws(AuthFailureError::class)
                override fun getHeaders(): Map<String, String> {
                    val params = HashMap<String, String>()
                    if (loggedIn) {
                        params.put("Cookie", "connect.sid=" + sid)
                    }

                    return params
                }
            }

            playlistRequestQueue.addRequestFinishedListener<Any> {
                cache.save("playlistView", currentListData)
                swipeRefreshLayout.isRefreshing = false
                isPlaylist = true
            }

            playlistRequestQueue.add(playlistRequest)
        }
    }

    private fun loadPlaylistTracks(view: View, playlist: HashMap<String, Any?>, force: Boolean) {
        val swipeRefreshLayout = view.findViewById<SwipeRefreshLayout>(R.id.swipeRefresh)
        swipeRefreshLayout.isRefreshing = true

        val cache = Cache("playlistTrackCache", view.context)

        if (isPlaylist) {
            val playlistTrackCache = cache.load(currentPlaylist["playlistId"] as String) as ArrayList<HashMap<String, Any?>>?

            if (playlistTrackCache != null && !force) {
                currentListData = playlistTrackCache
                isPlaylist = false
                updateListView(view, false)
                redrawListView(view)
                swipeRefreshLayout.isRefreshing = false
                return
            }
        }

        var finishedRequests = 0
        var totalRequestsCount = 0

        val playlistTrackRequestQueue = Volley.newRequestQueue(view.context)

        val tempList = arrayOfNulls<HashMap<String, Any?>>(currentPlaylist["trackCount"] as Int)

        playlistTrackRequestQueue.addRequestFinishedListener<Any> {
            finishedRequests++

            if (finishedRequests >= totalRequestsCount) {
                val sortedList = ArrayList<HashMap<String, Any?>>()

                for (i in tempList.indices) {
                    if (tempList[i] != null) {
                        if (tempList.isNotEmpty()) {
                            sortedList.add(tempList[i]!!)
                        }
                    }
                }

                //display list
                currentListData = sortedList
                isPlaylist = false
                updateListView(view, false)
                redrawListView(view)

                swipeRefreshLayout.isRefreshing = false

                //save to cache
                cache.save(currentPlaylist["playlistId"] as String, currentListData)

                //download cover art
                MainActivity.downloadHandler!!.addCoverArray(currentListData)
            }
        }

        for (i in (0..(currentPlaylist["trackCount"] as Int / 50))) {
            val playlistTrackUrl =
                view.context.getString(R.string.loadSongsUrl) + "?playlistId=" + currentPlaylist["playlistId"] as String + "&skip=" + (i * 50).toString() + "&limit=50"

            val playlistTrackRequest = object : StringRequest(Method.GET, playlistTrackUrl,
                Response.Listener { response ->
                    val jsonObject = JSONObject(response)
                    val jsonArray = jsonObject.getJSONArray("results")

                    for (k in (0 until jsonArray.length())) {
                        val playlistObject = jsonArray.getJSONObject(k)

                        val jsonParser = JSONParser()
                        val trackHashMap = jsonParser.parsePlaylistTracksToHashMap(playlistObject, view.context)

                        if (trackHashMap != null) {
                            tempList[i * 50 + k] = trackHashMap
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
            playlistTrackRequestQueue.add(playlistTrackRequest)


        }
    }

    fun playSong(context: Context, itemValue: HashMap<String, Any?>, playAfter: Boolean) {

        val artist = itemValue["artist"] as String
        val title = itemValue["title"] as String
        val version = itemValue["version"] as String
        val primaryCoverImage = itemValue["primaryImage"] as String

        val settings = Settings(context)

        val downloadType = settings.getSetting("downloadType")

        val downloadLocation =
            context.filesDir.toString() + "/" + artist + title + version + "." + downloadType

        if (File(downloadLocation).exists()) {
            if (playAfter) {
                MainActivity.musicPlayer!!.addSong(downloadLocation, title, artist, primaryCoverImage)
            } else {
                MainActivity.musicPlayer!!.playNow(downloadLocation, title, artist, primaryCoverImage)
            }

        } else {
            if (itemValue["streamable"] as Boolean) {
                val url =
                    context.getString(R.string.songStreamUrl) + itemValue["streamHash"]

                Toast.makeText(
                    context,
                    context.getString(R.string.songAddedToPlaylistMsg, "$title $version"),
                    Toast.LENGTH_SHORT
                ).show()

                if (playAfter) {
                    MainActivity.musicPlayer!!.addSong(url, title, artist, primaryCoverImage)
                } else {
                    MainActivity.musicPlayer!!.playNow(url, title, artist, primaryCoverImage)
                }
            }
        }
    }

    fun downloadSong(context: Context, listItem: HashMap<String, Any?>) {
        val id = listItem["id"] as String
        val albumId = listItem["albumId"] as String
        val title = listItem["title"] as String
        val artist = listItem["artist"] as String
        val version = listItem["version"] as String
        val shownTitle = listItem["shownTitle"] as String
        val downloadable = listItem["downloadable"] as Boolean

        if (downloadable) {
            val settings = Settings(context)

            val downloadType = settings.getSetting("downloadType")
            val downloadQuality = settings.getSetting("downloadQuality")

            val downloadUrl =
                context.getString(R.string.songDownloadUrl) + albumId + "/download?method=download&type=" + downloadType + "_" + downloadQuality + "&track=" + id

            val downloadLocation = context.filesDir.toString() + "/" + artist + title + version + "." + downloadType
            if (!File(downloadLocation).exists()) {
                if (sid != "") {
                    MainActivity.downloadHandler!!.addSong(downloadUrl, downloadLocation, shownTitle)
                } else {
                    Toast.makeText(context, context.getString(R.string.userNotSignedInMsg), Toast.LENGTH_SHORT)
                        .show()
                }
            } else {
                Toast.makeText(
                    context,
                    context.getString(R.string.alreadyDownloadedMsg, shownTitle),
                    Toast.LENGTH_SHORT
                )
                    .show()
            }
        } else {
            Toast.makeText(context, context.getString(R.string.downloadNotAvailableMsg, shownTitle), Toast.LENGTH_SHORT)
                .show()
        }
    }

    fun downloadPlaylist(context: Context, listItem: HashMap<String, Any?>) {
        val downloadTracks = ArrayList<HashMap<String, Any?>>()

        val playlistDownloadQueue = Volley.newRequestQueue(context)

        //request playlist

        val playlistId = listItem["playlistId"] as String
        val playlistName = listItem["playlistName"] as String

        val playlistTrackUrl =
            context.getString(R.string.loadSongsUrl) + "?playlistId=" + playlistId + "&skip=0&limit=50"

        val trackRequest =
            object : StringRequest(
                Method.GET, playlistTrackUrl, Response.Listener<String>
                { response ->
                    val jsonObject = JSONObject(response)
                    val jsonArray = jsonObject.getJSONArray("results")

                    for (i in (0 until jsonArray.length())) {
                        val playlistObject = jsonArray.getJSONObject(i)

                        val trackHashMap = JSONParser()
                            .parseDownloadPlaylistTracksToHashMap(playlistObject, context)

                        if (trackHashMap != null) {
                            downloadTracks.add(trackHashMap)
                        }

                    }
                }, Response.ErrorListener { error ->
                    println(error)
                }
            ) {
                @Throws(AuthFailureError::class)
                override fun getHeaders(): Map<String, String> {
                    val params = HashMap<String, String>()
                    if (loggedIn) {
                        params["Cookie"] = "connect.sid=$sid"
                    }

                    return params
                }
            }

        playlistDownloadQueue.addRequestFinishedListener<Any> {
            try {
                MainActivity.downloadHandler!!.addSongArray(downloadTracks)
            } catch (e: Exception) {

            }

        }

        playlistDownloadQueue.add(trackRequest)
    }


}