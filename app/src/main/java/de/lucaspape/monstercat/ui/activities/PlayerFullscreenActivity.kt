package de.lucaspape.monstercat.ui.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.ui.handlers.FullscreenPlayerHandler

class PlayerFullscreenActivity : AppCompatActivity() {

    private val fullscreenPlayerHandler = FullscreenPlayerHandler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_player_fullscreen)

        fullscreenPlayerHandler.onCreate(window.decorView.rootView, null)
    }
}