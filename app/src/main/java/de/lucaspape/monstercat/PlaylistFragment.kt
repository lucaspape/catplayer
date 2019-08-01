package de.lucaspape.monstercat

import android.os.Bundle
import android.view.*
import android.widget.AdapterView
import android.widget.ListView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.android.volley.toolbox.Volley

class PlaylistFragment : Fragment() {
    private val playlistHandler = PlaylistHandler()
    var playlistView:ListView? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_playlist, container, false)

    companion object {
        fun newInstance(): PlaylistFragment = PlaylistFragment()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        playlistView = view.findViewById<ListView>(R.id.listview)

        val settings = Settings(view.context)

        val username = settings.getSetting("email")
        val password = settings.getSetting("password")

        if(username == null || password == null){
            Toast.makeText(view.context, "Set your username and passwort in the settings!", Toast.LENGTH_SHORT)
                .show()
        }else{
            playlistHandler.loadPlaylist(view)

            playlistHandler.registerPullRefresh(view)

            playlistHandler.registerListViewClick(view)

            registerForContextMenu(playlistView)
        }
    }

    override fun onCreateContextMenu(menu: ContextMenu?, v: View?, menuInfo: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)

        //TODO replace string
        menu!!.add(0, v!!.id, 0, "Download")
    }

    override fun onContextItemSelected(item: MenuItem?): Boolean {
        val adapterContextInfo = item!!.menuInfo as AdapterView.AdapterContextMenuInfo
        val position = adapterContextInfo.position

        val listItem = playlistView!!.getItemAtPosition(position) as HashMap<String, Any?>

        //TODO replace string
        if(item.title == "Download"){
            if(listItem.get("type") == "playlist"){
                playlistHandler.downloadPlaylist(context!!, listItem)
            }else{
                playlistHandler.downloadSong(context!!, listItem)
            }

        }

        return super.onContextItemSelected(item)

    }

}