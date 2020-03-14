package de.lucaspape.monstercat.handlers

import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.view.View
import android.widget.*
import androidx.core.net.toUri
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.listeners.ClickEventHook
import com.mikepenz.fastadapter.scroll.EndlessRecyclerOnScrollListener
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.activities.SettingsActivity
import de.lucaspape.monstercat.database.helper.AlbumDatabaseHelper
import de.lucaspape.monstercat.database.helper.AlbumItemDatabaseHelper
import de.lucaspape.monstercat.database.helper.CatalogSongDatabaseHelper
import de.lucaspape.monstercat.database.helper.SongDatabaseHelper
import de.lucaspape.monstercat.download.addDownloadSong
import de.lucaspape.monstercat.handlers.abstract_items.AlbumItem
import de.lucaspape.monstercat.handlers.abstract_items.CatalogItem
import de.lucaspape.monstercat.handlers.abstract_items.HeaderTextItem
import de.lucaspape.monstercat.handlers.abstract_items.ProgressItem
import de.lucaspape.monstercat.handlers.async.*
import de.lucaspape.monstercat.music.clearQueue
import de.lucaspape.monstercat.music.playStream
import de.lucaspape.monstercat.util.*
import java.io.File
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

        @JvmStatic
        var currentCatalogViewData = ArrayList<CatalogItem>()
            private set

        @JvmStatic
        var currentAlbumViewData = ArrayList<AlbumItem>()
            private set

        //if contents of an album currently displayed
        @JvmStatic
        var albumContentsDisplayed = false
    }

    private var searchContentsDisplayed = false

    private var currentAlbumId = ""
    private var currentMCID = ""

    private var initDone = false

    private var recyclerView: RecyclerView? = null

    /**
     * Setup catalog view
     */
    private fun updateCatalogRecyclerView(view: View, headerText: String?) {
        recyclerView = view.findViewById(R.id.musiclistview)

        recyclerView?.layoutManager =
            LinearLayoutManager(view.context, LinearLayoutManager.VERTICAL, false)

        val itemAdapter = ItemAdapter<CatalogItem>()
        val headerAdapter = ItemAdapter<HeaderTextItem>()
        val footerAdapter = ItemAdapter<ProgressItem>()

        val fastAdapter: FastAdapter<GenericItem> =
            FastAdapter.with(listOf(headerAdapter, itemAdapter, footerAdapter))

        var itemIndexOffset = 0

        if (headerText != null) {
            headerAdapter.add(HeaderTextItem(headerText))
            itemIndexOffset = -1
        }

        recyclerView?.adapter = fastAdapter

        for (catalogItem in currentCatalogViewData) {
            itemAdapter.add(
                catalogItem
            )
        }

        restoreRecyclerViewPosition(view.context, "catalogView", recyclerView)

        /**
         * On song click
         */
        fastAdapter.onClickListener = { _, _, _, position ->
            val itemIndex = position + itemIndexOffset

            if (itemIndex >= 0) {
                clearQueue()

                val catalogItem = currentCatalogViewData[itemIndex]

                val nextSongIdsList = ArrayList<String>()

                for (i in (itemIndex + 1 until currentCatalogViewData.size)) {
                    try {
                        nextSongIdsList.add(currentCatalogViewData[i].songId)
                    } catch (e: IndexOutOfBoundsException) {

                    }
                }

                playSongFromId(
                    view.context,
                    catalogItem.songId,
                    true,
                    nextSongIdsList
                )
            }

            false
        }

        /**
         * On song long click
         */
        fastAdapter.onLongClickListener = { _, _, _, position ->
            val itemIndex = position + itemIndexOffset

            if (itemIndex >= 0) {
                val idList = ArrayList<String>()

                for (catalogItem in currentCatalogViewData) {
                    idList.add(catalogItem.songId)
                }

                CatalogItem.showContextMenu(view.context, idList, itemIndex)
            }

            false
        }

        /**
         * On menu button click
         */
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
                val itemIndex = position + itemIndexOffset

                if (itemIndex >= 0) {
                    val idList = ArrayList<String>()

                    for (catalogItem in currentCatalogViewData) {
                        idList.add(catalogItem.songId)
                    }

                    CatalogItem.showContextMenu(view.context, idList, itemIndex)
                }
            }
        })

        /**
         * On download button click
         */
        fastAdapter.addEventHook(object : ClickEventHook<CatalogItem>() {
            override fun onBind(viewHolder: RecyclerView.ViewHolder): View? {
                return if (viewHolder is CatalogItem.ViewHolder) {
                    viewHolder.titleDownloadButton
                } else null
            }

            override fun onClick(
                v: View,
                position: Int,
                fastAdapter: FastAdapter<CatalogItem>,
                item: CatalogItem
            ) {
                val songDatabaseHelper = SongDatabaseHelper(view.context)
                val song = songDatabaseHelper.getSong(view.context, item.songId)

                song?.let {
                    val titleDownloadButton = v as ImageButton

                    when {
                        File(song.downloadLocation).exists() -> {
                            File(song.downloadLocation).delete()
                            titleDownloadButton.setImageURI(song.getSongDownloadStatus().toUri())
                        }
                        File(song.streamDownloadLocation).exists() -> {
                            File(song.streamDownloadLocation).delete()
                            titleDownloadButton.setImageURI(song.getSongDownloadStatus().toUri())
                        }
                        else -> {
                            addDownloadSong(
                                v.context,
                                item.songId
                            ) {
                                titleDownloadButton.setImageURI(
                                    song.getSongDownloadStatus().toUri()
                                )
                            }
                        }
                    }
                }
            }
        })

        /**
         * On scroll down (load next)
         */
        recyclerView?.addOnScrollListener(object :
            EndlessRecyclerOnScrollListener(footerAdapter) {
            override fun onLoadMore(currentPage: Int) {
                if (!albumContentsDisplayed && !searchContentsDisplayed) {
                    footerAdapter.clear()
                    footerAdapter.add(ProgressItem())

                    loadSongList(view, itemAdapter)
                }
            }
        })

        BackgroundAsync({
            Thread.sleep(200)
        }, {
            initDone = true
        }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    /**
     * Setup album view
     */
    private fun updateAlbumRecyclerView(view: View) {
        recyclerView = view.findViewById(R.id.musiclistview)

        recyclerView?.layoutManager =
            LinearLayoutManager(view.context, LinearLayoutManager.VERTICAL, false)

        val itemAdapter = ItemAdapter<AlbumItem>()
        val footerAdapter = ItemAdapter<ProgressItem>()

        val fastAdapter: FastAdapter<GenericItem> =
            FastAdapter.with(listOf(itemAdapter, footerAdapter))

        recyclerView?.adapter = fastAdapter

        for (albumItem in currentAlbumViewData) {
            itemAdapter.add(
                albumItem
            )
        }

        restoreRecyclerViewPosition(view.context, "albumView", recyclerView)

        val albumDatabaseHelper = AlbumDatabaseHelper(view.context)

        /**
         * On item click
         */
        fastAdapter.onClickListener = { _, _, _, position ->
            saveRecyclerViewPosition(view.context, "albumView", recyclerView)

            val albumItem = currentAlbumViewData[position]

            albumDatabaseHelper.getAlbum(albumItem.albumId)?.mcID?.let { mcID ->
                loadAlbum(view, albumItem.albumId, mcID, false)
            }

            false
        }

        /**
         * On item long click
         */
        fastAdapter.onLongClickListener = { _, _, _, position ->
            val albumMcIdList = ArrayList<String>()

            for (albumItem in currentAlbumViewData) {
                albumDatabaseHelper.getAlbum(albumItem.albumId)?.mcID?.let { mcID ->
                    albumMcIdList.add(mcID)
                }
            }

            AlbumItem.showContextMenu(view.context, albumMcIdList, position)

            false
        }

        /**
         * On scroll down (load more)
         */
        recyclerView?.addOnScrollListener(object :
            EndlessRecyclerOnScrollListener(footerAdapter) {
            override fun onLoadMore(currentPage: Int) {
                if (!albumContentsDisplayed && !searchContentsDisplayed) {
                    footerAdapter.clear()
                    footerAdapter.add(ProgressItem())

                    loadAlbumList(view, itemAdapter)
                }
            }
        })

        BackgroundAsync({
            Thread.sleep(200)
        }, {
            initDone = true
        }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
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
        settings.setBoolean(
            view.context.getString(R.string.albumViewSelectedSetting),
            albumViewSelected
        )

        viewSelector.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {

                if (albumViewSelected) {
                    viewSelector.setSelection(1)
                    updateAlbumRecyclerView(view)
                } else {
                    viewSelector.setSelection(0)
                    updateCatalogRecyclerView(view, null)
                }
            }

            override fun onItemSelected(
                parent: AdapterView<*>?,
                v: View?,
                position: Int,
                id: Long
            ) {
                when {
                    viewSelector.getItemAtPosition(position) == view.context.getString(R.string.catalogView) -> {
                        albumView = false
                        albumViewSelected = false
                        settings.setBoolean(
                            view.context.getString(R.string.albumViewSelectedSetting),
                            albumViewSelected
                        )
                    }
                    viewSelector.getItemAtPosition(position) == view.context.getString(R.string.albumView) -> {
                        albumView = true
                        albumViewSelected = true
                        settings.setBoolean(
                            view.context.getString(R.string.albumViewSelectedSetting),
                            albumViewSelected
                        )
                    }
                }

                if (albumView) {
                    saveRecyclerViewPosition(view.context, "catalogView", recyclerView)
                    initAlbumListLoad(view, false)
                } else {
                    saveRecyclerViewPosition(view.context, "albumView", recyclerView)
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

            stream.getStreamInfo(view.context, "monstercat") { _, _, _, _ ->
                playStream(stream)
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

    private fun saveRecyclerViewPosition(
        context: Context,
        savePrefix: String,
        recyclerView: RecyclerView?
    ) {
        recyclerView?.let {
            val layoutManager = recyclerView.layoutManager as LinearLayoutManager

            val positionIndex = layoutManager.findFirstVisibleItemPosition()
            val startView = recyclerView.getChildAt(0)

            startView?.let {
                val topView = it.top - recyclerView.paddingTop

                val settings = Settings(context)
                settings.setInt("$savePrefix-positionIndex", positionIndex)
                settings.setInt("$savePrefix-topView", topView)
            }
        }
    }

    private fun restoreRecyclerViewPosition(
        context: Context,
        savePrefix: String,
        recyclerView: RecyclerView?
    ) {
        recyclerView?.let {
            val settings = Settings(context)
            settings.getInt("$savePrefix-positionIndex")?.let { positionIndex ->
                settings.getInt("$savePrefix-topView")?.let { topView ->
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    layoutManager.scrollToPositionWithOffset(positionIndex, topView)
                }
            }
        }
    }

    fun resetRecyclerViewPosition(context: Context, savePrefix: String) {
        val settings = Settings(context)
        settings.setInt("$savePrefix-positionIndex", 0)
        settings.setInt("$savePrefix-topView", 0)
    }

    /**
     * Loads first 50 catalog songs
     */
    fun initSongListLoad(view: View, forceReload: Boolean) {
        initDone = false

        val swipeRefreshLayout =
            view.findViewById<SwipeRefreshLayout>(R.id.pullToRefresh)

        if (currentCatalogViewData.size < 50 || forceReload || albumContentsDisplayed) {
            val catalogSongDatabaseHelper =
                CatalogSongDatabaseHelper(view.context)

            resetRecyclerViewPosition(view.context, "catalogView")
            currentCatalogViewData = ArrayList()

            if (forceReload) {
                catalogSongDatabaseHelper.reCreateTable()
            }

            LoadSongListAsync(WeakReference(view.context), forceReload, 0, {
                swipeRefreshLayout.isRefreshing = true
            }, {
                BackgroundAsync({
                    val songIdList = catalogSongDatabaseHelper.getSongs(0, 50)

                    for (i in (songIdList.size - 1 downTo 0)) {
                        currentCatalogViewData.add(CatalogItem(songIdList[i].songId))
                    }

                }, {
                    updateCatalogRecyclerView(view, null)

                    swipeRefreshLayout.isRefreshing = false
                    searchContentsDisplayed = false
                    albumContentsDisplayed = false
                }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
            }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        } else {
            updateCatalogRecyclerView(view, null)

            swipeRefreshLayout.isRefreshing = false
            searchContentsDisplayed = false
            albumContentsDisplayed = false
        }
    }

    /**
     * Loads next 50 songs
     */
    fun loadSongList(view: View, itemAdapter: ItemAdapter<CatalogItem>) {
        if (initDone) {
            LoadSongListAsync(WeakReference(view.context), false, currentCatalogViewData.size, {}, {
                val catalogSongDatabaseHelper =
                    CatalogSongDatabaseHelper(view.context)

                val songList =
                    catalogSongDatabaseHelper.getSongs(currentCatalogViewData.size.toLong(), 50)

                for (i in (songList.size - 1 downTo 0)) {
                    val catalogItem = CatalogItem(songList[i].songId)

                    itemAdapter.add(
                        catalogItem
                    )

                    currentCatalogViewData.add(catalogItem)
                }

                searchContentsDisplayed = false
                albumContentsDisplayed = false
            }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        }
    }

    /**
     * Loads first 50 albums
     */
    fun initAlbumListLoad(view: View, forceReload: Boolean) {
        initDone = false

        val swipeRefreshLayout =
            view.findViewById<SwipeRefreshLayout>(R.id.pullToRefresh)

        if (currentAlbumViewData.size < 50 || forceReload) {
            val contextReference = WeakReference(view.context)

            val albumDatabaseHelper = AlbumDatabaseHelper(view.context)

            if (forceReload) {
                currentCatalogViewData = ArrayList()
                resetRecyclerViewPosition(view.context, "albumView")
                albumDatabaseHelper.reCreateTable(view.context, false)
            }

            LoadAlbumListAsync(
                contextReference,
                forceReload,
                0, {
                    swipeRefreshLayout.isRefreshing = true
                }
            ) {
                BackgroundAsync({
                    val albumList = albumDatabaseHelper.getAlbums(0, 50)

                    for (i in (albumList.size - 1 downTo 0)) {
                        currentAlbumViewData.add(AlbumItem(albumList[i].albumId))
                    }
                }, {
                    updateAlbumRecyclerView(view)

                    swipeRefreshLayout.isRefreshing = false

                    searchContentsDisplayed = false
                    albumContentsDisplayed = false
                    swipeRefreshLayout.isRefreshing = false
                }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)

            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        } else {
            updateAlbumRecyclerView(view)

            swipeRefreshLayout.isRefreshing = false

            searchContentsDisplayed = false
            albumContentsDisplayed = false
            swipeRefreshLayout.isRefreshing = false
        }
    }

    /**
     * Loads next 50 albums
     */
    fun loadAlbumList(view: View, itemAdapter: ItemAdapter<AlbumItem>) {
        if (initDone) {
            LoadAlbumListAsync(WeakReference(view.context),
                false, currentAlbumViewData.size, {}, {
                    val albumDatabaseHelper =
                        AlbumDatabaseHelper(view.context)
                    val albumList =
                        albumDatabaseHelper.getAlbums(currentAlbumViewData.size.toLong(), 50)

                    for (album in albumList) {
                        val albumItem = AlbumItem(album.albumId)

                        itemAdapter.add(
                            albumItem
                        )

                        currentAlbumViewData.add(albumItem)
                    }

                    searchContentsDisplayed = false
                    albumContentsDisplayed = false
                }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        }
    }

    /**
     * Load single album
     */
    private fun loadAlbum(view: View, albumId: String, mcId: String, forceReload: Boolean) {
        val contextReference = WeakReference(view.context)

        val swipeRefreshLayout =
            view.findViewById<SwipeRefreshLayout>(R.id.pullToRefresh)

        var albumName = ""

        LoadAlbumAsync(
            contextReference,
            forceReload,
            albumId,
            mcId, {
                swipeRefreshLayout.isRefreshing = true
            }
        ) {
            BackgroundAsync({
                AlbumDatabaseHelper(view.context).getAlbum(albumId)?.let {
                    albumName = "${it.artist} - ${it.title}"
                }
                val albumItemDatabaseHelper =
                    AlbumItemDatabaseHelper(contextReference.get()!!, albumId)
                val albumItemList = albumItemDatabaseHelper.getAllData()

                val dbSongs = ArrayList<CatalogItem>()

                for (albumItem in albumItemList) {
                    dbSongs.add(
                        CatalogItem(albumItem.songId)
                    )
                }

                currentCatalogViewData = dbSongs
            }, {
                albumView = false

                updateCatalogRecyclerView(view, albumName)

                swipeRefreshLayout.isRefreshing = false

                searchContentsDisplayed = false
                albumContentsDisplayed = true

                currentAlbumId = albumId
                currentMCID = mcId
            }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)

        }.executeOnExecutor(
            AsyncTask.THREAD_POOL_EXECUTOR
        )
    }

    /**
     * Search for string TODO load more
     */
    fun searchSong(view: View, searchString: String) {
        //search can also be performed without this view
        val search = view.findViewById<SearchView>(R.id.homeSearch)
        search.onActionViewExpanded()
        search.setQuery(searchString, false)
        search.clearFocus()

        searchResults = ArrayList()

        val contextReference = WeakReference(view.context)

        val swipeRefreshLayout =
            view.findViewById<SwipeRefreshLayout>(R.id.pullToRefresh)

        swipeRefreshLayout.isRefreshing = true

        LoadTitleSearchAsync(
            contextReference,
            searchString,
            0
        ) {
            searchContentsDisplayed = true
            albumContentsDisplayed = false
            albumView = false

            currentCatalogViewData = searchResults

            updateCatalogRecyclerView(view, null)

            swipeRefreshLayout.isRefreshing = false
        }.executeOnExecutor(
            AsyncTask.THREAD_POOL_EXECUTOR
        )
    }

    fun onPause(context: Context) {
        if (albumView && !albumContentsDisplayed) {
            saveRecyclerViewPosition(context, "albumView", recyclerView)
        } else {
            saveRecyclerViewPosition(context, "catalogView", recyclerView)
        }
    }
}