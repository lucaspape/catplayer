package de.lucaspape.monstercat.ui.pages

import android.view.View
import android.widget.ImageButton
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.core.music.util.playStream
import de.lucaspape.monstercat.core.twitch.Stream
import de.lucaspape.monstercat.ui.pages.util.Page
import de.lucaspape.monstercat.ui.pages.recycler.*
import de.lucaspape.monstercat.ui.pages.util.RecyclerViewPage

class ExplorePage(
    private val onSearch: (searchString: String?) -> Unit,
    private val openSettings: () -> Unit,
    private val resetPosition: Boolean
) : Page() {

    constructor() : this({},{},false)

    companion object{
        @JvmStatic val explorePageName = "explore"
    }

    private var explorePageObject: RecyclerViewPage? = null
    override val layout: Int = R.layout.fragment_explore

    override fun onBackPressed(view: View) {
        onCreate(view)
    }

    override fun onPause(view: View) {
        explorePageObject?.saveRecyclerViewPosition(view.context)
    }

    override fun onCreate(view: View) {
        registerListeners(view)

        exploreView(view)
    }

    override val pageName: String = explorePageName

    private fun exploreView(view: View) {
        explorePageObject?.saveRecyclerViewPosition(view.context)

        explorePageObject = ExploreRecyclerPage(openMood = { moodId ->
            moodView(view, moodId)
        }, openGenre = { genreId -> genreView(view, genreId) })

        if (resetPosition)
            explorePageObject?.resetRecyclerViewSavedPosition(view.context)

        explorePageObject?.onCreate(view)
    }

    private fun moodView(view: View, moodId: String) {
        explorePageObject?.saveRecyclerViewPosition(view.context)

        explorePageObject = MoodContentsRecyclerPage(moodId)

        if (resetPosition)
            explorePageObject?.resetRecyclerViewSavedPosition(view.context)

        explorePageObject?.onCreate(view)
    }

    private fun genreView(view: View, genreId: String) {
        explorePageObject?.saveRecyclerViewPosition(view.context)

        explorePageObject = GenreContentsRecyclerPage(genreId)

        if (resetPosition)
            explorePageObject?.resetRecyclerViewSavedPosition(view.context)

        explorePageObject?.onCreate(view)
    }

    /**
     * Listeners (buttons, refresh etc)
     */
    private fun registerListeners(view: View) {
        //settings button
        view.findViewById<ImageButton>(R.id.settingsButton).setOnClickListener {
            openSettings()
        }

        //livestream button
        view.findViewById<ImageButton>(R.id.liveButton).setOnClickListener {
            playStream(
                view.context,
                Stream()
            )
        }

        view.findViewById<ImageButton>(R.id.searchButton).setOnClickListener {
            onSearch(null)
        }
    }
}