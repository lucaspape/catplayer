package de.lucaspape.monstercat.fragments

import android.os.Bundle
import android.view.*
import android.widget.AdapterView
import android.widget.ListView
import android.widget.Toast
import androidx.fragment.app.Fragment
import de.lucaspape.monstercat.handlers.PlaylistHandler
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.database.SongDatabaseHelper
import de.lucaspape.monstercat.handlers.downloadPlaylist
import de.lucaspape.monstercat.handlers.downloadSong
import de.lucaspape.monstercat.handlers.playSongFromId
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

        playlistView = view.findViewById<ListView>(R.id.playlistView)

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
                downloadPlaylist(context!!, listItem["playlistId"] as String)
            } else {
                val songDatabaseHelper = SongDatabaseHelper(view!!.context)
                val song = songDatabaseHelper.getSong(listItem["id"] as String)

                if(song != null){
                    downloadSong(context!!, song)
                }
            }

        } else if (item.title == getString(R.string.playNext)) {
            playSongFromId(context!!, listItem["id"] as String, false)
        }

        return super.onContextItemSelected(item)
    }


}