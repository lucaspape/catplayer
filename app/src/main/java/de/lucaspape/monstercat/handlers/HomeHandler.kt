package de.lucaspape.monstercat.handlers

import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.*
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.activities.SettingsActivity
import de.lucaspape.monstercat.database.Song
import de.lucaspape.monstercat.database.helper.AlbumDatabaseHelper
import de.lucaspape.monstercat.database.helper.AlbumItemDatabaseHelper
import de.lucaspape.monstercat.database.helper.CatalogSongDatabaseHelper
import de.lucaspape.monstercat.database.helper.SongDatabaseHelper
import de.lucaspape.monstercat.download.addDownloadCoverArray
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

    var currentListViewData = ArrayList<HashMap<String, Any?>>()
    private var simpleAdapter: SimpleAdapter? = null

    //if contents of an album currently displayed
    var albumContentsDisplayed = false

    var currentAlbumId = ""

    private fun redrawListView(view: View) {
        val musicList = view.findViewById<ListView>(R.id.musiclistview)
        simpleAdapter?.notifyDataSetChanged()
        musicList.invalidateViews()
        musicList.refreshDrawableState()
    }

    /**
     * Updates content of listView
     */
    private fun updateListView(view: View) {
        val musicList = view.findViewById<ListView>(R.id.musiclistview)

        if (albumView) {
            val from = arrayOf("title", "artist", "primaryImage")
            val to = arrayOf(R.id.albumTitle, R.id.albumArtist, R.id.cover)
            simpleAdapter = SimpleAdapter(
                view.context,
                currentListViewData,
                R.layout.list_album_view,
                from,
                to.toIntArray()
            )
            musicList.adapter = simpleAdapter
        } else {
            val from = arrayOf("shownTitle", "artist", "secondaryImage", "downloadedCheck")
            val to = arrayOf(R.id.title, R.id.artist, R.id.cover, R.id.titleDownloadStatus)
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

    /**
     * Update listview every second (for album covers)
     */
    fun setupListView(view: View) {
        updateListView(view)
        redrawListView(view)

        //setup auto reload
        Thread(Runnable {
            while (true) {
                Handler(Looper.getMainLooper()).post { redrawListView(view) }
                Thread.sleep(200)
            }

        }).start()
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

        //click on list
        val musicList = view.findViewById<ListView>(R.id.musiclistview)
        musicList.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            if (albumView) {
                val itemValue = musicList.getItemAtPosition(position) as HashMap<*, *>
                loadAlbum(view, itemValue, false)
            } else {
                clearContinuous()

                val itemValue = musicList.getItemAtPosition(position) as HashMap<*, *>
                playSongFromId(
                    view.context,
                    itemValue["id"] as String,
                    true,
                    musicList,
                    position
                )
            }
        }

        //spinner (select catalog or albumview)

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
            swipeRefreshLayout.isRefreshing = true

            val dbSongs = ArrayList<HashMap<String, Any?>>()

            LoadSongListAsync(
                contextReference,
                forceReload,
                Integer.parseInt(it)
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

                    for (song in songList) {
                        dbSongs.add(parseSongToHashMap(view.context, song))
                    }

                }, {
                    //display list
                    currentListViewData = dbSongs

                    updateListView(view)
                    redrawListView(view)

                    //download cover art
                    addDownloadCoverArray(currentListViewData)

                    val listView = view.findViewById<ListView>(R.id.musiclistview)
                    val settings = Settings(view.context)
                    val lastScroll = settings.getSetting("currentListViewLastScrollIndex")
                    val top = settings.getSetting("currentListViewTop")

                    if (top != null && lastScroll != null) {
                        listView.setSelectionFromTop(lastScroll.toInt(), top.toInt())
                    } else {
                        println("IT IS NULL")
                    }

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
            swipeRefreshLayout.isRefreshing = true

            LoadAlbumListAsync(
                contextReference,
                forceReload,
                Integer.parseInt(it)
            ) {
                val albumDatabaseHelper =
                    AlbumDatabaseHelper(view.context)
                val albumList = albumDatabaseHelper.getAllAlbums()

                val sortedList = ArrayList<HashMap<String, Any?>>()

                for (album in albumList) {
                    sortedList.add(parseAlbumToHashMap(view.context, album))
                }

                currentListViewData = sortedList

                updateListView(view)
                redrawListView(view)

                //download cover art
                addDownloadCoverArray(currentListViewData)

                val listView = view.findViewById<ListView>(R.id.musiclistview)
                val settings = Settings(view.context)
                val lastScroll = settings.getSetting("currentListAlbumViewLastScrollIndex")
                val top = settings.getSetting("currentListAlbumViewTop")

                if (top != null && lastScroll != null) {
                    listView.setSelectionFromTop(lastScroll.toInt(), top.toInt())
                }

                swipeRefreshLayout.isRefreshing = false

                albumContentsDisplayed = false

            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        }
    }

    /**
     * Load single album
     */
    private fun loadAlbum(view: View, itemValue: HashMap<*, *>, forceReload: Boolean) {
        val listView = view.findViewById<ListView>(R.id.musiclistview)

        val settings = Settings(view.context)
        settings.saveSetting(
            "currentListAlbumViewLastScrollIndex",
            listView.firstVisiblePosition.toString()
        )
        settings.saveSetting(
            "currentListAlbumViewTop",
            (listView.getChildAt(0).top - listView.paddingTop).toString()
        )

        val contextReference = WeakReference<Context>(view.context)

        val swipeRefreshLayout =
            view.findViewById<SwipeRefreshLayout>(R.id.pullToRefresh)
        swipeRefreshLayout.isRefreshing = true

        LoadAlbumAsync(
            contextReference,
            forceReload,
            itemValue
        ) {
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

            albumView = false

            updateListView(view)
            redrawListView(view)

            //download cover art
            addDownloadCoverArray(currentListViewData)

            swipeRefreshLayout.isRefreshing = false

            albumContentsDisplayed = true
            currentAlbumId = itemValue["id"] as String
        }.executeOnExecutor(
            AsyncTask.THREAD_POOL_EXECUTOR
        )
    }

    //search for string
    fun searchSong(view: View, searchString: String) {
        val contextReference = WeakReference<Context>(view.context)

        LoadTitleSearchAsync(
            contextReference,
            searchString
        ) {
            updateListView(view)
            redrawListView(view)

            //download cover art
            addDownloadCoverArray(searchResults)

            albumContentsDisplayed = false
        }.executeOnExecutor(
            AsyncTask.THREAD_POOL_EXECUTOR
        )
    }
}