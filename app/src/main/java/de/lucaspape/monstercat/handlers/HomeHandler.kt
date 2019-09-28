package de.lucaspape.monstercat.handlers

import android.app.AlertDialog
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.*
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.android.volley.*
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import de.lucaspape.monstercat.MainActivity
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.auth.sid
import de.lucaspape.monstercat.auth.loggedIn
import de.lucaspape.monstercat.cache.Cache
import de.lucaspape.monstercat.json.JSONParser
import de.lucaspape.monstercat.music.*
import de.lucaspape.monstercat.settings.Settings
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import java.lang.ref.WeakReference

/**
 * Does everything for the home page
 */
class HomeHandler {

    var currentListViewData = ArrayList<HashMap<String, Any?>>()

    //maximum songs loaded
    private val loadMax = 200

    private var simpleAdapter: SimpleAdapter? = null


    companion object{
        @JvmStatic var albumViewSelected = false
        @JvmStatic var albumView = false
    }


    fun setupListView(view: View) {
        updateListView(view)
        redrawListView(view)

        //setup auto reload
        Thread(Runnable {
            while (true) {
                Handler(Looper.getMainLooper()).post({ redrawListView(view) })
                Thread.sleep(1000)
            }

        }).start()
    }

    fun setupSpinner(view: View) {
        val viewSelector = view.findViewById<Spinner>(R.id.viewSelector)

        val selectorItems = arrayOf(
            view.context.getString(R.string.catalogView),
            view.context.getString(R.string.albumView)
        )
        val arrayAdapter = ArrayAdapter<Any?>(
            view.context,
            R.layout.support_simple_spinner_dropdown_item,
            selectorItems
        )

        viewSelector.adapter = arrayAdapter
    }

    fun setupMusicPlayer(view: View) {
        val textview1 = view.findViewById<TextView>(R.id.songCurrent1)
        val textview2 = view.findViewById<TextView>(R.id.songCurrent2)
        val coverBarImageView = view.findViewById<ImageView>(R.id.barCoverImage)
        val musicToolBar = view.findViewById<androidx.appcompat.widget.Toolbar>(R.id.muscicbar)
        val playButton = view.findViewById<ImageButton>(R.id.playButton)
        val seekBar = view.findViewById<SeekBar>(R.id.seekBar)

        val weakReference = WeakReference(view.context)

        //setup musicPlayer

        contextReference = (weakReference)
        setTextView(textview1, textview2)
        setSeekBar(seekBar)
        setBarCoverImageView(coverBarImageView)
        setMusicBar(musicToolBar)
        setPlayButton(playButton)
    }

    fun registerListeners(view: View) {
        //refresh
        val swipeRefreshLayout = view.findViewById<SwipeRefreshLayout>(R.id.pullToRefresh)
        swipeRefreshLayout.setOnRefreshListener {
            if(albumView) {
                loadAlbumList(view, true)
            }else{
                loadSongList(view, true)
            }
        }

        //music control buttons
        val playButton = view.findViewById<ImageButton>(R.id.playButton)
        val backButton = view.findViewById<ImageButton>(R.id.backbutton)
        val nextButton = view.findViewById<ImageButton>(R.id.nextbutton)

        playButton.setOnClickListener {
            toggleMusic()
        }

        nextButton.setOnClickListener {
            next()
        }

        backButton.setOnClickListener {
            previous()
        }

        //click on list
        val musicList = view.findViewById<ListView>(R.id.musiclistview)
        musicList.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            if (albumView) {
                val itemValue = musicList.getItemAtPosition(position) as HashMap<String, Any?>
                loadAlbum(view, itemValue, false)
            } else {
                val itemValue = musicList.getItemAtPosition(position) as HashMap<String, Any?>
                playSong(itemValue, true, view.context)
            }
        }

        val viewSelector = view.findViewById<Spinner>(R.id.viewSelector)

        if(albumViewSelected){
            viewSelector.setSelection(1)
        }else{
            viewSelector.setSelection(0)
        }

        val cache = Cache("homeCache", view.context)
        cache.save("albumViewSelected", albumViewSelected)

        viewSelector.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {

                if(albumViewSelected){
                    viewSelector.setSelection(1)
                }else{
                    viewSelector.setSelection(0)
                }

                updateListView(view)
            }

            override fun onItemSelected(
                parent: AdapterView<*>?,
                v: View?,
                position: Int,
                id: Long
            ) {
                when {
                    viewSelector.getItemAtPosition(position) == "Catalog View" -> {
                        albumView = false
                        albumViewSelected  = false
                        cache.save("albumViewSelected", albumViewSelected)
                    }
                    viewSelector.getItemAtPosition(position) == "Album View" -> {
                        albumView = true
                        albumViewSelected  = true
                        cache.save("albumViewSelected", albumViewSelected)
                    }
                }

                if (albumView) {
                    loadAlbumList(view, false)
                } else {
                    loadSongList(view, false)
                }
            }
        }
    }

    /**
     * Load single album
     */
    private fun loadAlbum(view: View, itemValue: HashMap<String, Any?>, forceReload: Boolean) {
        val albumId = itemValue["id"] as String

        val swipeRefreshLayout = view.findViewById<SwipeRefreshLayout>(R.id.pullToRefresh)
        swipeRefreshLayout.isRefreshing = true

        val requestQueue = Volley.newRequestQueue(view.context)

        //used to sort list
        val tempList = ArrayList<HashMap<String, Any?>>()

        val cache = Cache("homeCache", view.context)
        val albumCache = cache.load(albumId)

        if(albumCache != null && !forceReload){
            currentListViewData = albumCache as ArrayList<HashMap<String, Any?>>
            albumView = false

            updateListView(view)
            redrawListView(view)

            swipeRefreshLayout.isRefreshing = false

            //download cover art
            MainActivity.downloadHandler!!.addCoverArray(currentListViewData)
        }else{
            requestQueue.addRequestFinishedListener<Any> {
                cache.save(albumId, tempList)

                //display list
                currentListViewData = tempList
                albumView = false

                updateListView(view)
                redrawListView(view)

                swipeRefreshLayout.isRefreshing = false

                //download cover art
                MainActivity.downloadHandler!!.addCoverArray(currentListViewData)
            }

            val requestUrl =
                view.context.getString(R.string.loadSongsUrl) + "?albumId=" + albumId

            val listRequest = object : StringRequest(
                Method.GET, requestUrl, Response.Listener { response ->
                    val json = JSONObject(response)
                    val jsonArray = json.getJSONArray("results")

                    //parse every single song into list
                    for (k in (0 until jsonArray.length())) {
                        val jsonParser = JSONParser()
                        val hashMap =
                            jsonParser.parseCatalogSongsToHashMap(
                                jsonArray.getJSONObject(k),
                                view.context
                            )

                        tempList.add(hashMap)
                    }

                }, Response.ErrorListener { }
            ) {
                //add authentication
                @Throws(AuthFailureError::class)
                override fun getHeaders(): Map<String, String> {
                    val params = HashMap<String, String>()
                    if (loggedIn) {
                        params["Cookie"] = "connect.sid=$sid"
                    }
                    return params
                }
            }

            requestQueue.add(listRequest)
        }
    }

    private fun redrawListView(view: View) {
        val musicList = view.findViewById<ListView>(R.id.musiclistview)
        simpleAdapter!!.notifyDataSetChanged()
        musicList.invalidateViews()
        musicList.refreshDrawableState()
    }

    /**
     * Updates content
     */
    private fun updateListView(view: View) {
        val musicList = view.findViewById<ListView>(R.id.musiclistview)

        if (albumView) {
            val from = arrayOf("title", "primaryImage")
            val to = arrayOf(R.id.description, R.id.cover)
            simpleAdapter = SimpleAdapter(
                view.context,
                currentListViewData,
                R.layout.list_album_view,
                from,
                to.toIntArray()
            )
            musicList.adapter = simpleAdapter
        } else {
            val from = arrayOf("shownTitle", "secondaryImage")
            val to = arrayOf(R.id.title, R.id.cover)
            simpleAdapter = SimpleAdapter(
                view.context,
                currentListViewData,
                R.layout.list_single,
                from,
                to.toIntArray()
            )
            musicList.adapter = simpleAdapter
        }

    }

    fun loadSongList(view: View, forceReload: Boolean) {
        val swipeRefreshLayout = view.findViewById<SwipeRefreshLayout>(R.id.pullToRefresh)
        swipeRefreshLayout.isRefreshing = true

        val cache = Cache("homeCache", view.context)
        val loadedCache = cache.load("listView")

        if (loadedCache != null && !forceReload) {
            currentListViewData = loadedCache as ArrayList<HashMap<String, Any?>>
            updateListView(view)
            redrawListView(view)

            swipeRefreshLayout.isRefreshing = false

            //download cover art
            MainActivity.downloadHandler!!.addCoverArray(currentListViewData)
        } else {
            val requestQueue = Volley.newRequestQueue(view.context)

            //used to sort list
            val tempList = arrayOfNulls<HashMap<String, Any?>>(loadMax)

            //if all finished continue
            var finishedRequests = 0
            var totalRequestsCount = 0

            requestQueue.addRequestFinishedListener<Any> {
                finishedRequests++

                //check if all done
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
                    currentListViewData = sortedList
                    updateListView(view)
                    redrawListView(view)

                    swipeRefreshLayout.isRefreshing = false

                    //save to cache
                    cache.save("listView", currentListViewData)

                    //download cover art
                    MainActivity.downloadHandler!!.addCoverArray(currentListViewData)
                }
            }

            for (i in (0 until loadMax / 50)) {
                val requestUrl =
                    view.context.getString(R.string.loadSongsUrl) + "?limit=50&skip=" + i * 50

                val listRequest = object : StringRequest(
                    Method.GET, requestUrl, Response.Listener { response ->
                        val json = JSONObject(response)
                        val jsonArray = json.getJSONArray("results")

                        //parse every single song into list
                        for (k in (0 until jsonArray.length())) {
                            val jsonParser = JSONParser()
                            val hashMap =
                                jsonParser.parseCatalogSongsToHashMap(
                                    jsonArray.getJSONObject(k),
                                    view.context
                                )

                            tempList[i * 50 + k] = hashMap
                        }

                    }, Response.ErrorListener { }
                ) {
                    //add authentication
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
                requestQueue.add(listRequest)
            }
        }

    }

    fun loadAlbumList(view: View, forceReload: Boolean) {
        val swipeRefreshLayout = view.findViewById<SwipeRefreshLayout>(R.id.pullToRefresh)
        swipeRefreshLayout.isRefreshing = true

        val requestQueue = Volley.newRequestQueue(view.context)

        val tempList = arrayOfNulls<HashMap<String, Any?>>(loadMax)

        val cache = Cache("homeCache", view.context)
        val loadedCache = cache.load("albumListView") as ArrayList<HashMap<String,Any?>>?

        if(loadedCache != null && !forceReload){
            currentListViewData = loadedCache as ArrayList<HashMap<String, Any?>>
            updateListView(view)
            redrawListView(view)

            swipeRefreshLayout.isRefreshing = false

            //download cover art
            MainActivity.downloadHandler!!.addCoverArray(currentListViewData)
        }else{
            //if all finished continue
            var finishedRequests = 0
            var totalRequestsCount = 0

            requestQueue.addRequestFinishedListener<Any?> {
                finishedRequests++

                //check if all done
                if (finishedRequests >= totalRequestsCount) {

                    val sortedList = ArrayList<HashMap<String, Any?>>()

                    for (i in tempList.indices) {
                        if (tempList[i] != null) {
                            if (tempList.isNotEmpty()) {
                                sortedList.add(tempList[i]!!)
                            }
                        }
                    }

                    cache.save("albumListView", sortedList)

                    currentListViewData = sortedList
                    updateListView(view)
                    redrawListView(view)

                    swipeRefreshLayout.isRefreshing = false

                    //download cover art
                    MainActivity.downloadHandler!!.addCoverArray(currentListViewData)

                }

            }

            for (i in (0 until loadMax / 50)) {
                val requestUrl = view.context.getString(R.string.loadAlbumsUrl) + "?limit=50&skip=" + i * 50
                val albumsRequest = object: StringRequest(
                    Request.Method.GET, requestUrl,
                    Response.Listener { response ->
                        val json = JSONObject(response)
                        val jsonArray = json.getJSONArray("results")

                        for (k in (0 until jsonArray.length())) {
                            val jsonObject = jsonArray.getJSONObject(k)

                            val jsonParser = JSONParser()

                            tempList[i * 50 + k] = (jsonParser.parseAlbumViewToHashMap(jsonObject, view.context))
                        }
                    },
                    Response.ErrorListener { }
                ){
                    //add authentication
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
                requestQueue.add(albumsRequest)
            }
        }
    }

    fun playSong(song: HashMap<String, Any?>, playNow: Boolean, context: Context) {
        val settings = Settings(context)
        val downloadType = settings.getSetting("downloadType")

        //check if song is already downloaded
        val songDownloadLocation =
            context.filesDir.toString() + "/" + song["artist"] + song["title"] + song["version"] + "." + downloadType

        if (File(songDownloadLocation).exists()) {
            song["songDownloadLocation"] = songDownloadLocation
            if (playNow) {
                playNow(Song(song))
            } else {
                addSong(Song(song))
            }

        } else {
            //check if song can be streamed
            if (song["streamable"] as Boolean) {
                val streamHashQueue = Volley.newRequestQueue(context)

                //get stream hash
                val streamHashUrl =
                    context.getString(R.string.loadSongsUrl) + "?albumId=" + song["albumId"]

                val hashRequest = object : StringRequest(Method.GET, streamHashUrl,
                    Response.Listener { response ->
                        val jsonObject = JSONObject(response)

                        val jsonParser = JSONParser()
                        val streamHash = jsonParser.parseObjectToStreamHash(jsonObject, song)

                        if (streamHash != null) {
                            song["songStreamLocation"] =
                                context.getString(R.string.songStreamUrl) + streamHash
                            if (playNow) {
                                playNow(Song(song))
                            } else {
                                addSong(Song(song))
                            }
                        } else {
                            //could not find song
                        }
                    },
                    Response.ErrorListener { }) {
                    //add authentication
                    @Throws(AuthFailureError::class)
                    override fun getHeaders(): Map<String, String> {
                        val params = HashMap<String, String>()
                        if (loggedIn) {
                            params["Cookie"] = "connect.sid=$sid"
                        }
                        return params
                    }
                }

                streamHashQueue.add(hashRequest)
            } else {
                //fu no song for u
            }
        }
    }

    fun downloadSong(song: HashMap<String, Any?>, context: Context) {
        if (song["downloadable"] as Boolean) {
            val settings = Settings(context)

            val downloadType = settings.getSetting("downloadType")
            val downloadQuality = settings.getSetting("downloadQuality")

            val downloadUrl =
                context.getString(R.string.songDownloadUrl) + song["albumId"] as String + "/download?method=download&type=" + downloadType + "_" + downloadQuality + "&track=" + song["id"] as String

            val downloadLocation =
                context.filesDir.toString() + "/" + song["artist"] as String + song["title"] as String + song["version"] as String + "." + downloadType
            if (!File(downloadLocation).exists()) {
                if (sid != "") {
                    MainActivity.downloadHandler!!.addSong(
                        downloadUrl,
                        downloadLocation,
                        song["shownTitle"] as String
                    )
                } else {
                    //not signed in
                }
            } else {
                //already downloaded
            }
        } else {
            //not available
        }
    }

    fun addSongToPlaylist(song: HashMap<String, Any?>, context: Context) {
        var playlistNames = arrayOfNulls<String>(0)
        var playlistIds = arrayOfNulls<String>(0)
        var tracksArrays = arrayOfNulls<JSONArray>(0)

        val queue = Volley.newRequestQueue(context)

        //get all playlists
        val playlistUrl = context.getString(R.string.playlistsUrl)

        val playlistRequest = object : StringRequest(playlistUrl,
            Response.Listener { response ->
                val jsonObject = JSONObject(response)
                val jsonArray = jsonObject.getJSONArray("results")

                playlistNames = arrayOfNulls(jsonArray.length())
                playlistIds = arrayOfNulls(jsonArray.length())
                tracksArrays = arrayOfNulls(jsonArray.length())

                for (i in (0 until jsonArray.length())) {
                    val playlistObject = jsonArray.getJSONObject(i)

                    val playlistName = playlistObject.getString("name")
                    val playlistId = playlistObject.getString("_id")
                    val trackArray = playlistObject.getJSONArray("tracks")

                    playlistNames[i] = playlistName
                    playlistIds[i] = playlistId
                    tracksArrays[i] = trackArray
                }
            },

            Response.ErrorListener { _ ->

            }) {
            @Throws(AuthFailureError::class)
            override fun getHeaders(): Map<String, String> {
                val params = HashMap<String, String>()
                if (loggedIn) {
                    params["Cookie"] = "connect.sid=$sid"
                }
                return params
            }
        }

        queue.addRequestFinishedListener<Any> {
            val alertDialogBuilder = AlertDialog.Builder(context)
            alertDialogBuilder.setTitle(context.getString(R.string.pickPlaylistMsg))
            alertDialogBuilder.setItems(playlistNames) { _, i ->
                println(playlistNames[i])
                val playlistPatchUrl = context.getString(R.string.playlistUrl) + playlistIds[i]
                val patchParams = JSONObject()

                val trackArray = tracksArrays[i]

                val jsonParser = JSONParser()
                val patchedArray = jsonParser.parsePatchedPlaylist(trackArray!!, song)

                patchParams.put("tracks", JSONArray(patchedArray))

                val patchRequest =
                    object : JsonObjectRequest(
                        Method.PATCH,
                        playlistPatchUrl,
                        patchParams,
                        Response.Listener {
                            //TODO reload playlist
                        },
                        Response.ErrorListener {

                        }) {
                        @Throws(AuthFailureError::class)
                        override fun getHeaders(): Map<String, String> {
                            val params = HashMap<String, String>()
                            if (loggedIn) {
                                params["Cookie"] = "connect.sid=$sid"
                            }
                            return params
                        }
                    }

                val addToPlaylistQueue = Volley.newRequestQueue(context)
                addToPlaylistQueue.addRequestFinishedListener<Any> {
                    //TODO add msg
                }

                addToPlaylistQueue.add(patchRequest)
            }
            alertDialogBuilder.show()
        }

        queue.add(playlistRequest)
    }
}