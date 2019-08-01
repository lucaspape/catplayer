package de.lucaspape.monstercat

import android.os.Bundle
import android.view.*
import android.widget.AdapterView
import android.widget.ListView
import androidx.fragment.app.Fragment

class HomeFragment : Fragment() {
    private var listView:ListView? = null
    private val homeHandler = HomeHandler()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    companion object {
        fun newInstance(): HomeFragment = HomeFragment()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        listView = view.findViewById<ListView>(R.id.musiclistview)

        homeHandler.loadTitlesFromCache(view)

        homeHandler.registerPullRefresh(view)

        homeHandler.setupMusicPlayer(view)

        homeHandler.registerListViewClick(view)

        homeHandler.registerButtons(view)

        registerForContextMenu(listView)
    }

    override fun onCreateContextMenu(menu: ContextMenu?, v: View?, menuInfo: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)

        menu!!.add(0, v!!.id, 0, getString(R.string.download))
    }

    override fun onContextItemSelected(item: MenuItem?): Boolean {
        val adapterContextInfo = item!!.menuInfo as AdapterView.AdapterContextMenuInfo
        val position = adapterContextInfo.position

        val listItem = listView!!.getItemAtPosition(position) as HashMap<String, Any?>

        if(item.title == getString(R.string.download)){
            homeHandler.downloadSong(context!!, listItem)
        }

        return super.onContextItemSelected(item)

    }
}