package de.lucaspape.monstercat.ui.pages.util

import android.annotation.SuppressLint
import android.content.Context

import android.view.View
import android.view.ViewTreeObserver
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.annotation.CallSuper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.switchmaterial.SwitchMaterial
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.listeners.ClickEventHook
import com.mikepenz.fastadapter.scroll.EndlessRecyclerOnScrollListener
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.core.util.Settings
import de.lucaspape.monstercat.ui.abstract_items.content.CatalogItem
import de.lucaspape.monstercat.ui.abstract_items.content.FilterItem
import de.lucaspape.monstercat.ui.abstract_items.content.PlaylistItem
import de.lucaspape.monstercat.ui.abstract_items.content.QueueItem
import de.lucaspape.monstercat.ui.abstract_items.settings.SettingsButtonItem
import de.lucaspape.monstercat.ui.abstract_items.settings.SettingsLoginItem
import de.lucaspape.monstercat.ui.abstract_items.settings.SettingsProfileItem
import de.lucaspape.monstercat.ui.abstract_items.settings.SettingsToggleItem
import de.lucaspape.monstercat.ui.abstract_items.util.HeaderTextItem
import de.lucaspape.monstercat.ui.abstract_items.util.ProgressItem
import de.lucaspape.monstercat.ui.abstract_items.util.SpacerItem
import de.lucaspape.monstercat.ui.displaySnackBar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

open class Item(open val typeId: String?, open val itemId: Any)
class StringItem(override val typeId: String?, override val itemId: String) : Item(typeId, itemId)

abstract class RecyclerViewPage {
    companion object {
        @JvmStatic
        private val saveData = HashMap<String, HashMap<String, String>>()
    }

    private val scope = CoroutineScope(Dispatchers.Default)

    private var lastClick: Long = 0

    private val blockClick: Boolean
        get() {
            return System.currentTimeMillis() - lastClick < 300
        }

    @CallSuper
    open suspend fun onItemClick(context: Context, viewData: List<GenericItem>, itemIndex: Int) {
        lastClick = System.currentTimeMillis()
    }

    @CallSuper
    open suspend fun onItemLongClick(view: View, viewData: List<GenericItem>, itemIndex: Int) {
        lastClick = System.currentTimeMillis()
    }

    open suspend fun onMenuButtonClick(view: View, viewData: List<GenericItem>, itemIndex: Int) {}
    open suspend fun onDownloadButtonClick(
        context: Context,
        item: GenericItem,
        downloadImageButton: ImageButton
    ) {
    }

    abstract suspend fun itemToAbstractItem(view: View, item: Item): GenericItem?
    abstract suspend fun load(
        context: Context,
        forceReload: Boolean,
        skip: Int,
        displayLoading: () -> Unit,
        callback: (itemList: ArrayList<Item>) -> Unit,
        errorCallback: (errorMessage: String) -> Unit
    )

    open fun registerListeners(view: View) {

    }

    open fun restore(data: HashMap<String, String>?): Boolean {
        return false
    }

    open fun save(): HashMap<String, String> {
        return HashMap()
    }

    open fun getHeader(context: Context): String? {
        return null
    }

    private var recyclerView: RecyclerView? = null
    private var itemAdapter = ItemAdapter<GenericItem>()
    private var headerAdapter = ItemAdapter<HeaderTextItem>()
    private var footerAdapter = ItemAdapter<GenericItem>()

    private var itemHeaderOffset = 0

    fun onCreate(view: View) {
        if (!restore(saveData[id])) {
            registerListeners(view)

            scope.launch {
                setupRecyclerView(view)
                loadInit(view, false)
            }
        }
    }

    fun reload(view: View) {
        scope.launch {
            clear()

            loadInit(view, true)
        }
    }


    open fun clearDatabase(context: Context) {

    }

    open fun getOrientation(view: View): Int {
        return LinearLayout.VERTICAL
    }

    open val id = UUID.randomUUID().toString()

    private var fastAdapter: FastAdapter<GenericItem>? = null

    @SuppressLint("WrongConstant")
    private suspend fun setupRecyclerView(view: View) {
        withContext(Dispatchers.Main) {
            recyclerView = view.findViewById(R.id.recyclerView)

            clear()

            fastAdapter = FastAdapter.with(listOf(headerAdapter, itemAdapter, footerAdapter))

            recyclerView?.layoutManager =
                WrappedLinearLayoutManager(view.context, getOrientation(view), false)

            recyclerView?.adapter = fastAdapter

            //OnClick
            fastAdapter?.onClickListener = { _, _, _, position ->
                scope.launch {
                    val itemIndex = position + itemHeaderOffset

                    if (itemIndex >= 0 && itemIndex < itemAdapter.adapterItemCount) {
                        if (!blockClick)
                            onItemClick(view.context, itemAdapter.adapterItems, itemIndex)
                    }
                }

                false
            }

            /**
             * On long click
             */
            fastAdapter?.onLongClickListener = { _, _, _, position ->
                scope.launch {
                    val itemIndex = position + itemHeaderOffset

                    if (itemIndex >= 0 && itemIndex < itemAdapter.adapterItemCount) {
                        if (!blockClick)
                            onItemLongClick(view, itemAdapter.adapterItems, itemIndex)
                    }
                }

                false
            }

            /**
             * On menu button click
             */
            fastAdapter?.addEventHook(object : ClickEventHook<GenericItem>() {
                override fun onBind(viewHolder: RecyclerView.ViewHolder): View? {
                    return when (viewHolder) {
                        is CatalogItem.ViewHolder -> {
                            viewHolder.titleMenuButton
                        }
                        is PlaylistItem.ViewHolder -> {
                            viewHolder.titleMenuButton
                        }
                        is QueueItem.ViewHolder -> {
                            viewHolder.titleMenuButton
                        }
                        is FilterItem.ViewHolder -> {
                            viewHolder.titleMenuButton
                        }
                        is SettingsToggleItem.ViewHolder -> {
                            viewHolder.informationButton
                        }
                        else -> null
                    }
                }

                override fun onClick(
                    v: View,
                    position: Int,
                    fastAdapter: FastAdapter<GenericItem>,
                    item: GenericItem
                ) {
                    scope.launch {
                        val itemIndex = position + itemHeaderOffset

                        if (itemIndex >= 0 && itemIndex < itemAdapter.adapterItemCount) {
                            onMenuButtonClick(view, itemAdapter.adapterItems, itemIndex)
                        }
                    }
                }
            })

            /**
             * On download button click
             */
            fastAdapter?.addEventHook(object : ClickEventHook<GenericItem>() {
                override fun onBind(viewHolder: RecyclerView.ViewHolder): View? {
                    return when (viewHolder) {
                        is CatalogItem.ViewHolder -> {
                            viewHolder.titleDownloadButton
                        }
                        is PlaylistItem.ViewHolder -> {
                            viewHolder.titleDownloadButton
                        }
                        else -> null
                    }
                }

                override fun onClick(
                    v: View,
                    position: Int,
                    fastAdapter: FastAdapter<GenericItem>,
                    item: GenericItem
                ) {
                    scope.launch {
                        onDownloadButtonClick(view.context, item, v as ImageButton)
                    }
                }
            })

            //settings stuff

            fastAdapter?.addEventHook(object : ClickEventHook<GenericItem>() {
                override fun onBind(viewHolder: RecyclerView.ViewHolder): View? {
                    return when (viewHolder) {
                        is SettingsToggleItem.ViewHolder -> {
                            viewHolder.alertItemSwitch
                        }
                        is SettingsButtonItem.ViewHolder -> {
                            viewHolder.button
                        }
                        is SettingsLoginItem.ViewHolder -> {
                            viewHolder.button
                        }
                        is SettingsProfileItem.ViewHolder -> {
                            viewHolder.button
                        }
                        else -> null
                    }
                }

                override fun onClick(
                    v: View,
                    position: Int,
                    fastAdapter: FastAdapter<GenericItem>,
                    item: GenericItem
                ) {
                    if (item is SettingsToggleItem && v is SwitchMaterial) {
                        v.isChecked = item.onSwitchChange(v.isChecked)
                    } else if (item is SettingsButtonItem && v is Button) {
                        item.onClick()
                    } else if (item is SettingsLoginItem) {
                        val usernameTextInput =
                            view.findViewById<EditText>(R.id.settings_usernameInput)
                        val passwordTextInput =
                            view.findViewById<EditText>(R.id.settings_passwordInput)

                        item.onLogin(
                            usernameTextInput.text.toString(),
                            passwordTextInput.text.toString()
                        )
                    } else if (item is SettingsProfileItem) {
                        item.onLogout()
                    }
                }
            })
        }
    }

    private suspend fun addHeader(headerText: String) {
        withContext(Dispatchers.Main) {
            headerAdapter.add(
                HeaderTextItem(
                    headerText
                )
            )
            itemHeaderOffset += -1
        }
    }

    private var databaseItemCount = 0

    private suspend fun addItem(item: GenericItem) {
        withContext(Dispatchers.Main) {
            itemAdapter.add(item)
        }
    }

    fun removeItem(position: Int, callback: () -> Unit) {
        scope.launch {
            withContext(Dispatchers.Main) {
                itemAdapter.remove(position)

                callback()
            }
        }
    }

    var scrollListener: EndlessRecyclerOnScrollListener? = null

    private var currentLoaderId = ""

    private suspend fun clear() {
        withContext(Dispatchers.Main) {
            itemAdapter.clear()
            headerAdapter.clear()
            footerAdapter.clear()

            itemHeaderOffset = 0
            databaseItemCount = 0
        }
    }

    private suspend fun loadInit(view: View, forceReload: Boolean) {
        val id = UUID.randomUUID().toString()

        if (currentLoaderId.isEmpty()) {
            currentLoaderId = id

            val swipeRefreshLayout =
                view.findViewById<SwipeRefreshLayout>(R.id.swipeRefresh)

            if (forceReload) {
                clearDatabase(view.context)
                resetRecyclerViewSavedPosition(view.context)
            }

            load(view.context, forceReload, 0, displayLoading = {
                scope.launch {
                    withContext(Dispatchers.Main) {
                        swipeRefreshLayout?.isRefreshing = true
                    }
                }
            }, callback = { itemList ->
                scope.launch {
                    if (currentLoaderId == id) {
                        clear()

                        for (item in itemList) {
                            itemToAbstractItem(view, item)?.let {
                                addItem(it)
                            }

                            databaseItemCount++
                        }

                        getHeader(view.context)?.let {
                            addHeader(it)
                        }

                        withContext(Dispatchers.Main) {
                            /**
                             * On scroll down (load next)
                             */

                            scrollListener = object :
                                EndlessRecyclerOnScrollListener(footerAdapter) {
                                override fun onLoadMore(currentPage: Int) {
                                    scrollListener?.disable()
                                    loadNext(view, false)
                                }
                            }

                            scrollListener?.let {
                                recyclerView?.addOnScrollListener(it)
                            }

                            //refresh
                            swipeRefreshLayout?.setOnRefreshListener {
                                scope.launch {
                                    reload(view)
                                }
                            }

                            swipeRefreshLayout?.isRefreshing = false

                            currentLoaderId = ""

                            if (getOrientation(view) == LinearLayout.VERTICAL) {
                                footerAdapter.add(SpacerItem())
                            }

                            restoreRecyclerViewPosition(view)
                        }
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

                currentLoaderId = ""
            })

        }
    }

    private fun loadNext(view: View, restorePosition: Boolean) {
        val id = UUID.randomUUID().toString()

        if (currentLoaderId.isEmpty()) {
            currentLoaderId = id

            recyclerView?.post {
                footerAdapter.clear()
                footerAdapter.add(ProgressItem())

                scope.launch {
                    load(
                        view.context,
                        false,
                        databaseItemCount,
                        displayLoading = {},
                        callback = { itemList ->
                            scope.launch {
                                if (currentLoaderId == id) {
                                    for (item in itemList) {
                                        itemToAbstractItem(view, item)?.let {
                                            addItem(it)
                                        }

                                        databaseItemCount++
                                    }

                                    withContext(Dispatchers.Main) {
                                        footerAdapter.clear()

                                        scrollListener?.enable()

                                        currentLoaderId = ""

                                        if (getOrientation(view) == LinearLayout.VERTICAL) {
                                            footerAdapter.add(SpacerItem())
                                        }

                                        if (restorePosition) {
                                            restoreRecyclerViewPosition(view)
                                        }
                                    }
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
                                loadNext(view, false)
                            }

                            currentLoaderId = ""
                        })
                }
            }
        }
    }

    fun resetSaveData() {
        saveData[id] = HashMap()
    }

    fun saveData() {
        saveData[id] = save()
    }

    fun resetRecyclerViewSavedPosition(context: Context) {
        val settings = Settings.getSettings(context)
        settings.setInt("$id-positionIndex", 0)
        settings.setInt("$id-topView", 0)
    }

    private fun restoreRecyclerViewPosition(view: View) {
        recyclerView?.let {
            val settings = Settings.getSettings(view.context)
            settings.getInt("$id-positionIndex")?.let { positionIndex ->
                settings.getInt("$id-topView")?.let { topView ->
                    if (positionIndex > databaseItemCount) {
                        loadNext(view, true)
                    } else {
                        recyclerView?.viewTreeObserver?.addOnGlobalLayoutListener(object :
                            ViewTreeObserver.OnGlobalLayoutListener {
                            override fun onGlobalLayout() {
                                val layoutManager = it.layoutManager as LinearLayoutManager
                                layoutManager.scrollToPositionWithOffset(positionIndex, topView)

                                recyclerView?.viewTreeObserver?.removeOnGlobalLayoutListener(this)
                            }
                        })
                    }
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