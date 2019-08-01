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
import de.lucaspape.monstercat.MainActivity.Companion.sid
import org.json.JSONObject
import java.io.*
import java.lang.Exception

class PlaylistHandler {

    fun loadPlaylist(view: View) {
        val list = ArrayList<HashMap<String, Any?>>()
        val playlistView = view.findViewById<ListView>(R.id.listview)

        val from = arrayOf("playlistName", "coverUrl")
        val to = arrayOf(R.id.title, R.id.cover)

        var simpleAdapter = SimpleAdapter(view.context, list, R.layout.list_single, from, to.toIntArray())

        val playlistUrl = "https://connect.monstercat.com/v2/self/playlists"

        val playlistRequest = object : StringRequest(Request.Method.GET, playlistUrl, Response.Listener<String>
        { response ->
            val jsonObject = JSONObject(response)
            val jsonArray = jsonObject.getJSONArray("results")

            for (i in (0 until jsonArray.length())) {
                val playlistObject = jsonArray.getJSONObject(i)
                val playlistName = playlistObject.getString("name")
                val playlistId = playlistObject.getString("_id")

                val tracks = playlistObject.getJSONArray("tracks")

                val playlistHashMap = HashMap<String, Any?>()
                playlistHashMap.put("playlistName", playlistName)
                playlistHashMap.put("coverUrl", "")
                playlistHashMap.put("titles", tracks)
                playlistHashMap.put("playlistId", playlistId)
                playlistHashMap.put("type", "playlist")

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
                params.put("Cookie", "connect.sid=" + sid)

                return params
            }
        }
        val queue = Volley.newRequestQueue(view.context)
        queue.add(playlistRequest)

    }

    fun registerListViewClick(view: View) {
        //TODO sort
        val list = ArrayList<HashMap<String, Any?>>()
        val playlistView = view.findViewById<ListView>(R.id.listview)

        val from = arrayOf("playlistName", "coverUrl")
        val to = arrayOf(R.id.title, R.id.cover)

        val settings = Settings(view.context)

        val primaryResolution = settings.getSetting("primaryCoverResolution")
        val secondaryResolution = settings.getSetting("secondaryCoverResolution")

        var simpleAdapter = SimpleAdapter(view.context, list, R.layout.list_single, from, to.toIntArray())

        playlistView.onItemClickListener = object : AdapterView.OnItemClickListener {
            override fun onItemClick(adapterView: AdapterView<*>?, view: View?, position: Int, p3: Long) {
                val itemValue = playlistView.getItemAtPosition(position) as HashMap<String, Any?>

                val titleQueue = Volley.newRequestQueue(view!!.context)

                if (itemValue.get("type") == "playlist") {
                    val playlistName = itemValue.get("playlistName")
                    val playlistId = itemValue.get("playlistId")

                    val coverDownloadList = ArrayList<HashMap<String, Any?>>()

                    //TODO this only downloads 50 tracks
                    val playlistTrackUrl =
                        "https://connect.monstercat.com/api/catalog/browse/?playlistId=" + playlistId + "&skip=0&limit=50"
                    val tracks = ArrayList<HashMap<String, Any?>>()

                    val trackRequest =
                        object : StringRequest(Request.Method.GET, playlistTrackUrl, Response.Listener<String>
                        { response ->
                            val jsonObject = JSONObject(response)
                            val jsonArray = jsonObject.getJSONArray("results")

                            for (i in (0 until jsonArray.length())) {
                                val playlistObject = jsonArray.getJSONObject(i)

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

                                tracks.add(trackHashMap)

                                if (!File(view.context.cacheDir.toString() + "/" + title + version + artist + ".png" + primaryResolution).exists()) {
                                    val coverHashMap = HashMap<String, Any?>()

                                    coverHashMap.put("primaryRes", primaryResolution)
                                    coverHashMap.put("secondaryRes", secondaryResolution)
                                    coverHashMap.put("coverUrl", coverUrl)
                                    coverHashMap.put("location", view.context.cacheDir.toString() + "/" + title + version + artist + ".png")
                                    coverDownloadList.add(coverHashMap)
                                }

                                val fromTrack = arrayOf("shownTitle", "secondaryImage")
                                val toTrack = arrayOf(R.id.title, R.id.cover)

                                simpleAdapter = SimpleAdapter(
                                    view.context,
                                    tracks,
                                    R.layout.list_single,
                                    fromTrack,
                                    toTrack.toIntArray()
                                )
                                playlistView.adapter = simpleAdapter
                            }
                        }, Response.ErrorListener { error ->

                        }
                        ) {
                            @Throws(AuthFailureError::class)
                            override fun getHeaders(): Map<String, String> {
                                val params = HashMap<String, String>()
                                params.put("Cookie", "connect.sid=" + sid)

                                return params
                            }
                        }

                    titleQueue.addRequestFinishedListener<Any> {
                        MainActivity.downloadCoverArray(coverDownloadList, simpleAdapter).execute()
                    }

                    titleQueue.add(trackRequest)
                } else {
                    //do song things

                    val artist = itemValue.get("artist") as String
                    val title = itemValue.get("title") as String
                    val version = itemValue.get("version")
                    val shownTitle = itemValue.get("shownTitle") as String
                    val coverUrl = itemValue.get("coverUrl") as String
                    val primaryCoverImage = itemValue.get("primaryImage") as String

                    val settings = Settings(view.context)

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
        val swipeRefreshLayout = view.findViewById<SwipeRefreshLayout>(R.id.pullToRefresh)
    }

    fun downloadSong(context: Context, listItem: HashMap<String, Any?>) {
        val id = listItem.get("id")
        val albumId = listItem.get("albumId")
        val title = listItem.get("title")
        val artist = listItem.get("artist")
        val coverUrl = listItem.get("coverUrl")
        val version = listItem.get("version")
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

    //TODO implement
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
                    params.put("Cookie", "connect.sid=" + sid)

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