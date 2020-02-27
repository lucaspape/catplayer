package de.lucaspape.monstercat.handlers

import android.app.AlertDialog
import android.content.Context
import android.os.AsyncTask
import android.view.View
import android.widget.ImageButton
import androidx.core.net.toUri
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
import de.lucaspape.monstercat.download.addDownloadSong
import de.lucaspape.monstercat.handlers.abstract_items.CatalogItem
import de.lucaspape.monstercat.handlers.abstract_items.PlaylistItem
import de.lucaspape.monstercat.handlers.async.BackgroundAsync
import de.lucaspape.monstercat.handlers.async.LoadPlaylistAsync
import de.lucaspape.monstercat.handlers.async.LoadPlaylistTracksAsync
import de.lucaspape.monstercat.util.displayInfo
import java.io.File
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

    /**
     * List for playlist content
     */
    private fun updateCatalogRecyclerView(view: View, data: ArrayList<CatalogItem>) {
        val playlistList = view.findViewById<RecyclerView>(R.id.playlistView)

        playlistList.layoutManager =
            LinearLayoutManager(view.context, LinearLayoutManager.VERTICAL, false)

        val itemAdapter = ItemAdapter<CatalogItem>()
        val fastAdapter = FastAdapter.with(itemAdapter)

        playlistList.adapter = fastAdapter

        for (catalogItem in data) {
            itemAdapter.add(
                catalogItem
            )
        }

        /**
         * On song click
         */
        fastAdapter.onClickListener = { _, _, _, position ->
            monstercatPlayer.clearContinuous()

            val songId = data[position].songId

            val nextSongIdsList = ArrayList<String>()

            for (i in (position + 1 until data.size)) {
                nextSongIdsList.add(data[i].songId)
            }

            playSongFromId(view.context, songId, true, nextSongIdsList)
            false
        }

        /**
         * On song long click
         */
        fastAdapter.onLongClickListener = { _, _, _, position ->
            val idList = ArrayList<String>()

            for (catalogItem in data) {
                idList.add(catalogItem.songId)
            }

            showContextMenu(view, idList, false, position)
            false
        }

        /**
         * On song menu button click
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

                for (catalogItem in data) {
                    idList.add(catalogItem.songId)
                }

                showContextMenu(view, idList, false, position)
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
                            addDownloadSong(v.context, item.songId) { titleDownloadButton.setImageURI(song.getSongDownloadStatus().toUri()) }
                        }
                    }
                }
            }
        })

    }

    /**
     * List for playlists
     */
    private fun updatePlaylistRecyclerView(view: View, data: ArrayList<PlaylistItem>) {
        val playlistList = view.findViewById<RecyclerView>(R.id.playlistView)

        playlistList.layoutManager =
            LinearLayoutManager(view.context, LinearLayoutManager.VERTICAL, false)

        val itemAdapter = ItemAdapter<PlaylistItem>()
        val fastAdapter = FastAdapter.with(itemAdapter)

        playlistList.adapter = fastAdapter

        for (playlist in data) {
            itemAdapter.add(
                playlist
            )
        }

        /**
         * On playlist click
         */
        fastAdapter.onClickListener = { _, _, _, position ->
            currentPlaylistId = data[position].playlistId
            loadPlaylistTracks(view, false, currentPlaylistId!!)
            false
        }

        /**
         * On playlist long click
         */
        fastAdapter.onLongClickListener = { _, _, _, position ->
            val playlistIdList = ArrayList<String>()

            for (playlist in data) {
                playlistIdList.add(playlist.playlistId)
            }

            showContextMenu(view, playlistIdList, true, position)
            false
        }

        /**
         * On playlist menu click
         */
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

                for (playlist in data) {
                    playlistIdList.add(playlist.playlistId)
                }

                showContextMenu(view, playlistIdList, true, position)
            }
        })

        /**
         * On download button click
         */
        fastAdapter.addEventHook(object : ClickEventHook<PlaylistItem>() {
            override fun onBind(viewHolder: RecyclerView.ViewHolder): View? {
                return if (viewHolder is PlaylistItem.ViewHolder) {
                    viewHolder.titleDownloadButton
                } else null
            }

            override fun onClick(
                v: View,
                position: Int,
                fastAdapter: FastAdapter<PlaylistItem>,
                item: PlaylistItem
            ) {
                if (item.getDownloadStatus(view.context) == "android.resource://de.lucaspape.monstercat/drawable/ic_offline_pin_green_24dp") {
                    val titleDownloadButton = v as ImageButton

                    deleteDownloadedPlaylistTracks(
                        view.context,
                        item.playlistId
                    ) { titleDownloadButton.setImageURI(item.getDownloadStatus(view.context).toUri()) }
                } else {
                    val titleDownloadButton = v as ImageButton

                    downloadPlaylist(view.context, item.playlistId) {
                        titleDownloadButton.setImageURI(item.getDownloadStatus(view.context).toUri())
                    }
                }
            }
        })
    }

    /**
     * Context menu
     */
    private fun showContextMenu(
        view: View,
        data: ArrayList<String>,
        isPlaylist: Boolean,
        listViewPosition: Int
    ) {
        val menuItems: Array<String> = arrayOf(
            view.context.getString(R.string.download),
            view.context.getString(R.string.playNext),
            view.context.getString(R.string.delete),
            view.context.getString(R.string.shareAlbum)
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
                            id,
                            {}
                        )
                    } else {
                        view.let { view ->
                            val songDatabaseHelper =
                                SongDatabaseHelper(view.context)
                            val song =
                                songDatabaseHelper.getSong(view.context, id)

                            if (song != null) {
                                addDownloadSong(view.context, song.songId, {})
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
                } else if(item == view.context.getString(R.string.shareAlbum)){
                    if(isPlaylist){
                        displayInfo(context, "Sharing playlist currently not implemented.")
                    }else{
                        view.let { view ->
                            val songDatabaseHelper =
                                SongDatabaseHelper(view.context)
                            val song =
                                songDatabaseHelper.getSong(view.context, id)

                            if (song != null) {
                                shareAlbum(context, song.mcAlbumId)
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

        //create new playlist button
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

        currentPlaylistsData = ArrayList()

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

                for (playlist in playlists) {
                    currentPlaylistsData.add(PlaylistItem(playlist.playlistId))
                }

            }, {
                listViewDataIsPlaylistView = true

                updatePlaylistRecyclerView(view, currentPlaylistsData)

                swipeRefreshLayout.isRefreshing = false

            }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    /**
     * Load the tracks from a playlist TODO load more
     */
    private fun loadPlaylistTracks(
        view: View,
        forceReload: Boolean,
        playlistId: String
    ) {
        val contextReference = WeakReference<Context>(view.context)

        val swipeRefreshLayout =
            view.findViewById<SwipeRefreshLayout>(R.id.playlistSwipeRefresh)

        currentPlaylistContentData = ArrayList()

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

                val playlistItems = playlistItemDatabaseHelper.getAllData()

                for (i in (playlistItems.size - 1 downTo 0)) {
                    currentPlaylistContentData.add(CatalogItem(playlistItems[i].songId))
                }

            }, {
                listViewDataIsPlaylistView = false

                updateCatalogRecyclerView(view, currentPlaylistContentData)

                swipeRefreshLayout.isRefreshing = false
            }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)

        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }
}