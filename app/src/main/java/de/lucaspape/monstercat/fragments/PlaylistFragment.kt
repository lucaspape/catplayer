package de.lucaspape.monstercat.fragments

import android.os.Bundle
import android.view.*
import android.widget.AdapterView
import android.widget.ListView
import androidx.fragment.app.Fragment
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.activities.fragmentBackPressedCallback
import de.lucaspape.monstercat.database.helper.SongDatabaseHelper
import de.lucaspape.monstercat.handlers.*
import de.lucaspape.monstercat.handlers.deletePlaylist
import de.lucaspape.monstercat.handlers.downloadPlaylist
import de.lucaspape.monstercat.handlers.downloadSong
import de.lucaspape.monstercat.handlers.playSongFromId
import de.lucaspape.monstercat.util.Settings
import de.lucaspape.monstercat.util.displayInfo

class PlaylistFragment : Fragment() {
    private val playlistHandler = PlaylistHandler()
    private var playlistView: ListView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? =
        inflater.inflate(R.layout.fragment_playlist, container, false)

    companion object {
        fun newInstance(): PlaylistFragment =
            PlaylistFragment()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        playlistView = view.findViewById(R.id.playlistView)

        val settings = Settings(view.context)

        val username = settings.getSetting("email")
        val password = settings.getSetting("password")

        if (username == null || password == null) {
            displayInfo(
                view.context,
                view.context.getString(R.string.setUsernamePasswordSettingsMsg)
            )

            fragmentBackPressedCallback = {

            }
        } else {
            playlistHandler.setupListView(view)

            playlistHandler.registerListeners(view)

            playlistHandler.loadPlaylist(view, false)

            registerForContextMenu(playlistView as ListView)

            fragmentBackPressedCallback = {
                playlistHandler.loadPlaylist(view, false)
            }
        }
    }

    override fun onCreateContextMenu(
        menu: ContextMenu,
        v: View,
        menuInfo: ContextMenu.ContextMenuInfo?
    ) {
        super.onCreateContextMenu(menu, v, menuInfo)

        val adapterContextInfo = menuInfo as AdapterView.AdapterContextMenuInfo
        val position = adapterContextInfo.position

        playlistHandler.showContextMenu(v, position)
    }
}