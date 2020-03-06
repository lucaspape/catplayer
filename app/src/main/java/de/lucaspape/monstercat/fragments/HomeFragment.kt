package de.lucaspape.monstercat.fragments

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.activities.fragmentBackPressedCallback
import de.lucaspape.monstercat.handlers.*
import de.lucaspape.monstercat.util.Settings

class HomeFragment() : Fragment() {
    private var search: String? = null

    constructor(search: String) : this() {
        if (search != "") {
            this.search = search
        }
    }

    private val homeHandler = HomeHandler()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    companion object {
        fun newInstance(search: String): HomeFragment =
            HomeFragment(search)

        fun newInstance(): HomeFragment =
            HomeFragment()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val settings = Settings(view.context)
        if (settings.getSetting(view.context.getString(R.string.albumViewSelectedSetting)) != null) {
            HomeHandler.albumViewSelected =
                settings.getSetting(view.context.getString(R.string.albumViewSelectedSetting))!!
                    .toBoolean()
        }

        homeHandler.setupSpinner(view)
        homeHandler.registerListeners(view)

        fragmentBackPressedCallback = {
            if (HomeHandler.albumViewSelected) {
                HomeHandler.albumView = true
                homeHandler.initAlbumListLoad(view, false)
            } else {
                homeHandler.initSongListLoad(view, false)
            }
        }

        search?.let {
            homeHandler.searchSong(view, it)
        }
    }
}