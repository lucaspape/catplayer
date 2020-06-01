package de.lucaspape.monstercat.ui.handlers.playlist

import android.content.Context
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
import de.lucaspape.monstercat.database.helper.PlaylistDatabaseHelper
import de.lucaspape.monstercat.database.helper.PlaylistItemDatabaseHelper
import de.lucaspape.monstercat.database.helper.SongDatabaseHelper
import de.lucaspape.monstercat.download.addDownloadSong
import de.lucaspape.monstercat.request.async.loadPlaylistTracksAsync
import de.lucaspape.monstercat.ui.abstract_items.CatalogItem
import de.lucaspape.monstercat.ui.abstract_items.HeaderTextItem
import de.lucaspape.monstercat.ui.activities.lastOpenId
import de.lucaspape.monstercat.ui.activities.lastOpenType
import de.lucaspape.monstercat.ui.handlers.playSongsFromViewDataAsync
import de.lucaspape.monstercat.util.displaySnackBar
import de.lucaspape.util.Cache
import de.lucaspape.util.Settings
import java.io.File

class PlaylistContentsHandler(private val playlistId: String) : PlaylistHandlerInterface {
    override fun onCreate(view: View) {
        loadPlaylistContents(view, false)
    }

    private var recyclerView: RecyclerView? = null
    private var itemAdapter = ItemAdapter<CatalogItem>()
    private var headerAdapter = ItemAdapter<HeaderTextItem>()

    private var viewData = ArrayList<CatalogItem>()

    private var itemHeaderOffset = 0

    private fun setupRecyclerView(view: View) {
        recyclerView = view.findViewById(R.id.playlistView)

        itemAdapter = ItemAdapter()
        headerAdapter = ItemAdapter()

        viewData = ArrayList()
        itemHeaderOffset = 0

        val fastAdapter: FastAdapter<GenericItem> =
            FastAdapter.with(listOf(headerAdapter, itemAdapter))

        recyclerView?.layoutManager =
            LinearLayoutManager(view.context, LinearLayoutManager.VERTICAL, false)

        recyclerView?.adapter = fastAdapter

        /**
         * On song click
         */
        fastAdapter.onClickListener = { _, _, _, position ->
            val itemIndex = position + itemHeaderOffset

            if (itemIndex >= 0) {
                playSongsFromViewDataAsync(
                    view.context,
                    Settings(view.context).getBoolean(view.context.getString(R.string.skipMonstercatSongsSetting)) == true,
                    viewData,
                    itemIndex
                )
            }

            false
        }

        /**
         * On song long click
         */
        fastAdapter.onLongClickListener = { _, _, _, position ->
            val itemIndex = position + itemHeaderOffset

            if (itemIndex >= 0) {
                val idList = ArrayList<String>()

                for (catalogItem in viewData) {
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
                val itemIndex = position + itemHeaderOffset

                if (itemIndex >= 0) {
                    val idList = ArrayList<String>()

                    for (catalogItem in viewData) {
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
    }

    private fun addHeader(headerText: String) {
        headerAdapter.add(HeaderTextItem(headerText))
        itemHeaderOffset += -1
    }

    private fun addSong(songId: String) {
        val item = CatalogItem(songId)

        viewData.add(item)
        itemAdapter.add(item)

        val cache = Cache()
        var cacheList = cache.get<ArrayList<String>>("playlist-view-$playlistId")

        if (cacheList == null) {
            cacheList = ArrayList()
        }

        cacheList.add(songId)
        cache.set("playlist-view-$playlistId", cacheList)
    }

    private fun addSongFromCache(songId: String) {
        val item = CatalogItem(songId)

        viewData.add(item)
        itemAdapter.add(item)
    }

    private fun loadPlaylistContents(view: View, forceReload: Boolean) {
        setupRecyclerView(view)

        val swipeRefreshLayout =
            view.findViewById<SwipeRefreshLayout>(R.id.playlistSwipeRefresh)

        val playlistContentsCache = Cache().get<ArrayList<String>>("playlist-view-$playlistId")

        lastOpenId = playlistId
        lastOpenType = "playlist"

        if (playlistContentsCache.isNullOrEmpty() || forceReload) {
            loadPlaylistTracksAsync(
                view.context,
                forceReload,
                playlistId, displayLoading = {
                    swipeRefreshLayout.isRefreshing = true
                }
                , finishedCallback = { _, _, _ ->
                    PlaylistDatabaseHelper(view.context).getPlaylist(playlistId)?.playlistName?.let {
                        addHeader(it)
                    }

                    val playlistItemDatabaseHelper =
                        PlaylistItemDatabaseHelper(
                            view.context,
                            playlistId
                        )

                    val playlistItems = playlistItemDatabaseHelper.getAllData()

                    for (i in (playlistItems.size - 1 downTo 0)) {
                        addSong(playlistItems[i].songId)
                    }

                    //refresh
                    swipeRefreshLayout.setOnRefreshListener {
                        loadPlaylistContents(view, true)
                    }

                    swipeRefreshLayout.isRefreshing = false

                }, errorCallback = { _, _, _ ->
                    swipeRefreshLayout.isRefreshing = false
                    displaySnackBar(
                        view,
                        view.context.getString(R.string.errorLoadingPlaylistTracks),
                        view.context.getString(R.string.retry)
                    ) {
                        loadPlaylistContents(view, forceReload)
                    }
                })
        } else {
            PlaylistDatabaseHelper(view.context).getPlaylist(playlistId)?.playlistName?.let {
                addHeader(it)
            }

            for (songId in playlistContentsCache) {
                addSongFromCache(songId)
            }

            //refresh
            swipeRefreshLayout.setOnRefreshListener {
                loadPlaylistContents(view, true)
            }

            swipeRefreshLayout.isRefreshing = false

            restoreRecyclerViewPosition(view.context)
            resetRecyclerViewSavedPosition(view.context)

        }
    }

    override fun resetRecyclerViewSavedPosition(context: Context) {
        val settings = Settings.getSettings(context)
        settings.setInt("playlistview-$playlistId-positionIndex", 0)
        settings.setInt("playlistview-$playlistId-topView", 0)
    }

    private fun restoreRecyclerViewPosition(context: Context) {
        recyclerView?.let {
            val settings = Settings.getSettings(context)
            settings.getInt("playlistview-$playlistId-positionIndex")?.let { positionIndex ->
                settings.getInt("playlistview-$playlistId-topView")?.let { topView ->
                    val layoutManager = it.layoutManager as LinearLayoutManager
                    layoutManager.scrollToPositionWithOffset(positionIndex, topView)
                }
            }
        }
    }

    override fun saveRecyclerViewPosition(context: Context) {
        recyclerView?.let {
            val layoutManager = it.layoutManager as LinearLayoutManager

            val positionIndex = layoutManager.findFirstVisibleItemPosition()
            val startView = it.getChildAt(0)

            startView?.let { sView ->
                val topView = sView.top - sView.paddingTop

                val settings = Settings.getSettings(context)
                settings.setInt("playlistview-$playlistId-positionIndex", positionIndex)
                settings.setInt("playlistview-$playlistId-topView", topView)
            }
        }
    }
}