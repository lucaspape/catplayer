package de.lucaspape.monstercat.ui.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.ui.handlers.FullscreenPlayerHandler

class PlayerFullscreenActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_player_fullscreen)

        val fullscreenPlayerHandler = FullscreenPlayerHandler()
        fullscreenPlayerHandler.setupMusicPlayer(window.decorView.rootView)
        fullscreenPlayerHandler.registerListeners(window.decorView.rootView)

        val titleTextView = findViewById<TextView>(R.id.fullscreenTitle)
        val artistTextView = findViewById<TextView>(R.id.fullscreenArtist)

        titleTextView.setOnClickListener {
            search(this, titleTextView.text.toString())
        }

        artistTextView.setOnClickListener {
            search(this, artistTextView.text.toString())
        }
    }

    private fun search(context: Context, term: String) {
        val intent = Intent(context, MainActivity::class.java)
        intent.putExtra("search", term)
        context.startActivity(intent)
    }
}