package de.lucaspape.monstercat.ui.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import de.lucaspape.monstercat.ui.handlers.FullscreenPlayerHandler

class PlayerFullscreenActivity : AppCompatActivity() {

    private val fullscreenPlayerHandler = FullscreenPlayerHandler { search ->
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("search", search)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(fullscreenPlayerHandler.layout)

        fullscreenPlayerHandler.onCreate(window.decorView.rootView)
    }
}