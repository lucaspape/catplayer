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
import de.lucaspape.monstercat.handlers.addSongToPlaylist
import de.lucaspape.monstercat.handlers.downloadAlbum
import de.lucaspape.monstercat.handlers.downloadSong
import de.lucaspape.monstercat.handlers.playSongFromId
import de.lucaspape.monstercat.util.Settings

class HomeFragment : Fragment() {
    private var listView: ListView? = null
    private val homeHandler = HomeHandler()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    companion object {
        fun newInstance(): HomeFragment =
            HomeFragment()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val settings = Settings(view.context)
        if (settings.getSetting("albumViewSelected") != null) {
            HomeHandler.albumViewSelected = settings.getSetting("albumViewSelected")!!.toBoolean()
        }

        homeHandler.setupListView(view)
        homeHandler.setupSpinner(view)
        homeHandler.registerListeners(view)

        listView = view.findViewById(R.id.musiclistview)
        registerForContextMenu(listView as ListView)

        fragmentBackPressedCallback = {
            if (HomeHandler.albumViewSelected) {
                HomeHandler.albumView = true
                homeHandler.loadAlbumList(view, false)
            } else {
                homeHandler.loadSongList(view, false)
            }
        }
    }

    override fun onCreateContextMenu(
        menu: ContextMenu,
        v: View,
        menuInfo: ContextMenu.ContextMenuInfo?
    ) {
        super.onCreateContextMenu(menu, v, menuInfo)

        if (!HomeHandler.albumView) {
            menu.add(0, v.id, 0, getString(R.string.download))
            menu.add(0, v.id, 0, getString(R.string.playNext))
            menu.add(0, v.id, 0, getString(R.string.addToPlaylist))
        } else {
            menu.add(0, v.id, 0, getString(R.string.downloadAlbum))
            menu.add(0, v.id, 0, getString(R.string.playAlbumNext))
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val adapterContextInfo = item.menuInfo as AdapterView.AdapterContextMenuInfo
        val position = adapterContextInfo.position

        val listItem = listView?.getItemAtPosition(position) as HashMap<*, *>

        view?.let { view ->
            val songDatabaseHelper =
                SongDatabaseHelper(view.context)
            val song = songDatabaseHelper.getSong(view.context, listItem["id"] as String)

            context?.let {
                if (song != null) {
                    when (item.title) {
                        getString(R.string.download) -> downloadSong(
                            it,
                            song
                        )
                        getString(R.string.playNext) -> playSongFromId(
                            listItem["id"].toString(),
                            false
                        )
                        getString(R.string.addToPlaylist) -> addSongToPlaylist(
                            it,
                            song
                        )
                    }
                } else {
                    when (item.title) {
                        getString(R.string.downloadAlbum) -> downloadAlbum(
                            it,
                            listItem["mcID"].toString()
                        )
                        getString(R.string.playAlbumNext) -> playAlbumNext(
                            it,
                            listItem["mcID"].toString()
                        )
                    }
                }
            }
        }

        return super.onContextItemSelected(item)
    }
}