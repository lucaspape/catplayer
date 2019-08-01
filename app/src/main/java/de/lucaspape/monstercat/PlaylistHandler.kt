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

class PlaylistHandler {

    private var currentPlaylist = HashMap<String, Any?>()

    fun loadPlaylist(view: View) {
        var list = ArrayList<HashMap<String, Any?>>()
        val playlistView = view.findViewById<ListView>(R.id.listview)

        val from = arrayOf("playlistName", "coverUrl")
        val to = arrayOf(R.id.title, R.id.cover)

        var simpleAdapter = SimpleAdapter(view.context, list, R.layout.list_single, from, to.toIntArray())

        val playlistCacheFile = File(view.context.cacheDir.toString() + "/playlists.list")

        if(!playlistCacheFile.exists()){
            val playlistUrl = "https://connect.monstercat.com/v2/self/playlists"

            val playlistRequest = object : StringRequest(Request.Method.GET, playlistUrl, Response.Listener<String>
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
                    playlistHashMap.put("playlistName", playlistName)
                    playlistHashMap.put("coverUrl", "")
                    playlistHashMap.put("titles", tracks)
                    playlistHashMap.put("playlistId", playlistId)
                    playlistHashMap.put("type", "playlist")
                    playlistHashMap.put("trackCount", playlistTrackCount)

                    list.add(playlistHashMap)

                    simpleAdapter = SimpleAdapter(view.context, list, R.layout.list_single, from, to.toIntArray())
                    playlistView.adapter = simpleAdapter
                }
            }, Response.ErrorListener { error ->

            }
            ) {
                @Throws(AuthFailureError::class)
                override fun getHeaders(): Map<String, String> {
                    val params = HashMap<String, String>()
                    if(loggedIn){
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
                if(swipeRefreshLayout != null){
                    swipeRefreshLayout.isRefreshing = false
                }
            }

            queue.add(playlistRequest)
        }else{
            val ois = ObjectInputStream(FileInputStream(playlistCacheFile))
            list = ois.readObject() as ArrayList<HashMap<String, Any?>>

            simpleAdapter = SimpleAdapter(view.context, list, R.layout.list_single, from, to.toIntArray())
            playlistView.adapter = simpleAdapter
        }

    }

    fun loadPlaylistTracks(view: View, itemValue:HashMap<String, Any?>, playlistView: ListView){
        val settings = Settings(view.context)

        var list = ArrayList<HashMap<String, Any?>>()

        val primaryResolution = settings.getSetting("primaryCoverResolution")
        val secondaryResolution = settings.getSetting("secondaryCoverResolution")
        val titleQueue = Volley.newRequestQueue(view.context)

        val playlistName = itemValue.get("playlistName")
        val playlistId = itemValue.get("playlistId")

        val trackCount = itemValue.get("trackCount") as Int

        val from = arrayOf("playlistName", "coverUrl")
        val to = arrayOf(R.id.title, R.id.cover)
        var simpleAdapter = SimpleAdapter(view.context, list, R.layout.list_single, from, to.toIntArray())

        val coverDownloadList = ArrayList<HashMap<String, Any?>>()

        val playlistTrackCacheFile = File(view.context.cacheDir.toString() + "/" + playlistId + ".list")

        if(playlistTrackCacheFile.exists()){
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
        }else{
            val tempList = Array<HashMap<String, Any?>>(trackCount, { HashMap<String, Any?>() })

            val todo = (trackCount/50) + 1
            var done = 0

            titleQueue.addRequestFinishedListener<Any> {
                done ++

                if(done >= todo){

                    for(i in tempList.indices){
                        list.add(tempList[i])
                    }

                    MainActivity.downloadCoverArray(coverDownloadList, simpleAdapter).execute()
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
                    if(swipeRefreshLayout != null){
                        swipeRefreshLayout.isRefreshing = false
                    }
                }
            }

            for(i in (0 .. (trackCount/50) +1)){
                val playlistTrackUrl =
                    "https://connect.monstercat.com/api/catalog/browse/?playlistId=" + playlistId + "&skip=" + (i*50).toString() + "&limit=50"

                val trackRequest =
                    object : StringRequest(Request.Method.GET, playlistTrackUrl, Response.Listener<String>
                    { response ->
                        val jsonObject = JSONObject(response)
                        val jsonArray = jsonObject.getJSONArray("results")

                        for (k in (0 until jsonArray.length())) {
                            val playlistObject = jsonArray.getJSONObject(k)

                            val title = playlistObject.getString("title")
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
                            trackHashMap.put("title", title)
                            trackHashMap.put("version", version)
                            trackHashMap.put("artist", artist)
                            trackHashMap.put("coverUrl", coverUrl)
                            trackHashMap.put("id", id)
                            trackHashMap.put("streamHash", streamHash)
                            trackHashMap.put("shownTitle", artist + " " + title + " " + version)
                            trackHashMap.put("downloadable", downloadable)
                            trackHashMap.put("streamable", streamable)
                            trackHashMap.put("albumId", albumId)

                            trackHashMap.put(
                                "primaryImage",
                                view.context.cacheDir.toString() + "/" + title + version + artist + ".png" + primaryResolution.toString()
                            )

                            trackHashMap.put(
                                "secondaryImage",
                                view.context.cacheDir.toString() + "/" + title + version + artist + ".png" + secondaryResolution.toString()
                            )

                            if (!File(view.context.cacheDir.toString() + "/" + title + version + artist + ".png" + primaryResolution).exists()) {
                                val coverHashMap = HashMap<String, Any?>()

                                coverHashMap.put("primaryRes", primaryResolution)
                                coverHashMap.put("secondaryRes", secondaryResolution)
                                coverHashMap.put("coverUrl", coverUrl)
                                coverHashMap.put("location", view.context.cacheDir.toString() + "/" + title + version + artist + ".png")
                                coverDownloadList.add(coverHashMap)
                            }

                            tempList[i*50 + k] = trackHashMap


                        }
                    }, Response.ErrorListener { error ->

                    }
                    ) {
                        @Throws(AuthFailureError::class)
                        override fun getHeaders(): Map<String, String> {
                            val params = HashMap<String, String>()
                            if(loggedIn){
                                params.put("Cookie", "connect.sid=" + sid)
                            }

                            return params
                        }
                    }



                titleQueue.add(trackRequest)
            }

        }


    }

    fun registerListViewClick(view: View) {
        //TODO sort

        val playlistView = view.findViewById<ListView>(R.id.listview)


        playlistView.onItemClickListener = object : AdapterView.OnItemClickListener {
            override fun onItemClick(adapterView: AdapterView<*>?, view: View?, position: Int, p3: Long) {
                val itemValue = playlistView.getItemAtPosition(position) as HashMap<String, Any?>

                if (itemValue.get("type") == "playlist") {
                    loadPlaylistTracks(view!!, itemValue, playlistView)
                } else {
                    //do song things

                    val artist = itemValue.get("artist") as String
                    val title = itemValue.get("title") as String
                    val version = itemValue.get("version")
                    val shownTitle = itemValue.get("shownTitle") as String
                    val coverUrl = itemValue.get("coverUrl") as String
                    val primaryCoverImage = itemValue.get("primaryImage") as String

                    val settings = Settings(view!!.context)

                    val downloadType = settings.getSetting("downloadType")

                    val downloadLocation =
                        view.context.filesDir.toString() + "/" + artist + title + version + "." + downloadType

                    if (File(downloadLocation).exists()) {
                        MainActivity.musicPlayer!!.addSong(downloadLocation, title, artist, primaryCoverImage)
                    } else {
                        if (itemValue.get("streamable") as Boolean) {
                            val url =
                                "https://s3.amazonaws.com/data.monstercat.com/blobs/" + itemValue.get("streamHash")

                            Toast.makeText(
                                view.context,
                                title + " " + version as String + " added to playlist!",
                                Toast.LENGTH_SHORT
                            ).show()

                            MainActivity.musicPlayer!!.addSong(url, shownTitle, artist, primaryCoverImage)
                        }
                    }

                }

            }
        }
    }

    //TODO implement
    fun registerPullRefresh(view: View) {
        val swipeRefreshLayout = view.findViewById<SwipeRefreshLayout>(R.id.swipeRefresh)
        swipeRefreshLayout.setOnRefreshListener {
            val listView = view.findViewById<ListView>(R.id.listview)
            val listViewItem = listView.getItemAtPosition(0) as HashMap<String, Any?>

            if(listViewItem.get("type") == "playlist"){
                val playlistCacheFile = File(view.context.cacheDir.toString() + "/playlists.list")
                playlistCacheFile.delete()

                loadPlaylist(view)
            }else{
                File(view.context.cacheDir.toString() + "/" + currentPlaylist.get("playlistId") + ".list").delete()
                loadPlaylistTracks(view, currentPlaylist, listView)
            }
        }
    }

    fun downloadSong(context: Context, listItem: HashMap<String, Any?>) {
        val id = listItem.get("id") as String
        val albumId = listItem.get("albumId") as String
        val title = listItem.get("title") as String
        val artist = listItem.get("artist") as String
        val coverUrl = listItem.get("coverUrl") as String
        val version = listItem.get("version") as String
        val shownTitle = listItem.get("shownTitle") as String
        val downloadable = listItem.get("downloadable") as Boolean

        if (downloadable) {
            val settings = Settings(context)

            val downloadType = settings.getSetting("downloadType")
            val downloadQuality = settings.getSetting("downloadQuality")

            val downloadUrl =
                "https://connect.monstercat.com/api/release/" + albumId + "/download?method=download&type=" + downloadType + "_" + downloadQuality + "&track=" + id

            val downloadLocation = context.filesDir.toString() + "/" + artist + title + version + "." + downloadType
            if (!File(downloadLocation).exists()) {
                if (sid != "") {
                    downloadSong(downloadUrl, downloadLocation, sid, shownTitle, context).execute()
                } else {
                    Toast.makeText(context, "Not signed in!", Toast.LENGTH_SHORT)
                        .show()
                }
            } else {
                Toast.makeText(context, shownTitle + " already downloaded!", Toast.LENGTH_SHORT)
                    .show()
            }
        } else {
            Toast.makeText(context, shownTitle + " download not available!", Toast.LENGTH_SHORT)
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

        val playlistId = listItem.get("playlistId")

        val playlistTrackUrl =
            "https://connect.monstercat.com/api/catalog/browse/?playlistId=" + playlistId + "&skip=0&limit=50"

        val trackRequest =
            object : StringRequest(Request.Method.GET, playlistTrackUrl, Response.Listener<String>
            { response ->
                val jsonObject = JSONObject(response)
                val jsonArray = jsonObject.getJSONArray("results")

                for (i in (0 until jsonArray.length())) {
                    val playlistObject = jsonArray.getJSONObject(i)

                    val downloadable = playlistObject.getBoolean("downloadable") as Boolean

                    if(downloadable){
                        val title = playlistObject.getString("title")
                        var version = playlistObject.getString("version")
                        val artist = playlistObject.getString("artistsTitle")
                        val coverUrl = playlistObject.getJSONObject("release").getString("coverUrl")
                        val id = playlistObject.getString("_id")
                        val albumId = playlistObject.getJSONObject("albums").getString("albumId")
                        val downloadLocation = context.filesDir.toString() + "/" + artist + title + version + "." + downloadType


                        if (version == "null") {
                            version = ""
                        }

                        val downloadUrl =
                            "https://connect.monstercat.com/api/release/" + albumId + "/download?method=download&type=" + downloadType + "_" + downloadQuality + "&track=" + id

                        val hashMap = HashMap<String, Any?>()
                        hashMap.put("title", title)
                        hashMap.put("version", version)
                        hashMap.put("artist", artist)
                        hashMap.put("coverUrl", coverUrl)
                        hashMap.put("id", id)
                        hashMap.put("albumId", albumId)
                        hashMap.put("downloadable", downloadable)
                        hashMap.put("downloadUrl", downloadUrl)
                        hashMap.put("downloadLocation", downloadLocation)

                        downloadTracks.add(hashMap)
                    }else{
                        //fu no download for you
                    }


                }
            }, Response.ErrorListener { error ->

            }
            ) {
                @Throws(AuthFailureError::class)
                override fun getHeaders(): Map<String, String> {
                    val params = HashMap<String, String>()
                    if(loggedIn){
                        params.put("Cookie", "connect.sid=" + sid)
                    }

                    return params
                }
            }

        playlistDownloadQueue.addRequestFinishedListener<Any> {
            try{
                downloadSongArray(downloadTracks, sid, context).execute()
            }catch (e: Exception){

            }

        }

        playlistDownloadQueue.add(trackRequest)
    }

    class downloadSongArray(tracks: ArrayList<HashMap<String, Any?>>, sid:String, context:Context):AsyncTask<Void, Void, String>(){
        val tracks = tracks
        val sid = sid
        var context = context

        override fun doInBackground(vararg p0: Void?): String? {
            for(i in tracks.indices){
                try {

                    val location = tracks[i].get("downloadLocation") as String
                    val url = tracks[i].get("downloadUrl") as String

                    val glideUrl = GlideUrl(
                        url, LazyHeaders.Builder()
                            .addHeader("Cookie", "connect.sid=" + sid).build()
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

            Toast.makeText(context, "Download of playlist finished!", Toast.LENGTH_SHORT)
                .show()
        }

    }

    class downloadSong(url: String, location: String, sid: String, shownTitle: String, context: Context) :
        AsyncTask<Void, Void, String>() {
        val url = url
        val location = location
        val context = context
        val sid = sid
        val shownTitle = shownTitle

        override fun doInBackground(vararg params: Void?): String? {
            try {

                val glideUrl = GlideUrl(
                    url, LazyHeaders.Builder()
                        .addHeader("Cookie", "connect.sid=" + sid).build()
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

        override fun onPreExecute() {
            super.onPreExecute()
            // ...
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            Toast.makeText(context, shownTitle + " downloaded!", Toast.LENGTH_SHORT)
                .show()
        }
    }
}