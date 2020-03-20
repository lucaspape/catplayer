package de.lucaspape.monstercat.ui.handlers

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
import de.lucaspape.monstercat.ui.activities.SettingsActivity
import de.lucaspape.monstercat.ui.activities.fragmentBackPressedCallback
import de.lucaspape.monstercat.database.helper.AlbumDatabaseHelper
import de.lucaspape.monstercat.database.helper.AlbumItemDatabaseHelper
import de.lucaspape.monstercat.database.helper.CatalogSongDatabaseHelper
import de.lucaspape.monstercat.database.helper.SongDatabaseHelper
import de.lucaspape.monstercat.download.addDownloadSong
import de.lucaspape.monstercat.music.*
import de.lucaspape.monstercat.ui.abstract_items.AlbumItem
import de.lucaspape.monstercat.ui.abstract_items.CatalogItem
import de.lucaspape.monstercat.ui.abstract_items.HeaderTextItem
import de.lucaspape.monstercat.ui.abstract_items.ProgressItem
import de.lucaspape.monstercat.request.async.*
import de.lucaspape.monstercat.music.util.playStream
import de.lucaspape.monstercat.twitch.Stream
import de.lucaspape.monstercat.util.*
import java.io.File
import java.lang.IndexOutOfBoundsException
import java.lang.ref.WeakReference
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

/**
 * Does everything for the home page
 */
class HomeHandler {
    companion object {
        @JvmStatic
        private var viewDataCache = HashMap<String, WeakReference<ArrayList<*>>>()

        @JvmStatic
        var addSongsTaskId = ""
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
        catalogViewData: ArrayList<CatalogItem>,
        cacheId: String,
        loadMoreListener: (itemAdapter: ItemAdapter<CatalogItem>, footerAdapter: ItemAdapter<ProgressItem>, currentPage: Int, callback: (newList: ArrayList<CatalogItem>) -> Unit) -> Unit,
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

        for (catalogItem in catalogViewData) {
            itemAdapter.add(
                catalogItem
            )
        }

        if (restoreScrollPosition) {
            restoreRecyclerViewPosition(view.context, "catalogView")
        }

        viewDataCache[cacheId] = WeakReference(catalogViewData as ArrayList<*>)

        /**
         * On song click
         */
        fastAdapter.onClickListener = { _, _, _, position ->
            val itemIndex = position + itemIndexOffset

            if (itemIndex >= 0 && itemIndex < catalogViewData.size) {
                val fistItem = catalogViewData[itemIndex]

                addSongsTaskId = ""

                clearPlaylist()
                clearQueue()

                songQueue.add(fistItem.songId)
                skipPreviousInPlaylist()
                next()

                val skipMonstercatSongs =
                    Settings(view.context).getBoolean(view.context.getString(R.string.skipMonstercatSongsSetting))

                val songDatabaseHelper = SongDatabaseHelper(view.context)

                if (albumId == null) {
                    //add next songs from database
                    val catalogSongDatabaseHelper = CatalogSongDatabaseHelper(view.context)

                    BackgroundAsync({
                        val id = UUID.randomUUID().toString()
                        addSongsTaskId = id

                        catalogSongDatabaseHelper.getIndexFromSongId(fistItem.songId)?.toLong()
                            ?.let { skip ->
                                val nextSongs = catalogSongDatabaseHelper.getSongs(skip)

                                nextSongs.reverse()

                                for (catalogSong in nextSongs) {

                                    if (skipMonstercatSongs == true) {
                                        val song =
                                            songDatabaseHelper.getSong(
                                                view.context,
                                                catalogSong.songId
                                            )
                                        if (!song?.artist.equals("monstercat", true)) {
                                            if (id == addSongsTaskId) {
                                                songQueue.add(catalogSong.songId)
                                            } else {
                                                break
                                            }
                                        }
                                    } else {
                                        if (id == addSongsTaskId) {
                                            songQueue.add(catalogSong.songId)
                                        } else {
                                            break
                                        }
                                    }

                                }
                            }
                    }, {}).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
                } else {
                    //add visible next songs
                    BackgroundAsync({
                        val id = UUID.randomUUID().toString()
                        addSongsTaskId = id

                        for (i in (itemIndex + 1 until catalogViewData.size)) {
                            try {
                                if (skipMonstercatSongs == true) {
                                    val song =
                                        songDatabaseHelper.getSong(
                                            view.context,
                                            catalogViewData[i].songId
                                        )

                                    if (!song?.artist.equals("monstercat", true)) {
                                        if (id == addSongsTaskId) {
                                            songQueue.add(catalogViewData[i].songId)
                                        } else {
                                            break
                                        }
                                    }
                                } else {
                                    if (id == addSongsTaskId) {
                                        songQueue.add(catalogViewData[i].songId)
                                    } else {
                                        break
                                    }
                                }

                            } catch (e: IndexOutOfBoundsException) {

                            }

                        }
                    }, {}).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
                }
            }

            false
        }

        /**
         * On song long click
         */
        fastAdapter.onLongClickListener = { _, _, _, position ->
            val itemIndex = position + itemIndexOffset

            if (itemIndex >= 0 && itemIndex < catalogViewData.size) {
                val idList = ArrayList<String>()

                for (catalogItem in catalogViewData) {
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

                if (itemIndex >= 0 && itemIndex < catalogViewData.size) {
                    val idList = ArrayList<String>()

                    for (catalogItem in catalogViewData) {
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
                loadMoreListener(itemAdapter, footerAdapter, currentPage) {
                    for (catalogItem in it) {
                        catalogViewData.add(catalogItem)
                    }

                    viewDataCache[cacheId] = WeakReference(catalogViewData as ArrayList<*>)
                }
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
        albumViewData: ArrayList<AlbumItem>,
        cacheId: String,
        loadMoreListener: (itemAdapter: ItemAdapter<AlbumItem>, footerAdapter: ItemAdapter<ProgressItem>, currentPage: Int, callback: (newList: ArrayList<AlbumItem>) -> Unit) -> Unit,
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

        for (albumItem in albumViewData) {
            itemAdapter.add(
                albumItem
            )
        }

        restoreRecyclerViewPosition(view.context, "albumView")

        viewDataCache[cacheId] = WeakReference(albumViewData as ArrayList<*>)

        val albumDatabaseHelper = AlbumDatabaseHelper(view.context)

        /**
         * On item click
         */
        fastAdapter.onClickListener = { _, _, _, position ->
            if (position < albumViewData.size) {
                saveRecyclerViewPosition(view.context, "albumView")

                val albumItem = albumViewData[position]

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
            if (position < albumViewData.size) {
                val albumMcIdList = ArrayList<String>()

                for (albumItem in albumViewData) {
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
                loadMoreListener(itemAdapter, footerAdapter, currentPage) {
                    for (albumItem in it) {
                        albumViewData.add(albumItem)
                    }

                    viewDataCache[cacheId] = WeakReference(albumViewData as ArrayList<*>)
                }
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
            playStream(Stream(view.context.getString(R.string.twitchClientID), "monstercat"))
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

        val finished: (catalogViewData: ArrayList<CatalogItem>) -> Unit = { catalogViewData ->
            updateCatalogRecyclerView(
                view,
                null,
                null,
                null,
                true,
                catalogViewData,
                "catalogView",
                { itemAdapter, footerAdapter, currentPage, callback ->
                    footerAdapter.clear()
                    footerAdapter.add(ProgressItem())

                    loadSongList(view, itemAdapter, footerAdapter, currentPage, callback)
                },
                { _, _ ->
                    initSongListLoad(view, true)
                },
                {
                    initSongListLoad(view, false)
                })

            swipeRefreshLayout.isRefreshing = false
        }

        var isEmpty = viewDataCache["catalogView"]?.get()?.isEmpty()

        if (isEmpty == null) {
            isEmpty = true
        }

        if (isEmpty || forceReload) {
            val catalogViewData = ArrayList<CatalogItem>()

            if (forceReload) {
                resetRecyclerViewPosition(view.context, "catalogView")
            }

            val catalogSongDatabaseHelper =
                CatalogSongDatabaseHelper(view.context)

            if (forceReload) {
                catalogSongDatabaseHelper.reCreateTable()
            }

            LoadSongListAsync(WeakReference(view.context), forceReload, 0, displayLoading = {
                swipeRefreshLayout.isRefreshing = true
            }, finishedCallback = { _, _, _ ->
                BackgroundAsync({
                    val itemList = catalogSongDatabaseHelper.getSongs(0, 50)

                    for (item in itemList) {
                        catalogViewData.add(CatalogItem(item.songId))
                    }

                }, {
                    finished(catalogViewData)
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
            viewDataCache["catalogView"]?.get()?.let {
                finished(it as ArrayList<CatalogItem>)
            }
        }
    }

    /**
     * Loads next 50 songs
     */
    private fun loadSongList(
        view: View,
        itemAdapter: ItemAdapter<CatalogItem>,
        footerAdapter: ItemAdapter<ProgressItem>,
        currentPage: Int,
        callback: (list: ArrayList<CatalogItem>) -> Unit
    ) {
        if (initDone) {
            LoadSongListAsync(WeakReference(view.context),
                false,
                (currentPage * 50),
                displayLoading = {},
                finishedCallback = { _, _, _ ->
                    val catalogSongDatabaseHelper =
                        CatalogSongDatabaseHelper(view.context)

                    val itemList =
                        catalogSongDatabaseHelper.getSongs((currentPage * 50).toLong(), 50)

                    val list = ArrayList<CatalogItem>()

                    for (item in itemList) {
                        val catalogItem = CatalogItem(item.songId)

                        itemAdapter.add(
                            catalogItem
                        )

                        list.add(catalogItem)
                    }

                    footerAdapter.clear()

                    callback(list)
                },
                errorCallback = { _, _, _ ->
                    footerAdapter.clear()

                    displaySnackbar(
                        view,
                        view.context.getString(R.string.errorLoadingSongList),
                        view.context.getString(R.string.retry)
                    ) {
                        loadSongList(view, itemAdapter, footerAdapter, currentPage, callback)
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

        val finished: (albumViewData: ArrayList<AlbumItem>) -> Unit = { albumViewData ->
            updateAlbumRecyclerView(
                view,
                albumViewData,
                "albumView",
                { itemAdapter, footerAdapter, currentPage, callback ->
                    footerAdapter.clear()
                    footerAdapter.add(ProgressItem())

                    loadAlbumList(view, itemAdapter, footerAdapter, currentPage, callback)
                },
                {
                    initAlbumListLoad(view, true)
                },
                {
                    initAlbumListLoad(view, false)
                })

            swipeRefreshLayout.isRefreshing = false
        }

        var isEmpty = viewDataCache["albumView"]?.get()?.isEmpty()

        if (isEmpty == null) {
            isEmpty = true
        }

        if (isEmpty || forceReload) {
            val albumViewData = ArrayList<AlbumItem>()

            if (forceReload) {
                resetRecyclerViewPosition(view.context, "albumView")
            }

            val contextReference = WeakReference(view.context)

            val albumDatabaseHelper = AlbumDatabaseHelper(view.context)

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

                        for (album in albumList) {
                            albumViewData.add(AlbumItem(album.albumId))
                        }
                    }, {
                        finished(albumViewData)
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
            viewDataCache["albumView"]?.get()?.let {
                finished(it as ArrayList<AlbumItem>)
            }
        }
    }

    /**
     * Loads next 50 albums
     */
    private fun loadAlbumList(
        view: View,
        itemAdapter: ItemAdapter<AlbumItem>,
        footerAdapter: ItemAdapter<ProgressItem>,
        currentPage: Int,
        callback: (newList: ArrayList<AlbumItem>) -> Unit
    ) {
        if (initDone) {
            LoadAlbumListAsync(WeakReference(view.context),
                false, (currentPage * 50), displayLoading = {}, finishedCallback = { _, _, _ ->
                    val albumDatabaseHelper =
                        AlbumDatabaseHelper(view.context)
                    val albumList =
                        albumDatabaseHelper.getAlbums((currentPage * 50).toLong(), 50)

                    val albumItemList = ArrayList<AlbumItem>()

                    for (album in albumList) {
                        val albumItem = AlbumItem(album.albumId)

                        itemAdapter.add(
                            albumItem
                        )

                        albumItemList.add(albumItem)
                    }

                    footerAdapter.clear()

                    callback(albumItemList)

                }, errorCallback = { _, _, _ ->
                    footerAdapter.clear()

                    displaySnackbar(
                        view,
                        view.context.getString(R.string.errorLoadingAlbumList),
                        view.context.getString(R.string.retry)
                    ) {
                        loadAlbumList(view, itemAdapter, footerAdapter, currentPage, callback)
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
        val catalogViewData = ArrayList<CatalogItem>()

        AlbumDatabaseHelper(view.context).getAlbum(albumId)?.let {
            albumName = it.title
        }

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

                    for (albumItem in albumItemList) {
                        catalogViewData.add(
                            CatalogItem(albumItem.songId)
                        )
                    }
                }, {
                    updateCatalogRecyclerView(
                        view,
                        albumName,
                        albumId,
                        mcId,
                        false,
                        catalogViewData,
                        "singleAlbum-$albumId",
                        { _, _, _, _ -> },
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

        LoadTitleSearchAsync(
            contextReference,
            searchString,
            0
            , finishedCallback = { _, _, searchResults ->
                updateCatalogRecyclerView(
                    view,
                    null,
                    null,
                    null,
                    false,
                    searchResults,
                    "search-$searchString",
                    { itemAdapter, footerAdapter, currentPage, callback ->
                        footerAdapter.clear()
                        footerAdapter.add(ProgressItem())

                        searchMore(
                            view,
                            searchString,
                            itemAdapter,
                            footerAdapter,
                            currentPage,
                            callback
                        )
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

                swipeRefreshLayout.isRefreshing = false
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

    }

    private fun searchMore(
        view: View,
        searchString: String,
        itemAdapter: ItemAdapter<CatalogItem>,
        footerAdapter: ItemAdapter<ProgressItem>,
        currentPage: Int,
        callback: (newList: ArrayList<CatalogItem>) -> Unit
    ) {
        val contextReference = WeakReference(view.context)

        val skip = currentPage * 50

        LoadTitleSearchAsync(
            contextReference,
            searchString,
            skip
            , finishedCallback = { _, _, searchResults ->
                for (result in searchResults) {
                    itemAdapter.add(result)
                }

                callback(searchResults)

                footerAdapter.clear()
            }, errorCallback = { _, _ ->
                footerAdapter.clear()

                displaySnackbar(
                    view,
                    view.context.getString(R.string.errorLoadingSearch),
                    view.context.getString(R.string.retry)
                ) {
                    searchMore(
                        view,
                        searchString,
                        itemAdapter,
                        footerAdapter,
                        currentPage,
                        callback
                    )
                }
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