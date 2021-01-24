package de.lucaspape.monstercat.ui.pages.util

import android.annotation.SuppressLint
import android.content.Context

import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.annotation.CallSuper
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.collections.ArrayList

abstract class RecyclerViewPage {
    val scope = CoroutineScope(Dispatchers.Default)

    private var lastClick:Long = 0

    private val blockClick:Boolean
        get() {
            return System.currentTimeMillis() - lastClick < 300
        }

    @CallSuper
    open suspend fun onItemClick(context: Context, viewData: ArrayList<GenericItem>, itemIndex: Int){
        lastClick = System.currentTimeMillis()
    }

    @CallSuper
    open suspend fun onItemLongClick(view: View, viewData: ArrayList<GenericItem>, itemIndex: Int){
        lastClick = System.currentTimeMillis()
    }
    abstract suspend fun onMenuButtonClick(view: View, viewData: ArrayList<GenericItem>, itemIndex: Int)
    abstract suspend fun onDownloadButtonClick(
        context: Context,
        item: GenericItem,
        downloadImageButton: ImageButton
    )

    abstract suspend fun idToAbstractItem(view: View, id: String): GenericItem
    abstract suspend fun load(
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
        scope.launch {
            loadInit(view, false)
        }
    }

    open fun clearDatabase(context: Context) {

    }

    open fun getOrientation(view: View): Int {
        return LinearLayout.VERTICAL
    }

    open val id = UUID.randomUUID().toString()

    @SuppressLint("WrongConstant")
    private suspend fun setupRecyclerView(view: View) {
        withContext(Dispatchers.Main){
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
                scope.launch {
                    val itemIndex = position + itemHeaderOffset

                    if (itemIndex >= 0 && itemIndex < viewData.size) {
                        if(!blockClick)
                            onItemClick(view.context, viewData, itemIndex)
                    }
                }

                false
            }

            /**
             * On long click
             */
            fastAdapter.onLongClickListener = { _, _, _, position ->
                scope.launch {
                    val itemIndex = position + itemHeaderOffset

                    if (itemIndex >= 0 && itemIndex < viewData.size) {
                        if(!blockClick)
                            onItemLongClick(view, viewData, itemIndex)
                    }
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
                    scope.launch {
                        val itemIndex = position + itemHeaderOffset

                        if (itemIndex >= 0 && itemIndex < viewData.size) {
                            onMenuButtonClick(view, viewData, itemIndex)
                        }
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
                    scope.launch {
                        onDownloadButtonClick(view.context, item, v as ImageButton)
                    }
                }
            })
        }
    }

    private suspend fun addHeader(headerText: String) {
        withContext(Dispatchers.Main){
            headerAdapter.add(
                HeaderTextItem(
                    headerText
                )
            )
            itemHeaderOffset += -1
        }
    }

    private suspend fun addItem(item: GenericItem) {
        withContext(Dispatchers.Main){
            viewData.add(item)
            itemAdapter.add(item)
        }
    }

    suspend fun loadInit(view: View, forceReload: Boolean) {
        val swipeRefreshLayout =
            view.findViewById<SwipeRefreshLayout>(R.id.swipeRefresh)

        if (forceReload)
            clearDatabase(view.context)

        load(view.context, forceReload, 0, displayLoading = {
            scope.launch {
                withContext(Dispatchers.Main){
                    swipeRefreshLayout?.isRefreshing = true
                }
            }
        }, callback = { idList ->
            scope.launch {
                setupRecyclerView(view)

                for (id in idList) {
                    addItem(idToAbstractItem(view, id))
                }

                getHeader(view.context)?.let {
                    addHeader(it)
                }

                withContext(Dispatchers.Main){
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
                    swipeRefreshLayout?.setOnRefreshListener {
                        scope.launch {
                            loadInit(view, true)
                        }
                    }

                    swipeRefreshLayout?.isRefreshing = false

                    restoreRecyclerViewPosition(view.context)
                }
            }

        }, errorCallback = { errorMessage ->
            swipeRefreshLayout?.isRefreshing = false

            displaySnackBar(
                view,
                errorMessage,
                view.context.getString(R.string.retry)
            ) {
                scope.launch {
                    loadInit(view, forceReload)
                }
            }
        })
    }

    open val pageSize = 50

    private fun loadNext(view: View, currentPage: Int) {
        recyclerView?.post {
            footerAdapter.clear()
            footerAdapter.add(ProgressItem())

            scope.launch {
                load(
                    view.context,
                    false,
                    (currentPage * pageSize),
                    displayLoading = {},
                    callback = { idList ->
                        scope.launch {
                            for (id in idList) {
                                addItem(idToAbstractItem(view, id))
                            }

                            withContext(Dispatchers.Main){
                                footerAdapter.clear()
                            }
                        }
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
    }

    fun resetRecyclerViewSavedPosition(context: Context) {
        val settings = Settings.getSettings(context)
        settings.setInt("$id-positionIndex", 0)
        settings.setInt("$id-topView", 0)
    }

    private fun restoreRecyclerViewPosition(context: Context) {
        recyclerView?.let {
            val settings = Settings.getSettings(context)
            settings.getInt("$id-positionIndex")?.let { positionIndex ->
                settings.getInt("$id-topView")?.let { topView ->
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
                settings.setInt("$id-positionIndex", positionIndex)
                settings.setInt("$id-topView", topView)
            }
        }
    }
}