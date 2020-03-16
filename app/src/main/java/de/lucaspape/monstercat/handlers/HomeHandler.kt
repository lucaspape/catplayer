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
import de.lucaspape.monstercat.activities.fragmentBackPressedCallback
import de.lucaspape.monstercat.database.helper.AlbumDatabaseHelper
import de.lucaspape.monstercat.database.helper.AlbumItemDatabaseHelper
import de.lucaspape.monstercat.database.helper.CatalogSongDatabaseHelper
import de.lucaspape.monstercat.database.helper.SongDatabaseHelper
import de.lucaspape.monstercat.download.addDownloadSong
import de.lucaspape.monstercat.handlers.abstract_items.AlbumItem
import de.lucaspape.monstercat.handlers.abstract_items.CatalogItem
import de.lucaspape.monstercat.handlers.abstract_items.HeaderTextItem
import de.lucaspape.monstercat.handlers.abstract_items.ProgressItem
import de.lucaspape.monstercat.request.async.*
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
        var catalogViewData = ArrayList<CatalogItem>()

        @JvmStatic
        var albumViewData = ArrayList<AlbumItem>()

        @JvmStatic
        var albumContentViewData = HashMap<String, ArrayList<CatalogItem>>()

        @JvmStatic
        var searchResultsData = HashMap<String, ArrayList<CatalogItem>>()
    }

    private var initDone = false

    private var recyclerView: RecyclerView? = null

    var onHomeHandlerPause: () -> Unit = {}

    /**
     * Setup catalog view
     */
    private fun updateCatalogRecyclerView(
        view: View,
        headerText: String?,
        albumId: String?,
        albumMcId: String?,
        restoreScrollPosition: Boolean,
        currentCatalogViewData: ArrayList<CatalogItem>,
        loadMoreListener: (itemAdapter: ItemAdapter<CatalogItem>, footerAdapter: ItemAdapter<ProgressItem>) -> Unit,
        refreshListener: (albumId: String?, albumMcId: String?) -> Unit,
        backPressedCallback: () -> Unit
    ) {
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

        if (restoreScrollPosition) {
            restoreRecyclerViewPosition(view.context, "catalogView")
        }

        /**
         * On song click
         */
        fastAdapter.onClickListener = { _, _, _, position ->
            val itemIndex = position + itemIndexOffset

            if (itemIndex >= 0 && itemIndex < currentCatalogViewData.size) {
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

            if (itemIndex >= 0 && itemIndex < currentCatalogViewData.size) {
                val idList = ArrayList<String>()

                for (catalogItem in currentCatalogViewData) {
                    idList.add(catalogItem.songId)
                }

                CatalogItem.showContextMenu(view, idList, itemIndex)
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

                if (itemIndex >= 0 && itemIndex < currentCatalogViewData.size) {
                    val idList = ArrayList<String>()

                    for (catalogItem in currentCatalogViewData) {
                        idList.add(catalogItem.songId)
                    }

                    CatalogItem.showContextMenu(view, idList, itemIndex)
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
                loadMoreListener(itemAdapter, footerAdapter)
            }
        })

        val swipeRefreshLayout = view.findViewById<SwipeRefreshLayout>(R.id.pullToRefresh)
        swipeRefreshLayout.setOnRefreshListener {
            refreshListener(albumId, albumMcId)
        }

        fragmentBackPressedCallback = backPressedCallback

        onHomeHandlerPause = {
            saveRecyclerViewPosition(view.context, "catalogView")
        }

        BackgroundAsync({
            Thread.sleep(200)
        }, {
            initDone = true
        }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    /**
     * Setup album view
     */
    private fun updateAlbumRecyclerView(
        view: View,
        currentAlbumViewData: ArrayList<AlbumItem>,
        loadMoreListener: (itemAdapter: ItemAdapter<AlbumItem>, footerAdapter: ItemAdapter<ProgressItem>) -> Unit,
        refreshListener: () -> Unit,
        backPressedCallback: () -> Unit
    ) {
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

        restoreRecyclerViewPosition(view.context, "albumView")

        val albumDatabaseHelper = AlbumDatabaseHelper(view.context)

        /**
         * On item click
         */
        fastAdapter.onClickListener = { _, _, _, position ->
            if (position < currentAlbumViewData.size) {
                saveRecyclerViewPosition(view.context, "albumView")

                val albumItem = currentAlbumViewData[position]

                albumDatabaseHelper.getAlbum(albumItem.albumId)?.mcID?.let { mcID ->
                    loadAlbum(view, albumItem.albumId, mcID, false)
                }

            }

            false
        }

        /**
         * On item long click
         */
        fastAdapter.onLongClickListener = { _, _, _, position ->
            if (position < currentAlbumViewData.size) {
                val albumMcIdList = ArrayList<String>()

                for (albumItem in currentAlbumViewData) {
                    albumDatabaseHelper.getAlbum(albumItem.albumId)?.mcID?.let { mcID ->
                        albumMcIdList.add(mcID)
                    }
                }

                AlbumItem.showContextMenu(view, albumMcIdList, position)
            }

            false
        }

        /**
         * On scroll down (load more)
         */
        recyclerView?.addOnScrollListener(object :
            EndlessRecyclerOnScrollListener(footerAdapter) {
            override fun onLoadMore(currentPage: Int) {
                loadMoreListener(itemAdapter, footerAdapter)
            }
        })

        val swipeRefreshLayout = view.findViewById<SwipeRefreshLayout>(R.id.pullToRefresh)
        swipeRefreshLayout.setOnRefreshListener {
            refreshListener()
        }

        fragmentBackPressedCallback = backPressedCallback

        onHomeHandlerPause = {
            saveRecyclerViewPosition(view.context, "albumView")
        }

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
        val viewSelector = view.findViewById<Spinner>(R.id.viewSelector)

        val settings = Settings(view.context)

        var albumViewSelected =
            settings.getBoolean(view.context.getString(R.string.albumViewSelectedSetting))

        if (albumViewSelected == null) {
            albumViewSelected = false
        }

        if (albumViewSelected) {
            viewSelector.setSelection(1)
        } else {
            viewSelector.setSelection(0)
        }

        viewSelector.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {

                if (albumViewSelected == true) {
                    viewSelector.setSelection(1)
                } else {
                    viewSelector.setSelection(0)
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
                        albumViewSelected = false

                        settings.setBoolean(
                            view.context.getString(R.string.albumViewSelectedSetting),
                            false
                        )

                        resetRecyclerViewPosition(view.context, "catalogView")
                        resetRecyclerViewPosition(view.context, "albumView")

                        initSongListLoad(view, false)
                    }
                    viewSelector.getItemAtPosition(position) == view.context.getString(R.string.albumView) -> {
                        albumViewSelected = true

                        settings.setBoolean(
                            view.context.getString(R.string.albumViewSelectedSetting),
                            true
                        )

                        resetRecyclerViewPosition(view.context, "catalogView")
                        resetRecyclerViewPosition(view.context, "albumView")

                        initAlbumListLoad(view, false)
                    }
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
            if (albumViewSelected == true) {
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
                query?.let {
                    searchSong(view, it, false)
                }

                return false
            }
        })
    }

    /**
     * Loads first 50 catalog songs
     */
    fun initSongListLoad(view: View, forceReload: Boolean) {
        initDone = false

        val swipeRefreshLayout =
            view.findViewById<SwipeRefreshLayout>(R.id.pullToRefresh)

        val displayData: () -> Unit = {
            updateCatalogRecyclerView(
                view,
                null,
                null,
                null,
                true,
                catalogViewData, { itemAdapter, footerAdapter ->
                    footerAdapter.clear()
                    footerAdapter.add(ProgressItem())

                    loadSongList(view, itemAdapter, footerAdapter)
                }, { _, _ ->
                    initSongListLoad(view, true)
                }, {
                    initSongListLoad(view, false)
                })

            swipeRefreshLayout.isRefreshing = false
        }

        if (catalogViewData.size < 50 || forceReload) {
            resetRecyclerViewPosition(view.context, "catalogView")

            val catalogSongDatabaseHelper =
                CatalogSongDatabaseHelper(view.context)

            catalogViewData = ArrayList()

            if (forceReload) {
                catalogSongDatabaseHelper.reCreateTable()
            }

            LoadSongListAsync(WeakReference(view.context), forceReload, 0, displayLoading = {
                swipeRefreshLayout.isRefreshing = true
            }, finishedCallback = { _, _, _ ->
                BackgroundAsync({
                    val songIdList = catalogSongDatabaseHelper.getSongs(0, 50)

                    for (i in (songIdList.size - 1 downTo 0)) {
                        catalogViewData.add(CatalogItem(songIdList[i].songId))
                    }

                }, {
                    displayData()
                }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
            }, errorCallback = { _, _, _ ->
                swipeRefreshLayout.isRefreshing = false

                displaySnackbar(
                    view,
                    view.context.getString(R.string.errorLoadingSongList),
                    view.context.getString(R.string.retry)
                ) {
                    initSongListLoad(view, forceReload)
                }
            }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        } else {
            displayData()
        }
    }

    /**
     * Loads next 50 songs
     */
    private fun loadSongList(
        view: View,
        itemAdapter: ItemAdapter<CatalogItem>,
        footerAdapter: ItemAdapter<ProgressItem>
    ) {
        if (initDone) {
            LoadSongListAsync(WeakReference(view.context),
                false,
                catalogViewData.size,
                displayLoading = {},
                finishedCallback = { _, _, _ ->
                    val catalogSongDatabaseHelper =
                        CatalogSongDatabaseHelper(view.context)

                    val songList =
                        catalogSongDatabaseHelper.getSongs(catalogViewData.size.toLong(), 50)

                    for (i in (songList.size - 1 downTo 0)) {
                        val catalogItem = CatalogItem(songList[i].songId)

                        itemAdapter.add(
                            catalogItem
                        )

                        catalogViewData.add(catalogItem)

                        footerAdapter.clear()
                    }
                },
                errorCallback = { _, _, _ ->
                    footerAdapter.clear()

                    displaySnackbar(
                        view,
                        view.context.getString(R.string.errorLoadingSongList),
                        view.context.getString(R.string.retry)
                    ) {
                        loadSongList(view, itemAdapter, footerAdapter)
                    }
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

        val displayData: () -> Unit = {
            updateAlbumRecyclerView(view, albumViewData, { itemAdapter, footerAdapter ->
                footerAdapter.clear()
                footerAdapter.add(ProgressItem())

                loadAlbumList(view, itemAdapter, footerAdapter)
            }, {
                initAlbumListLoad(view, true)
            }, {
                initAlbumListLoad(view, false)
            })

            swipeRefreshLayout.isRefreshing = false
        }

        if (albumViewData.size < 50 || forceReload) {
            resetRecyclerViewPosition(view.context, "albumView")

            val contextReference = WeakReference(view.context)

            val albumDatabaseHelper = AlbumDatabaseHelper(view.context)

            albumViewData = ArrayList()

            if (forceReload) {
                albumDatabaseHelper.reCreateTable(view.context, false)
            }

            LoadAlbumListAsync(
                contextReference,
                forceReload,
                0, displayLoading = {
                    swipeRefreshLayout.isRefreshing = true
                }
                , finishedCallback = { _, _, _ ->
                    BackgroundAsync({
                        val albumList = albumDatabaseHelper.getAlbums(0, 50)

                        for (i in (albumList.size - 1 downTo 0)) {
                            albumViewData.add(AlbumItem(albumList[i].albumId))
                        }
                    }, {
                        displayData()
                    }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)

                }, errorCallback = { _, _, _ ->
                    swipeRefreshLayout.isRefreshing = false

                    displaySnackbar(
                        view,
                        view.context.getString(R.string.errorLoadingAlbumList),
                        view.context.getString(R.string.retry)
                    ) {
                        initAlbumListLoad(view, forceReload)
                    }
                }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        } else {
            displayData()
        }
    }

    /**
     * Loads next 50 albums
     */
    private fun loadAlbumList(
        view: View,
        itemAdapter: ItemAdapter<AlbumItem>,
        footerAdapter: ItemAdapter<ProgressItem>
    ) {
        if (initDone) {
            LoadAlbumListAsync(WeakReference(view.context),
                false, albumViewData.size, displayLoading = {}, finishedCallback = { _, _, _ ->
                    val albumDatabaseHelper =
                        AlbumDatabaseHelper(view.context)
                    val albumList =
                        albumDatabaseHelper.getAlbums(albumViewData.size.toLong(), 50)

                    for (album in albumList) {
                        val albumItem = AlbumItem(album.albumId)

                        itemAdapter.add(
                            albumItem
                        )

                        albumViewData.add(albumItem)

                        footerAdapter.clear()
                    }

                }, errorCallback = { _, _, _ ->
                    footerAdapter.clear()

                    displaySnackbar(
                        view,
                        view.context.getString(R.string.errorLoadingAlbumList),
                        view.context.getString(R.string.retry)
                    ) {
                        loadAlbumList(view, itemAdapter, footerAdapter)
                    }
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

        val displayData: () -> Unit = {
            albumContentViewData[albumId]?.let {
                updateCatalogRecyclerView(
                    view,
                    albumName,
                    albumId,
                    mcId,
                    false,
                    it,
                    { _, _ -> },
                    { albumId, albumMcId ->
                        albumId?.let {
                            albumMcId?.let {
                                loadAlbum(view, albumId, albumMcId, true)
                            }
                        }
                    },
                    {
                        initAlbumListLoad(view, false)
                    }
                )

                swipeRefreshLayout.isRefreshing = false
            }
        }

        AlbumDatabaseHelper(view.context).getAlbum(albumId)?.let {
            albumName = it.title
        }

        if (albumContentViewData[albumId] == null || forceReload) {
            LoadAlbumAsync(
                contextReference,
                forceReload,
                albumId,
                mcId, displayLoading = {
                    swipeRefreshLayout.isRefreshing = true
                }
                , finishedCallback = { _, _, _, _ ->
                    BackgroundAsync({
                        val albumItemDatabaseHelper =
                            AlbumItemDatabaseHelper(contextReference.get()!!, albumId)
                        val albumItemList = albumItemDatabaseHelper.getAllData()

                        val dbSongs = ArrayList<CatalogItem>()

                        for (albumItem in albumItemList) {
                            dbSongs.add(
                                CatalogItem(albumItem.songId)
                            )
                        }

                        albumContentViewData[albumId] = dbSongs
                    }, {
                        displayData()
                    }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)

                }, errorCallback = { _, _, _, _ ->
                    swipeRefreshLayout.isRefreshing = false

                    displaySnackbar(
                        view,
                        view.context.getString(R.string.errorLoadingAlbum),
                        view.context.getString(R.string.retry)
                    ) {
                        loadAlbum(view, albumId, mcId, forceReload)
                    }
                }).executeOnExecutor(
                AsyncTask.THREAD_POOL_EXECUTOR
            )
        } else {
            displayData()
        }
    }

    /**
     * Search for string
     */
    fun searchSong(view: View, searchString: String, forceReload: Boolean) {
        //search can also be performed without this view
        val search = view.findViewById<SearchView>(R.id.homeSearch)
        search.onActionViewExpanded()
        search.setQuery(searchString, false)
        search.clearFocus()

        val contextReference = WeakReference(view.context)

        val swipeRefreshLayout =
            view.findViewById<SwipeRefreshLayout>(R.id.pullToRefresh)

        swipeRefreshLayout.isRefreshing = true

        val displayData: () -> Unit = {
            searchResultsData[searchString]?.let {
                updateCatalogRecyclerView(
                    view,
                    null,
                    null,
                    null,
                    false,
                    it,
                    { itemAdapter, footerAdapter ->
                        footerAdapter.clear()
                        footerAdapter.add(ProgressItem())

                        searchMore(view, searchString, itemAdapter, footerAdapter)
                    },
                    { _, _ ->
                        searchSong(view, searchString, true)
                    },
                    {
                        if (Settings(view.context).getBoolean(view.context.getString(R.string.albumViewSelectedSetting)) == true) {
                            initAlbumListLoad(view, false)
                        } else {
                            initSongListLoad(view, false)
                        }
                    })
            }

            swipeRefreshLayout.isRefreshing = false
        }

        if (searchResultsData[searchString] == null || forceReload) {
            LoadTitleSearchAsync(
                contextReference,
                searchString,
                0
                , finishedCallback = { _, _, searchResults ->
                    searchResultsData[searchString] = searchResults

                    displayData()
                }, errorCallback = { _, _ ->
                    swipeRefreshLayout.isRefreshing = false

                    displaySnackbar(
                        view,
                        view.context.getString(R.string.errorLoadingSearch),
                        view.context.getString(R.string.retry)
                    ) { searchSong(view, searchString, forceReload) }
                }).executeOnExecutor(
                AsyncTask.THREAD_POOL_EXECUTOR
            )
        } else {
            displayData()
        }
    }

    private fun searchMore(
        view: View,
        searchString: String,
        itemAdapter: ItemAdapter<CatalogItem>,
        footerAdapter: ItemAdapter<ProgressItem>
    ) {
        val contextReference = WeakReference(view.context)

        var skip = searchResultsData[searchString]?.size

        if (skip == null) {
            skip = 0
        }

        LoadTitleSearchAsync(
            contextReference,
            searchString,
            skip
            , finishedCallback = { _, _, searchResults ->
                for (result in searchResults) {
                    itemAdapter.add(result)
                    searchResultsData[searchString]?.add(result)
                }

                footerAdapter.clear()
            }, errorCallback = { _, _ ->
                footerAdapter.clear()

                displaySnackbar(
                    view,
                    view.context.getString(R.string.errorLoadingSearch),
                    view.context.getString(R.string.retry)
                ) { searchMore(view, searchString, itemAdapter, footerAdapter) }
            }).executeOnExecutor(
            AsyncTask.THREAD_POOL_EXECUTOR
        )
    }

    private fun saveRecyclerViewPosition(
        context: Context,
        savePrefix: String
    ) {
        recyclerView?.let {
            val layoutManager = it.layoutManager as LinearLayoutManager

            val positionIndex = layoutManager.findFirstVisibleItemPosition()
            val startView = it.getChildAt(0)

            startView?.let { sView ->
                val topView = sView.top - sView.paddingTop

                val settings = Settings(context)
                settings.setInt("$savePrefix-positionIndex", positionIndex)
                settings.setInt("$savePrefix-topView", topView)
            }
        }
    }

    private fun restoreRecyclerViewPosition(
        context: Context,
        savePrefix: String
    ) {
        recyclerView?.let {
            val settings = Settings(context)
            settings.getInt("$savePrefix-positionIndex")?.let { positionIndex ->
                settings.getInt("$savePrefix-topView")?.let { topView ->
                    val layoutManager = it.layoutManager as LinearLayoutManager
                    layoutManager.scrollToPositionWithOffset(positionIndex, topView)
                }
            }
        }
    }

    private fun resetRecyclerViewPosition(context: Context, savePrefix: String) {
        val settings = Settings(context)
        settings.setInt("$savePrefix-positionIndex", 0)
        settings.setInt("$savePrefix-topView", 0)
    }
}