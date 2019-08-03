package de.lucaspape.monstercat

import android.content.Context
import android.os.AsyncTask
import android.view.View
import android.widget.AdapterView
import android.widget.ListView
import android.widget.SimpleAdapter
import android.widget.Toast
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.android.volley.*
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import com.bumptech.glide.request.target.Target
import de.lucaspape.monstercat.MainActivity.Companion.loggedIn
import de.lucaspape.monstercat.MainActivity.Companion.sid

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
            val playlistUrl = view.context.getString(R.string.playlistUrl)

            val playlistRequest = object : StringRequest(
                Method.GET, playlistUrl, Response.Listener<String>
                { response ->
                    val jsonObject = JSONObject(response)
                    val jsonArray = jsonObject.getJSONArray("results")

                    for (i in (0 until jsonArray.length())) {
                        val playlistObject = jsonArray.getJSONObject(i)
                        val playlistName = playlistObject.getString("name") as String
                        val playlistId = playlistObject.getString("_id") as String
                        val playlistTrackCount = playlistObject.getJSONArray("tracks").length()

                        val tracks = playlistObject.getJSONArray("tracks").toString()

                        val playlistHashMap = HashMap<String, Any?>()
                        playlistHashMap["playlistName"] = playlistName
                        playlistHashMap["coverUrl"] = ""
                        playlistHashMap["titles"] = tracks
                        playlistHashMap["playlistId"] = playlistId
                        playlistHashMap["type"] = "playlist"
                        playlistHashMap["trackCount"] = playlistTrackCount

                        list.add(playlistHashMap)

                        simpleAdapter = SimpleAdapter(view.context, list, R.layout.list_single, from, to.toIntArray())
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
        val settings = Settings(view.context)

        var list = ArrayList<HashMap<String, Any?>>()

        val primaryResolution = settings.getSetting("primaryCoverResolution")
        val secondaryResolution = settings.getSetting("secondaryCoverResolution")
        val titleQueue = Volley.newRequestQueue(view.context)

        val playlistId = itemValue["playlistId"]

        val trackCount = itemValue["trackCount"] as Int

        val from = arrayOf("playlistName", "coverUrl")
        val to = arrayOf(R.id.title, R.id.cover)
        var simpleAdapter = SimpleAdapter(view.context, list, R.layout.list_single, from, to.toIntArray())

        val coverDownloadList = ArrayList<HashMap<String, Any?>>()

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
            //TODO fix last 2 songs not visible
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

                    DownloadCoverArray(coverDownloadList, simpleAdapter).execute()
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

                                val title = playlistObject.getString("title")

                                if (title != "null") {
                                    var version = playlistObject.getString("version")
                                    val artist = playlistObject.getString("artistsTitle")
                                    val coverUrl = playlistObject.getJSONObject("release").getString("coverUrl")
                                    val id = playlistObject.getString("_id")
                                    val albumId = playlistObject.getJSONObject("albums").getString("albumId")
                                    val streamHash = playlistObject.getJSONObject("albums").getString("streamHash")
                                    val downloadable = playlistObject.getBoolean("downloadable")
                                    val streamable = playlistObject.getBoolean("streamable")


                                    if (version == "null") {
                                        version = ""
                                    }

                                    val trackHashMap = HashMap<String, Any?>()
                                    trackHashMap["title"] = title
                                    trackHashMap["version"] = version
                                    trackHashMap["artist"] = artist
                                    trackHashMap["coverUrl"] = coverUrl
                                    trackHashMap["id"] = id
                                    trackHashMap["streamHash"] = streamHash
                                    trackHashMap["shownTitle"] = "$artist $title $version"
                                    trackHashMap["downloadable"] = downloadable
                                    trackHashMap["streamable"] = streamable
                                    trackHashMap["albumId"] = albumId

                                    trackHashMap["primaryImage"] =
                                        view.context.cacheDir.toString() + "/" + title + version + artist + ".png" + primaryResolution.toString()

                                    trackHashMap["secondaryImage"] =
                                        view.context.cacheDir.toString() + "/" + title + version + artist + ".png" + secondaryResolution.toString()

                                    if (!File(view.context.cacheDir.toString() + "/" + title + version + artist + ".png" + primaryResolution).exists()) {
                                        val coverHashMap = HashMap<String, Any?>()

                                        coverHashMap["primaryRes"] = primaryResolution
                                        coverHashMap["secondaryRes"] = secondaryResolution
                                        coverHashMap["coverUrl"] = coverUrl
                                        coverHashMap["location"] =
                                            view.context.cacheDir.toString() + "/" + title + version + artist + ".png"
                                        coverDownloadList.add(coverHashMap)
                                    }

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

    fun playSong(context: Context, itemValue: HashMap<String, Any?>, playAfter:Boolean){

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
            if(playAfter){
                MainActivity.musicPlayer!!.addSong(downloadLocation, title, artist, primaryCoverImage)
            }else{
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

                if(playAfter){
                    MainActivity.musicPlayer!!.addSong(url, title, artist, primaryCoverImage)
                }else{
                    MainActivity.musicPlayer!!.playNow(url, title, artist, primaryCoverImage)
                }
            }
        }
    }

    fun registerPullRefresh(view: View) {
        val swipeRefreshLayout = view.findViewById<SwipeRefreshLayout>(R.id.swipeRefresh)
        swipeRefreshLayout.setOnRefreshListener {
            val listView = view.findViewById<ListView>(R.id.listview)
            val listViewItem:HashMap<String, Any?>

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
            }catch (e:IndexOutOfBoundsException){
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

        val settings = Settings(context)

        val downloadType = settings.getSetting("downloadType")
        val downloadQuality = settings.getSetting("downloadQuality")

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

                        val downloadable = playlistObject.getBoolean("downloadable")

                        if (downloadable) {
                            val title = playlistObject.getString("title")
                            var version = playlistObject.getString("version")
                            val artist = playlistObject.getString("artistsTitle")
                            val coverUrl = playlistObject.getJSONObject("release").getString("coverUrl")
                            val id = playlistObject.getString("_id")
                            val albumId = playlistObject.getJSONObject("albums").getString("albumId")
                            val downloadLocation =
                                context.filesDir.toString() + "/" + artist + title + version + "." + downloadType


                            if (version == "null") {
                                version = ""
                            }

                            val downloadUrl =
                                context.getString(R.string.songDownloadUrl) + albumId + "/download?method=download&type=" + downloadType + "_" + downloadQuality + "&track=" + id

                            val hashMap = HashMap<String, Any?>()
                            hashMap["title"] = title
                            hashMap["version"] = version
                            hashMap["artist"] = artist
                            hashMap["coverUrl"] = coverUrl
                            hashMap["id"] = id
                            hashMap["albumId"] = albumId
                            hashMap["downloadable"] = downloadable
                            hashMap["downloadUrl"] = downloadUrl
                            hashMap["downloadLocation"] = downloadLocation

                            downloadTracks.add(hashMap)
                        } else {
                            //fu no download for you
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

    class DownloadSongArray(
        private val tracks: ArrayList<HashMap<String, Any?>>, private val playlistName: String,
        private val sid: String, private val context: Context
    ) : AsyncTask<Void, Void, String>() {

        override fun doInBackground(vararg p0: Void?): String? {
            for (i in tracks.indices) {
                try {

                    val location = tracks[i]["downloadLocation"] as String
                    val url = tracks[i]["downloadUrl"] as String

                    val glideUrl = GlideUrl(
                        url, LazyHeaders.Builder()
                            .addHeader("Cookie", "connect.sid=$sid").build()
                    )

                    try {
                        val downloadFile = Glide.with(context)
                            .load(glideUrl)
                            .downloadOnly(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                            .get()

                        val destFile = File(location)

                        val bufferedInputStream = BufferedInputStream(FileInputStream(downloadFile))
                        val bufferedOutputStream = BufferedOutputStream(FileOutputStream(destFile))

                        val buffer = ByteArray(1024)

                        var len: Int
                        len = bufferedInputStream.read(buffer)
                        while (len > 0) {
                            bufferedOutputStream.write(buffer, 0, len)
                            len = bufferedInputStream.read(buffer)
                        }
                        bufferedOutputStream.flush()
                        bufferedOutputStream.close()
                    } catch (e: Exception) {
                    }

                } catch (e: IOException) {
                    // Log exception
                    return null
                }
            }

            return null
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)

            Toast.makeText(
                context,
                context.getString(R.string.downloadPlaylistSuccessfulMsg, playlistName),
                Toast.LENGTH_SHORT
            )
                .show()
        }

    }

    class DownloadSong(
        private val url: String, private val location: String,
        private val sid: String, private val shownTitle: String,
        private val context: Context
    ) :
        AsyncTask<Void, Void, String>() {

        override fun doInBackground(vararg params: Void?): String? {
            try {

                val glideUrl = GlideUrl(
                    url, LazyHeaders.Builder()
                        .addHeader("Cookie", "connect.sid=$sid").build()
                )


                try {
                    val downloadFile = Glide.with(context)
                        .load(glideUrl)
                        .downloadOnly(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                        .get()

                    val destFile = File(location)

                    val bufferedInputStream = BufferedInputStream(FileInputStream(downloadFile))
                    val bufferedOutputStream = BufferedOutputStream(FileOutputStream(destFile))

                    val buffer = ByteArray(1024)

                    var len: Int
                    len = bufferedInputStream.read(buffer)
                    while (len > 0) {
                        bufferedOutputStream.write(buffer, 0, len)
                        len = bufferedInputStream.read(buffer)
                    }
                    bufferedOutputStream.flush()
                    bufferedOutputStream.close()
                } catch (e: GlideException) {
                }

            } catch (e: IOException) {
                // Log exception
                return null
            }

            return null
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            Toast.makeText(context, context.getString(R.string.downloadSuccessfulMsg, shownTitle), Toast.LENGTH_SHORT)
                .show()
        }
    }
}