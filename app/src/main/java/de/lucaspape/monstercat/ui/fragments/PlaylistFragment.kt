package de.lucaspape.monstercat.ui.fragments

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.ui.activities.fragmentBackPressedCallback
import de.lucaspape.monstercat.ui.handlers.*
import de.lucaspape.monstercat.util.Settings
import de.lucaspape.monstercat.util.displayInfo

class PlaylistFragment : Fragment() {
    private val playlistHandler = PlaylistHandler()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? =
        inflater.inflate(R.layout.fragment_playlist, container, false)

    companion object {
        fun newInstance(): PlaylistFragment =
            PlaylistFragment()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val settings = Settings(view.context)

        val username = settings.getString(view.context.getString(R.string.emailSetting))
        val password = settings.getString(view.context.getString(R.string.passwordSetting))

        if (username == null || password == null) {
            displayInfo(
                view.context,
                view.context.getString(R.string.setUsernamePasswordSettingsMsg)
            )

            fragmentBackPressedCallback = {

            }
        } else {
            playlistHandler.registerListeners(view)

            playlistHandler.loadPlaylist(view, false)

            fragmentBackPressedCallback = {
                playlistHandler.loadPlaylist(view, false)
            }
        }
    }
}