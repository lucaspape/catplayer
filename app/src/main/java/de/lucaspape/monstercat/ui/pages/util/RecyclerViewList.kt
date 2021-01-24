package de.lucaspape.monstercat.ui.pages.util

import android.annotation.SuppressLint
import android.content.Context

import android.view.View
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.scroll.EndlessRecyclerOnScrollListener
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.core.util.Settings
import de.lucaspape.monstercat.ui.abstract_items.util.HeaderTextItem
import de.lucaspape.monstercat.ui.abstract_items.util.ProgressItem
import de.lucaspape.monstercat.ui.displaySnackBar
import de.lucaspape.util.Cache

abstract class RecyclerViewList(var cacheId: String?) {
    abstract fun onItemClick(context: Context, viewData: ArrayList<GenericItem>, itemIndex: Int)
    abstract fun onItemLongClick(view: View, viewData: ArrayList<GenericItem>, itemIndex: Int)

    abstract fun idToAbstractItem(view: View, id: String): GenericItem
    abstract fun load(
        context: Context,
        forceReload: Boolean,
        skip: Int,
        callback: (itemIdList: ArrayList<String>) -> Unit,
        errorCallback: (errorMessage: String) -> Unit
    )

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
        return LinearLayout.HORIZONTAL
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
    }

    private fun addItem(item: GenericItem, id: String) {
        viewData.add(item)
        itemAdapter.add(item)

        val cacheId = cacheId
        if(cacheId != null){
            val cache = Cache()
            var cacheList = cache.get<ArrayList<String>>(cacheId)

            if (cacheList == null) {
                cacheList = ArrayList()
            }

            cacheList.add(id)
            cache.set(cacheId, cacheList)
        }
    }

    private fun addItemFromCache(item: GenericItem) {
        viewData.add(item)
        itemAdapter.add(item)
    }

    fun loadInit(view: View, forceReload: Boolean) {
        val cacheId = cacheId

        val cache = if(cacheId != null) {
            Cache().get<ArrayList<String>>(cacheId)
        }else{
            null
        }

        if (cache.isNullOrEmpty() || forceReload) {
            if(cacheId != null)
                Cache().set(cacheId, null)

            if(forceReload)
                clearDatabase(view.context)

            load(view.context, forceReload, 0, callback = { idList ->
                setupRecyclerView(view)

                for (id in idList) {
                    addItem(idToAbstractItem(view, id), id)
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

            }, errorCallback = { errorMessage ->
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

            /**
             * On scroll down (load next)
             */
            recyclerView?.addOnScrollListener(object :
                EndlessRecyclerOnScrollListener(footerAdapter) {
                override fun onLoadMore(currentPage: Int) {
                    loadNext(view, currentPage)
                }
            })

            restoreRecyclerViewPosition(view.context)
        }
    }

    open val pageSize = 50

    private fun loadNext(view: View, currentPage: Int) {
        recyclerView?.post {
            footerAdapter.clear()
            footerAdapter.add(ProgressItem())

            load(
                view.context,
                false,
                (currentPage * pageSize),
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