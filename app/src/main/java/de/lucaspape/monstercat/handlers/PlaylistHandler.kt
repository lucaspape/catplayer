package de.lucaspape.monstercat.handlers

import android.content.Context
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
import de.lucaspape.monstercat.settings.Settings
import de.lucaspape.monstercat.download.DownloadCoverArray
import de.lucaspape.monstercat.download.DownloadSong
import de.lucaspape.monstercat.download.DownloadSongArray
import org.json.JSONObject
import java.io.*
import java.lang.Exception
import java.lang.IndexOutOfBoundsException

class PlaylistHandler {

    private var currentPlaylist = HashMap<String, Any?>()

    fun loadPlaylist(view: View) {
        var list = ArrayList<HashMap<String, Any?>>()
        val playlistView = view.findViewById<ListView>(R.id.listview)

        val from = arrayOf("playlistName", "coverUrl")
        val to = arrayOf(R.id.title, R.id.cover)

        var simpleAdapter: SimpleAdapter

        val playlistCacheFile =
            File(view.context.getString(R.string.playlistCacheFile, view.context.cacheDir.toString()))

        if (!playlistCacheFile.exists()) {
            val playlistUrl = view.context.getString(R.string.playlistsUrl)

            val playlistRequest = object : StringRequest(
                Method.GET, playlistUrl, Response.Listener<String>
                { response ->
                    val jsonObject = JSONObject(response)
                    val jsonArray = jsonObject.getJSONArray("results")

                    for (i in (0 until jsonArray.length())) {
                        val jsonParser = JSONParser()
                        list.add(jsonParser.parsePlaylistToHashMap(jsonArray.getJSONObject(i)))

                        simpleAdapter = SimpleAdapter(view.context, list,
                            R.layout.list_single, from, to.toIntArray())
                        playlistView.adapter = simpleAdapter
                    }
                }, Response.ErrorListener { error ->
                    println(error)
                }
            ) {
                @Throws(AuthFailureError::class)
                override fun getHeaders(): Map<String, String> {
                    val params = HashMap<String, String>()
                    if (loggedIn) {
                        params.put("Cookie", "connect.sid=" + sid)
                    }

                    return params
                }
            }

            val queue = Volley.newRequestQueue(view.context)

            queue.addRequestFinishedListener<Any> {
                val oos = ObjectOutputStream(FileOutputStream(playlistCacheFile))
                oos.writeObject(list)
                oos.flush()
                oos.close()

                val swipeRefreshLayout = view.findViewById<SwipeRefreshLayout>(R.id.swipeRefresh)
                if (swipeRefreshLayout != null) {
                    swipeRefreshLayout.isRefreshing = false
                }
            }

            queue.add(playlistRequest)
        } else {
            val ois = ObjectInputStream(FileInputStream(playlistCacheFile))
            list = ois.readObject() as ArrayList<HashMap<String, Any?>>

            simpleAdapter = SimpleAdapter(view.context, list, R.layout.list_single, from, to.toIntArray())
            playlistView.adapter = simpleAdapter
        }

    }

    private fun loadPlaylistTracks(view: View, itemValue: HashMap<String, Any?>, playlistView: ListView) {
        var list = ArrayList<HashMap<String, Any?>>()
        val titleQueue = Volley.newRequestQueue(view.context)

        val playlistId = itemValue["playlistId"]

        val trackCount = itemValue["trackCount"] as Int

        val from = arrayOf("playlistName", "coverUrl")
        val to = arrayOf(R.id.title, R.id.cover)
        var simpleAdapter = SimpleAdapter(view.context, list, R.layout.list_single, from, to.toIntArray())

        val coverDownloadList = ArrayList<HashMap<String, Any?>?>()

        val playlistTrackCacheFile =
            File(view.context.getString(R.string.playlistTracksCacheFile, view.context.cacheDir.toString(), playlistId))

        if (playlistTrackCacheFile.exists()) {
            val ois = ObjectInputStream(FileInputStream(playlistTrackCacheFile))
            list = ois.readObject() as ArrayList<HashMap<String, Any?>>
            ois.close()

            val fromTrack = arrayOf("shownTitle", "secondaryImage")
            val toTrack = arrayOf(R.id.title, R.id.cover)

            simpleAdapter = SimpleAdapter(
                view.context,
                list,
                R.layout.list_single,
                fromTrack,
                toTrack.toIntArray()
            )

            playlistView.adapter = simpleAdapter

            currentPlaylist = itemValue
        } else {
            val tempList = Array<HashMap<String, Any?>>(trackCount) { HashMap() }

            val todo = (trackCount / 50) + 1
            var done = 0

            titleQueue.addRequestFinishedListener<Any> {
                done++

                if (done >= todo) {

                    for (i in tempList.indices) {
                        if (tempList[i].isNotEmpty()) {
                            list.add(tempList[i])
                        }

                    }

                    DownloadCoverArray(
                        coverDownloadList,
                        simpleAdapter
                    ).execute()
                    currentPlaylist = itemValue

                    val oos = ObjectOutputStream(FileOutputStream(playlistTrackCacheFile))
                    oos.writeObject(list)
                    oos.flush()
                    oos.close()

                    val fromTrack = arrayOf("shownTitle", "secondaryImage")
                    val toTrack = arrayOf(R.id.title, R.id.cover)

                    simpleAdapter = SimpleAdapter(
                        view.context,
                        list,
                        R.layout.list_single,
                        fromTrack,
                        toTrack.toIntArray()
                    )

                    playlistView.adapter = simpleAdapter

                    val swipeRefreshLayout = view.findViewById<SwipeRefreshLayout>(R.id.swipeRefresh)
                    if (swipeRefreshLayout != null) {
                        swipeRefreshLayout.isRefreshing = false
                    }
                }
            }

            for (i in (0..(trackCount / 50))) {
                val playlistTrackUrl =
                    view.context.getString(R.string.loadSongsUrl) + "?playlistId=" + playlistId + "&skip=" + (i * 50).toString() + "&limit=50"
                val trackRequest =
                    object : StringRequest(
                        Method.GET, playlistTrackUrl, Response.Listener<String>
                        { response ->
                            val jsonObject = JSONObject(response)
                            val jsonArray = jsonObject.getJSONArray("results")

                            for (k in (0 until jsonArray.length())) {
                                val playlistObject = jsonArray.getJSONObject(k)

                                val jsonParser = JSONParser()
                                val trackHashMap = jsonParser.parsePlaylistTracksToHashMap(playlistObject, view.context)

                                if(trackHashMap != null){
                                    coverDownloadList.add(jsonParser.parsePlaylistTrackCoverToHashMap(trackHashMap, view.context))
                                    tempList[i * 50 + k] = trackHashMap
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

                titleQueue.add(trackRequest)
            }

        }


    }

    fun registerListViewClick(view: View) {
        val playlistView = view.findViewById<ListView>(R.id.listview)

        playlistView.onItemClickListener =
            AdapterView.OnItemClickListener { _, listViewView, position, _ ->
                val itemValue = playlistView.getItemAtPosition(position) as HashMap<String, Any?>

                if (itemValue["type"] == "playlist") {
                    loadPlaylistTracks(listViewView!!, itemValue, playlistView)
                } else {
                    playSong(view.context, itemValue, false)
                }
            }
    }

    fun playSong(context: Context, itemValue: HashMap<String, Any?>, playAfter: Boolean) {

        val artist = itemValue["artist"] as String
        val title = itemValue["title"] as String
        val version = itemValue["version"] as String
        val shownTitle = itemValue["shownTitle"] as String
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

    fun registerPullRefresh(view: View) {
        val swipeRefreshLayout = view.findViewById<SwipeRefreshLayout>(R.id.swipeRefresh)
        swipeRefreshLayout.setOnRefreshListener {
            val listView = view.findViewById<ListView>(R.id.listview)
            val listViewItem: HashMap<String, Any?>

            try {
                listViewItem = listView.getItemAtPosition(0) as HashMap<String, Any?>
                if (listViewItem["type"] == "playlist") {
                    File(view.context.getString(R.string.playlistCacheFile, view.context.cacheDir.toString())).delete()
                    loadPlaylist(view)
                } else {
                    File(
                        view.context.getString(
                            R.string.playlistTracksCacheFile, view.context.cacheDir.toString(),
                            currentPlaylist["playlistId"]
                        )
                    ).delete()
                    loadPlaylistTracks(view, currentPlaylist, listView)
                }
            } catch (e: IndexOutOfBoundsException) {
                File(view.context.getString(R.string.playlistCacheFile, view.context.cacheDir.toString())).delete()
                loadPlaylist(view)
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
                    DownloadSong(downloadUrl, downloadLocation, sid, shownTitle, context).execute()
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

                        if(trackHashMap != null){
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
                DownloadSongArray(downloadTracks, playlistName, sid, context).execute()
            } catch (e: Exception) {

            }

        }

        playlistDownloadQueue.add(trackRequest)
    }




}