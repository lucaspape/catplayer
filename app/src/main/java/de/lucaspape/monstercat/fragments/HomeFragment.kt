package de.lucaspape.monstercat.fragments

import android.os.Bundle
import android.view.*
import android.widget.AdapterView
import android.widget.ListView
import androidx.fragment.app.Fragment
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.activities.fragmentBackPressedCallback
import de.lucaspape.monstercat.handlers.*
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

        val adapterContextInfo = menuInfo as AdapterView.AdapterContextMenuInfo
        val position = adapterContextInfo.position

        homeHandler.showContextMenu(v, position)
    }
}