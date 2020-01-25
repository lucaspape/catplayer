package de.lucaspape.monstercat.handlers

import android.app.AlertDialog
import android.content.Context
import android.os.AsyncTask
import android.view.View
import android.widget.ImageButton
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.listeners.ClickEventHook
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
import de.lucaspape.monstercat.util.parsePlaylistToAbstractPlaylistItem
import de.lucaspape.monstercat.util.parseSongToAbstractCatalogItem
import java.lang.ref.WeakReference

class PlaylistHandler {
    companion object {
        @JvmStatic
        var currentPlaylistId: String? = null
    }

    private var currentPlaylistContentData = ArrayList<CatalogItem>()
    private var currentPlaylistsData = ArrayList<PlaylistItem>()

    //private var currentListViewData = ArrayList<HashMap<String, Any?>>()
    private var listViewDataIsPlaylistView = true

    private fun updateCatalogRecyclerView(view: View, data:ArrayList<CatalogItem>){
        val playlistList = view.findViewById<RecyclerView>(R.id.playlistView)

        playlistList.layoutManager =
            LinearLayoutManager(view.context, LinearLayoutManager.VERTICAL, false)

        val itemAdapter = ItemAdapter<CatalogItem>()
        val fastAdapter = FastAdapter.with(itemAdapter)

        playlistList.adapter = fastAdapter

        for(catalogItem in data){
            itemAdapter.add(
                catalogItem
            )
        }

        fastAdapter.onClickListener = { _, _, _, position ->
            monstercatPlayer.clearContinuous()

            val songId = data[position].id

            val nextSongIdsList = ArrayList<String>()

            for (i in (position + 1 until data.size)) {
                nextSongIdsList.add(data[i].id)
            }

            playSongFromId(view.context, songId, true, nextSongIdsList)
            false
        }

        fastAdapter.onLongClickListener = { _, _, _, position ->
            val idList = ArrayList<String>()

            for (catalogItem in data) {
                idList.add(catalogItem.id)
            }

            showContextMenu(view, idList, false, position)
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
                val idList = ArrayList<String>()

                for (catalogItem in data) {
                    idList.add(catalogItem.id)
                }

                showContextMenu(view, idList, false, position)
            }
        })

    }

    private fun updatePlaylistRecyclerView(view: View, data: ArrayList<PlaylistItem>){
        val playlistList = view.findViewById<RecyclerView>(R.id.playlistView)

        playlistList.layoutManager =
            LinearLayoutManager(view.context, LinearLayoutManager.VERTICAL, false)

        val itemAdapter = ItemAdapter<PlaylistItem>()
        val fastAdapter = FastAdapter.with(itemAdapter)

        playlistList.adapter = fastAdapter

        for(playlist in data){
            itemAdapter.add(
                playlist
            )
        }

        fastAdapter.onClickListener = { _, _, _, position ->
            currentPlaylistId = data[position].id
            loadPlaylistTracks(view, false, currentPlaylistId!!)
            false
        }

        fastAdapter.onLongClickListener = { _, _, _, position ->
            val playlistIdList = ArrayList<String>()

            for(playlist in data){
                playlistIdList.add(playlist.id)
            }

            showContextMenu(view, playlistIdList, true, position)
            false
        }

        fastAdapter.addEventHook(object : ClickEventHook<PlaylistItem>() {
            override fun onBind(viewHolder: RecyclerView.ViewHolder): View? {
                return if (viewHolder is PlaylistItem.ViewHolder) {
                    viewHolder.titleMenuButton
                } else null
            }

            override fun onClick(
                v: View,
                position: Int,
                fastAdapter: FastAdapter<PlaylistItem>,
                item: PlaylistItem
            ) {
                val playlistIdList = ArrayList<String>()

                for(playlist in data){
                    playlistIdList.add(playlist.id)
                }

                showContextMenu(view, playlistIdList, true, position)
            }
        })
    }

    /**
     * Context menu
     */
    private fun showContextMenu(view: View, data: ArrayList<String>, isPlaylist:Boolean, listViewPosition: Int) {
        val menuItems: Array<String> = arrayOf(
            view.context.getString(R.string.download),
            view.context.getString(R.string.playNext),
            view.context.getString(R.string.delete)
        )

        val alertDialogBuilder = AlertDialog.Builder(view.context)
        alertDialogBuilder.setTitle("")
        alertDialogBuilder.setItems(menuItems) { _, which ->
            val id = data[listViewPosition]

            view.context.let { context ->
                val item = menuItems[which]

                if (item == context.getString(R.string.download)) {
                    if (isPlaylist) {
                        downloadPlaylist(
                            context,
                            id
                        )
                    } else {
                        view.let { view ->
                            val songDatabaseHelper =
                                SongDatabaseHelper(view.context)
                            val song =
                                songDatabaseHelper.getSong(view.context, id)

                            if (song != null) {
                                downloadSong(context, song)
                            }
                        }
                    }

                } else if (item == context.getString(R.string.playNext)) {
                    if (isPlaylist) {
                        playPlaylistNext(context, id)
                    } else {
                        playSongFromId(
                            id,
                            false
                        )
                    }

                } else if (item == view.context.getString(R.string.delete)) {
                    if (isPlaylist) {
                        deletePlaylist(context, id)
                    } else {
                        view.let { view ->
                            val songDatabaseHelper =
                                SongDatabaseHelper(view.context)
                            val song =
                                songDatabaseHelper.getSong(view.context, id)

                            if (song != null) {
                                currentPlaylistId?.let { playlistId ->
                                    deletePlaylistSong(
                                        context,
                                        song,
                                        playlistId,
                                        listViewPosition + 1,
                                        data.size
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

                val playlistItems = ArrayList<PlaylistItem>()

                for (playlist in playlists) {
                    playlistItems.add(parsePlaylistToAbstractPlaylistItem(view.context, playlist))
                }

                currentPlaylistsData = playlistItems

            }, {
                listViewDataIsPlaylistView = true

                updatePlaylistRecyclerView(view, currentPlaylistsData)

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

                val sortedList = ArrayList<CatalogItem>()

                val playlistItems = playlistItemDatabaseHelper.getAllData()

                val songDatabaseHelper =
                    SongDatabaseHelper(view.context)

                for (playlistItem in playlistItems) {
                    val catalogItem = parseSongToAbstractCatalogItem(
                        songDatabaseHelper.getSong(view.context, playlistItem.songId)
                    )
                    sortedList.add(catalogItem)
                }

                //display list
                currentPlaylistContentData = ArrayList()

                for (i in (sortedList.size - 1) downTo 0) {
                    currentPlaylistContentData.add(sortedList[i])
                }
            }, {
                listViewDataIsPlaylistView = false

                updateCatalogRecyclerView(view, currentPlaylistContentData)

                swipeRefreshLayout.isRefreshing = false
            }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)

        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }
}