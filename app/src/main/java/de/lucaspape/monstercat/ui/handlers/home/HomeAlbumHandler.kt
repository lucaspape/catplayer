package de.lucaspape.monstercat.ui.handlers.home

import android.content.Context
import android.content.res.Configuration
import android.view.View
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.scroll.EndlessRecyclerOnScrollListener
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.core.database.helper.AlbumDatabaseHelper
import de.lucaspape.monstercat.request.async.loadAlbumListAsync
import de.lucaspape.monstercat.ui.abstract_items.content.AlbumItem
import de.lucaspape.monstercat.ui.abstract_items.util.ProgressItem
import de.lucaspape.monstercat.ui.activities.lastOpenType
import de.lucaspape.monstercat.util.displaySnackBar
import de.lucaspape.util.Cache
import de.lucaspape.util.Settings

class HomeAlbumHandler(private val onSingleAlbumLoad: (albumId: String, albumMcId: String) -> Unit) :
    HomeHandlerInterface {
    override fun onCreate(view: View) {
        setupRecyclerView(view)
        loadInitAlbumList(view, false)
    }

    private var recyclerView: RecyclerView? = null
    private var itemAdapter = ItemAdapter<AlbumItem>()
    private var footerAdapter = ItemAdapter<ProgressItem>()

    private var viewData = ArrayList<AlbumItem>()

    private fun setupRecyclerView(view: View) {
        recyclerView = view.findViewById(R.id.homeRecyclerView)

        itemAdapter = ItemAdapter()
        footerAdapter = ItemAdapter()

        viewData = ArrayList()

        val fastAdapter: FastAdapter<GenericItem> =
            FastAdapter.with(listOf(itemAdapter, footerAdapter))

        val orientation = if(view.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE){
            LinearLayout.HORIZONTAL
        }else{
            LinearLayout.VERTICAL
        }

        recyclerView?.layoutManager =
            LinearLayoutManager(view.context, orientation, false)

        recyclerView?.adapter = fastAdapter

        val albumDatabaseHelper = AlbumDatabaseHelper(view.context)

        /**
         * On item click
         */
        fastAdapter.onClickListener = { _, _, _, position ->
            if (position < viewData.size) {
                val albumItem = viewData[position]

                albumDatabaseHelper.getAlbum(albumItem.albumId)?.mcID?.let { mcID ->
                    onSingleAlbumLoad(albumItem.albumId, mcID)
                }

            }

            false
        }

        /**
         * On item long click
         */
        fastAdapter.onLongClickListener = { _, _, _, position ->
            if (position < viewData.size) {
                val albumMcIdList = ArrayList<String>()

                for (albumItem in viewData) {
                    albumDatabaseHelper.getAlbum(albumItem.albumId)?.mcID?.let { mcID ->
                        albumMcIdList.add(mcID)
                    }
                }

                AlbumItem.showContextMenu(view, albumMcIdList, position)
            }

            false
        }
    }

    private fun addAlbum(view: View, albumId: String) {
        val item =
            AlbumItem(albumId, (view.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE))

        itemAdapter.add(item)
        viewData.add(item)

        val cache = Cache()
        var cacheList = cache.get<ArrayList<String>>("album-view")

        if (cacheList == null) {
            cacheList = ArrayList()
        }

        cacheList.add(albumId)
        cache.set("album-view", cacheList)
    }

    private fun addAlbumFromCache(view: View, albumId: String) {
        val item =
            AlbumItem(albumId, (view.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE))

        itemAdapter.add(item)
        viewData.add(item)
    }

    private fun loadInitAlbumList(view: View, forceReload: Boolean) {
        val swipeRefreshLayout =
            view.findViewById<SwipeRefreshLayout>(R.id.homePullToRefresh)

        val albumListCache = Cache().get<ArrayList<String>>("album-view")

        lastOpenType = "album-list"

        if (albumListCache.isNullOrEmpty() || forceReload) {
            Cache().set("album-view", null)

            val albumDatabaseHelper = AlbumDatabaseHelper(view.context)

            if (forceReload) {
                albumDatabaseHelper.reCreateTable(view.context, false)
            }

            loadAlbumListAsync(view.context, forceReload, 0, {
                swipeRefreshLayout.isRefreshing = true
            }, { _, _, _ ->
                setupRecyclerView(view)

                val albumList = albumDatabaseHelper.getAlbums(0, 50)

                for (album in albumList) {
                    addAlbum(view, album.albumId)
                }

                /**
                 * On scroll down (load next)
                 */
                recyclerView?.addOnScrollListener(object :
                    EndlessRecyclerOnScrollListener(footerAdapter) {
                    override fun onLoadMore(currentPage: Int) {
                        loadAlbumList(view, currentPage)
                    }
                })

                //refresh
                swipeRefreshLayout.setOnRefreshListener {
                    loadInitAlbumList(view, true)
                }

                swipeRefreshLayout.isRefreshing = false
            }, { _, _, _ ->
                swipeRefreshLayout.isRefreshing = false

                displaySnackBar(
                    view,
                    view.context.getString(R.string.errorLoadingAlbumList),
                    view.context.getString(R.string.retry)
                ) {
                    loadInitAlbumList(view, forceReload)
                }
            })
        } else {
            setupRecyclerView(view)

            for (albumId in albumListCache) {
                addAlbumFromCache(view, albumId)
            }

            /**
             * On scroll down (load next)
             */
            recyclerView?.addOnScrollListener(object :
                EndlessRecyclerOnScrollListener(footerAdapter) {
                override fun onLoadMore(currentPage: Int) {
                    loadAlbumList(view, currentPage)
                }
            })

            //refresh
            swipeRefreshLayout.setOnRefreshListener {
                loadInitAlbumList(view, true)
            }

            swipeRefreshLayout.isRefreshing = false

            restoreRecyclerViewPosition(view.context)
        }
    }

    private fun loadAlbumList(view: View, currentPage: Int) {
        recyclerView?.post {
            footerAdapter.clear()
            footerAdapter.add(ProgressItem())

            loadAlbumListAsync(view.context, false, (currentPage * 50), {}, { _, _, _ ->
                val albumDatabaseHelper =
                    AlbumDatabaseHelper(view.context)
                val albumList =
                    albumDatabaseHelper.getAlbums((currentPage * 50).toLong(), 50)

                for (album in albumList) {
                    addAlbum(view, album.albumId)
                }

                footerAdapter.clear()
            }, { _, _, _ ->
                footerAdapter.clear()

                displaySnackBar(
                    view,
                    view.context.getString(R.string.errorLoadingAlbumList),
                    view.context.getString(R.string.retry)
                ) {
                    loadAlbumList(view, currentPage)
                }
            })
        }
    }

    override fun resetRecyclerViewSavedPosition(context: Context) {
        val settings = Settings.getSettings(context)
        settings.setInt("albumview-positionIndex", 0)
        settings.setInt("albumview-topView", 0)
    }

    private fun restoreRecyclerViewPosition(context: Context) {
        recyclerView?.let {
            val settings = Settings.getSettings(context)
            settings.getInt("albumview-positionIndex")?.let { positionIndex ->
                settings.getInt("albumview-topView")?.let { topView ->
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
                settings.setInt("albumview-positionIndex", positionIndex)
                settings.setInt("albumview-topView", topView)
            }
        }
    }

}