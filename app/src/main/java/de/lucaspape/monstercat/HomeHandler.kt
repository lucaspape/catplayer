package de.lucaspape.monstercat

import android.content.Context
import android.os.AsyncTask
import android.view.View
import android.widget.*
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
import java.lang.reflect.InvocationTargetException

/**
 * Does everything for the home page
 */
class HomeHandler {

    fun loadTitlesFromCache(view: View) {
        val musicList = view.findViewById<ListView>(R.id.musiclistview)

        var list = ArrayList<HashMap<String, Any?>>()
        val listFile = File(view.context.cacheDir.toString() + "/" + "songs.list")

        val from = arrayOf("shownTitle", "secondaryImage")
        val to = arrayOf(R.id.title, R.id.cover)

        var simpleAdapter = SimpleAdapter(view.context, list, R.layout.list_single, from, to.toIntArray())
        musicList.adapter = simpleAdapter

        if (listFile.exists()) {
            try {
                val ois = ObjectInputStream(FileInputStream(listFile))
                list = ois.readObject() as ArrayList<HashMap<String, Any?>>
                ois.close()
            } catch (e: Exception) {
                println(e)
            }

            simpleAdapter = SimpleAdapter(view.context, list, R.layout.list_single, from, to.toIntArray())
            simpleAdapter.notifyDataSetChanged()
            musicList.adapter = simpleAdapter
        }
    }

    fun registerPullRefresh(view: View) {
        val musicList = view.findViewById<ListView>(R.id.musiclistview)
        var list = ArrayList<HashMap<String, Any?>>()
        val queue = Volley.newRequestQueue(view.context)
        val listFile = File(view.context.cacheDir.toString() + "/" + "songs.list")
        val from = arrayOf("shownTitle", "secondaryImage")
        val to = arrayOf(R.id.title, R.id.cover)
        var simpleAdapter = SimpleAdapter(view.context, list, R.layout.list_single, from, to.toIntArray())

        val settings = Settings(view.context)

        val primaryResolution = settings.getSetting("primaryCoverResolution")
        val secondaryResolution = settings.getSetting("secondaryCoverResolution")

        val swipeRefreshLayout = view.findViewById<SwipeRefreshLayout>(R.id.pullToRefresh)
        swipeRefreshLayout.setOnRefreshListener {
            var requestCount = 0

            val loadMax = 200
            val coverDownloadList = ArrayList<HashMap<String, Any?>>()
            val tempList = Array<HashMap<String, Any?>>(loadMax, { HashMap<String, Any?>() })
            list = ArrayList<HashMap<String, Any?>>()

            //wait for all request to finish and sort
            var finishedRequest = 0
            queue.addRequestFinishedListener<Any> {
                finishedRequest++
                if (finishedRequest == requestCount) {
                    for (i in tempList.indices) {
                        if(!tempList[i].isEmpty()){
                            list.add(tempList[i])
                        }

                    }

                    //download cover arts
                    MainActivity.downloadCoverArray(coverDownloadList, simpleAdapter).execute()

                    val oos = ObjectOutputStream(FileOutputStream(listFile))
                    oos.writeObject(list)
                    oos.flush()
                    oos.close()

                    //update listview
                    simpleAdapter = SimpleAdapter(view.context, list, R.layout.list_single, from, to.toIntArray())
                    simpleAdapter.notifyDataSetChanged()
                    musicList.adapter = simpleAdapter

                    swipeRefreshLayout.isRefreshing = false
                }
            }

            //can only load 50 at a time
            for (i in (0 until loadMax / 50)) {
                val url = view.context.getString(R.string.loadSongsUrl) + "?limit=50&skip=" + i * 50

                val stringRequest = object : StringRequest(
                    Request.Method.GET, url,
                    Response.Listener<String> { response ->
                        val json = JSONObject(response)
                        val jsonArray = json.getJSONArray("results")

                        for (k in (0 until jsonArray.length())) {
                            var id = ""
                            var title = ""
                            var artist = ""
                            var coverUrl = ""
                            var version = ""
                            var songId = ""
                            var downloadable = false
                            var streamable = false

                            try {
                                id = jsonArray.getJSONObject(k).getJSONObject("albums").getString("albumId")
                                title = jsonArray.getJSONObject(k).getString("title")
                                artist = jsonArray.getJSONObject(k).getString("artistsTitle")
                                coverUrl = jsonArray.getJSONObject(k).getJSONObject("release").getString("coverUrl")
                                version = jsonArray.getJSONObject(k).getString("version")
                                songId = jsonArray.getJSONObject(k).getString("_id")
                                downloadable = jsonArray.getJSONObject(k).getBoolean("downloadable")
                                streamable = jsonArray.getJSONObject(k).getBoolean("streamable")
                            } catch (e: InvocationTargetException) {
                            }

                            if (version == "null") {
                                version = ""
                            }

                            val hashMap = HashMap<String, Any?>()

                            hashMap.put("id", id)
                            hashMap.put("title", title)
                            hashMap.put("artist", artist)
                            hashMap.put(
                                "primaryImage",
                                view.context.cacheDir.toString() + "/" + title + version + artist + ".png" + primaryResolution.toString()
                            )

                            hashMap.put(
                                "secondaryImage",
                                view.context.cacheDir.toString() + "/" + title + version + artist + ".png" + secondaryResolution.toString()
                            )

                            hashMap.put("version", version)

                            hashMap.put("shownTitle", artist + " " + title + " " + version)
                            hashMap.put("songId", songId)
                            hashMap.put("downloadable", downloadable)
                            hashMap.put("streamable", streamable)


                            if (!File(view.context.cacheDir.toString() + "/" + title + version + artist + ".png" + primaryResolution).exists()) {
                                val coverHashMap = HashMap<String, Any?>()

                                coverHashMap.put("primaryRes", primaryResolution)
                                coverHashMap.put("secondaryRes", secondaryResolution)
                                coverHashMap.put("coverUrl", coverUrl)
                                coverHashMap.put(
                                    "location",
                                    view.context.cacheDir.toString() + "/" + title + version + artist + ".png"
                                )
                                coverDownloadList.add(coverHashMap)
                            }

                            tempList[i * 50 + k] = hashMap

                        }

                    },
                    Response.ErrorListener { println("Error!") }) {
                    @Throws(AuthFailureError::class)
                    override fun getHeaders(): Map<String, String> {
                        val params = HashMap<String, String>()
                        if (loggedIn) {
                            params.put("Cookie", "connect.sid=" + sid)
                        }
                        return params
                    }
                }

                // Add the request to the RequestQueue
                queue.add(stringRequest)
                requestCount++

            }


        }
    }

    fun setupMusicPlayer(view: View) {
        val textview1 = view.findViewById<TextView>(R.id.songCurrent1)
        val textview2 = view.findViewById<TextView>(R.id.songCurrent2)
        val coverBarImageView = view.findViewById<ImageView>(R.id.barCoverImage)
        val musicToolBar = view.findViewById<androidx.appcompat.widget.Toolbar>(R.id.muscicbar)
        val playButton = view.findViewById<ImageButton>(R.id.playButton)
        val seekBar = view.findViewById<SeekBar>(R.id.seekBar)

        //setup musicPlayer
        if (MainActivity.musicPlayer == null) {
            MainActivity.musicPlayer =
                MusicPlayer(view.context, textview1, textview2, seekBar, coverBarImageView, musicToolBar, playButton)
        } else {
            MainActivity.musicPlayer!!.setContext(view.context)
            MainActivity.musicPlayer!!.setTextView(textview1, textview2)
            MainActivity.musicPlayer!!.setSeekBar(seekBar)
            MainActivity.musicPlayer!!.setBarCoverImageView(coverBarImageView)
            MainActivity.musicPlayer!!.setMusicBar(musicToolBar)
            MainActivity.musicPlayer!!.setPlayButton(playButton)
        }
    }

    fun registerListViewClick(view: View) {
        val musicQueue = Volley.newRequestQueue(view.context)
        val musicList = view.findViewById<ListView>(R.id.musiclistview)

        musicList.onItemClickListener = object : AdapterView.OnItemClickListener {
            override fun onItemClick(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                val itemValue = musicList.getItemAtPosition(p2) as HashMap<String, Any?>

                val title = itemValue.get("title") as String
                val artist = itemValue.get("artist") as String
                val version = itemValue.get("version") as String

                val coverImage = itemValue.get("primaryImage") as String

                val streamable = itemValue.get("streamable") as Boolean

                val settings = Settings(view.context)
                val downloadType = settings.getSetting("downloadType")

                if (streamable) {
                    val downloadLocation =
                        view.context.filesDir.toString() + "/" + artist + title + version + "." + downloadType

                    if (!File(downloadLocation).exists()) {
                        val streamHashUrl =
                            view.context.getString(R.string.loadSongsUrl) +"?albumId=" + itemValue.get("id")
                        val streamHashRequest = object : StringRequest(
                            Request.Method.GET, streamHashUrl,
                            Response.Listener<String> { response ->
                                val json = JSONObject(response)
                                val jsonArray = json.getJSONArray("results")

                                //trying to retreive streamHash
                                var streamHash = ""

                                //search entire album for corrent song
                                for (i in (0 until jsonArray.length())) {
                                    val searchSong = title + version
                                    if (jsonArray.getJSONObject(i).getString("title") + jsonArray.getJSONObject(i).getString(
                                            "version"
                                        ) == searchSong
                                    ) {
                                        streamHash =
                                            jsonArray.getJSONObject(i).getJSONObject("albums").getString("streamHash")
                                    }
                                }

                                if (streamHash != "") {
                                    MainActivity.musicPlayer!!.addSong(
                                        view.context.getString(R.string.songStreamUrl) + streamHash,
                                        title + " " + version,
                                        artist,
                                        coverImage
                                    )

                                    Toast.makeText(
                                        view.context,
                                        title + " " + version + " added to playlist!",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                }

                            },
                            Response.ErrorListener { println("Error!") }) {
                            @Throws(AuthFailureError::class)
                            override fun getHeaders(): Map<String, String> {
                                val params = HashMap<String, String>()
                                if (loggedIn) {
                                    params.put("Cookie", "connect.sid=" + sid)
                                }
                                return params
                            }
                        }

                        musicQueue.add(streamHashRequest)
                    } else {
                        MainActivity.musicPlayer!!.addSong(
                            downloadLocation,
                            title + " " + version, artist, coverImage
                        )
                    }
                } else {
                    Toast.makeText(view.context, "Song not yet streamable!", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }

    fun registerButtons(view: View) {
        val playButton = view.findViewById<ImageButton>(R.id.playButton)
        val backButton = view.findViewById<ImageButton>(R.id.backbutton)
        val nextButton = view.findViewById<ImageButton>(R.id.nextbutton)

        playButton.setOnClickListener {
            MainActivity.musicPlayer!!.toggleMusic()
        }

        nextButton.setOnClickListener {
            MainActivity.musicPlayer!!.next()
        }

        backButton.setOnClickListener {
            MainActivity.musicPlayer!!.previous()
        }
    }

    fun downloadSong(context: Context, listItem: HashMap<String, Any?>) {
        val id = listItem.get("songId") as String

        //TODO albumid == id is confusing
        val albumId = listItem.get("id") as String

        val title = listItem.get("title") as String
        val artist = listItem.get("artist") as String
        val version = listItem.get("version") as String
        val shownTitle = listItem.get("shownTitle") as String
        val downloadable = listItem.get("downloadable") as Boolean

        if (downloadable) {
            val settings = Settings(context)

            val downloadType = settings.getSetting("downloadType")
            val downloadQuality = settings.getSetting("downloadQuality")

            val downloadUrl =
                context.getString(R.string.songDownloadUrl) + albumId + "/download?method=download&type=" + downloadType + "_" + downloadQuality + "&track=" + id

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

    class downloadSong(url: String, location: String, sid: String, shownTitle: String, context: Context) :
        AsyncTask<Void, Void, String>() {

        //yeah this is not great
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