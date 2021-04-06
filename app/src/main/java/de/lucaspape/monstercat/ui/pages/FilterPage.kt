package de.lucaspape.monstercat.ui.pages

import android.view.View
import android.widget.ImageButton
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.ui.pages.recycler.FiltersRecyclerPage
import de.lucaspape.monstercat.ui.pages.util.Page
import de.lucaspape.monstercat.ui.pages.util.RecyclerViewPage
import de.lucaspape.monstercat.ui.pages.util.addFilter

class FilterPage(private val closeFilter: () -> Unit) : Page() {
    constructor() : this({})

    override val layout = R.layout.fragment_filter

    override fun onBackPressed(view: View):Boolean {
        closeFilter()
        return false
    }


    private var filterPageObject: RecyclerViewPage? = null

    override fun onPause(view: View) {
        filterPageObject?.saveRecyclerViewPosition(view.context)
    }

    override fun onCreate(view: View) {
        registerListeners(view)
        filterPageObject = FiltersRecyclerPage()
        filterPageObject?.onCreate(view)
    }

    override val pageName = "filter"

    private fun registerListeners(view: View) {
        //add filter button
        view.findViewById<ImageButton>(R.id.addFilterButton).setOnClickListener {
            addFilter(view) {
                filterPageObject?.reload(view)
            }
        }
    }
}