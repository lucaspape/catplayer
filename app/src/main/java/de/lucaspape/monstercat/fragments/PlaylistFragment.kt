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

        menu.add(0, v.id, 0, getString(R.string.download))
        menu.add(0, v.id, 0, getString(R.string.playNext))
        menu.add(0, v.id, 0, "Delete")
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val adapterContextInfo = item.menuInfo as AdapterView.AdapterContextMenuInfo
        val position = adapterContextInfo.position

        val listItem = playlistView?.getItemAtPosition(position) as HashMap<*, *>

        context?.let {
            if (item.title == getString(R.string.download)) {
                if (listItem["type"] == "playlist") {
                    downloadPlaylist(
                        it,
                        listItem["playlistId"] as String
                    )
                } else {
                    view?.let { view ->
                        val songDatabaseHelper =
                            SongDatabaseHelper(view.context)
                        val song =
                            songDatabaseHelper.getSong(view.context, listItem["id"] as String)

                        if (song != null) {
                            downloadSong(it, song)
                        }
                    }
                }

            } else if (item.title == getString(R.string.playNext)) {
                if (listItem["type"] == "playlist") {
                    playPlaylistNext(it, listItem["playlistId"] as String)
                } else {
                    playSongFromId(
                        listItem["id"] as String,
                        false
                    )
                }

            } else if (item.title == "Delete") {
                if (listItem["type"] == "playlist") {
                    deletePlaylist(it, listItem["playlistId"] as String)
                } else {
                    view?.let { view ->
                        val songDatabaseHelper =
                            SongDatabaseHelper(view.context)
                        val song =
                            songDatabaseHelper.getSong(view.context, listItem["id"] as String)

                        if (song != null) {
                            PlaylistHandler.currentPlaylistId?.let { playlistId ->
                                playlistView?.adapter?.count?.let { count ->
                                    deletePlaylistSong(it, song, playlistId, position + 1, count)
                                }
                            }
                        }
                    }
                }
            } else {

            }
        }

        return super.onContextItemSelected(item)
    }
}