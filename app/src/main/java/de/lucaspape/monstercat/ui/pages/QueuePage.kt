package de.lucaspape.monstercat.ui.pages

import android.view.View
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.ui.pages.recycler.QueueRecyclerPage
import de.lucaspape.monstercat.ui.pages.util.Page
import de.lucaspape.monstercat.ui.pages.util.RecyclerViewPage

class QueuePage(private val closeQueue: () -> Unit) : Page() {
    constructor() : this({})

    override val layout = R.layout.fragment_queue

    override fun onBackPressed(view: View):Boolean {
        closeQueue()
        return false
    }


    private var queuePageObject: RecyclerViewPage? = null

    override fun onPause(view: View) {
        queuePageObject?.saveRecyclerViewPosition(view.context)
    }

    override fun onCreate(view: View) {
        queuePageObject = QueueRecyclerPage()
        queuePageObject?.onCreate(view)
    }

    override val pageName = "queue"
}