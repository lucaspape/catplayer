package de.lucaspape.monstercat.ui.pages.util

import android.annotation.SuppressLint
import android.content.Context

import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.listeners.ClickEventHook
import com.mikepenz.fastadapter.scroll.EndlessRecyclerOnScrollListener
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.core.util.Settings
import de.lucaspape.monstercat.ui.abstract_items.content.CatalogItem
import de.lucaspape.monstercat.ui.abstract_items.util.HeaderTextItem
import de.lucaspape.monstercat.ui.abstract_items.util.ProgressItem
import de.lucaspape.monstercat.ui.displaySnackBar
import de.lucaspape.util.Cache

abstract class RecyclerViewPage(private val cacheId: String) {
    abstract fun onItemClick(context: Context, viewData: ArrayList<GenericItem>, itemIndex: Int)
    abstract fun onItemLongClick(view: View, viewData: ArrayList<GenericItem>, itemIndex: Int)
    abstract fun onMenuButtonClick(view: View, viewData: ArrayList<GenericItem>, itemIndex: Int)
    abstract fun onDownloadButtonClick(
        context: Context,
        item: GenericItem,
        downloadImageButton: ImageButton
    )

    abstract fun idToAbstractItem(view: View, id: String): GenericItem
    abstract fun load(
        context: Context,
        forceReload: Boolean,
        skip: Int,
        displayLoading: () -> Unit,
        callback: (itemIdList: ArrayList<String>) -> Unit,
        errorCallback: (errorMessage: String) -> Unit
    )

    abstract fun getHeader(context: Context): String?

    private var recyclerView: RecyclerView? = null
    private var itemAdapter = ItemAdapter<GenericItem>()
    private var headerAdapter = ItemAdapter<HeaderTextItem>()
    private var footerAdapter = ItemAdapter<ProgressItem>()

    private var viewData = ArrayList<GenericItem>()

    private var itemHeaderOffset = 0

    open fun onCreate(view: View) {
        loadInit(view, false)
    }

    open fun clearDatabase(context: Context) {

    }

    open fun getOrientation(view:View): Int {
        return LinearLayout.VERTICAL
    }

    @SuppressLint("WrongConstant")
    private fun setupRecyclerView(view: View) {
        recyclerView = view.findViewById(R.id.recyclerView)

        itemAdapter = ItemAdapter()
        headerAdapter = ItemAdapter()
        footerAdapter = ItemAdapter()

        itemHeaderOffset = 0

        viewData = ArrayList()

        val fastAdapter: FastAdapter<GenericItem> =
            FastAdapter.with(listOf(headerAdapter, itemAdapter, footerAdapter))

        recyclerView?.layoutManager =
            LinearLayoutManager(view.context, getOrientation(view), false)

        recyclerView?.adapter = fastAdapter

        //OnClick
        fastAdapter.onClickListener = { _, _, _, position ->
            val itemIndex = position + itemHeaderOffset

            if (itemIndex >= 0 && itemIndex < viewData.size) {
                onItemClick(view.context, viewData, itemIndex)
            }

            false
        }

        /**
         * On long click
         */
        fastAdapter.onLongClickListener = { _, _, _, position ->
            val itemIndex = position + itemHeaderOffset

            if (itemIndex >= 0 && itemIndex < viewData.size) {
                onItemLongClick(view, viewData, itemIndex)

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
                    onMenuButtonClick(view, viewData, itemIndex)
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
                onDownloadButtonClick(view.context, item, v as ImageButton)
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

    private fun addItem(item: GenericItem, id: String) {
        viewData.add(item)
        itemAdapter.add(item)

        val cache = Cache()
        var cacheList = cache.get<ArrayList<String>>(cacheId)

        if (cacheList == null) {
            cacheList = ArrayList()
        }

        cacheList.add(id)
        cache.set(cacheId, cacheList)
    }

    private fun addItemFromCache(item: GenericItem) {
        viewData.add(item)
        itemAdapter.add(item)
    }

    fun loadInit(view: View, forceReload: Boolean) {
        val cache = Cache().get<ArrayList<String>>(cacheId)

        val swipeRefreshLayout =
            view.findViewById<SwipeRefreshLayout>(R.id.swipeRefresh)

        if (cache.isNullOrEmpty() || forceReload) {
            Cache().set(cacheId, null)
            clearDatabase(view.context)

            load(view.context, forceReload, 0, displayLoading = {
                swipeRefreshLayout.isRefreshing = true
            }, callback = { idList ->
                setupRecyclerView(view)

                for (id in idList) {
                    addItem(idToAbstractItem(view, id), id)
                }

                getHeader(view.context)?.let {
                    addHeader(it)
                }

                /**
                 * On scroll down (load next)
                 */
                recyclerView?.addOnScrollListener(object :
                    EndlessRecyclerOnScrollListener(footerAdapter) {
                    override fun onLoadMore(currentPage: Int) {
                        loadNext(view, currentPage)
                    }
                })

                //refresh
                swipeRefreshLayout.setOnRefreshListener {
                    loadInit(view, true)
                }

                swipeRefreshLayout.isRefreshing = false

            }, errorCallback = { errorMessage ->
                swipeRefreshLayout.isRefreshing = false

                displaySnackBar(
                    view,
                    errorMessage,
                    view.context.getString(R.string.retry)
                ) {
                    loadInit(view, forceReload)
                }
            })
        } else {
            setupRecyclerView(view)

            for (id in cache) {
                addItemFromCache(idToAbstractItem(view, id))
            }

            getHeader(view.context)?.let {
                addHeader(it)
            }

            /**
             * On scroll down (load next)
             */
            recyclerView?.addOnScrollListener(object :
                EndlessRecyclerOnScrollListener(footerAdapter) {
                override fun onLoadMore(currentPage: Int) {
                    loadNext(view, currentPage)
                }
            })

            //refresh
            swipeRefreshLayout.setOnRefreshListener {
                loadInit(view, true)
            }

            swipeRefreshLayout.isRefreshing = false

            restoreRecyclerViewPosition(view.context)
        }
    }

    private fun loadNext(view: View, currentPage: Int) {
        recyclerView?.post {
            footerAdapter.clear()
            footerAdapter.add(ProgressItem())

            load(
                view.context,
                false,
                (currentPage * 50),
                displayLoading = {},
                callback = { idList ->
                    for (id in idList) {
                        addItem(idToAbstractItem(view, id), id)
                    }

                    footerAdapter.clear()

                },
                errorCallback = { errorMessage ->
                    footerAdapter.clear()

                    displaySnackBar(
                        view,
                        errorMessage,
                        view.context.getString(R.string.retry)
                    ) {
                        loadNext(view, currentPage)
                    }
                })
        }
    }

    fun resetRecyclerViewSavedPosition(context: Context) {
        val settings = Settings.getSettings(context)
        settings.setInt("$cacheId-positionIndex", 0)
        settings.setInt("$cacheId-topView", 0)
    }

    private fun restoreRecyclerViewPosition(context: Context) {
        recyclerView?.let {
            val settings = Settings.getSettings(context)
            settings.getInt("$cacheId-positionIndex")?.let { positionIndex ->
                settings.getInt("$cacheId-topView")?.let { topView ->
                    val layoutManager = it.layoutManager as LinearLayoutManager
                    layoutManager.scrollToPositionWithOffset(positionIndex, topView)
                }
            }
        }
    }

    fun saveRecyclerViewPosition(context: Context) {
        recyclerView?.let {
            val layoutManager = it.layoutManager as LinearLayoutManager

            val positionIndex = layoutManager.findFirstVisibleItemPosition()
            val startView = it.getChildAt(0)

            startView?.let { sView ->
                val topView = sView.top - sView.paddingTop

                val settings = Settings.getSettings(context)
                settings.setInt("$cacheId-positionIndex", positionIndex)
                settings.setInt("$cacheId-topView", topView)
            }
        }
    }
}