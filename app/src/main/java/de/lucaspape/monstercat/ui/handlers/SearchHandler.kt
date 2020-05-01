package de.lucaspape.monstercat.ui.handlers

import android.os.AsyncTask
import android.view.View
import android.widget.ImageButton
import android.widget.SearchView
import androidx.core.net.toUri
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.listeners.ClickEventHook
import com.mikepenz.fastadapter.scroll.EndlessRecyclerOnScrollListener
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.database.helper.SongDatabaseHelper
import de.lucaspape.monstercat.download.addDownloadSong
import de.lucaspape.monstercat.request.async.LoadTitleSearchAsync
import de.lucaspape.monstercat.ui.abstract_items.CatalogItem
import de.lucaspape.monstercat.ui.abstract_items.HeaderTextItem
import de.lucaspape.monstercat.ui.abstract_items.ProgressItem
import de.lucaspape.monstercat.util.displaySnackBar
import de.lucaspape.util.Settings
import java.io.File
import java.lang.ref.WeakReference
import kotlin.collections.ArrayList

class SearchHandler(
    private val search: String?,
    private val closeSearch: () -> Unit
) : Handler {
    override val layout: Int = R.layout.fragment_search

    override fun onBackPressed(view: View) {
        closeSearch()
    }

    override fun onPause(view: View) {

    }

    override fun onCreate(view: View) {
        registerListeners(view)

        val searchView = view.findViewById<SearchView>(R.id.searchInput)
        searchView.onActionViewExpanded()

        search?.let {
            searchSong(view, it, false)
        }
    }

    private fun registerListeners(view: View) {
        //search
        val search = view.findViewById<SearchView>(R.id.searchInput)

        search.setOnCloseListener {
            closeSearch()

            false
        }

        search.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }

            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let {
                    searchSong(view, it, false)
                }

                return false
            }
        })
    }

    private var recyclerView: RecyclerView? = null

    /**
     * Setup catalog view
     */
    private fun updateCatalogRecyclerView(
        view: View,
        catalogViewData: ArrayList<CatalogItem>,
        loadMoreListener: (itemAdapter: ItemAdapter<CatalogItem>, footerAdapter: ItemAdapter<ProgressItem>, currentPage: Int, callback: (newList: ArrayList<CatalogItem>) -> Unit) -> Unit,
        refreshListener: () -> Unit
    ) {
        recyclerView = view.findViewById(R.id.searchRecyclerView)

        recyclerView?.layoutManager =
            LinearLayoutManager(view.context, LinearLayoutManager.VERTICAL, false)

        val itemAdapter = ItemAdapter<CatalogItem>()
        val headerAdapter = ItemAdapter<HeaderTextItem>()
        val footerAdapter = ItemAdapter<ProgressItem>()

        val fastAdapter: FastAdapter<GenericItem> =
            FastAdapter.with(listOf(headerAdapter, itemAdapter, footerAdapter))

        val itemIndexOffset = 0

        recyclerView?.adapter = fastAdapter

        for (catalogItem in catalogViewData) {
            itemAdapter.add(
                catalogItem
            )
        }

        /**
         * On song click
         */
        fastAdapter.onClickListener = { _, _, _, position ->
            val itemIndex = position + itemIndexOffset

            if (itemIndex >= 0 && itemIndex < catalogViewData.size) {
                val skipMonstercatSongs =
                    Settings(view.context).getBoolean(view.context.getString(R.string.skipMonstercatSongsSetting)) == true

                playSongsFromViewDataAsync(view.context, skipMonstercatSongs, catalogViewData, itemIndex)
            }

            false
        }

        /**
         * On song long click
         */
        fastAdapter.onLongClickListener = { _, _, _, position ->
            val itemIndex = position + itemIndexOffset

            if (itemIndex >= 0 && itemIndex < catalogViewData.size) {
                val idList = ArrayList<String>()

                for (catalogItem in catalogViewData) {
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
                val itemIndex = position + itemIndexOffset

                if (itemIndex >= 0 && itemIndex < catalogViewData.size) {
                    val idList = ArrayList<String>()

                    for (catalogItem in catalogViewData) {
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

        /**
         * On scroll down (load next)
         */
        recyclerView?.addOnScrollListener(object :
            EndlessRecyclerOnScrollListener(footerAdapter) {
            override fun onLoadMore(currentPage: Int) {
                loadMoreListener(itemAdapter, footerAdapter, currentPage) {
                    for (catalogItem in it) {
                        catalogViewData.add(catalogItem)
                    }
                }
            }
        })

        val swipeRefreshLayout = view.findViewById<SwipeRefreshLayout>(R.id.searchPullToRefresh)
        swipeRefreshLayout.setOnRefreshListener {
            refreshListener()
        }
    }

    /**
     * Search for string
     */
    fun searchSong(view: View, searchString: String, forceReload: Boolean) {
        //search can also be performed without this view
        val search = view.findViewById<SearchView>(R.id.searchInput)
        search.onActionViewExpanded()
        search.setQuery(searchString, false)
        search.clearFocus()

        val contextReference = WeakReference(view.context)

        val swipeRefreshLayout =
            view.findViewById<SwipeRefreshLayout>(R.id.searchPullToRefresh)

        swipeRefreshLayout.isRefreshing = true

        LoadTitleSearchAsync(
            contextReference,
            searchString,
            0
            , finishedCallback = { _, _, searchResults ->
                updateCatalogRecyclerView(
                    view,
                    searchResults,
                    { itemAdapter, footerAdapter, currentPage, callback ->
                        footerAdapter.clear()
                        footerAdapter.add(ProgressItem())

                        searchMore(
                            view,
                            searchString,
                            itemAdapter,
                            footerAdapter,
                            currentPage,
                            callback
                        )
                    },
                    {
                        searchSong(view, searchString, true)
                    })

                swipeRefreshLayout.isRefreshing = false
            }, errorCallback = { _, _ ->
                swipeRefreshLayout.isRefreshing = false

                displaySnackBar(
                    view,
                    view.context.getString(R.string.errorLoadingSearch),
                    view.context.getString(R.string.retry)
                ) { searchSong(view, searchString, forceReload) }
            }).executeOnExecutor(
            AsyncTask.THREAD_POOL_EXECUTOR
        )

    }

    private fun searchMore(
        view: View,
        searchString: String,
        itemAdapter: ItemAdapter<CatalogItem>,
        footerAdapter: ItemAdapter<ProgressItem>,
        currentPage: Int,
        callback: (newList: ArrayList<CatalogItem>) -> Unit
    ) {
        val contextReference = WeakReference(view.context)

        val skip = currentPage * 50

        LoadTitleSearchAsync(
            contextReference,
            searchString,
            skip
            , finishedCallback = { _, _, searchResults ->
                for (result in searchResults) {
                    itemAdapter.add(result)
                }

                callback(searchResults)

                footerAdapter.clear()
            }, errorCallback = { _, _ ->
                footerAdapter.clear()

                displaySnackBar(
                    view,
                    view.context.getString(R.string.errorLoadingSearch),
                    view.context.getString(R.string.retry)
                ) {
                    searchMore(
                        view,
                        searchString,
                        itemAdapter,
                        footerAdapter,
                        currentPage,
                        callback
                    )
                }
            }).executeOnExecutor(
            AsyncTask.THREAD_POOL_EXECUTOR
        )
    }
}