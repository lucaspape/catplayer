package de.lucaspape.monstercat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

class HomeFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    companion object {
        fun newInstance(): HomeFragment = HomeFragment()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        val homeHandler = HomeHandler()

        homeHandler.loadTitlesFromCache(view)

        homeHandler.registerPullRefresh(view)

        homeHandler.login(view)

        homeHandler.registerListViewClick(view)

        homeHandler.registerButtons(view)


    }


}