package de.lucaspape.monstercat.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.handlers.FullscreenPlayerHandler

class PlayerFullscreenActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_player_fullscreen)

        val fullscreenPlayerHandler = FullscreenPlayerHandler()
        fullscreenPlayerHandler.setupMusicPlayer(window.decorView.rootView)
        fullscreenPlayerHandler.registerListeners(window.decorView.rootView)
    }
}