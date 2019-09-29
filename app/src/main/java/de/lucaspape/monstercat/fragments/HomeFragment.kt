package de.lucaspape.monstercat.fragments

import android.os.Bundle
import android.view.*
import android.widget.AdapterView
import android.widget.ListView
import androidx.fragment.app.Fragment
import de.lucaspape.monstercat.handlers.HomeHandler
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.cache.Cache

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

        when {
            item.title == getString(R.string.download) -> homeHandler.downloadSong(listItem, context!!)
            item.title == getString(R.string.playNext) -> homeHandler.playSong(listItem["id"].toString(), false, context!!)
            item.title == getString(R.string.addToPlaylist) -> homeHandler.addSongToPlaylist(listItem, context!!)
        }

        return super.onContextItemSelected(item)
    }


}