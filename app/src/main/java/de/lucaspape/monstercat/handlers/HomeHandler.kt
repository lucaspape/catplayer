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
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.activities.SettingsActivity
import de.lucaspape.monstercat.activities.monstercatPlayer
import de.lucaspape.monstercat.database.Song
import de.lucaspape.monstercat.database.helper.AlbumDatabaseHelper
import de.lucaspape.monstercat.database.helper.AlbumItemDatabaseHelper
import de.lucaspape.monstercat.database.helper.CatalogSongDatabaseHelper
import de.lucaspape.monstercat.database.helper.SongDatabaseHelper
import de.lucaspape.monstercat.download.addDownloadCoverArray
import de.lucaspape.monstercat.handlers.abstract_items.AlbumItem
import de.lucaspape.monstercat.handlers.abstract_items.CatalogItem
import de.lucaspape.monstercat.handlers.async.*
import de.lucaspape.monstercat.music.*
import de.lucaspape.monstercat.util.Settings
import de.lucaspape.monstercat.util.parseAlbumToHashMap
import de.lucaspape.monstercat.util.parseSongToHashMap
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
    private fun updateListView(view: View) {
        val musicList = view.findViewById<RecyclerView>(R.id.musiclistview)

        musicList.layoutManager =
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

            musicList.adapter = fastAdapter

            fastAdapter.onClickListener = { _, _, _, position ->
                val itemValue = currentListViewData[position] as HashMap<*, *>
                loadAlbum(view, itemValue, false)

                false
            }

            fastAdapter.onLongClickListener = { _, _, _, position ->
                showContextMenu(view, position)

                false
            }
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

            musicList.adapter = fastAdapter

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
     * Update listview every second (for album covers)
     */
    fun setupListView(view: View) {
        updateListView(view)
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
                    loadAlbumList(view, true)
                }
            } else {
                loadSongList(view, true)
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
                    loadAlbumList(view, false)
                } else {
                    loadSongList(view, false)
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
                loadAlbumList(view, false)
            } else {
                loadSongList(view, false)
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
     * Load song list ("catalog view")
     */
    fun loadSongList(view: View, forceReload: Boolean) {
        Settings(view.context).getSetting("maximumLoad")?.let {
            val contextReference = WeakReference<Context>(view.context)

            val swipeRefreshLayout =
                view.findViewById<SwipeRefreshLayout>(R.id.pullToRefresh)

            LoadSongListAsync(
                contextReference,
                forceReload,
                Integer.parseInt(it), {
                    swipeRefreshLayout.isRefreshing = true
                }
            ) {
                BackgroundAsync({
                    val catalogSongDatabaseHelper =
                        CatalogSongDatabaseHelper(view.context)
                    val songIdList = catalogSongDatabaseHelper.getAllSongs()

                    val songDatabaseHelper =
                        SongDatabaseHelper(view.context)
                    val songList = ArrayList<Song>()

                    for (song in songIdList) {
                        songList.add(songDatabaseHelper.getSong(view.context, song.songId))
                    }

                    val dbSongs = ArrayList<HashMap<String, Any?>>()

                    for (song in songList) {
                        dbSongs.add(parseSongToHashMap(view.context, song))
                    }

                    //display list
                    currentListViewData = dbSongs

                }, {
                    updateListView(view)

                    //download cover art
                    addDownloadCoverArray(currentListViewData)

                    swipeRefreshLayout.isRefreshing = false

                    albumContentsDisplayed = false
                }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)

            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        }

    }

    /**
     * Load album list ("album view")
     */
    fun loadAlbumList(view: View, forceReload: Boolean) {
        Settings(view.context).getSetting("maximumLoad")?.let {
            val contextReference = WeakReference<Context>(view.context)

            val swipeRefreshLayout =
                view.findViewById<SwipeRefreshLayout>(R.id.pullToRefresh)

            LoadAlbumListAsync(
                contextReference,
                forceReload,
                Integer.parseInt(it), {
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
                    updateListView(view)

                    //download cover art
                    addDownloadCoverArray(currentListViewData)

                    swipeRefreshLayout.isRefreshing = false

                    albumContentsDisplayed = false
                }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)

            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
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
                                context,
                                songDatabaseHelper.getSong(context, albumItem.songId)
                            )
                        )
                    }

                }

                currentListViewData = dbSongs
            }, {
                albumView = false

                updateListView(view)

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
            updateListView(view)

            //download cover art
            addDownloadCoverArray(searchResults)

            albumContentsDisplayed = false

            swipeRefreshLayout.isRefreshing = false
        }.executeOnExecutor(
            AsyncTask.THREAD_POOL_EXECUTOR
        )
    }
}