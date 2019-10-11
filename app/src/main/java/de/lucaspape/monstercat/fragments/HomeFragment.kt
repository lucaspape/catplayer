package de.lucaspape.monstercat.fragments

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.AdapterView
import android.widget.ImageButton
import android.widget.ListView
import androidx.fragment.app.Fragment
import de.lucaspape.monstercat.handlers.HomeHandler
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.activities.SettingsActivity
import de.lucaspape.monstercat.database.SongDatabaseHelper
import de.lucaspape.monstercat.handlers.util.addSongToPlaylist
import de.lucaspape.monstercat.handlers.util.downloadSong
import de.lucaspape.monstercat.handlers.util.playSongFromId
import de.lucaspape.monstercat.settings.Settings

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
        if(settings.getSetting("albumViewSelected") != null){
            HomeHandler.albumViewSelected = settings.getSetting("albumViewSelected")!!.toBoolean()
        }

        homeHandler.setupListView(view)
        homeHandler.setupSpinner(view)
        homeHandler.registerListeners(view)
        homeHandler.setupMusicPlayer(view)

        //TODO context menu for album view
        listView = view.findViewById(R.id.musiclistview)
        registerForContextMenu(listView as ListView)

        val settingButton = view.findViewById<ImageButton>(R.id.settingsButton)
        settingButton.setOnClickListener {
            view.context.startActivity(Intent(view.context, SettingsActivity::class.java))
        }
    }

    override fun onCreateContextMenu(
        menu: ContextMenu,
        v: View,
        menuInfo: ContextMenu.ContextMenuInfo?
    ) {
        super.onCreateContextMenu(menu, v, menuInfo)

        if(!HomeHandler.albumView){
            menu.add(0, v.id, 0, getString(R.string.download))
            menu.add(0, v.id, 0, getString(R.string.playNext))
            menu.add(0, v.id, 0, getString(R.string.addToPlaylist))
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val adapterContextInfo = item.menuInfo as AdapterView.AdapterContextMenuInfo
        val position = adapterContextInfo.position

        val listItem = listView!!.getItemAtPosition(position) as HashMap<String, Any?>

        val songDatabaseHelper = SongDatabaseHelper(view!!.context)
        val song = songDatabaseHelper.getSong(listItem["id"] as String)

        if (song != null) {
            when {
                item.title == getString(R.string.download) -> downloadSong(
                    context!!,
                    song
                )
                item.title == getString(R.string.playNext) -> playSongFromId(
                    context!!,
                    listItem["id"].toString(),
                    false
                )
                item.title == getString(R.string.addToPlaylist) -> addSongToPlaylist(
                    context!!,
                    song
                )
            }
        }

        return super.onContextItemSelected(item)
    }
}