package de.lucaspape.monstercat.handlers

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.view.View
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.listeners.ClickEventHook
import com.mikepenz.fastadapter.scroll.EndlessRecyclerOnScrollListener
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.activities.SettingsActivity
import de.lucaspape.monstercat.activities.monstercatPlayer
import de.lucaspape.monstercat.database.CatalogSong
import de.lucaspape.monstercat.database.Song
import de.lucaspape.monstercat.database.helper.AlbumDatabaseHelper
import de.lucaspape.monstercat.database.helper.AlbumItemDatabaseHelper
import de.lucaspape.monstercat.database.helper.CatalogSongDatabaseHelper
import de.lucaspape.monstercat.database.helper.SongDatabaseHelper
import de.lucaspape.monstercat.handlers.abstract_items.AlbumItem
import de.lucaspape.monstercat.handlers.abstract_items.CatalogItem
import de.lucaspape.monstercat.handlers.abstract_items.ProgressItem
import de.lucaspape.monstercat.handlers.async.*
import de.lucaspape.monstercat.music.*
import de.lucaspape.monstercat.util.*
import java.lang.ref.WeakReference

/**
 * Does everything for the home page
 */
class HomeHandler {

    companion object {
        @JvmStatic
        var searchResults = ArrayList<HashMap<String, Any?>>()

        //if albumView selected in spinner
        @JvmStatic
        var albumViewSelected = false

        //for UI
        @JvmStatic
        var albumView = false
    }

    private var currentListViewData = ArrayList<HashMap<String, Any?>>()

    //if contents of an album currently displayed
    private var albumContentsDisplayed = false

    private var currentAlbumId = ""
    private var currentMCID = ""

    private fun updateCatalogRecyclerView(view: View, data: ArrayList<HashMap<String, Any?>>){
        val recyclerView = view.findViewById<RecyclerView>(R.id.musiclistview)

        recyclerView.layoutManager =
            LinearLayoutManager(view.context, LinearLayoutManager.VERTICAL, false)

        val itemAdapter = ItemAdapter<CatalogItem>()
        val fastAdapter = FastAdapter.with(itemAdapter)

        for (hashMap in data) {
            itemAdapter.add(
                parseHashMapToAbstractCatalogItem(hashMap)
            )
        }

        recyclerView.adapter = fastAdapter

        fastAdapter.onClickListener = { _, _, _, position ->
            monstercatPlayer.clearContinuous()

            val itemValue = data[position] as HashMap<*, *>
            playSongFromId(
                view.context,
                itemValue["id"] as String,
                true,
                data,
                position
            )

            false
        }

        fastAdapter.onLongClickListener = { _, _, _, position ->
            showContextMenu(view, data, position)

            false
        }

        fastAdapter.addEventHook(object : ClickEventHook<CatalogItem>() {
            override fun onBind(viewHolder: RecyclerView.ViewHolder): View? {
                return if (viewHolder is CatalogItem.ViewHolder) {
                    viewHolder.titleMenuButton
                } else null
            }

            override fun onClick(
                v: View,
                position: Int,
                fastAdapter: FastAdapter<CatalogItem>,
                item: CatalogItem
            ) {
                showContextMenu(view, data, position)
            }
        })

        val footerAdapter = ItemAdapter<ProgressItem>()
        recyclerView.addOnScrollListener(object :
            EndlessRecyclerOnScrollListener(footerAdapter) {
            override fun onLoadMore(currentPage: Int) {
                if (!albumContentsDisplayed) {
                    footerAdapter.clear()
                    footerAdapter.add(ProgressItem())

                    loadSongList(view, itemAdapter)
                }
            }
        })
    }

    private fun updateAlbumRecyclerView(view: View, data:ArrayList<HashMap<String, Any?>>){
        val recyclerView = view.findViewById<RecyclerView>(R.id.musiclistview)

        recyclerView.layoutManager =
            LinearLayoutManager(view.context, LinearLayoutManager.VERTICAL, false)

        val itemAdapter = ItemAdapter<AlbumItem>()
        val fastAdapter = FastAdapter.with(itemAdapter)

        for (hashMap in data) {
            itemAdapter.add(
                parseHashMapToAbstractAlbumItem(hashMap)
            )
        }

        recyclerView.adapter = fastAdapter

        fastAdapter.onClickListener = { _, _, _, position ->
            val itemValue = data[position] as HashMap<*, *>
            loadAlbum(view, itemValue, false)

            false
        }

        fastAdapter.onLongClickListener = { _, _, _, position ->
            showContextMenu(view, data, position)

            false
        }

        val footerAdapter = ItemAdapter<ProgressItem>()
        recyclerView.addOnScrollListener(object :
            EndlessRecyclerOnScrollListener(footerAdapter) {
            override fun onLoadMore(currentPage: Int) {
                if(!albumContentsDisplayed){
                    footerAdapter.clear()
                    footerAdapter.add(ProgressItem())

                    loadAlbumList(view, itemAdapter)
                }
            }
        })
    }

    private fun showContextMenu(view: View, contentList:ArrayList<HashMap<String, Any?>> ,listViewPosition: Int) {
        val menuItems: Array<String> = if (!albumView) {
            arrayOf(
                view.context.getString(R.string.download),
                view.context.getString(R.string.playNext),
                view.context.getString(R.string.addToPlaylist)
            )
        } else {
            arrayOf(
                view.context.getString(R.string.downloadAlbum),
                view.context.getString(R.string.playAlbumNext)
            )
        }

        val alertDialogBuilder = AlertDialog.Builder(view.context)
        alertDialogBuilder.setTitle("")
        alertDialogBuilder.setItems(menuItems) { _, which ->
            val listItem = contentList[listViewPosition] as HashMap<*, *>

            view.context.let { context ->
                val songDatabaseHelper =
                    SongDatabaseHelper(context)
                val song = songDatabaseHelper.getSong(context, listItem["id"] as String)

                val item = menuItems[which]

                if (song != null) {
                    when (item) {
                        context.getString(R.string.download) -> downloadSong(
                            context,
                            song
                        )
                        context.getString(R.string.playNext) -> playSongFromId(
                            listItem["id"].toString(),
                            false
                        )
                        context.getString(R.string.addToPlaylist) -> addSongToPlaylist(
                            context,
                            song
                        )
                    }
                } else {
                    when (item) {
                        context.getString(R.string.downloadAlbum) -> downloadAlbum(
                            context,
                            listItem["mcID"].toString()
                        )
                        context.getString(R.string.playAlbumNext) -> playAlbumNext(
                            context,
                            listItem["mcID"].toString()
                        )
                    }
                }
            }
        }

        alertDialogBuilder.create().show()
    }

    /**
     * Album/Catalog view selector
     */
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

    /**
     * Listeners (buttons, refresh etc)
     */
    fun registerListeners(view: View) {
        //refresh
        val swipeRefreshLayout = view.findViewById<SwipeRefreshLayout>(R.id.pullToRefresh)
        swipeRefreshLayout.setOnRefreshListener {
            if (albumView || albumContentsDisplayed) {
                if (albumContentsDisplayed) {
                    val itemValue = HashMap<String, Any?>()
                    itemValue["id"] = currentAlbumId
                    itemValue["mcID"] = currentMCID
                    loadAlbum(view, itemValue, true)
                } else {
                    initAlbumListLoad(view, true)
                }
            } else {
                initSongListLoad(view, true)
            }
        }

        val viewSelector = view.findViewById<Spinner>(R.id.viewSelector)

        if (albumViewSelected) {
            viewSelector.setSelection(1)
        } else {
            viewSelector.setSelection(0)
        }

        val settings = Settings(view.context)
        settings.saveSetting("albumViewSelected", albumViewSelected.toString())

        viewSelector.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {

                if (albumViewSelected) {
                    viewSelector.setSelection(1)
                    updateAlbumRecyclerView(view, currentListViewData)
                } else {
                    viewSelector.setSelection(0)
                    updateCatalogRecyclerView(view, currentListViewData)
                }
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
                        albumViewSelected = false
                        settings.saveSetting("albumViewSelected", albumViewSelected.toString())
                    }
                    viewSelector.getItemAtPosition(position) == "Album View" -> {
                        albumView = true
                        albumViewSelected = true
                        settings.saveSetting("albumViewSelected", albumViewSelected.toString())
                    }
                }

                if (albumView) {
                    initAlbumListLoad(view, false)
                } else {
                    initSongListLoad(view, false)
                }
            }
        }

        //settings button
        view.findViewById<ImageButton>(R.id.settingsButton).setOnClickListener {
            view.context.startActivity(Intent(view.context, SettingsActivity::class.java))
        }

        //livestream button
        view.findViewById<ImageButton>(R.id.liveButton).setOnClickListener {
            val stream =
                de.lucaspape.monstercat.twitch.Stream(view.context.getString(R.string.twitchClientID))

            stream.getStreamInfo(view.context, "monstercat") {
                playStream(it)
            }
        }

        //search
        val search = view.findViewById<SearchView>(R.id.homeSearch)

        search.setOnCloseListener {
            if (albumView) {
                initAlbumListLoad(view, false)
            } else {
                initSongListLoad(view, false)
            }

            false
        }

        search.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }

            override fun onQueryTextSubmit(query: String?): Boolean {
                albumView = false
                albumViewSelected = false

                query?.let {
                    searchSong(view, it)
                }

                return false
            }

        })
    }

    fun initSongListLoad(view: View, forceReload: Boolean) {
        val swipeRefreshLayout =
            view.findViewById<SwipeRefreshLayout>(R.id.pullToRefresh)

        currentListViewData = ArrayList()

        val catalogSongDatabaseHelper =
            CatalogSongDatabaseHelper(view.context)

        if(catalogSongDatabaseHelper.getAllSongs().isEmpty() || forceReload){
            LoadSongListAsync(WeakReference(view.context), true, 0, {}, {
                BackgroundAsync({
                    val songIdList = catalogSongDatabaseHelper.getAllSongs()

                    val songDatabaseHelper =
                        SongDatabaseHelper(view.context)
                    val songList = ArrayList<Song>()

                    for (song in songIdList) {
                        songList.add(songDatabaseHelper.getSong(view.context, song.songId))
                    }

                    songList.reverse()

                    for (song in songList) {
                        val hashMap = parseSongToHashMap(song)
                        currentListViewData.add(hashMap)
                    }
                },{
                    updateCatalogRecyclerView(view, currentListViewData)

                    swipeRefreshLayout.isRefreshing = false
                }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
            }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        }else{
            BackgroundAsync({
                val songIdList = catalogSongDatabaseHelper.getAllSongs()

                val songDatabaseHelper =
                    SongDatabaseHelper(view.context)
                val songList = ArrayList<Song>()

                for (song in songIdList) {
                    songList.add(songDatabaseHelper.getSong(view.context, song.songId))
                }

                songList.reverse()

                for (song in songList) {
                    val hashMap = parseSongToHashMap(song)
                    currentListViewData.add(hashMap)
                }
            },{
                updateCatalogRecyclerView(view, currentListViewData)

                swipeRefreshLayout.isRefreshing = false
            }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        }
    }

    fun loadSongList(view: View, itemAdapter: ItemAdapter<CatalogItem>) {
        val loaded = CatalogSongDatabaseHelper(view.context).getAllSongs().size

        LoadSongListAsync(WeakReference(view.context), true, loaded, {}, {
            val catalogSongDatabaseHelper =
                CatalogSongDatabaseHelper(view.context)

            val allSongs = catalogSongDatabaseHelper.getAllSongs()
            val songIdList = ArrayList<CatalogSong>()

            for (i in (0 until allSongs.size - loaded)) {
                songIdList.add(allSongs[i])
            }

            val songDatabaseHelper =
                SongDatabaseHelper(view.context)
            val songList = ArrayList<Song>()

            for (song in songIdList) {
                songList.add(songDatabaseHelper.getSong(view.context, song.songId))
            }

            songList.reverse()

            for (song in songList) {
                val hashMap = parseSongToHashMap(song)

                itemAdapter.add(
                    parseHashMapToAbstractCatalogItem(hashMap)
                )
                currentListViewData.add(hashMap)
            }
        }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    fun loadAlbumList(view: View, itemAdapter: ItemAdapter<AlbumItem>) {
        val loaded = AlbumDatabaseHelper(view.context).getAllAlbums().size

        LoadAlbumListAsync(WeakReference(view.context),
            true, loaded, {}, {
                val albumDatabaseHelper =
                    AlbumDatabaseHelper(view.context)
                val allAlbums = albumDatabaseHelper.getAllAlbums()

                val sortedList = ArrayList<HashMap<String, Any?>>()

                val albumList = ArrayList<de.lucaspape.monstercat.database.Album>()

                for (i in (0 until allAlbums.size - loaded)) {
                    albumList.add(allAlbums[i])
                }

                for (album in albumList) {
                    val hashMap = parseAlbumToHashMap(view.context, album)

                    sortedList.add(hashMap)

                    itemAdapter.add(
                        parseHashMapToAbstractAlbumItem(hashMap)
                    )

                    currentListViewData.add(hashMap)
                }

                albumContentsDisplayed = false
            }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    fun initAlbumListLoad(view: View, forceReload: Boolean) {
        val contextReference = WeakReference<Context>(view.context)

        val swipeRefreshLayout =
            view.findViewById<SwipeRefreshLayout>(R.id.pullToRefresh)

        val albumDatabaseHelper = AlbumDatabaseHelper(view.context)

        if(albumDatabaseHelper.getAllAlbums().isEmpty() || forceReload){
            LoadAlbumListAsync(
                contextReference,
                true,
                0, {
                    swipeRefreshLayout.isRefreshing = true
                }
            ) {
                BackgroundAsync({
                    var albumList = albumDatabaseHelper.getAllAlbums()

                    val sortedList = ArrayList<HashMap<String, Any?>>()

                    albumList = albumList.reversed()

                    for (album in albumList) {
                        sortedList.add(parseAlbumToHashMap(view.context, album))
                    }

                    currentListViewData = sortedList
                }, {
                    updateAlbumRecyclerView(view, currentListViewData)

                    swipeRefreshLayout.isRefreshing = false

                    albumContentsDisplayed = false

                    swipeRefreshLayout.isRefreshing = false
                }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)

            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        }else{
            BackgroundAsync({
                var albumList = albumDatabaseHelper.getAllAlbums()

                val sortedList = ArrayList<HashMap<String, Any?>>()

                albumList = albumList.reversed()

                for (album in albumList) {
                    sortedList.add(parseAlbumToHashMap(view.context, album))
                }

                currentListViewData = sortedList
            }, {
                updateAlbumRecyclerView(view, currentListViewData)

                swipeRefreshLayout.isRefreshing = false

                albumContentsDisplayed = false

                swipeRefreshLayout.isRefreshing = false
            }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        }

    }

    /**
     * Load single album
     */
    private fun loadAlbum(view: View, itemValue: HashMap<*, *>, forceReload: Boolean) {
        val contextReference = WeakReference<Context>(view.context)

        val swipeRefreshLayout =
            view.findViewById<SwipeRefreshLayout>(R.id.pullToRefresh)

        LoadAlbumAsync(
            contextReference,
            forceReload,
            itemValue, {
                swipeRefreshLayout.isRefreshing = true
            }
        ) {
            BackgroundAsync({
                val albumId = itemValue["id"] as String

                val albumItemDatabaseHelper =
                    AlbumItemDatabaseHelper(contextReference.get()!!, albumId)
                val albumItemList = albumItemDatabaseHelper.getAllData()

                val dbSongs = ArrayList<HashMap<String, Any?>>()

                val songDatabaseHelper = SongDatabaseHelper(contextReference.get()!!)

                for (albumItem in albumItemList) {
                    contextReference.get()?.let { context ->
                        dbSongs.add(
                            parseSongToHashMap(
                                songDatabaseHelper.getSong(context, albumItem.songId)
                            )
                        )
                    }

                }

                currentListViewData = dbSongs
            }, {
                albumView = false

                updateCatalogRecyclerView(view, currentListViewData)

                swipeRefreshLayout.isRefreshing = false

                albumContentsDisplayed = true
                currentAlbumId = itemValue["id"] as String
                currentMCID = itemValue["mcID"] as String
            }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)

        }.executeOnExecutor(
            AsyncTask.THREAD_POOL_EXECUTOR
        )
    }

    //search for string
    fun searchSong(view: View, searchString: String) {
        val contextReference = WeakReference<Context>(view.context)

        val swipeRefreshLayout =
            view.findViewById<SwipeRefreshLayout>(R.id.pullToRefresh)

        swipeRefreshLayout.isRefreshing = true

        LoadTitleSearchAsync(
            contextReference,
            searchString,
            0
        ) {
            updateCatalogRecyclerView(view, searchResults)

            albumContentsDisplayed = false

            swipeRefreshLayout.isRefreshing = false
        }.executeOnExecutor(
            AsyncTask.THREAD_POOL_EXECUTOR
        )
    }
}