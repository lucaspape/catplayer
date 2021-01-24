package de.lucaspape.monstercat.ui.pages

import android.view.View
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.ui.pages.recycler.SearchRecyclerPage
import de.lucaspape.monstercat.ui.pages.util.Page
import de.lucaspape.monstercat.ui.pages.util.RecyclerViewPage

class SearchPage(
    private var search: String?,
    private val closeSearch: () -> Unit
) : Page() {
    companion object{
        @JvmStatic val searchPageName = "search"
    }

    override val layout: Int = R.layout.fragment_search

    override fun onBackPressed(view: View) {
        closeSearch()
    }

    private var searchPageObject: RecyclerViewPage? = null

    override fun onPause(view: View) {
        searchPageObject?.saveRecyclerViewPosition(view.context)
    }

    override fun onCreate(view: View) {
        searchPageObject = SearchRecyclerPage(search, closeSearch)
        searchPageObject?.onCreate(view)
    }

    override val pageName: String = searchPageName
}