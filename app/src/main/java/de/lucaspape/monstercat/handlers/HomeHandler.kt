package de.lucaspape.monstercat.handlers

import android.app.AlertDialog
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
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.listeners.ClickEventHook
import com.mikepenz.fastadapter.scroll.EndlessRecyclerOnScrollListener
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.activities.SettingsActivity
import de.lucaspape.monstercat.activities.monstercatPlayer
import de.lucaspape.monstercat.database.helper.AlbumDatabaseHelper
import de.lucaspape.monstercat.database.helper.AlbumItemDatabaseHelper
import de.lucaspape.monstercat.database.helper.CatalogSongDatabaseHelper
import de.lucaspape.monstercat.database.helper.SongDatabaseHelper
import de.lucaspape.monstercat.download.addDownloadSong
import de.lucaspape.monstercat.handlers.abstract_items.AlbumItem
import de.lucaspape.monstercat.handlers.abstract_items.CatalogItem
import de.lucaspape.monstercat.handlers.abstract_items.ProgressItem
import de.lucaspape.monstercat.handlers.async.*
import de.lucaspape.monstercat.music.*
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
    }

    private var currentCatalogViewData = ArrayList<CatalogItem>()
    private var currentAlbumViewData = ArrayList<AlbumItem>()

    //if contents of an album currently displayed
    private var albumContentsDisplayed = false
    private var searchContentsDisplayed = false

    private var currentAlbumId = ""
    private var currentMCID = ""

    private var initDone = false

    /**
     * Setup catalog view
     */
    private fun updateCatalogRecyclerView(view: View) {
        val recyclerView = view.findViewById<RecyclerView>(R.id.musiclistview)

        recyclerView.layoutManager =
            LinearLayoutManager(view.context, LinearLayoutManager.VERTICAL, false)

        val itemAdapter = ItemAdapter<CatalogItem>()
        val fastAdapter = FastAdapter.with(itemAdapter)

        recyclerView.adapter = fastAdapter

        for (catalogItem in currentCatalogViewData) {
            itemAdapter.add(
                catalogItem
            )
        }

        /**
         * On song click
         */
        fastAdapter.onClickListener = { _, _, _, position ->
            monstercatPlayer.clearContinuous()

            val catalogItem = currentCatalogViewData[position]

            val nextSongIdsList = ArrayList<String>()

            for (i in (position + 1 until currentCatalogViewData.size)) {
                nextSongIdsList.add(currentCatalogViewData[i].songId)
            }

            playSongFromId(
                view.context,
                catalogItem.songId,
                true,
                nextSongIdsList
            )

            false
        }

        /**
         * On song long click
         */
        fastAdapter.onLongClickListener = { _, _, _, position ->
            val idList = ArrayList<String>()

            for (catalogItem in currentCatalogViewData) {
                idList.add(catalogItem.songId)
            }

            showContextMenu(view, idList, position)

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
                val idList = ArrayList<String>()

                for (catalogItem in currentCatalogViewData) {
                    idList.add(catalogItem.songId)
                }

                showContextMenu(view, idList, position)
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
                            addDownloadSong(v.context, item.songId) {titleDownloadButton.setImageURI(song.getSongDownloadStatus().toUri())}
                        }
                    }
                }
            }
        })

        /**
         * On scroll down (load next)
         */
        val footerAdapter = ItemAdapter<ProgressItem>()
        recyclerView.addOnScrollListener(object :
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
        val recyclerView = view.findViewById<RecyclerView>(R.id.musiclistview)

        recyclerView.layoutManager =
            LinearLayoutManager(view.context, LinearLayoutManager.VERTICAL, false)

        val itemAdapter = ItemAdapter<AlbumItem>()
        val fastAdapter = FastAdapter.with(itemAdapter)

        recyclerView.adapter = fastAdapter

        for (albumItem in currentAlbumViewData) {
            itemAdapter.add(
                albumItem
            )
        }

        val albumDatabaseHelper = AlbumDatabaseHelper(view.context)

        /**
         * On item click
         */
        fastAdapter.onClickListener = { _, _, _, position ->
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

            showContextMenu(view, albumMcIdList, position)

            false
        }

        /**
         * On scroll down (load more)
         */
        val footerAdapter = ItemAdapter<ProgressItem>()
        recyclerView.addOnScrollListener(object :
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
     * Context menu
     */
    private fun showContextMenu(
        view: View,
        contentList: ArrayList<String>,
        listViewPosition: Int
    ) {
        val menuItems: Array<String> = if (!albumView) {
            arrayOf(
                view.context.getString(R.string.download),
                view.context.getString(R.string.playNext),
                view.context.getString(R.string.addToPlaylist),
                view.context.getString(R.string.shareAlbum)
            )
        } else {
            arrayOf(
                view.context.getString(R.string.downloadAlbum),
                view.context.getString(R.string.playAlbumNext),
                view.context.getString(R.string.shareAlbum)
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
                        context.getString(R.string.download) -> addDownloadSong(context, song.songId, {})
                        context.getString(R.string.playNext) -> playSongFromId(
                            id,
                            false
                        )
                        context.getString(R.string.addToPlaylist) -> addSongToPlaylist(
                            context,
                            song
                        )
                        view.context.getString(R.string.shareAlbum) -> shareAlbum(context, song.mcAlbumId)
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
                        context.getString(R.string.shareAlbum) -> shareAlbum(context, id)
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
                    updateAlbumRecyclerView(view)
                } else {
                    viewSelector.setSelection(0)
                    updateCatalogRecyclerView(view)
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

    /**
     * Loads first 50 catalog songs
     */
    fun initSongListLoad(view: View, forceReload: Boolean) {
        initDone = false
        currentCatalogViewData = ArrayList()

        val swipeRefreshLayout =
            view.findViewById<SwipeRefreshLayout>(R.id.pullToRefresh)

        val catalogSongDatabaseHelper =
            CatalogSongDatabaseHelper(view.context)

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
                updateCatalogRecyclerView(view)

                swipeRefreshLayout.isRefreshing = false
                searchContentsDisplayed = false
                albumContentsDisplayed = false
            }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
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
        currentAlbumViewData = ArrayList()

        val contextReference = WeakReference<Context>(view.context)

        val swipeRefreshLayout =
            view.findViewById<SwipeRefreshLayout>(R.id.pullToRefresh)

        val albumDatabaseHelper = AlbumDatabaseHelper(view.context)

        if (forceReload) {
            albumDatabaseHelper.reCreateTable()
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

                for (albumItem in albumItemList) {
                    dbSongs.add(
                        CatalogItem(albumItem.songId)
                    )
                }

                currentCatalogViewData = dbSongs
            }, {
                albumView = false

                updateCatalogRecyclerView(view)

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

        val contextReference = WeakReference<Context>(view.context)

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

            updateCatalogRecyclerView(view)

            swipeRefreshLayout.isRefreshing = false
        }.executeOnExecutor(
            AsyncTask.THREAD_POOL_EXECUTOR
        )
    }
}