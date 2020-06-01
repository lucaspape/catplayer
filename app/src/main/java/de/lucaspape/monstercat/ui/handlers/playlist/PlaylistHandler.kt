package de.lucaspape.monstercat.ui.handlers.playlist

import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.ImageButton
import de.lucaspape.monstercat.ui.handlers.Handler
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.ui.activities.MainActivity
import de.lucaspape.monstercat.ui.handlers.createPlaylist

interface PlaylistHandlerInterface{
    fun saveRecyclerViewPosition(context: Context)
    fun onCreate(view: View)
    fun resetRecyclerViewSavedPosition(context: Context)
}

class PlaylistHandler(private val playlistId: String?, private val resetPosition:Boolean, private var returnToHome:() -> Unit):Handler{
    override val layout: Int = R.layout.fragment_playlist
    private var playlistHandlerObject:PlaylistHandlerInterface? = null

    private var lastOpen = ""

    override fun onBackPressed(view: View) {
        if(lastOpen == "playlistContents"){
            playlistListView(view)
        }else{
            returnToHome()
        }
    }

    override fun onPause(view: View) {
        playlistHandlerObject?.saveRecyclerViewPosition(view.context)
    }

    override fun onCreate(view: View) {
        registerListeners(view)

        if(playlistId != null){
            playlistContentView(view, playlistId)
        }else{
            playlistListView(view)
        }
    }

    private fun playlistListView(view: View){
        playlistHandlerObject?.saveRecyclerViewPosition(view.context)

        playlistHandlerObject = PlaylistListHandler { playlistId ->
            playlistContentView(view, playlistId)
        }

        playlistHandlerObject?.onCreate(view)

        if(resetPosition)
            playlistHandlerObject?.resetRecyclerViewSavedPosition(view.context)

        lastOpen = "playlistList"
    }

    private fun playlistContentView(view: View, playlistId:String){
        playlistHandlerObject?.saveRecyclerViewPosition(view.context)

        playlistHandlerObject = PlaylistContentsHandler(playlistId)
        playlistHandlerObject?.onCreate(view)

        if(resetPosition)
            playlistHandlerObject?.resetRecyclerViewSavedPosition(view.context)

        lastOpen = "playlistContents"
    }

    private fun registerListeners(view: View){
        //create new playlist button
        view.findViewById<ImageButton>(R.id.newPlaylistButton).setOnClickListener {
            createPlaylist(view)
        }
    }
}