package de.lucaspape.monstercat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.android.volley.toolbox.Volley

class PlaylistFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_playlist, container, false)

    companion object {
        fun newInstance(): PlaylistFragment = PlaylistFragment()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val playlistHandler = PlaylistHandler()

        val settings = Settings(view.context)

        val username = settings.getSetting("email")
        val password = settings.getSetting("password")

        if(username == null || password == null){
            Toast.makeText(view.context, "Set your username and passwort in the settings!", Toast.LENGTH_SHORT)
                .show()
        }else{
            val queue = Volley.newRequestQueue(view.context)

            playlistHandler.login(view, queue)

            playlistHandler.loadPlaylist(view, queue)

            playlistHandler.registerPullRefresh(view)

            playlistHandler.registerListViewClick(view)


        }
    }

}