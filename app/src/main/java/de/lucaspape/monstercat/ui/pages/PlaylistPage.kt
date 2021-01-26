package de.lucaspape.monstercat.ui.pages

import android.view.View
import android.widget.ImageButton
import de.lucaspape.monstercat.ui.pages.util.Page
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.ui.pages.recycler.PlaylistContentsRecyclerPage
import de.lucaspape.monstercat.ui.pages.recycler.PlaylistListRecyclerPage
import de.lucaspape.monstercat.ui.pages.util.RecyclerViewPage
import de.lucaspape.monstercat.ui.pages.util.createPlaylist

class PlaylistPage(
    private val playlistId: String?,
    private val resetPosition: Boolean,
    private var returnToHome: () -> Unit
) : Page() {
    constructor() : this(null,false,{})

    companion object{
        @JvmStatic val playlistPageName = "playlist"
    }

    override val layout: Int = R.layout.fragment_playlist
    private var playlistPageObject: RecyclerViewPage? = null

    private var currentView = ""

    private var resetData = false

    override fun onBackPressed(view: View) {
        if(currentView == "list"){
            returnToHome()
        }else{
            resetData = true
            playlistListView(view)
        }
    }

    override fun onPause(view: View) {
        playlistPageObject?.saveRecyclerViewPosition(view.context)
    }

    override fun onCreate(view: View) {
        registerListeners(view)

        if (playlistId != null) {
            playlistContentView(view, playlistId)
        } else {
            playlistListView(view)
        }
    }

    override val pageName: String = playlistPageName

    private fun playlistListView(view: View) {
        currentView = "list"

        playlistPageObject?.saveRecyclerViewPosition(view.context)

        playlistPageObject = PlaylistListRecyclerPage { playlistId ->
            playlistContentView(view, playlistId)
        }

        if (resetPosition){
            playlistPageObject?.resetRecyclerViewSavedPosition(view.context)
            playlistPageObject?.resetSaveData()
        }

        if(resetData){
            playlistPageObject?.resetSaveData()
        }

        playlistPageObject?.onCreate(view)
    }

    private fun playlistContentView(view: View, playlistId: String) {
        currentView = "playlist"

        playlistPageObject?.saveRecyclerViewPosition(view.context)

        playlistPageObject = PlaylistContentsRecyclerPage(playlistId)

        if (resetPosition){
            playlistPageObject?.resetRecyclerViewSavedPosition(view.context)
            playlistPageObject?.resetSaveData()
        }

        if(resetData){
            playlistPageObject?.resetSaveData()
        }

        playlistPageObject?.onCreate(view)
    }

    private fun registerListeners(view: View) {
        //create new playlist button
        view.findViewById<ImageButton>(R.id.newPlaylistButton).setOnClickListener {
            createPlaylist(view)
        }
    }
}