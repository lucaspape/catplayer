package de.lucaspape.monstercat.ui.pages.recycler

import android.content.Context
import android.view.View
import android.widget.SearchView
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.request.async.loadTitleSearchAsync
import de.lucaspape.monstercat.ui.pages.util.Page
import kotlinx.coroutines.launch
import kotlin.collections.ArrayList

class SearchRecyclerPage(
    private var search: String?,
    private val closeSearch: () -> Unit
) : HomeCatalogRecyclerPage() {
    override fun onCreate(view: View) {
        registerListeners(view)

        val searchView = view.findViewById<SearchView>(R.id.searchInput)
        searchView.onActionViewExpanded()

        search?.let {
            searchView.setQuery(search, false)

            scope.launch {
                loadInit(view, false)
            }
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

                    scope.launch {
                        loadInit(view, false)
                    }
                }

                return false
            }
        })
    }

    override suspend fun load(
        context: Context,
        forceReload: Boolean,
        skip: Int,
        displayLoading: () -> Unit,
        callback: (itemIdList: ArrayList<String>) -> Unit,
        errorCallback: (errorMessage: String) -> Unit
    ) {
        search?.let {
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
                    errorCallback(context.getString(R.string.errorLoadingSearch))
                })
        }
    }
}