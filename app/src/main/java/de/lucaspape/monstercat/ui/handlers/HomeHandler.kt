package de.lucaspape.monstercat.ui.handlers

import android.content.Context
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
import de.lucaspape.monstercat.database.helper.AlbumDatabaseHelper
import de.lucaspape.monstercat.database.helper.AlbumItemDatabaseHelper
import de.lucaspape.monstercat.database.helper.CatalogSongDatabaseHelper
import de.lucaspape.monstercat.database.helper.SongDatabaseHelper
import de.lucaspape.monstercat.download.addDownloadSong
import de.lucaspape.monstercat.ui.abstract_items.AlbumItem
import de.lucaspape.monstercat.ui.abstract_items.CatalogItem
import de.lucaspape.monstercat.ui.abstract_items.HeaderTextItem
import de.lucaspape.monstercat.ui.abstract_items.ProgressItem
import de.lucaspape.monstercat.request.async.*
import de.lucaspape.monstercat.music.util.playStream
import de.lucaspape.monstercat.twitch.Stream
import de.lucaspape.monstercat.util.*
import de.lucaspape.util.BackgroundAsync
import de.lucaspape.util.Cache
import de.lucaspape.util.CustomSpinnerClass
import de.lucaspape.util.Settings
import de.lucaspape.util.Settings.Companion.getSettings
import java.io.File
import java.lang.ref.WeakReference
import kotlin.collections.ArrayList

/**
 * Does everything for the home page, pass albumMcId to open album on init
 */
class HomeHandler(
    private val onSearch: (searchString: String?) -> Unit,
    private val openSettings:() -> Unit,
    private val albumMcId: String?
) : Handler {
    companion object {
        @JvmStatic
        var addSongsTaskId = ""
    }

    private var initDone = false

    private var recyclerView: RecyclerView? = null

    var onFragmentPause: () -> Unit = {}
    var onFragmentBack: () -> Unit = {}

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
        recyclerView = view.findViewById(R.id.homeRecyclerView)

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

        val cache = Cache()
        cache.set(cacheId, catalogViewData)

        /**
         * On song click
         */
        fastAdapter.onClickListener = { _, _, _, position ->
            val itemIndex = position + itemIndexOffset

            if (itemIndex >= 0 && itemIndex < catalogViewData.size) {
                val fistItem = catalogViewData[itemIndex]

                val skipMonstercatSongs =
                    Settings(view.context).getBoolean(view.context.getString(R.string.skipMonstercatSongsSetting)) == true

                if (albumId == null) {
                    playSongsFromCatalogDb(view.context, skipMonstercatSongs, fistItem.songId)
                } else {
                    playSongsFromViewData(view.context, skipMonstercatSongs, catalogViewData, itemIndex)
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

                    cache.set(cacheId, catalogViewData)
                }
            }
        })

        val swipeRefreshLayout = view.findViewById<SwipeRefreshLayout>(R.id.homePullToRefresh)
        swipeRefreshLayout.setOnRefreshListener {
            refreshListener(albumId, albumMcId)
        }

        onFragmentBack = backPressedCallback

        onFragmentPause = {
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
        recyclerView = view.findViewById(R.id.homeRecyclerView)

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

        val cache = Cache()
        cache.set(cacheId, albumViewData)

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

                    cache.set(cacheId, albumViewData)
                }
            }
        })

        val swipeRefreshLayout = view.findViewById<SwipeRefreshLayout>(R.id.homePullToRefresh)
        swipeRefreshLayout.setOnRefreshListener {
            refreshListener()
        }

        onFragmentBack = backPressedCallback

        onFragmentPause = {
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
    private fun setupSpinner(view: View) {
        val viewSelector = view.findViewById<CustomSpinnerClass>(R.id.viewSelector)

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
    private fun registerListeners(view: View): Boolean {
        val viewSelector = view.findViewById<CustomSpinnerClass>(R.id.viewSelector)

        val settings = getSettings(view.context)

        var albumViewSelected =
            settings.getBoolean(view.context.getString(R.string.albumViewSelectedSetting))

        if (albumViewSelected == null) {
            albumViewSelected = false
        }

        if (albumViewSelected) {
            viewSelector.programmaticallySetPosition(1, false)
        } else {
            viewSelector.programmaticallySetPosition(0, false)
        }

        var selected = 0

        viewSelector.setOnItemSelectedListener(object : CustomSpinnerClass.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {

                if (albumViewSelected == true) {
                    viewSelector.programmaticallySetPosition(1, false)
                } else {
                    viewSelector.programmaticallySetPosition(0, false)
                }
            }

            override fun onItemSelected(
                parent: AdapterView<*>?,
                v: View?,
                position: Int,
                id: Long,
                userSelected: Boolean
            ) {
                //dont call on initial change, only if from user
                if (++selected > 1 && userSelected) {
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
        })

        //settings button
        view.findViewById<ImageButton>(R.id.settingsButton).setOnClickListener {
            openSettings()
        }

        //livestream button
        view.findViewById<ImageButton>(R.id.liveButton).setOnClickListener {
            playStream(Stream(view.context.getString(R.string.twitchClientID), view.context.getString(R.string.twitchChannel)))
        }

        view.findViewById<ImageButton>(R.id.searchButton).setOnClickListener {
            onSearch(null)
        }

        return albumViewSelected == true
    }

    /**
     * Loads first 50 catalog songs
     */
    fun initSongListLoad(view: View, forceReload: Boolean) {
        initDone = false

        val swipeRefreshLayout =
            view.findViewById<SwipeRefreshLayout>(R.id.homePullToRefresh)

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

        val cache = Cache()
        var isEmpty = cache.get<ArrayList<CatalogItem>>("catalogView")?.isEmpty()

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

                displaySnackBar(
                    view,
                    view.context.getString(R.string.errorLoadingSongList),
                    view.context.getString(R.string.retry)
                ) {
                    initSongListLoad(view, forceReload)
                }
            }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        } else {
            cache.get<ArrayList<CatalogItem>>("catalogView")?.let(finished)
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

                    displaySnackBar(
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
            view.findViewById<SwipeRefreshLayout>(R.id.homePullToRefresh)

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

        val cache = Cache()
        var isEmpty = cache.get<ArrayList<AlbumItem>>("albumView")?.isEmpty()

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

                    displaySnackBar(
                        view,
                        view.context.getString(R.string.errorLoadingAlbumList),
                        view.context.getString(R.string.retry)
                    ) {
                        initAlbumListLoad(view, forceReload)
                    }
                }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        } else {
            cache.get<ArrayList<AlbumItem>>("albumView")?.let(finished)
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

                    displaySnackBar(
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
            view.findViewById<SwipeRefreshLayout>(R.id.homePullToRefresh)

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
                        AlbumItemDatabaseHelper(view.context, albumId)
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

                displaySnackBar(
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

                val settings = getSettings(context)
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
            val settings = getSettings(context)
            settings.getInt("$savePrefix-positionIndex")?.let { positionIndex ->
                settings.getInt("$savePrefix-topView")?.let { topView ->
                    val layoutManager = it.layoutManager as LinearLayoutManager
                    layoutManager.scrollToPositionWithOffset(positionIndex, topView)
                }
            }
        }
    }

    private fun resetRecyclerViewPosition(context: Context, savePrefix: String) {
        val settings = getSettings(context)
        settings.setInt("$savePrefix-positionIndex", 0)
        settings.setInt("$savePrefix-topView", 0)
    }

    override fun onBackPressed(view: View) {
        onFragmentBack()
    }

    override fun onPause(view: View) {
        onFragmentPause()
    }

    override val layout: Int = R.layout.fragment_home

    override fun onCreate(view: View) {
        setupSpinner(view)

        if (albumMcId != null) {
            //open album
            val albumViewSelected = registerListeners(view)

            val swipeRefreshLayout =
                view.findViewById<SwipeRefreshLayout>(R.id.homePullToRefresh)

            swipeRefreshLayout.isRefreshing = true

            loadAlbumTracks(view.context, albumMcId, finishedCallback = { _ ->
                val albumDatabaseHelper = AlbumDatabaseHelper(view.context)
                albumDatabaseHelper.getAlbumFromMcId(albumMcId)?.let { album ->
                    val albumItemDatabaseHelper =
                        AlbumItemDatabaseHelper(view.context, album.albumId)
                    val albumItemList = albumItemDatabaseHelper.getAllData()

                    val catalogViewData = ArrayList<CatalogItem>()

                    for (albumItem in albumItemList) {
                        catalogViewData.add(
                            CatalogItem(albumItem.songId)
                        )
                    }

                    swipeRefreshLayout.isRefreshing = false

                    updateCatalogRecyclerView(
                        view,
                        album.title,
                        album.albumId,
                        albumMcId,
                        false,
                        catalogViewData,
                        "singleAlbum-${album.albumId}",
                        { _, _, _, _ -> },
                        { albumId, albumMcId ->
                            albumId?.let {
                                albumMcId?.let {
                                    loadAlbum(view, albumId, albumMcId, true)
                                }
                            }
                        },
                        {
                            init(view, albumViewSelected)
                        }
                    )
                }


            }, errorCallback = {
                //TODO handle error
                swipeRefreshLayout.isRefreshing = false
            })
        } else {
            //open catalog/album view
            init(view, registerListeners(view))
        }
    }

    private fun init(view: View, albumViewSelected:Boolean){
        val settings = getSettings(view.context)
        if (albumViewSelected) {
            settings.setBoolean(
                view.context.getString(R.string.albumViewSelectedSetting),
                true
            )

            resetRecyclerViewPosition(view.context, "catalogView")
            resetRecyclerViewPosition(view.context, "albumView")

            initAlbumListLoad(view, false)
        } else {
            settings.setBoolean(
                view.context.getString(R.string.albumViewSelectedSetting),
                false
            )

            resetRecyclerViewPosition(view.context, "catalogView")
            resetRecyclerViewPosition(view.context, "albumView")

            initSongListLoad(view, false)
        }
    }
}