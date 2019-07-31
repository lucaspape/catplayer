package de.lucaspape.monstercat

import android.content.Context
import android.os.AsyncTask
import android.view.View
import android.widget.*
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.android.volley.*
import com.android.volley.toolbox.HttpHeaderParser
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import com.bumptech.glide.request.target.Target
import org.json.JSONException
import org.json.JSONObject
import java.io.*
import java.lang.Exception
import java.lang.reflect.InvocationTargetException

class HomeHandler {
    private var sid = ""

    fun loadTitlesFromCache(view: View) {
        val musicList = view.findViewById<ListView>(R.id.musiclistview)

        var list = ArrayList<HashMap<String, Any?>>()
        val listFile = File(view.context.cacheDir.toString() + "/" + "songs.list")

        val from = arrayOf("shownTitle", "coverUrl")
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
        val from = arrayOf("shownTitle", "coverUrl")
        val to = arrayOf(R.id.title, R.id.cover)
        var simpleAdapter = SimpleAdapter(view.context, list, R.layout.list_single, from, to.toIntArray())

        val swipeRefreshLayout = view.findViewById<SwipeRefreshLayout>(R.id.pullToRefresh)
        swipeRefreshLayout.setOnRefreshListener {
            var requestCount = 0

            val loadMax = 200
            val tempList = Array<HashMap<String, Any?>>(loadMax, { HashMap<String, Any?>() })
            list = ArrayList<HashMap<String, Any?>>()

            //can only load 50 at a time
            for (i in (0 until loadMax / 50)) {
                val url = "https://connect.monstercat.com/api/catalog/browse/?limit=50&skip=" + i * 50

                val stringRequest = object: StringRequest(
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
                                "coverUrl",
                                view.context.cacheDir.toString() + "/" + title + version + artist + ".png"
                            )
                            hashMap.put("version", version)

                            hashMap.put("shownTitle", artist + " " + title + " " + version)
                            hashMap.put("songId", songId)
                            hashMap.put("downloadable", downloadable)
                            hashMap.put("streamable", streamable)


                            if (!File(view.context.cacheDir.toString() + "/" + title + version + artist + ".png").exists()) {
                                MainActivity.downloadCover(
                                    coverUrl + "?image_width=64",
                                    view.context.cacheDir.toString() + "/" + title + version + artist + ".png",
                                    simpleAdapter
                                ).execute()
                            }

                            tempList[i * 50 + k] = hashMap

                        }

                    },
                    Response.ErrorListener { println("Error!") }){
                    @Throws(AuthFailureError::class)
                    override fun getHeaders(): Map<String, String> {
                        val params = HashMap<String, String>()
                        if (sid != "") {
                            params.put("Cookie", "connect.sid=" + sid)
                        }

                        return params
                    }
                }

                // Add the request to the RequestQueue.
                queue.add(stringRequest)
                requestCount++

            }

            var finishedRequest = 0
            queue.addRequestFinishedListener<Any> {
                finishedRequest++
                if (finishedRequest == requestCount) {
                    for (i in tempList.indices) {
                        list.add(tempList[i])
                    }

                    val oos = ObjectOutputStream(FileOutputStream(listFile))
                    oos.writeObject(list)
                    oos.flush()
                    oos.close()

                    simpleAdapter = SimpleAdapter(view.context, list, R.layout.list_single, from, to.toIntArray())
                    simpleAdapter.notifyDataSetChanged()
                    musicList.adapter = simpleAdapter

                    swipeRefreshLayout.isRefreshing = false
                }
            }
        }
    }

    fun login(view: View) {
        val settings = Settings(view.context)
        val username = settings.getSetting("email")
        val password = settings.getSetting("password")

        if (username != null || password != null) {
            val loginPostParams = JSONObject()
            loginPostParams.put("email", username)
            loginPostParams.put("password", password)

            val loginUrl = "https://connect.monstercat.com/v2/signin"


            val loginPostRequest = object : JsonObjectRequest(Request.Method.POST,
                loginUrl, loginPostParams, Response.Listener { response ->
                    val headers = response.getJSONObject("headers")

                    try {
                        //get SID
                        sid = headers.getString("Set-Cookie").substringBefore(';').replace("connect.sid=", "")
                    } catch (e: JSONException) {
                        println(headers)
                        println(e)
                    }

                }, Response.ErrorListener { error ->

                }) {
                override fun parseNetworkResponse(response: NetworkResponse?): Response<JSONObject> {
                    try {
                        val jsonResponse = JSONObject()
                        jsonResponse.put("headers", JSONObject(response!!.headers as Map<*, *>))

                        return Response.success(
                            jsonResponse,
                            HttpHeaderParser.parseCacheHeaders(response)
                        )
                    } catch (e: UnsupportedEncodingException) {

                        return Response.error<JSONObject>(ParseError(e))
                    }
                }
            }

            val loginQueue = Volley.newRequestQueue(view.context)
            loginQueue.add(loginPostRequest)
        }
    }

    fun registerListViewClick(view: View) {
        val musicQueue = Volley.newRequestQueue(view.context)
        val textview1 = view.findViewById<TextView>(R.id.songCurrent1)
        val textview2 = view.findViewById<TextView>(R.id.songCurrent2)

        val musicList = view.findViewById<ListView>(R.id.musiclistview)

        val seekBar = view.findViewById<SeekBar>(R.id.seekBar)

        if (MainActivity.musicPlayer == null) {
            MainActivity.musicPlayer = MusicPlayer(view.context, textview1, textview2, seekBar)
        } else {
            MainActivity.musicPlayer!!.setContext(view.context)
            MainActivity.musicPlayer!!.setTextView(textview1, textview2)
            MainActivity.musicPlayer!!.setSeekBar(seekBar)
        }

        musicList.onItemClickListener = object : AdapterView.OnItemClickListener {
            override fun onItemClick(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                val itemValue = musicList.getItemAtPosition(p2) as HashMap<String, Any?>

                val title = itemValue.get("title")
                val artist = itemValue.get("artist")
                val version = itemValue.get("version")

                val settings = Settings(view.context)
                val downloadType = settings.getSetting("downloadType")

                val downloadLocation = view.context.filesDir.toString() + "/" + artist + title + version + "." + downloadType

                if(!File(downloadLocation).exists()){
                    val streamHashUrl =
                        "https://connect.monstercat.com/api/catalog/browse/?albumId=" + itemValue.get("id")
                    val streamHashRequest = object : StringRequest(
                        Request.Method.GET, streamHashUrl,
                        Response.Listener<String> { response ->
                            val json = JSONObject(response)
                            val jsonArray = json.getJSONArray("results")
                            var streamHash = ""

                            for (i in (0 until jsonArray.length())) {
                                val searchSong = itemValue.get("title") as String + itemValue.get("version") as String
                                if (jsonArray.getJSONObject(i).getString("title") + jsonArray.getJSONObject(i).getString(
                                        "version"
                                    ) == searchSong
                                ) {
                                    if (jsonArray.getJSONObject(i).getBoolean("streamable")) {
                                        streamHash =
                                            jsonArray.getJSONObject(i).getJSONObject("albums").getString("streamHash")
                                    } else {
                                        Toast.makeText(view.context, "Song not yet streamable!", Toast.LENGTH_SHORT)
                                            .show()
                                    }
                                }
                            }

                            if (streamHash != "") {
                                MainActivity.musicPlayer!!.addSong(
                                    "https://s3.amazonaws.com/data.monstercat.com/blobs/" + streamHash,
                                    itemValue.get("artist") as String + " " + itemValue.get("title") as String + " " + itemValue.get(
                                        "version"
                                    ) as String
                                )
                                Toast.makeText(
                                    view.context,
                                    itemValue.get("title") as String + " " + itemValue.get("version") as String + " added to playlist!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                        },
                        Response.ErrorListener { println("Error!") }) {
                        @Throws(AuthFailureError::class)
                        override fun getHeaders(): Map<String, String> {
                            val params = HashMap<String, String>()
                            if (sid != "") {
                                params.put("Cookie", "connect.sid=" + sid)
                            }

                            return params
                        }
                    }

                    musicQueue.add(streamHashRequest)
                }else{
                    MainActivity.musicPlayer!!.addSong(
                        downloadLocation,
                        itemValue.get("artist") as String + " " + itemValue.get("title") as String + " " + itemValue.get(
                            "version"
                        ) as String
                    )
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
        val id = listItem.get("songId")
        val albumId = listItem.get("id")
        val title = listItem.get("title")
        val artist = listItem.get("artist")
        val coverUrl = listItem.get("coverUrl")
        val version = listItem.get("version")
        val shownTitle = listItem.get("shownTitle") as String
        val downloadable = listItem.get("downloadable") as Boolean

        if(downloadable){
            val settings = Settings(context)

            val downloadType = settings.getSetting("downloadType")
            val downloadQuality = settings.getSetting("downloadQuality")

            val downloadUrl =
                "https://connect.monstercat.com/api/release/" + albumId + "/download?method=download&type=" + downloadType + "_" +downloadQuality + "&track=" + id

            val downloadLocation = context.filesDir.toString() + "/" + artist + title + version + "." + downloadType
            if(!File(downloadLocation).exists()){
                if(sid != ""){
                    downloadSong(downloadUrl,downloadLocation, sid, shownTitle, context).execute()
                }else{
                    Toast.makeText(context, "Not signed in!", Toast.LENGTH_SHORT)
                        .show()
                }
            }else{
                Toast.makeText(context, shownTitle + " already downloaded!", Toast.LENGTH_SHORT)
                    .show()
            }
        }else{
            Toast.makeText(context, shownTitle + " download not available!", Toast.LENGTH_SHORT)
                .show()
        }
    }

    class downloadSong(url:String, location:String, sid:String, shownTitle:String, context: Context) : AsyncTask<Void, Void, String>() {
        val url = url
        val location = location
        val context = context
        val sid = sid
        val shownTitle = shownTitle

        override fun doInBackground(vararg params: Void?): String? {
            try {

                val glideUrl = GlideUrl(url, LazyHeaders.Builder()
                    .addHeader("Cookie", "connect.sid=" + sid).build())


                try{
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
                }catch (e: GlideException){
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