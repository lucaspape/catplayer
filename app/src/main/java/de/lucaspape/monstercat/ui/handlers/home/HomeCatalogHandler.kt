package de.lucaspape.monstercat.ui.handlers.home

import android.content.Context
import android.view.View
import android.widget.ImageButton
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.listeners.ClickEventHook
import com.mikepenz.fastadapter.scroll.EndlessRecyclerOnScrollListener
import de.lucaspape.monstercat.ui.abstract_items.content.CatalogItem
import de.lucaspape.monstercat.ui.abstract_items.util.HeaderTextItem
import de.lucaspape.monstercat.ui.abstract_items.util.ProgressItem
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.database.helper.AlbumDatabaseHelper
import de.lucaspape.monstercat.database.helper.AlbumItemDatabaseHelper
import de.lucaspape.monstercat.database.helper.CatalogSongDatabaseHelper
import de.lucaspape.monstercat.database.helper.SongDatabaseHelper
import de.lucaspape.monstercat.database.objects.Album
import de.lucaspape.monstercat.download.addDownloadSong
import de.lucaspape.monstercat.request.async.loadAlbumAsync
import de.lucaspape.monstercat.request.async.loadSongListAsync
import de.lucaspape.monstercat.ui.activities.lastOpenType
import de.lucaspape.monstercat.ui.activities.lastOpenedAlbumId
import de.lucaspape.monstercat.ui.handlers.loadAlbumTracks
import de.lucaspape.monstercat.ui.handlers.playSongsFromCatalogDbAsync
import de.lucaspape.monstercat.ui.handlers.playSongsFromViewDataAsync
import de.lucaspape.monstercat.util.displaySnackBar
import de.lucaspape.util.Cache
import de.lucaspape.util.Settings
import java.io.File

class HomeCatalogHandler(
    private val albumId: String?,
    private val albumMcId: String?
) : HomeHandlerInterface {

    override fun onCreate(view: View) {
        if (albumMcId != null) {
            loadAlbum(view, albumId, albumMcId, false)
        } else {
            loadInitSongList(view, false)
        }
    }

    private var recyclerView: RecyclerView? = null
    private var itemAdapter = ItemAdapter<CatalogItem>()
    private var headerAdapter = ItemAdapter<HeaderTextItem>()
    private var footerAdapter = ItemAdapter<ProgressItem>()

    private var viewData = ArrayList<CatalogItem>()

    private var itemHeaderOffset = 0

    private fun setupRecyclerView(view: View) {
        recyclerView = view.findViewById(R.id.homeRecyclerView)

        itemAdapter = ItemAdapter()
        headerAdapter = ItemAdapter()
        footerAdapter = ItemAdapter()

        itemHeaderOffset = 0

        viewData = ArrayList()

        val fastAdapter: FastAdapter<GenericItem> =
            FastAdapter.with(listOf(headerAdapter, itemAdapter, footerAdapter))

        recyclerView?.layoutManager =
            LinearLayoutManager(view.context, LinearLayoutManager.VERTICAL, false)

        recyclerView?.adapter = fastAdapter

        /**
         * On song click
         */
        fastAdapter.onClickListener = { _, _, _, position ->
            val itemIndex = position + itemHeaderOffset

            if (itemIndex >= 0 && itemIndex < viewData.size) {
                val fistItem = viewData[itemIndex]

                val skipMonstercatSongs =
                    Settings(view.context).getBoolean(view.context.getString(R.string.skipMonstercatSongsSetting)) == true

                if (albumId == null) {
                    playSongsFromCatalogDbAsync(
                        view.context,
                        skipMonstercatSongs,
                        fistItem.songId
                    )
                } else {
                    playSongsFromViewDataAsync(
                        view.context,
                        skipMonstercatSongs,
                        viewData,
                        itemIndex
                    )
                }
            }

            false
        }

        /**
         * On song long click
         */
        fastAdapter.onLongClickListener = { _, _, _, position ->
            val itemIndex = position + itemHeaderOffset

            if (itemIndex >= 0 && itemIndex < viewData.size) {
                val idList = ArrayList<String>()

                for (catalogItem in viewData) {
                    idList.add(catalogItem.songId)
                }

                CatalogItem.showContextMenu(view, idList, itemIndex)
            }

            false
        }

        /**
         * On menu button click
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

                if (itemIndex >= 0 && itemIndex < viewData.size) {
                    val idList = ArrayList<String>()

                    for (catalogItem in viewData) {
                        idList.add(catalogItem.songId)
                    }

                    CatalogItem.showContextMenu(view, idList, itemIndex)
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
                            titleDownloadButton.setImageURI(song.downloadStatus)
                        }
                        else -> {
                            addDownloadSong(
                                v.context,
                                item.songId
                            ) {
                                titleDownloadButton.setImageURI(
                                    song.downloadStatus
                                )
                            }
                        }
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

    private fun addSong(songId: String) {
        val item =
            CatalogItem(songId)

        viewData.add(item)
        itemAdapter.add(item)

        val cache = Cache()
        var cacheList = cache.get<ArrayList<String>>("catalog-view")

        if (cacheList == null) {
            cacheList = ArrayList()
        }

        cacheList.add(songId)
        cache.set("catalog-view", cacheList)
    }

    private fun addSongFromCache(songId: String) {
        val item =
            CatalogItem(songId)

        viewData.add(item)
        itemAdapter.add(item)
    }

    private fun loadInitSongList(view: View, forceReload: Boolean) {
        val songListCache = Cache().get<ArrayList<String>>("catalog-view")

        lastOpenType = "catalog-list"
        lastOpenedAlbumId = ""

        val swipeRefreshLayout =
            view.findViewById<SwipeRefreshLayout>(R.id.homePullToRefresh)

        if (songListCache.isNullOrEmpty() || forceReload) {
            Cache().set("catalog-view", null)

            val catalogSongDatabaseHelper =
                CatalogSongDatabaseHelper(view.context)

            if (forceReload) {
                catalogSongDatabaseHelper.reCreateTable()
            }

            loadSongListAsync(view.context, forceReload, 0, displayLoading = {
                swipeRefreshLayout.isRefreshing = true
            }, finishedCallback = { _, _, _ ->
                setupRecyclerView(view)

                val songList = catalogSongDatabaseHelper.getSongs(0, 50)

                for (song in songList) {
                    addSong(song.songId)
                }

                /**
                 * On scroll down (load next)
                 */
                recyclerView?.addOnScrollListener(object :
                    EndlessRecyclerOnScrollListener(footerAdapter) {
                    override fun onLoadMore(currentPage: Int) {
                        loadSongList(view, currentPage)
                    }
                })

                //refresh
                swipeRefreshLayout.setOnRefreshListener {
                    loadInitSongList(view, true)
                }

                swipeRefreshLayout.isRefreshing = false
            }, errorCallback = { _, _, _ ->
                swipeRefreshLayout.isRefreshing = false

                displaySnackBar(
                    view,
                    view.context.getString(R.string.errorLoadingSongList),
                    view.context.getString(R.string.retry)
                ) {
                    loadInitSongList(view, forceReload)
                }
            })
        } else {
            setupRecyclerView(view)

            for (songId in songListCache) {
                addSongFromCache(songId)
            }

            /**
             * On scroll down (load next)
             */
            recyclerView?.addOnScrollListener(object :
                EndlessRecyclerOnScrollListener(footerAdapter) {
                override fun onLoadMore(currentPage: Int) {
                    loadSongList(view, currentPage)
                }
            })

            //refresh
            swipeRefreshLayout.setOnRefreshListener {
                loadInitSongList(view, true)
            }

            swipeRefreshLayout.isRefreshing = false

            restoreRecyclerViewPosition(view.context)
            resetRecyclerViewSavedPosition(view.context)
        }
    }

    /**
     * Loads next 50 songs
     */
    private fun loadSongList(
        view: View,
        currentPage: Int
    ) {
        footerAdapter.clear()
        footerAdapter.add(ProgressItem())

        lastOpenType = "catalog-list"
        lastOpenedAlbumId = ""

        loadSongListAsync(view.context,
            false,
            (currentPage * 50),
            displayLoading = {},
            finishedCallback = { _, _, _ ->
                val catalogSongDatabaseHelper =
                    CatalogSongDatabaseHelper(view.context)

                val songList =
                    catalogSongDatabaseHelper.getSongs((currentPage * 50).toLong(), 50)

                for (song in songList) {
                    addSong(song.songId)
                }

                footerAdapter.clear()

                //DONE
            },
            errorCallback = { _, _, _ ->
                footerAdapter.clear()

                displaySnackBar(
                    view,
                    view.context.getString(R.string.errorLoadingSongList),
                    view.context.getString(R.string.retry)
                ) {
                    loadSongList(view, currentPage)
                }
            })
    }

    private fun loadAlbum(view: View, albumId: String?, albumMcId: String, forceReload: Boolean) {
        val swipeRefreshLayout =
            view.findViewById<SwipeRefreshLayout>(R.id.homePullToRefresh)

        setupRecyclerView(view)

        lastOpenType = "album"
        lastOpenedAlbumId = albumMcId

        if (albumId == null) {
            val albumDatabaseHelper = AlbumDatabaseHelper(view.context)

            val finished: (album: Album) -> Unit = { album ->
                AlbumDatabaseHelper(view.context).getAlbum(album.albumId)?.let {
                    addHeader(it.title)
                }

                val albumItemDatabaseHelper =
                    AlbumItemDatabaseHelper(view.context, album.albumId)
                val albumItemList = albumItemDatabaseHelper.getAllData()

                for (albumItem in albumItemList) {
                    addSongFromCache(albumItem.songId)
                }

                /**
                 * On scroll down (load next)
                 */
                recyclerView?.addOnScrollListener(object :
                    EndlessRecyclerOnScrollListener(footerAdapter) {
                    override fun onLoadMore(currentPage: Int) {
                    }
                })

                //refresh
                swipeRefreshLayout.setOnRefreshListener {
                    loadAlbum(view, albumId, albumMcId, true)
                }

                swipeRefreshLayout.isRefreshing = false
            }

            val album = albumDatabaseHelper.getAlbumFromMcId(albumMcId)

            if (album == null || forceReload) {
                loadAlbumTracks(
                    view.context,
                    albumMcId,
                    finishedCallback = {
                        albumDatabaseHelper.getAlbumFromMcId(albumMcId)?.let { album ->
                            finished(album)
                        }
                    },
                    errorCallback = {
                        //TODO handle error
                        swipeRefreshLayout.isRefreshing = false
                    })
            } else {
                finished(album)
            }
        } else {
            loadAlbumAsync(view.context, forceReload, albumId, albumMcId, {
                swipeRefreshLayout.isRefreshing = true
            }, { _, _, _, _ ->
                AlbumDatabaseHelper(view.context).getAlbum(albumId)?.let {
                    addHeader(it.title)
                }

                val albumItemDatabaseHelper =
                    AlbumItemDatabaseHelper(view.context, albumId)

                val albumItemList = albumItemDatabaseHelper.getAllData()

                for (albumItem in albumItemList) {
                    addSongFromCache(albumItem.songId)
                }

                /**
                 * On scroll down (load next)
                 */
                recyclerView?.addOnScrollListener(object :
                    EndlessRecyclerOnScrollListener(footerAdapter) {
                    override fun onLoadMore(currentPage: Int) {
                    }
                })

                //refresh
                swipeRefreshLayout.setOnRefreshListener {
                    loadAlbum(view, albumId, albumMcId, true)
                }

                swipeRefreshLayout.isRefreshing = false

            }, { _, _, _, _ ->
                swipeRefreshLayout.isRefreshing = false

                displaySnackBar(
                    view,
                    view.context.getString(R.string.errorLoadingAlbum),
                    view.context.getString(R.string.retry)
                ) {
                    loadAlbum(view, albumId, albumMcId, forceReload)
                }
            })
        }
    }

    override fun resetRecyclerViewSavedPosition(context: Context) {
        val settings = Settings.getSettings(context)
        settings.setInt("catalogview-positionIndex", 0)
        settings.setInt("catalogview-topView", 0)
    }

    private fun restoreRecyclerViewPosition(context: Context) {
        recyclerView?.let {
            val settings = Settings.getSettings(context)
            settings.getInt("catalogview-positionIndex")?.let { positionIndex ->
                settings.getInt("catalogview-topView")?.let { topView ->
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
                settings.setInt("catalogview-positionIndex", positionIndex)
                settings.setInt("catalogview-topView", topView)
            }
        }
    }
}