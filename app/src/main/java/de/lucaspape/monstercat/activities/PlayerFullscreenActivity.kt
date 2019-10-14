package de.lucaspape.monstercat.activities

import android.content.Context
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.handlers.FullscreenPlayerHandler
import de.lucaspape.monstercat.music.*
import java.lang.ref.Reference
import java.lang.ref.WeakReference

class PlayerFullscreenActivity : AppCompatActivity() {

    var prevTextView1: WeakReference<TextView>? = null
    var prevTextView2: WeakReference<TextView>? = null
    var prevCoverBarImageView: WeakReference<ImageView>? = null
    var prevPlayButton: WeakReference<ImageButton>? = null
    var prevSeekBar: WeakReference<SeekBar>? = null
    var prevContext: WeakReference<Context>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_player_fullscreen)

        prevTextView1 = textView1Reference
        prevTextView2 = textView2Reference
        prevCoverBarImageView = barCoverImageReference
        prevPlayButton = playButtonReference
        prevSeekBar = seekBarReference
        prevContext = contextReference

        val fullscreenPlayerHandler = FullscreenPlayerHandler()
        fullscreenPlayerHandler.setupMusicPlayer(window.decorView.rootView)
        fullscreenPlayerHandler.registerListeners(window.decorView.rootView)
    }

    override fun onBackPressed() {
        setTextView(prevTextView1!!.get()!!, prevTextView2!!.get()!!)
        setBarCoverImageView(prevCoverBarImageView!!.get()!!)
        setPlayButton(playButtonReference!!.get()!!)
        setSeekBar(prevSeekBar!!.get()!!)
        contextReference = prevContext

        blackbuttons = false

        super.onBackPressed()
    }

}