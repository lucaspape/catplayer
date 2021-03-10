package de.lucaspape.monstercat.ui.pages

import android.view.View
import android.widget.ImageButton
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.ui.pages.util.Page
import de.lucaspape.monstercat.ui.pages.recycler.*
import de.lucaspape.monstercat.ui.pages.util.RecyclerViewPage

class ExplorePage(
    private val onSearch: (searchString: String?) -> Unit,
    private val openSettings: () -> Unit,
    private val resetPosition: Boolean,
    private var returnToHome: () -> Unit
) : Page() {

    constructor() : this({}, {}, false, {})

    companion object {
        @JvmStatic
        val explorePageName = "explore"
    }

    private var explorePageObject: RecyclerViewPage? = null
    override val layout: Int = R.layout.fragment_explore

    private var currentView = ""

    private var resetData = false

    override fun onBackPressed(view: View): Boolean {
        if (currentView == "explore") {
            returnToHome()
        } else {
            resetData = true
            exploreView(view)
        }

        return false
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
        currentView = "explore"

        explorePageObject?.saveRecyclerViewPosition(view.context)

        explorePageObject = ExploreRecyclerPage(
            openMood = { moodId ->
                moodView(view, moodId)
            },
            openGenre = { genreId -> genreView(view, genreId) },
            openPublicPlaylist = { publicPlaylistId ->
                publicPlaylistsView(
                    view,
                    publicPlaylistId
                )
            })

        if (resetPosition) {
            explorePageObject?.resetRecyclerViewSavedPosition(view.context)
            explorePageObject?.resetSaveData()
        }

        if (resetData) {
            explorePageObject?.resetSaveData()
        }

        explorePageObject?.onCreate(view)
    }

    private fun moodView(view: View, moodId: String) {
        currentView = "mood"

        explorePageObject?.saveRecyclerViewPosition(view.context)

        explorePageObject = MoodContentsRecyclerPage(moodId)

        if (resetPosition) {
            explorePageObject?.resetRecyclerViewSavedPosition(view.context)
            explorePageObject?.resetSaveData()
        }

        if (resetData) {
            explorePageObject?.resetSaveData()
        }

        explorePageObject?.onCreate(view)
    }

    private fun genreView(view: View, genreId: String) {
        currentView = "genre"

        explorePageObject?.saveRecyclerViewPosition(view.context)

        explorePageObject = GenreContentsRecyclerPage(genreId)

        if (resetPosition) {
            explorePageObject?.resetRecyclerViewSavedPosition(view.context)
            explorePageObject?.resetSaveData()
        }

        if (resetData) {
            explorePageObject?.resetSaveData()
        }

        explorePageObject?.onCreate(view)
    }

    private fun publicPlaylistsView(view: View, publicPlaylistId: String) {
        currentView = "public-playlists"

        explorePageObject?.saveRecyclerViewPosition(view.context)

        explorePageObject = PublicPlaylistContentsRecyclerPage(publicPlaylistId)

        if (resetPosition) {
            explorePageObject?.resetRecyclerViewSavedPosition(view.context)
            explorePageObject?.resetSaveData()
        }

        if (resetData) {
            explorePageObject?.resetSaveData()
        }

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

        view.findViewById<ImageButton>(R.id.searchButton).setOnClickListener {
            onSearch(null)
        }
    }
}