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
        val shuffleButton = view.findViewById<ImageButton>(R.id.fullscreenShuffle)
        val loopButton = view.findViewById<ImageButton>(R.id.fullscreenLoop)

        if(shuffle){
            shuffleButton.setImageResource(R.drawable.ic_shuffle_green_24dp)
        }else{
            shuffleButton.setImageResource(R.drawable.ic_shuffle_24dp)
        }

        if(loop){
            loopButton.setImageResource(R.drawable.ic_repeat_green_24dp)
        }else if(loopSingle){
            loopButton.setImageResource(R.drawable.ic_repeat_one_green_24dp)
        }else{
            loopButton.setImageResource(R.drawable.ic_repeat_24dp)
        }

        playButton.setOnClickListener {
            toggleMusic()
        }

        nextButton.setOnClickListener {
            next()
        }

        backButton.setOnClickListener {
            previous()
        }

        shuffleButton.setOnClickListener {
            if(shuffle){
                shuffle = false
                shuffleButton.setImageResource(R.drawable.ic_shuffle_24dp)
            }else{
                shuffle = true
                shuffleButton.setImageResource(R.drawable.ic_shuffle_green_24dp)
            }
        }

        loopButton.setOnClickListener {
            when {
                loop -> {
                    loop = false

                    loopSingle = true

                    loopButton.setImageResource(R.drawable.ic_repeat_one_green_24dp)
                }
                loopSingle -> {
                    loopSingle = false
                    loopButton.setImageResource(R.drawable.ic_repeat_24dp)
                }
                else -> {
                    loop = true
                    loopButton.setImageResource(R.drawable.ic_repeat_green_24dp)
                }
            }
        }
    }

}