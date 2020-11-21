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
import de.lucaspape.monstercat.core.database.helper.PlaylistDatabaseHelper
import de.lucaspape.monstercat.request.async.loadPlaylistAsync
import de.lucaspape.monstercat.ui.abstract_items.util.HeaderTextItem
import de.lucaspape.monstercat.ui.abstract_items.content.PlaylistItem
import de.lucaspape.monstercat.ui.activities.lastOpenType
import de.lucaspape.monstercat.ui.activities.lastOpenedPlaylistId
import de.lucaspape.monstercat.ui.displaySnackBar
import de.lucaspape.monstercat.ui.handlers.deleteDownloadedPlaylistTracks
import de.lucaspape.monstercat.ui.handlers.downloadPlaylistAsync
import de.lucaspape.monstercat.ui.offlineDrawable
import de.lucaspape.util.Cache
import de.lucaspape.util.Settings

class PlaylistListHandler(private val loadPlaylist: (playlistId: String) -> Unit) :
    PlaylistHandlerInterface {
    override fun onCreate(view: View) {
        loadPlaylistList(view, false)
    }

    private var recyclerView: RecyclerView? = null
    private var itemAdapter = ItemAdapter<PlaylistItem>()
    private var headerAdapter = ItemAdapter<HeaderTextItem>()

    private var viewData = ArrayList<PlaylistItem>()

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
         * On playlist click
         */
        fastAdapter.onClickListener = { _, _, _, position ->
            val itemIndex = position + itemHeaderOffset

            if (itemIndex >= 0) {
                loadPlaylist(viewData[itemIndex].playlistId)
            }

            false
        }

        /**
         * On playlist long click
         */
        fastAdapter.onLongClickListener = { _, _, _, position ->
            val itemIndex = position + itemHeaderOffset

            if (itemIndex >= 0) {
                val playlistIdList = ArrayList<String>()

                for (playlist in viewData) {
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
                val itemIndex = position + itemHeaderOffset

                if (itemIndex >= 0) {
                    val playlistIdList = ArrayList<String>()

                    for (playlist in viewData) {
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

                    downloadPlaylistAsync(
                        view,
                        item.playlistId
                    ) {
                        titleDownloadButton.setImageURI(
                            item.getDownloadStatus(view.context).toUri()
                        )
                    }
                }
            }
        })
    }

    private fun addHeader(headerText: String) {
        headerAdapter.add(
            HeaderTextItem(
                headerText
            )
        )
        itemHeaderOffset += -1
    }

    private fun addPlaylist(playlistId: String) {
        val item =
            PlaylistItem(
                playlistId
            )

        viewData.add(item)
        itemAdapter.add(item)

        val cache = Cache()
        var cacheList = cache.get<ArrayList<String>>("playlist-view-list")

        if (cacheList == null) {
            cacheList = ArrayList()
        }

        cacheList.add(playlistId)
        cache.set("playlist-view-list", cacheList)
    }

    private fun addPlaylistFromCache(playlistId: String) {
        val item =
            PlaylistItem(
                playlistId
            )

        viewData.add(item)
        itemAdapter.add(item)
    }

    private fun loadPlaylistList(view: View, forceReload: Boolean) {
        setupRecyclerView(view)

        lastOpenedPlaylistId = ""
        lastOpenType = "playlist-list"

        val swipeRefreshLayout =
            view.findViewById<SwipeRefreshLayout>(R.id.playlistSwipeRefresh)

        val playlistListCache = Cache().get<ArrayList<String>>("playlist-view-list")

        if (playlistListCache.isNullOrEmpty() || forceReload) {
            Cache().set("playlist-view-list", null)

            loadPlaylistAsync(view.context, forceReload, true, {}, { _, _ ->
                addHeader(view.context.getString(R.string.yourPlaylists))

                val playlistDatabaseHelper =
                    PlaylistDatabaseHelper(view.context)
                val playlists = playlistDatabaseHelper.getAllPlaylists()

                for (playlist in playlists) {
                    addPlaylist(playlist.playlistId)
                }

                //refresh
                swipeRefreshLayout.setOnRefreshListener {
                    loadPlaylistList(view, true)
                }

                swipeRefreshLayout.isRefreshing = false
            }, { _, _ ->
                swipeRefreshLayout.isRefreshing = false
                displaySnackBar(
                    view,
                    view.context.getString(R.string.errorLoadingPlaylists),
                    view.context.getString(R.string.retry)
                ) { loadPlaylistList(view, forceReload) }
            })
        } else {
            addHeader(view.context.getString(R.string.yourPlaylists))

            for (playlistId in playlistListCache) {
                addPlaylistFromCache(playlistId)
            }

            //refresh
            swipeRefreshLayout.setOnRefreshListener {
                loadPlaylistList(view, true)
            }

            swipeRefreshLayout.isRefreshing = false

            restoreRecyclerViewPosition(view.context)
            resetRecyclerViewSavedPosition(view.context)
        }
    }

    override fun resetRecyclerViewSavedPosition(context: Context) {
        val settings = Settings.getSettings(context)
        settings.setInt("playlistview-list-positionIndex", 0)
        settings.setInt("playlistview-list-topView", 0)
    }

    private fun restoreRecyclerViewPosition(context: Context) {
        recyclerView?.let {
            val settings = Settings.getSettings(context)
            settings.getInt("playlistview-list-positionIndex")?.let { positionIndex ->
                settings.getInt("playlistview-list-topView")?.let { topView ->
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
                settings.setInt("playlistview-list-positionIndex", positionIndex)
                settings.setInt("playlistview-list-topView", topView)
            }
        }
    }
}