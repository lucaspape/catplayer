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
import de.lucaspape.monstercat.download.addDownloadCoverArray
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

    /**
     * Updates content of listView
     */
    private fun updateRecyclerView(view: View) {
        val recyclerView = view.findViewById<RecyclerView>(R.id.musiclistview)

        recyclerView.layoutManager =
            LinearLayoutManager(view.context, LinearLayoutManager.VERTICAL, false)

        if (albumView) {
            val itemAdapter = ItemAdapter<AlbumItem>()
            val fastAdapter = FastAdapter.with(itemAdapter)

            for (hashMap in currentListViewData) {
                val title = hashMap["title"] as String
                val artist = hashMap["artist"] as String
                val cover = hashMap["primaryImage"] as String

                itemAdapter.add(
                    AlbumItem(
                        title,
                        artist,
                        cover
                    )
                )
            }

            recyclerView.adapter = fastAdapter

            fastAdapter.onClickListener = { _, _, _, position ->
                val itemValue = currentListViewData[position] as HashMap<*, *>
                loadAlbum(view, itemValue, false)

                false
            }

            fastAdapter.onLongClickListener = { _, _, _, position ->
                showContextMenu(view, position)

                false
            }

            val footerAdapter = ItemAdapter<ProgressItem>()
            recyclerView.addOnScrollListener(object :
                EndlessRecyclerOnScrollListener(footerAdapter) {
                override fun onLoadMore(currentPage: Int) {
                    footerAdapter.clear()
                    footerAdapter.add(ProgressItem())

                    loadAlbumList(view, itemAdapter)
                }
            })
        } else {
            val itemAdapter = ItemAdapter<CatalogItem>()
            val fastAdapter = FastAdapter.with(itemAdapter)

            for (hashMap in currentListViewData) {
                val title = hashMap["title"] as String
                val artist = hashMap["artist"] as String
                val cover = hashMap["secondaryImage"] as String
                val titleDownloadStatus = hashMap["downloadedCheck"] as String

                itemAdapter.add(
                    CatalogItem(
                        title,
                        artist,
                        cover,
                        titleDownloadStatus
                    )
                )
            }

            recyclerView.adapter = fastAdapter

            fastAdapter.onClickListener = { _, _, _, position ->
                monstercatPlayer.clearContinuous()

                val itemValue = currentListViewData[position] as HashMap<*, *>
                playSongFromId(
                    view.context,
                    itemValue["id"] as String,
                    true,
                    currentListViewData,
                    position
                )

                false
            }

            fastAdapter.onLongClickListener = { _, _, _, position ->
                showContextMenu(view, position)

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
                    showContextMenu(view, position)
                }
            })

            val footerAdapter = ItemAdapter<ProgressItem>()
            recyclerView.addOnScrollListener(object :
                EndlessRecyclerOnScrollListener(footerAdapter) {
                override fun onLoadMore(currentPage: Int) {
                    footerAdapter.clear()
                    footerAdapter.add(ProgressItem())

                    loadSongList(view, itemAdapter)
                }
            })
        }
    }

    private fun showContextMenu(view: View, listViewPosition: Int) {
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
            val listItem = currentListViewData[listViewPosition] as HashMap<*, *>

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
                    loadAlbum(view, itemValue, true)
                } else {
                    initAlbumListLoad(view)
                }
            } else {
                initSongListLoad(view)
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
                } else {
                    viewSelector.setSelection(0)
                }

                updateRecyclerView(view)
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
                    initAlbumListLoad(view)
                } else {
                    initSongListLoad(view)
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
                initAlbumListLoad(view)
            } else {
                initSongListLoad(view)
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

    fun initSongListLoad(view: View) {
        CatalogSongDatabaseHelper(view.context).reCreateTable()

        val swipeRefreshLayout =
            view.findViewById<SwipeRefreshLayout>(R.id.pullToRefresh)

        currentListViewData = ArrayList()

        val catalogSongDatabaseHelper =
            CatalogSongDatabaseHelper(view.context)

        LoadSongListAsync(WeakReference(view.context), true, 0, {}, {
            val songIdList = catalogSongDatabaseHelper.getAllSongs()

            val songDatabaseHelper =
                SongDatabaseHelper(view.context)
            val songList = ArrayList<Song>()

            for (song in songIdList) {
                songList.add(songDatabaseHelper.getSong(view.context, song.songId))
            }

            for (song in songList) {
                val hashMap = parseSongToHashMap(view.context, song)
                currentListViewData.add(hashMap)
            }

            updateRecyclerView(view)

            addDownloadCoverArray(currentListViewData)

            swipeRefreshLayout.isRefreshing = false
        }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
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

            for (song in songList) {
                val hashMap = parseSongToHashMap(view.context, song)

                val title = hashMap["title"] as String
                val artist = hashMap["artist"] as String
                val cover = hashMap["secondaryImage"] as String
                val titleDownloadStatus = hashMap["downloadedCheck"] as String

                itemAdapter.add(CatalogItem(title, artist, cover, titleDownloadStatus))
                currentListViewData.add(hashMap)
            }

            addDownloadCoverArray(currentListViewData)
        }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    fun loadAlbumList(view: View, itemAdapter: ItemAdapter<AlbumItem>) {
        val loaded = AlbumDatabaseHelper(view.context).getAllAlbums().size

        LoadAlbumListAsync(WeakReference(view.context),
            true, loaded, {}, {
                val albumDatabaseHelper =
                    AlbumDatabaseHelper(view.context)
                val albumList = albumDatabaseHelper.getAllAlbums()

                val sortedList = ArrayList<HashMap<String, Any?>>()

                for (album in albumList) {
                    val hashMap = parseAlbumToHashMap(view.context, album)

                    sortedList.add(hashMap)

                    val title = hashMap["title"] as String
                    val artist = hashMap["artist"] as String
                    val cover = hashMap["primaryImage"] as String

                    itemAdapter.add(
                        AlbumItem(
                            title,
                            artist,
                            cover
                        )
                    )

                    currentListViewData.add(hashMap)
                }

                //download cover art
                addDownloadCoverArray(currentListViewData)

                albumContentsDisplayed = false
            }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    fun initAlbumListLoad(view: View) {
        AlbumDatabaseHelper(view.context).reCreateTable()
        val contextReference = WeakReference<Context>(view.context)

        val swipeRefreshLayout =
            view.findViewById<SwipeRefreshLayout>(R.id.pullToRefresh)

        LoadAlbumListAsync(
            contextReference,
            true,
            0, {
                swipeRefreshLayout.isRefreshing = true
            }
        ) {
            BackgroundAsync({
                val albumDatabaseHelper =
                    AlbumDatabaseHelper(view.context)
                val albumList = albumDatabaseHelper.getAllAlbums()

                val sortedList = ArrayList<HashMap<String, Any?>>()

                for (album in albumList) {
                    sortedList.add(parseAlbumToHashMap(view.context, album))
                }

                currentListViewData = sortedList
            }, {
                updateRecyclerView(view)

                //download cover art
                addDownloadCoverArray(currentListViewData)

                swipeRefreshLayout.isRefreshing = false

                albumContentsDisplayed = false

                swipeRefreshLayout.isRefreshing = false
            }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)

        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
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
                                context,
                                songDatabaseHelper.getSong(context, albumItem.songId)
                            )
                        )
                    }

                }

                currentListViewData = dbSongs
            }, {
                albumView = false

                updateRecyclerView(view)

                //download cover art
                addDownloadCoverArray(currentListViewData)

                swipeRefreshLayout.isRefreshing = false

                albumContentsDisplayed = true
                currentAlbumId = itemValue["id"] as String
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
            searchString
        ) {
            updateRecyclerView(view)

            //download cover art
            addDownloadCoverArray(searchResults)

            albumContentsDisplayed = false

            swipeRefreshLayout.isRefreshing = false
        }.executeOnExecutor(
            AsyncTask.THREAD_POOL_EXECUTOR
        )
    }
}