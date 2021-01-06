package de.lucaspape.monstercat.ui.pages

import android.view.View
import android.widget.ImageButton
import de.lucaspape.monstercat.ui.pages.util.Page
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.ui.activities.pageName
import de.lucaspape.monstercat.ui.pages.recycler.PlaylistContentsRecyclerPage
import de.lucaspape.monstercat.ui.pages.recycler.PlaylistListRecyclerPage
import de.lucaspape.monstercat.ui.pages.util.RecyclerViewPage
import de.lucaspape.monstercat.ui.pages.util.createPlaylist

class PlaylistPage(
    private val playlistId: String?,
    private val resetPosition: Boolean,
    private var returnToHome: () -> Unit
) : Page {
    override val layout: Int = R.layout.fragment_playlist
    private var playlistPageObject: RecyclerViewPage? = null

    private var lastOpen = ""

    override fun onBackPressed(view: View) {
        if (lastOpen == "playlistContents") {
            playlistListView(view)
        } else {
            returnToHome()
        }
    }

    override fun onPause(view: View) {
        playlistPageObject?.saveRecyclerViewPosition(view.context)
    }

    override fun onCreate(view: View) {
        pageName = "playlist"
        registerListeners(view)

        if (playlistId != null) {
            playlistContentView(view, playlistId)
        } else {
            playlistListView(view)
        }
    }

    private fun playlistListView(view: View) {
        playlistPageObject?.saveRecyclerViewPosition(view.context)

        playlistPageObject = PlaylistListRecyclerPage { playlistId ->
            playlistContentView(view, playlistId)
        }

        playlistPageObject?.onCreate(view)

        if (resetPosition)
            playlistPageObject?.resetRecyclerViewSavedPosition(view.context)

        lastOpen = "playlistList"
    }

    private fun playlistContentView(view: View, playlistId: String) {
        playlistPageObject?.saveRecyclerViewPosition(view.context)

        playlistPageObject = PlaylistContentsRecyclerPage(playlistId)
        playlistPageObject?.onCreate(view)

        if (resetPosition)
            playlistPageObject?.resetRecyclerViewSavedPosition(view.context)

        lastOpen = "playlistContents"
    }

    private fun registerListeners(view: View) {
        //create new playlist button
        view.findViewById<ImageButton>(R.id.newPlaylistButton).setOnClickListener {
            createPlaylist(view)
        }
    }
}