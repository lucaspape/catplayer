package de.lucaspape.monstercat.ui.fragments

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.ui.handlers.*

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

        homeHandler.setupSpinner(view)
        homeHandler.registerListeners(view)

        search?.let {
            homeHandler.searchSong(view, it, false)
        }
    }

    override fun onPause() {
        super.onPause()

        homeHandler.onHomeHandlerPause()
    }
}