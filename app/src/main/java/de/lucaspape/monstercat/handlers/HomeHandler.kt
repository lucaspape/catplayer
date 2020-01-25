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
        var searchResults = ArrayList<CatalogItem>()

        //if albumView selected in spinner
        @JvmStatic
        var albumViewSelected = false

        //for UI
        @JvmStatic
        var albumView = false
    }

    private var currentCatalogViewData = ArrayList<CatalogItem>()
    private var currentAlbumViewData = ArrayList<AlbumItem>()

    //if contents of an album currently displayed
    private var albumContentsDisplayed = false

    private var currentAlbumId = ""
    private var currentMCID = ""
    private var loaded = 0

    private fun updateCatalogRecyclerView(view: View, data: ArrayList<CatalogItem>) {
        val recyclerView = view.findViewById<RecyclerView>(R.id.musiclistview)

        recyclerView.layoutManager =
            LinearLayoutManager(view.context, LinearLayoutManager.VERTICAL, false)

        val itemAdapter = ItemAdapter<CatalogItem>()
        val fastAdapter = FastAdapter.with(itemAdapter)

        recyclerView.adapter = fastAdapter

        for (catalogItem in data) {
            itemAdapter.add(
                catalogItem
            )
        }

        fastAdapter.onClickListener = { _, _, _, position ->
            monstercatPlayer.clearContinuous()

            val catalogItem = data[position]

            val nextSongIdsList = ArrayList<String>()

            for (i in (position + 1 until data.size)) {
                nextSongIdsList.add(data[i].id)
            }

            playSongFromId(
                view.context,
                catalogItem.id,
                true,
                nextSongIdsList
            )

            false
        }

        fastAdapter.onLongClickListener = { _, _, _, position ->
            val idList = ArrayList<String>()

            for (catalogItem in data) {
                idList.add(catalogItem.id)
            }

            showContextMenu(view, idList, position)

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
                val idList = ArrayList<String>()

                for (catalogItem in data) {
                    idList.add(catalogItem.id)
                }

                showContextMenu(view, idList, position)
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

    private fun updateAlbumRecyclerView(view: View, data: ArrayList<AlbumItem>) {
        val recyclerView = view.findViewById<RecyclerView>(R.id.musiclistview)

        recyclerView.layoutManager =
            LinearLayoutManager(view.context, LinearLayoutManager.VERTICAL, false)

        val itemAdapter = ItemAdapter<AlbumItem>()
        val fastAdapter = FastAdapter.with(itemAdapter)

        recyclerView.adapter = fastAdapter

        for (albumItem in data) {
            itemAdapter.add(
                albumItem
            )
        }

        fastAdapter.onClickListener = { _, _, _, position ->
            val albumItem = data[position]
            loadAlbum(view, albumItem.albumId, albumItem.mcID, false)

            false
        }

        fastAdapter.onLongClickListener = { _, _, _, position ->
            val albumMcIdList = ArrayList<String>()

            for (albumItem in data) {
                albumMcIdList.add(albumItem.mcID)
            }

            showContextMenu(view, albumMcIdList, position)

            false
        }

        val footerAdapter = ItemAdapter<ProgressItem>()
        recyclerView.addOnScrollListener(object :
            EndlessRecyclerOnScrollListener(footerAdapter) {
            override fun onLoadMore(currentPage: Int) {
                if (!albumContentsDisplayed) {
                    footerAdapter.clear()
                    footerAdapter.add(ProgressItem())

                    loadAlbumList(view, itemAdapter)
                }
            }
        })
    }

    private fun showContextMenu(
        view: View,
        contentList: ArrayList<String>,
        listViewPosition: Int
    ) {
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
            val id = contentList[listViewPosition]

            view.context.let { context ->
                val songDatabaseHelper =
                    SongDatabaseHelper(context)
                val song = songDatabaseHelper.getSong(context, id)

                val item = menuItems[which]

                if (song != null) {
                    when (item) {
                        context.getString(R.string.download) -> downloadSong(
                            context,
                            song
                        )
                        context.getString(R.string.playNext) -> playSongFromId(
                            id,
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
                            id
                        )
                        context.getString(R.string.playAlbumNext) -> playAlbumNext(
                            context,
                            id
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
                    loadAlbum(view, currentAlbumId, currentMCID, true)
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
                    updateAlbumRecyclerView(view, currentAlbumViewData)
                } else {
                    viewSelector.setSelection(0)
                    updateCatalogRecyclerView(view, currentCatalogViewData)
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
        loaded = 0

        val swipeRefreshLayout =
            view.findViewById<SwipeRefreshLayout>(R.id.pullToRefresh)

        currentCatalogViewData = ArrayList()

        val catalogSongDatabaseHelper =
            CatalogSongDatabaseHelper(view.context)

        if (catalogSongDatabaseHelper.getSongs(0, 1).isEmpty() || forceReload) {
            LoadSongListAsync(WeakReference(view.context), true, 0, {}, {
                BackgroundAsync({
                    val songIdList = catalogSongDatabaseHelper.getSongs(0, 50)

                    val songDatabaseHelper =
                        SongDatabaseHelper(view.context)
                    val songList = ArrayList<Song>()

                    for (song in songIdList) {
                        songList.add(songDatabaseHelper.getSong(view.context, song.songId))
                    }

                    songList.reverse()

                    for (song in songList) {
                        currentCatalogViewData.add(parseSongToAbstractCatalogItem(song))
                        loaded++
                    }
                }, {
                    updateCatalogRecyclerView(view, currentCatalogViewData)

                    swipeRefreshLayout.isRefreshing = false
                }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
            }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        } else {
            BackgroundAsync({
                val songIdList = catalogSongDatabaseHelper.getSongs(0, 50)

                val songDatabaseHelper =
                    SongDatabaseHelper(view.context)

                for (i in (1 until songIdList.size + 1)) {
                    val catalogItem = parseSongToAbstractCatalogItem(
                        songDatabaseHelper.getSong(
                            view.context,
                            songIdList[songIdList.size - i].songId
                        )
                    )
                    currentCatalogViewData.add(catalogItem)
                    loaded++
                }

            }, {
                updateCatalogRecyclerView(view, currentCatalogViewData)

                swipeRefreshLayout.isRefreshing = false
            }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        }
    }

    fun loadSongList(view: View, itemAdapter: ItemAdapter<CatalogItem>) {
        LoadSongListAsync(WeakReference(view.context), true, loaded, {}, {
            val catalogSongDatabaseHelper =
                CatalogSongDatabaseHelper(view.context)

            val songList = catalogSongDatabaseHelper.getSongs(loaded.toLong(), 50)

            val songDatabaseHelper =
                SongDatabaseHelper(view.context)

            for (i in (1 until songList.size + 1)) {
                val catalogItem = parseSongToAbstractCatalogItem(
                    songDatabaseHelper.getSong(
                        view.context,
                        songList[songList.size - i].songId
                    )
                )

                itemAdapter.add(
                    catalogItem
                )

                currentCatalogViewData.add(catalogItem)

                loaded++
            }
        }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    fun loadAlbumList(view: View, itemAdapter: ItemAdapter<AlbumItem>) {
        LoadAlbumListAsync(WeakReference(view.context),
            true, loaded, {}, {
                val albumDatabaseHelper =
                    AlbumDatabaseHelper(view.context)
                val albumList = albumDatabaseHelper.getAlbums(loaded.toLong(), 50)

                for (album in albumList) {
                    val albumItem = parseAlbumToAbstractAlbumItem(album)

                    itemAdapter.add(
                        albumItem
                    )

                    currentAlbumViewData.add(albumItem)

                    loaded++
                }

                albumContentsDisplayed = false
            }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    fun initAlbumListLoad(view: View, forceReload: Boolean) {
        currentAlbumViewData = ArrayList()

        loaded = 0

        val contextReference = WeakReference<Context>(view.context)

        val swipeRefreshLayout =
            view.findViewById<SwipeRefreshLayout>(R.id.pullToRefresh)

        val albumDatabaseHelper = AlbumDatabaseHelper(view.context)

        if (albumDatabaseHelper.getAlbums(0, 1).isEmpty() || forceReload) {
            LoadAlbumListAsync(
                contextReference,
                true,
                0, {
                    swipeRefreshLayout.isRefreshing = true
                }
            ) {
                BackgroundAsync({
                    var albumList = albumDatabaseHelper.getAlbums(0, 50)

                    val sortedList = ArrayList<AlbumItem>()

                    albumList = albumList.reversed()

                    for (album in albumList) {
                        sortedList.add(parseAlbumToAbstractAlbumItem(album))
                        loaded++
                    }

                    currentAlbumViewData = sortedList
                }, {
                    updateAlbumRecyclerView(view, currentAlbumViewData)

                    swipeRefreshLayout.isRefreshing = false

                    albumContentsDisplayed = false

                    swipeRefreshLayout.isRefreshing = false
                }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)

            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        } else {
            BackgroundAsync({
                var albumList = albumDatabaseHelper.getAlbums(0, 50)

                val sortedList = ArrayList<AlbumItem>()

                albumList = albumList.reversed()

                for (album in albumList) {
                    sortedList.add(parseAlbumToAbstractAlbumItem(album))
                    loaded++
                }

                currentAlbumViewData = sortedList
            }, {
                updateAlbumRecyclerView(view, currentAlbumViewData)

                swipeRefreshLayout.isRefreshing = false

                albumContentsDisplayed = false

                swipeRefreshLayout.isRefreshing = false
            }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        }

    }

    /**
     * Load single album
     */
    private fun loadAlbum(view: View, albumId:String, mcId:String, forceReload: Boolean) {
        val contextReference = WeakReference<Context>(view.context)

        val swipeRefreshLayout =
            view.findViewById<SwipeRefreshLayout>(R.id.pullToRefresh)

        LoadAlbumAsync(
            contextReference,
            forceReload,
            albumId,
            mcId, {
                swipeRefreshLayout.isRefreshing = true
            }
        ) {
            BackgroundAsync({
                val albumItemDatabaseHelper =
                    AlbumItemDatabaseHelper(contextReference.get()!!, albumId)
                val albumItemList = albumItemDatabaseHelper.getAllData()

                val dbSongs = ArrayList<CatalogItem>()

                val songDatabaseHelper = SongDatabaseHelper(contextReference.get()!!)

                for (albumItem in albumItemList) {
                    contextReference.get()?.let { context ->
                        dbSongs.add(
                            parseSongToAbstractCatalogItem(
                                songDatabaseHelper.getSong(context, albumItem.songId)
                            )
                        )
                    }

                }

                currentCatalogViewData = dbSongs
            }, {
                albumView = false

                updateCatalogRecyclerView(view, currentCatalogViewData)

                swipeRefreshLayout.isRefreshing = false

                albumContentsDisplayed = true
                currentAlbumId = albumId
                currentMCID = mcId
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