package de.lucaspape.monstercat.fragments

import android.os.Bundle
import android.view.*
import android.widget.AdapterView
import android.widget.ListView
import android.widget.Toast
import androidx.fragment.app.Fragment
import de.lucaspape.monstercat.handlers.PlaylistHandler
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.settings.Settings

class PlaylistFragment : Fragment() {
    private val playlistHandler = PlaylistHandler()
    private var playlistView: ListView? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_playlist, container, false)

    companion object {
        fun newInstance(): PlaylistFragment =
            PlaylistFragment()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        playlistView = view.findViewById<ListView>(R.id.listview)

        val settings = Settings(view.context)

        val username = settings.getSetting("email")
        val password = settings.getSetting("password")

        if (username == null || password == null) {
            Toast.makeText(
                view.context,
                view.context.getString(R.string.setUsernamePasswordSettingsMsg),
                Toast.LENGTH_SHORT
            )
                .show()
        } else {
            playlistHandler.setupListView(view)

            playlistHandler.registerListeners(view)

            playlistHandler.loadPlaylist(view, false)

            registerForContextMenu(playlistView as ListView)
        }
    }

    override fun onCreateContextMenu(
        menu: ContextMenu,
        v: View,
        menuInfo: ContextMenu.ContextMenuInfo?
    ) {
        super.onCreateContextMenu(menu, v, menuInfo)

        menu.add(0, v.id, 0, getString(R.string.download))
        menu.add(0, v.id, 0, getString(R.string.playNext))
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val adapterContextInfo = item.menuInfo as AdapterView.AdapterContextMenuInfo
        val position = adapterContextInfo.position

        val listItem = playlistView!!.getItemAtPosition(position) as HashMap<String, Any?>

        if (item.title == getString(R.string.download)) {
            if (listItem["type"] == "playlist") {
                playlistHandler.downloadPlaylist(context!!, listItem)
            } else {
                playlistHandler.downloadSong(context!!, listItem)
            }

        } else if (item.title == getString(R.string.playNext)) {
            playlistHandler.playSong(context!!, listItem, true)
        }

        return super.onContextItemSelected(item)
    }


}