package de.lucaspape.monstercat.ui.handlers

import android.content.Intent
import android.os.AsyncTask
import android.view.View
import android.widget.ImageButton
import androidx.core.net.toUri
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.listeners.ClickEventHook
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.ui.activities.offlineDrawable
import de.lucaspape.monstercat.database.helper.PlaylistDatabaseHelper
import de.lucaspape.monstercat.database.helper.PlaylistItemDatabaseHelper
import de.lucaspape.monstercat.database.helper.SongDatabaseHelper
import de.lucaspape.monstercat.download.addDownloadSong
import de.lucaspape.monstercat.music.*
import de.lucaspape.monstercat.ui.abstract_items.CatalogItem
import de.lucaspape.monstercat.ui.abstract_items.HeaderTextItem
import de.lucaspape.monstercat.ui.abstract_items.PlaylistItem
import de.lucaspape.monstercat.request.async.LoadPlaylistAsync
import de.lucaspape.monstercat.request.async.LoadPlaylistTracksAsync
import de.lucaspape.monstercat.ui.activities.MainActivity
import de.lucaspape.monstercat.util.*
import de.lucaspape.util.BackgroundAsync
import de.lucaspape.util.Settings
import java.io.File
import java.lang.IndexOutOfBoundsException
import java.lang.ref.WeakReference
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class PlaylistHandler: Handler {
    companion object {
        @JvmStatic
        var playlistContentViewData = HashMap<String, ArrayList<CatalogItem>>()

        @JvmStatic
        var playlistViewData = ArrayList<PlaylistItem>()
    }

    var onFragmentBackPressed = {}

    /**
     * List for playlist content
     */
    private fun updateCatalogRecyclerView(
        view: View,
        headerText: String,
        playlistId: String,
        data: ArrayList<CatalogItem>,
        refreshListener: (playlistId: String) -> Unit
    ) {
        val playlistList = view.findViewById<RecyclerView>(R.id.playlistView)

        playlistList.layoutManager =
            LinearLayoutManager(view.context, LinearLayoutManager.VERTICAL, false)

        val itemAdapter = ItemAdapter<CatalogItem>()
        val headerAdapter = ItemAdapter<HeaderTextItem>()

        val fastAdapter: FastAdapter<GenericItem> =
            FastAdapter.with(listOf(headerAdapter, itemAdapter))

        playlistList.adapter = fastAdapter

        headerAdapter.add(HeaderTextItem(headerText))

        val itemIndexOffset = -1

        for (catalogItem in data) {
            itemAdapter.add(
                catalogItem
            )
        }

        /**
         * On song click
         */
        fastAdapter.onClickListener = { _, _, _, position ->
            val itemIndex = position + itemIndexOffset

            if (itemIndex >= 0) {
                HomeHandler.addSongsTaskId = ""

                clearPlaylist()
                clearQueue()

                songQueue.add(data[itemIndex].songId)
                skipPreviousInPlaylist()
                next()

                BackgroundAsync({
                    val id = UUID.randomUUID().toString()
                    HomeHandler.addSongsTaskId = id

                    for (i in (itemIndex + 1 until data.size)) {
                        try {
                            if (HomeHandler.addSongsTaskId == id) {
                                songQueue.add(data[i].songId)
                            } else {
                                break
                            }
                        } catch (e: IndexOutOfBoundsException) {

                        }
                    }
                }, {}).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
            }

            false
        }

        /**
         * On song long click
         */
        fastAdapter.onLongClickListener = { _, _, _, position ->
            val itemIndex = position + itemIndexOffset

            if (itemIndex >= 0) {
                val idList = ArrayList<String>()

                for (catalogItem in data) {
                    idList.add(catalogItem.songId)
                }

                CatalogItem.showContextMenuPlaylist(view, idList, itemIndex, playlistId)
            }

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
                val itemIndex = position + itemIndexOffset

                if (itemIndex >= 0) {
                    val idList = ArrayList<String>()

                    for (catalogItem in data) {
                        idList.add(catalogItem.songId)
                    }

                    CatalogItem.showContextMenuPlaylist(view, idList, itemIndex, playlistId)
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

        val swipeRefreshLayout = view.findViewById<SwipeRefreshLayout>(R.id.playlistSwipeRefresh)
        swipeRefreshLayout.setOnRefreshListener {
            refreshListener(playlistId)
        }
    }

    /**
     * List for playlists
     */
    private fun updatePlaylistRecyclerView(
        view: View,
        data: ArrayList<PlaylistItem>,
        refreshListener: () -> Unit
    ) {
        val playlistList = view.findViewById<RecyclerView>(R.id.playlistView)

        playlistList.layoutManager =
            LinearLayoutManager(view.context, LinearLayoutManager.VERTICAL, false)

        val itemAdapter = ItemAdapter<PlaylistItem>()
        val headerAdapter = ItemAdapter<HeaderTextItem>()
        val fastAdapter: FastAdapter<GenericItem> =
            FastAdapter.with(listOf(headerAdapter, itemAdapter))

        headerAdapter.add(HeaderTextItem(view.context.getString(R.string.yourPlaylists)))

        val itemIndexOffset = -1

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
            val itemIndex = position + itemIndexOffset

            if (itemIndex >= 0) {
                loadPlaylistTracks(view, false, data[itemIndex].playlistId)
            }

            false
        }

        /**
         * On playlist long click
         */
        fastAdapter.onLongClickListener = { _, _, _, position ->
            val itemIndex = position + itemIndexOffset

            if (itemIndex >= 0) {
                val playlistIdList = ArrayList<String>()

                for (playlist in data) {
                    playlistIdList.add(playlist.playlistId)
                }

                PlaylistItem.showContextMenu(view, playlistIdList, itemIndex)
            }

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
                val itemIndex = position + itemIndexOffset

                if (itemIndex >= 0) {
                    val playlistIdList = ArrayList<String>()

                    for (playlist in data) {
                        playlistIdList.add(playlist.playlistId)
                    }

                    PlaylistItem.showContextMenu(view, playlistIdList, itemIndex)
                }
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
                if (item.getDownloadStatus(view.context) == offlineDrawable) {
                    val titleDownloadButton = v as ImageButton

                    deleteDownloadedPlaylistTracks(
                        view.context,
                        item.playlistId
                    ) {
                        titleDownloadButton.setImageURI(
                            item.getDownloadStatus(view.context).toUri()
                        )
                    }
                } else {
                    val titleDownloadButton = v as ImageButton

                    downloadPlaylist(view, item.playlistId) {
                        titleDownloadButton.setImageURI(
                            item.getDownloadStatus(view.context).toUri()
                        )
                    }
                }
            }
        })

        val swipeRefreshLayout = view.findViewById<SwipeRefreshLayout>(R.id.playlistSwipeRefresh)
        swipeRefreshLayout.setOnRefreshListener {
            refreshListener()
        }
    }

    /**
     * Register listeners (buttons etc)
     */
    private fun registerListeners(view: View) {
        //create new playlist button
        view.findViewById<ImageButton>(R.id.newPlaylistButton).setOnClickListener {
            createPlaylist(view)
        }
    }

    /**
     * Load playlists
     */
    private fun loadPlaylist(view: View, forceReload: Boolean) {
        val contextReference = WeakReference(view.context)

        val swipeRefreshLayout =
            view.findViewById<SwipeRefreshLayout>(R.id.playlistSwipeRefresh)

        val displayData: () -> Unit = {
            updatePlaylistRecyclerView(view, playlistViewData) { loadPlaylist(view, true) }

            swipeRefreshLayout.isRefreshing = false

            onFragmentBackPressed = {
                val intent = Intent(view.context, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NO_ANIMATION
                view.context.startActivity(intent)
            }
        }

        if (playlistViewData.isEmpty() || forceReload) {
            playlistViewData = ArrayList()

            LoadPlaylistAsync(
                contextReference,
                forceReload, displayLoading = {
                    swipeRefreshLayout.isRefreshing = true
                }
                , finishedCallback = { _, _ ->
                    BackgroundAsync({

                        val playlistDatabaseHelper =
                            PlaylistDatabaseHelper(view.context)
                        val playlists = playlistDatabaseHelper.getAllPlaylists()

                        for (playlist in playlists) {
                            playlistViewData.add(PlaylistItem(playlist.playlistId))
                        }

                    }, {
                        displayData()

                    }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
                }, errorCallback = { _, _ ->
                    swipeRefreshLayout.isRefreshing = false
                    displaySnackBar(
                        view,
                        view.context.getString(R.string.errorLoadingPlaylists),
                        view.context.getString(R.string.retry)
                    ) { loadPlaylist(view, forceReload) }
                }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        } else {
            displayData()
        }
    }

    /**
     * Load the tracks from a playlist TODO load more
     */
    private fun loadPlaylistTracks(
        view: View,
        forceReload: Boolean,
        playlistId: String
    ) {
        val contextReference = WeakReference(view.context)

        val swipeRefreshLayout =
            view.findViewById<SwipeRefreshLayout>(R.id.playlistSwipeRefresh)

        var playlistName = ""

        val playlistData = ArrayList<CatalogItem>()

        val displayData: () -> Unit = {
            PlaylistDatabaseHelper(view.context).getPlaylist(playlistId)?.playlistName?.let {
                playlistName = it
            }

            playlistContentViewData[playlistId]?.let {
                updateCatalogRecyclerView(view, playlistName, playlistId, it) { playlistId ->
                    loadPlaylistTracks(view, true, playlistId)
                }
            }

            swipeRefreshLayout.isRefreshing = false

            onFragmentBackPressed = {
                loadPlaylist(view, false)
            }
        }

        if (playlistContentViewData[playlistId] == null || forceReload) {
            LoadPlaylistTracksAsync(
                contextReference,
                forceReload,
                playlistId, displayLoading = {
                    swipeRefreshLayout.isRefreshing = true
                }
                , finishedCallback = { _, _, _ ->
                    BackgroundAsync({
                        val playlistItemDatabaseHelper =
                            PlaylistItemDatabaseHelper(
                                view.context,
                                playlistId
                            )

                        val playlistItems = playlistItemDatabaseHelper.getAllData()

                        for (i in (playlistItems.size - 1 downTo 0)) {
                            playlistData.add(CatalogItem(playlistItems[i].songId))
                        }

                        playlistContentViewData[playlistId] = playlistData
                    }, {
                        displayData()
                    }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)

                }, errorCallback = { _, _, _ ->
                    swipeRefreshLayout.isRefreshing = false
                    displaySnackBar(
                        view,
                        view.context.getString(R.string.errorLoadingPlaylistTracks),
                        view.context.getString(R.string.retry)
                    ) {
                        loadPlaylistTracks(view, forceReload, playlistId)
                    }
                }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        } else {
            displayData()
        }
    }

    override fun onBackPressed(view: View) {
        onFragmentBackPressed()
    }

    override fun onPause(view: View) {

    }

    override val layout: Int = R.layout.fragment_playlist

    override fun onCreate(view: View, search: String?) {
        val settings = Settings(view.context)

        val username = settings.getString(view.context.getString(R.string.emailSetting))
        val password = settings.getString(view.context.getString(R.string.passwordSetting))

        if (username == null || password == null) {
            displayInfo(
                view.context,
                view.context.getString(R.string.setUsernamePasswordSettingsMsg)
            )
        } else {
            registerListeners(view)

            loadPlaylist(view, false)
        }

        onFragmentBackPressed = {
            val intent = Intent(view.context, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NO_ANIMATION
            view.context.startActivity(intent)
        }
    }
}