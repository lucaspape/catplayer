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

class PlaylistHandler(private val playlistId: String?, private val resetPosition:Boolean):Handler{
    override val layout: Int = R.layout.fragment_playlist
    private var playlistHandlerObject:PlaylistHandlerInterface? = null

    override fun onBackPressed(view: View) {
        //TODO

        val intent = Intent(view.context, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NO_ANIMATION
        view.context.startActivity(intent)
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
    }

    private fun playlistContentView(view: View, playlistId:String){
        playlistHandlerObject?.saveRecyclerViewPosition(view.context)

        playlistHandlerObject = PlaylistContentsHandler(playlistId)
        playlistHandlerObject?.onCreate(view)

        if(resetPosition)
            playlistHandlerObject?.resetRecyclerViewSavedPosition(view.context)
    }

    private fun registerListeners(view: View){
        //create new playlist button
        view.findViewById<ImageButton>(R.id.newPlaylistButton).setOnClickListener {
            createPlaylist(view)
        }
    }
}