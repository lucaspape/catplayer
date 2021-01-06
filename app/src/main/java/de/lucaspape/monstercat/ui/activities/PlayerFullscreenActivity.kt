package de.lucaspape.monstercat.ui.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import de.lucaspape.monstercat.ui.pages.FullscreenPlayerPage

class PlayerFullscreenActivity : AppCompatActivity() {

    private val fullscreenPlayerPage = FullscreenPlayerPage({ search ->
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("search", search)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
    }, {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
    })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(fullscreenPlayerPage.layout)

        fullscreenPlayerPage.onCreate(window.decorView.rootView)
    }

    override fun onBackPressed() {
        super.onBackPressed()

        fullscreenPlayerPage.onBackPressed(window.decorView.rootView)
    }
}