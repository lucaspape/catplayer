package de.lucaspape.monstercat.ui.handlers

import android.content.Context
import android.view.View
import android.widget.SearchView
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.request.async.loadTitleSearchAsync
import de.lucaspape.monstercat.ui.handlers.home.HomeCatalogHandler
import kotlin.collections.ArrayList

class SearchHandler(
    private var search: String?,
    private val closeSearch: () -> Unit
) : Handler, HomeCatalogHandler("search") {
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
            loadInit(view, false)
        }
    }

    private fun registerListeners(view: View) {
        //search
        val searchInput = view.findViewById<SearchView>(R.id.searchInput)

        searchInput.setOnCloseListener {
            closeSearch()

            false
        }

        searchInput.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }

            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let {
                    search = it
                    loadInit(view, false)
                }

                return false
            }
        })
    }

    override fun load(
        context: Context,
        forceReload: Boolean,
        skip: Int,
        displayLoading: () -> Unit,
        callback: (itemIdList: ArrayList<String>) -> Unit,
        errorCallback: () -> Unit
    ) {
        search?.let{
            displayLoading()

            loadTitleSearchAsync(
                context,
                it,
                skip, finishedCallback = { _, _, searchResults ->
                    val idList = ArrayList<String>()

                    for (result in searchResults) {
                        idList.add(result.songId)
                    }

                    callback(idList)
                }, errorCallback = { _, _ ->
                    errorCallback()
                })
        }
    }
}