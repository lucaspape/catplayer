package de.lucaspape.monstercat.handlers

import android.view.View
import android.widget.*
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.music.*

class FullscreenPlayerHandler {
    fun setupMusicPlayer(view: View) {
        val textView1 = view.findViewById<TextView>(R.id.fullscreenSongCurrent1)
        val textView2 = view.findViewById<TextView>(R.id.fullscreenSongCurrent2)
        val coverBarImageView = view.findViewById<ImageView>(R.id.fullscreenAlbumImage)
        val playButton = view.findViewById<ImageButton>(R.id.fullScreenPlay)
        val seekBar = view.findViewById<SeekBar>(R.id.fullscreenSeekBar)

        //setup musicPlayer
        setFullscreenTextView(textView1, textView2)
        setFullscreenSeekBar(seekBar)
        setFullscreenCoverImageView(coverBarImageView)
        setFullscreenPlayButton(playButton, view.context)
    }

    fun registerListeners(view: View) {
        //music control buttons
        val playButton = view.findViewById<ImageButton>(R.id.fullScreenPlay)
        val backButton = view.findViewById<ImageButton>(R.id.fullscreenPrev)
        val nextButton = view.findViewById<ImageButton>(R.id.fullscreenNext)

        playButton.setOnClickListener {
            toggleMusic()
        }

        nextButton.setOnClickListener {
            next()
        }

        backButton.setOnClickListener {
            previous()
        }
    }

}