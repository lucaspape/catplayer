package de.lucaspape.monstercat.fragments

import android.os.Bundle
import android.view.*
import android.widget.AdapterView
import android.widget.ListView
import androidx.fragment.app.Fragment
import de.lucaspape.monstercat.handlers.HomeHandler
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.database.SongDatabaseHelper
import de.lucaspape.monstercat.handlers.addSongToPlaylist
import de.lucaspape.monstercat.handlers.downloadSong
import de.lucaspape.monstercat.handlers.playSongFromId

class HomeFragment : Fragment() {
    private var listView: ListView? = null
    private val homeHandler = HomeHandler()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    companion object {
        fun newInstance(): HomeFragment =
            HomeFragment()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        homeHandler.setupListView(view)
        homeHandler.setupSpinner(view)
        homeHandler.registerListeners(view)
        homeHandler.setupMusicPlayer(view)

        if(HomeHandler.albumViewSelected){
           // homeHandler.loadAlbumList(view, false)
        }else{
           // homeHandler.loadSongList(view, false)

            listView = view.findViewById(R.id.musiclistview)
            registerForContextMenu(listView as ListView)
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
        menu.add(0, v.id, 0, getString(R.string.addToPlaylist))
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val adapterContextInfo = item.menuInfo as AdapterView.AdapterContextMenuInfo
        val position = adapterContextInfo.position

        val listItem = listView!!.getItemAtPosition(position) as HashMap<String, Any?>

        val songDatabaseHelper = SongDatabaseHelper(view!!.context)
        val song = songDatabaseHelper.getSong(listItem["id"] as String)

        if(song != null){
            when {
                item.title == getString(R.string.download) -> downloadSong(context!!, song)
                item.title == getString(R.string.playNext) -> playSongFromId(context!!, listItem["id"].toString(), false)
                item.title == getString(R.string.addToPlaylist) -> addSongToPlaylist(context!!, song)
            }
        }

        return super.onContextItemSelected(item)
    }


}