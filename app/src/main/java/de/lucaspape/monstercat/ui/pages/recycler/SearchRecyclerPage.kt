package de.lucaspape.monstercat.ui.pages.recycler

import android.content.Context
import android.view.View
import android.widget.SearchView
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.request.async.loadTitleSearch
import de.lucaspape.monstercat.ui.pages.util.Item
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
                    reload(view)
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
        callback: (itemList: ArrayList<Item>) -> Unit,
        errorCallback: (errorMessage: String) -> Unit
    ) {
        val search = search

        if(search != null){
            displayLoading()

            loadTitleSearch(
                context,
                search,
                skip, finishedCallback = { searchResults ->
                    val itemList = ArrayList<Item>()

                    for (id in searchResults) {
                        itemList.add(Item(id, null))
                    }

                    callback(itemList)
                }, errorCallback = {
                    errorCallback(context.getString(R.string.errorLoadingSearch))
                })
        }else{
            callback(ArrayList())
        }
    }
}