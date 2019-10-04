package de.lucaspape.monstercat.handlers

import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.*
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.android.volley.*
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import de.lucaspape.monstercat.activities.MainActivity
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.auth.sid
import de.lucaspape.monstercat.auth.loggedIn
import de.lucaspape.monstercat.database.*
import de.lucaspape.monstercat.download.addDownloadCoverArray
import de.lucaspape.monstercat.json.JSONParser
import de.lucaspape.monstercat.music.*
import de.lucaspape.monstercat.settings.Settings
import org.json.JSONObject
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
                Handler(Looper.getMainLooper()).post { redrawListView(view) }
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
                playSongFromId(view.context, itemValue["id"] as String, true)
            }
        }

        val viewSelector = view.findViewById<Spinner>(R.id.viewSelector)

        if(albumViewSelected){
            viewSelector.setSelection(1)
        }else{
            viewSelector.setSelection(0)
        }

        val settings = Settings(view.context)
        settings.saveSetting("albumViewSelected", albumViewSelected.toString())

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
                        settings.saveSetting("albumViewSelected", albumViewSelected.toString())
                    }
                    viewSelector.getItemAtPosition(position) == "Album View" -> {
                        albumView = true
                        albumViewSelected  = true
                        settings.saveSetting("albumViewSelected", albumViewSelected.toString())
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

        val catalogSongsDatabaseHelper = CatalogSongsDatabaseHelper(view.context)
        var songIdList = catalogSongsDatabaseHelper.getAllSongs()

        if (!forceReload && songIdList.isNotEmpty()) {
            val dbSongs = ArrayList<HashMap<String, Any?>>()

            val songDatabaseHelper = SongDatabaseHelper(view.context)
            val songList = ArrayList<Song>()

            for(song in songIdList){
                songList.add(songDatabaseHelper.getSong(song.songId))
            }

            for(song in songList){
                val jsonParser = JSONParser()
                dbSongs.add(jsonParser.parseSongToHashMap(view.context, song))
            }

            //display list
            currentListViewData = dbSongs

            updateListView(view)
            redrawListView(view)

            swipeRefreshLayout.isRefreshing = false

            //download cover art
            addDownloadCoverArray(currentListViewData)
        } else {
            val requestQueue = Volley.newRequestQueue(view.context)

            val dbIds = ArrayList<Long>()

            //if all finished continue
            var finishedRequests = 0
            var totalRequestsCount = 0

            val sortedList = arrayOfNulls<Long>(loadMax)

            val requests = ArrayList<StringRequest>()

            requestQueue.addRequestFinishedListener<Any> {
                finishedRequests++

                //check if all done
                if (finishedRequests >= totalRequestsCount) {
                    val dbSongs = ArrayList<HashMap<String, Any?>>()

                    for(i in sortedList){
                        if(i != null){
                            if(catalogSongsDatabaseHelper.getCatalogSong(i) == null){
                                catalogSongsDatabaseHelper.insertSong(i)
                            }
                        }
                    }

                    songIdList = catalogSongsDatabaseHelper.getAllSongs()
                    val songDatabaseHelper = SongDatabaseHelper(view.context)
                    val songList = ArrayList<Song>()

                    for(song in songIdList){
                        songList.add(songDatabaseHelper.getSong(song.songId))
                    }

                    for(song in songList){
                        val jsonParser = JSONParser()
                        dbSongs.add(jsonParser.parseSongToHashMap(view.context, song))
                    }

                    //display list
                    currentListViewData = dbSongs
                    updateListView(view)
                    redrawListView(view)

                    swipeRefreshLayout.isRefreshing = false

                    //download cover art
                    addDownloadCoverArray(currentListViewData)
                }else{
                    requestQueue.add(requests[finishedRequests])
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

                            val dbId = jsonParser.parseCatalogSongToDB(jsonArray.getJSONObject(k), view.context)
                            dbIds.add(dbId)

                            sortedList[i * 50 + k] = dbId
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
                requests.add(listRequest)
            }

            requestQueue.add(requests[finishedRequests])
        }

    }

    fun loadAlbumList(view: View, forceReload: Boolean) {
        val swipeRefreshLayout = view.findViewById<SwipeRefreshLayout>(R.id.pullToRefresh)
        swipeRefreshLayout.isRefreshing = true

        val requestQueue = Volley.newRequestQueue(view.context)

        val tempList = arrayOfNulls<Long>(loadMax)

        val albumDatabaseHelper = AlbumDatabaseHelper(view.context)
        var albumList = albumDatabaseHelper.getAllAlbums()

        if(!forceReload && albumList.isNotEmpty()){
            val sortedList = ArrayList<HashMap<String, Any?>>()

            for(album in albumList){
                val jsonParser = JSONParser()
                sortedList.add(jsonParser.parseAlbumToHashMap(view.context, album))
            }

            currentListViewData = sortedList

            updateListView(view)
            redrawListView(view)

            swipeRefreshLayout.isRefreshing = false

            //download cover art
            addDownloadCoverArray(currentListViewData)
        }else{
            //if all finished continue
            var finishedRequests = 0
            var totalRequestsCount = 0

            val requests = ArrayList<StringRequest>()

            requestQueue.addRequestFinishedListener<Any?> {
                finishedRequests++

                //check if all done
                if (finishedRequests >= totalRequestsCount) {
                    val albums = ArrayList<Album>()

                    for(i in tempList){
                        if(i != null){
                            albums.add(albumDatabaseHelper.getAlbum(i))
                        }
                    }

                    val sortedList = ArrayList<HashMap<String, Any?>>()

                    for(album in albums){
                        val jsonParser = JSONParser()
                        sortedList.add(jsonParser.parseAlbumToHashMap(view.context, album))
                    }

                    currentListViewData = sortedList
                    updateListView(view)
                    redrawListView(view)

                    swipeRefreshLayout.isRefreshing = false

                    //download cover art
                    addDownloadCoverArray(currentListViewData)

                }else{
                    requestQueue.add(requests[finishedRequests])
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
                            tempList[i * 50 + k] = jsonParser.parseAlbumToDB(jsonObject, view.context)
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

                requests.add(albumsRequest)
            }

            requestQueue.add(requests[finishedRequests])
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
        val tempList = ArrayList<Long>()

        val songDatabaseHelper = SongDatabaseHelper(view.context)
        var songList = songDatabaseHelper.getAlbumSongs(albumId)

        if(!forceReload && songList.isNotEmpty()){
            // currentListViewData = albumCache as ArrayList<HashMap<String, Any?>>
            val dbSongs = ArrayList<HashMap<String, Any?>>()

            for(song in songList){
                val jsonParser = JSONParser()
                dbSongs.add(jsonParser.parseSongToHashMap(view.context, song))
            }

            currentListViewData = dbSongs

            albumView = false

            updateListView(view)
            redrawListView(view)

            swipeRefreshLayout.isRefreshing = false

            //download cover art
            addDownloadCoverArray(currentListViewData)
        }else{
            requestQueue.addRequestFinishedListener<Any> {
                val dbSongs = ArrayList<HashMap<String, Any?>>()
                songList = songDatabaseHelper.getAlbumSongs(albumId)

                for(song in songList){
                    val jsonParser = JSONParser()
                    dbSongs.add(jsonParser.parseSongToHashMap(view.context, song))
                }

                //display list
                currentListViewData = dbSongs
                albumView = false

                updateListView(view)
                redrawListView(view)

                swipeRefreshLayout.isRefreshing = false

                //download cover art
                addDownloadCoverArray(currentListViewData)
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
                        val songId =
                            jsonParser.parseCatalogSongToDB(
                                jsonArray.getJSONObject(k),
                                view.context
                            )

                        tempList.add(songId)
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
}