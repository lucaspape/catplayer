package de.lucaspape.monstercat.ui.pages.recycler

import android.content.Context
import android.view.View
import android.widget.SearchView
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.request.async.loadTitleSearch
import kotlin.collections.ArrayList

class SearchRecyclerPage(
    private var search: String?,
    private val closeSearch: () -> Unit
) : HomeCatalogRecyclerPage() {

    override fun registerListeners(view: View) {
        //search
        val searchInput = view.findViewById<SearchView>(R.id.searchInput)

        searchInput.onActionViewExpanded()

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
                    onCreate(view)
                }

                return false
            }
        })

        search?.let {
            searchInput.setQuery(search, false)
        }
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

            loadTitleSearch(
                context,
                it,
                skip, finishedCallback = { searchResults ->
                    val idList = ArrayList<String>()

                    for (result in searchResults) {
                        idList.add(result.songId)
                    }

                    callback(idList)
                }, errorCallback = {
                    errorCallback(context.getString(R.string.errorLoadingSearch))
                })
        }
    }
}