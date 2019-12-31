package de.lucaspape.monstercat.handlers

import android.content.Context
import android.os.AsyncTask
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.AdapterView
import android.widget.ImageButton
import android.widget.ListView
import android.widget.SimpleAdapter
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.database.helper.PlaylistDatabaseHelper
import de.lucaspape.monstercat.database.helper.PlaylistItemDatabaseHelper
import de.lucaspape.monstercat.database.helper.SongDatabaseHelper
import de.lucaspape.monstercat.download.addDownloadCoverArray
import de.lucaspape.monstercat.handlers.async.BackgroundAsync
import de.lucaspape.monstercat.handlers.async.LoadPlaylistAsync
import de.lucaspape.monstercat.handlers.async.LoadPlaylistTracksAsync
import de.lucaspape.monstercat.music.clearContinuous
import de.lucaspape.monstercat.util.parsePlaylistToHashMap
import de.lucaspape.monstercat.util.parseSongToHashMap
import java.lang.ref.WeakReference

class PlaylistHandler {
    companion object {
        @JvmStatic
        var currentPlaylistId: String? = null
    }

    private var currentListViewData = ArrayList<HashMap<String, Any?>>()
    private var listViewDataIsPlaylistView = true
    private var simpleAdapter: SimpleAdapter? = null

    /**
     * Updates listView content
     */
    private fun updateListView(view: View) {
        val playlistList = view.findViewById<ListView>(R.id.playlistView)

        var from = arrayOf("shownTitle", "artist", "secondaryImage", "downloadedCheck")
        var to = arrayOf(R.id.title, R.id.artist, R.id.cover, R.id.titleDownloadStatus)

        if (listViewDataIsPlaylistView) {
            from = arrayOf("playlistName", "coverUrl")
            to = arrayOf(R.id.title, R.id.cover)
        }

        simpleAdapter = SimpleAdapter(
            view.context,
            currentListViewData,
            R.layout.list_single,
            from,
            to.toIntArray()
        )

        playlistList.adapter = simpleAdapter

    }

    private fun redrawListView(view: View) {
        val playlistList = view.findViewById<ListView>(R.id.playlistView)
        simpleAdapter!!.notifyDataSetChanged()
        playlistList.invalidateViews()
        playlistList.refreshDrawableState()
    }

    /**
     * Update listView every second (for album covers)
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
     * Register listeners (buttons etc)
     */
    fun registerListeners(view: View) {
        //click on list
        val playlistList = view.findViewById<ListView>(R.id.playlistView)

        //refresh
        val swipeRefreshLayout = view.findViewById<SwipeRefreshLayout>(R.id.playlistSwipeRefresh)
        swipeRefreshLayout.setOnRefreshListener {
            if (listViewDataIsPlaylistView) {
                loadPlaylist(view, forceReload = true, showAfter = true)
            } else {
                loadPlaylistTracks(view, true, currentPlaylistId!!)
            }
        }

        playlistList.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val itemValue = playlistList.getItemAtPosition(position) as HashMap<*, *>

            if (listViewDataIsPlaylistView) {
                currentPlaylistId = itemValue["playlistId"] as String
                loadPlaylistTracks(view, false, currentPlaylistId!!)
            } else {
                clearContinuous()

                val songId = itemValue["id"] as String
                playSongFromId(view.context, songId, true, playlistList, position)
            }
        }

        view.findViewById<ImageButton>(R.id.newPlaylistButton).setOnClickListener {
            createPlaylist(view.context)
        }
    }

    /**
     * Load playlists
     */
    fun loadPlaylist(view: View, forceReload: Boolean, showAfter: Boolean) {
        val contextReference = WeakReference<Context>(view.context)

        val swipeRefreshLayout =
            view.findViewById<SwipeRefreshLayout>(R.id.playlistSwipeRefresh)
        swipeRefreshLayout.isRefreshing = true


        LoadPlaylistAsync(
            contextReference,
            forceReload
        ) {
            BackgroundAsync({

                val playlistDatabaseHelper =
                    PlaylistDatabaseHelper(view.context)
                val playlists = playlistDatabaseHelper.getAllPlaylists()

                val playlistHashMaps = ArrayList<HashMap<String, Any?>>()

                for (playlist in playlists) {
                    playlistHashMaps.add(parsePlaylistToHashMap(playlist))
                }

                if (showAfter) {
                    currentListViewData = playlistHashMaps
                }

            }, {
                if (showAfter) {

                    listViewDataIsPlaylistView = true

                    updateListView(view)
                    redrawListView(view)

                    swipeRefreshLayout.isRefreshing = false
                }
            }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    /**
     * Load the tracks from a playlist
     */
    private fun loadPlaylistTracks(
        view: View,
        forceReload: Boolean,
        playlistId: String
    ) {
        val contextReference = WeakReference<Context>(view.context)

        val swipeRefreshLayout =
            view.findViewById<SwipeRefreshLayout>(R.id.playlistSwipeRefresh)
        swipeRefreshLayout.isRefreshing = true

        LoadPlaylistTracksAsync(
            contextReference,
            forceReload,
            playlistId
        ) {
            BackgroundAsync({
                val playlistItemDatabaseHelper =
                    PlaylistItemDatabaseHelper(
                        view.context,
                        playlistId
                    )

                val sortedList = ArrayList<HashMap<String, Any?>>()

                val playlistItems = playlistItemDatabaseHelper.getAllData()

                val songDatabaseHelper =
                    SongDatabaseHelper(view.context)

                for (playlistItem in playlistItems) {
                    val hashMap = parseSongToHashMap(
                        view.context,
                        songDatabaseHelper.getSong(view.context, playlistItem.songId)
                    )
                    sortedList.add(hashMap)
                }

                //display list
                currentListViewData = ArrayList()

                for (i in (sortedList.size - 1) downTo 0) {
                    currentListViewData.add(sortedList[i])
                }
            }, {
                listViewDataIsPlaylistView = false

                updateListView(view)
                redrawListView(view)

                //download cover art
                addDownloadCoverArray(currentListViewData)

                swipeRefreshLayout.isRefreshing = false
            }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)

        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }
}