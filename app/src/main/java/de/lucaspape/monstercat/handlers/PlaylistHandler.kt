package de.lucaspape.monstercat.handlers

import android.app.AlertDialog
import android.content.Context
import android.os.AsyncTask
import android.view.View
import android.widget.ImageButton
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.activities.monstercatPlayer
import de.lucaspape.monstercat.database.helper.PlaylistDatabaseHelper
import de.lucaspape.monstercat.database.helper.PlaylistItemDatabaseHelper
import de.lucaspape.monstercat.database.helper.SongDatabaseHelper
import de.lucaspape.monstercat.handlers.abstract_items.CatalogItem
import de.lucaspape.monstercat.handlers.abstract_items.PlaylistItem
import de.lucaspape.monstercat.handlers.async.BackgroundAsync
import de.lucaspape.monstercat.handlers.async.LoadPlaylistAsync
import de.lucaspape.monstercat.handlers.async.LoadPlaylistTracksAsync
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

    /**
     * Updates listView content
     */
    private fun updateListView(view: View) {
        val playlistList = view.findViewById<RecyclerView>(R.id.playlistView)

        if (listViewDataIsPlaylistView) {
            val itemAdapter = ItemAdapter<PlaylistItem>()
            val fastAdapter = FastAdapter.with(itemAdapter)

            for(hashMap in currentListViewData){
                val name = hashMap["playlistName"] as String
                val coverUrl = hashMap["coverUrl"] as String

                itemAdapter.add(
                    PlaylistItem(
                        name,
                        coverUrl,
                        ""
                    )
                )
            }

            playlistList.adapter = fastAdapter

            fastAdapter.onClickListener = { _, _, _, position ->
                currentPlaylistId = currentListViewData[position]["playlistId"] as String
                loadPlaylistTracks(view, false, currentPlaylistId!!)
                false
            }

            fastAdapter.onLongClickListener = { _, _, _, position ->
                showContextMenu(view, position)
                false
            }

        }else{
            val itemAdapter = ItemAdapter<CatalogItem>()
            val fastAdapter = FastAdapter.with(itemAdapter)

            for(hashMap in currentListViewData){
                val title = hashMap["title"] as String
                val artist = hashMap["artist"] as String
                val titleDownloadStatus = hashMap["downloadedCheck"] as String

                itemAdapter.add(
                    CatalogItem(
                        title,
                        artist,
                        (hashMap["coverUrl"] as String),
                        titleDownloadStatus
                    )
                )
            }

            playlistList.adapter = fastAdapter

            fastAdapter.onClickListener = { _, _, _, position ->
                monstercatPlayer.clearContinuous()

                val songId = currentListViewData[position]["id"] as String
                playSongFromId(view.context, songId, true, currentListViewData, position)
                false
            }

            fastAdapter.onLongClickListener = { _, _, _, position ->
                showContextMenu(view, position)
                false
            }
        }
    }

    /**
     * Context menu
     */
    private fun showContextMenu(view: View, listViewPosition: Int) {
        val menuItems: Array<String> = arrayOf(
            view.context.getString(R.string.download),
            view.context.getString(R.string.playNext),
            view.context.getString(R.string.delete)
        )

        val alertDialogBuilder = AlertDialog.Builder(view.context)
        alertDialogBuilder.setTitle("")
        alertDialogBuilder.setItems(menuItems) { _, which ->
            val listItem = currentListViewData[listViewPosition]

            view.context.let { context ->
                val item = menuItems[which]

                if (item == context.getString(R.string.download)) {
                    if (listItem["type"] == "playlist") {
                        downloadPlaylist(
                            context,
                            listItem["playlistId"] as String
                        )
                    } else {
                        view.let { view ->
                            val songDatabaseHelper =
                                SongDatabaseHelper(view.context)
                            val song =
                                songDatabaseHelper.getSong(view.context, listItem["id"] as String)

                            if (song != null) {
                                downloadSong(context, song)
                            }
                        }
                    }

                } else if (item == context.getString(R.string.playNext)) {
                    if (listItem["type"] == "playlist") {
                        playPlaylistNext(context, listItem["playlistId"] as String)
                    } else {
                        playSongFromId(
                            listItem["id"] as String,
                            false
                        )
                    }

                } else if (item == view.context.getString(R.string.delete)) {
                    if (listItem["type"] == "playlist") {
                        deletePlaylist(context, listItem["playlistId"] as String)
                    } else {
                        view.let { view ->
                            val songDatabaseHelper =
                                SongDatabaseHelper(view.context)
                            val song =
                                songDatabaseHelper.getSong(view.context, listItem["id"] as String)

                            if (song != null) {
                                currentPlaylistId?.let { playlistId ->
                                    deletePlaylistSong(
                                        context,
                                        song,
                                        playlistId,
                                        listViewPosition + 1,
                                        currentListViewData.size
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        alertDialogBuilder.create().show()
    }

    /**
     * Update listView every second (for album covers)
     */
    fun setupListView(view: View) {
        updateListView(view)
    }

    /**
     * Register listeners (buttons etc)
     */
    fun registerListeners(view: View) {
        //refresh
        val swipeRefreshLayout = view.findViewById<SwipeRefreshLayout>(R.id.playlistSwipeRefresh)
        swipeRefreshLayout.setOnRefreshListener {
            if (listViewDataIsPlaylistView) {
                loadPlaylist(view, forceReload = true)
            } else {
                loadPlaylistTracks(view, true, currentPlaylistId!!)
            }
        }

        view.findViewById<ImageButton>(R.id.newPlaylistButton).setOnClickListener {
            createPlaylist(view.context)
        }
    }

    /**
     * Load playlists
     */
    fun loadPlaylist(view: View, forceReload: Boolean) {
        val contextReference = WeakReference<Context>(view.context)

        val swipeRefreshLayout =
            view.findViewById<SwipeRefreshLayout>(R.id.playlistSwipeRefresh)

        LoadPlaylistAsync(
            contextReference,
            forceReload, {
                swipeRefreshLayout.isRefreshing = true
            }
        ) {
            BackgroundAsync({

                val playlistDatabaseHelper =
                    PlaylistDatabaseHelper(view.context)
                val playlists = playlistDatabaseHelper.getAllPlaylists()

                val playlistHashMaps = ArrayList<HashMap<String, Any?>>()

                for (playlist in playlists) {
                    playlistHashMaps.add(parsePlaylistToHashMap(playlist))
                }

                currentListViewData = playlistHashMaps

            }, {
                listViewDataIsPlaylistView = true

                updateListView(view)

                swipeRefreshLayout.isRefreshing = false

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

        LoadPlaylistTracksAsync(
            contextReference,
            forceReload,
            playlistId, {
                swipeRefreshLayout.isRefreshing = true
            }
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

                swipeRefreshLayout.isRefreshing = false
            }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)

        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }
}