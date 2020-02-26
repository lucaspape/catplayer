package de.lucaspape.monstercat.handlers

import android.view.View
import android.widget.*
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.music.*
import java.lang.ref.WeakReference

class FullscreenPlayerHandler {
    fun setupMusicPlayer(view: View) {
        val titleTextView = view.findViewById<TextView>(R.id.fullscreenTitle)
        val artistTextView = view.findViewById<TextView>(R.id.fullscreenArtist)
        val coverBarImageView = view.findViewById<ImageView>(R.id.fullscreenAlbumImage)
        val playButton = view.findViewById<ImageButton>(R.id.fullScreenPlay)
        val seekBar = view.findViewById<SeekBar>(R.id.fullscreenSeekBar)

        //setup musicPlayer
        fullscreenTitleReference = WeakReference(titleTextView)
        fullscreenArtistReference = WeakReference(artistTextView)
        fullscreenSeekBarReference = WeakReference(seekBar)
        fullscreenCoverReference = WeakReference(coverBarImageView)
        fullscreenPlayButtonReference = WeakReference(playButton)
    }

    fun registerListeners(view: View) {
        //music control buttons
        val playButton = view.findViewById<ImageButton>(R.id.fullScreenPlay)
        val backButton = view.findViewById<ImageButton>(R.id.fullscreenPrev)
        val nextButton = view.findViewById<ImageButton>(R.id.fullscreenNext)
        val shuffleButton = view.findViewById<ImageButton>(R.id.fullscreenShuffle)
        val loopButton = view.findViewById<ImageButton>(R.id.fullscreenLoop)

        if (MonstercatPlayer.shuffle) {
            shuffleButton.setImageResource(R.drawable.ic_shuffle_green_24dp)
        } else {
            shuffleButton.setImageResource(R.drawable.ic_shuffle_24dp)
        }

        when {
            MonstercatPlayer.loop -> {
                loopButton.setImageResource(R.drawable.ic_repeat_green_24dp)
            }
            MonstercatPlayer.loopSingle -> {
                loopButton.setImageResource(R.drawable.ic_repeat_one_green_24dp)
            }
            else -> {
                loopButton.setImageResource(R.drawable.ic_repeat_24dp)
            }
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
            if (MonstercatPlayer.shuffle) {
                MonstercatPlayer.shuffle = false
                shuffleButton.setImageResource(R.drawable.ic_shuffle_24dp)
            } else {
                MonstercatPlayer.shuffle = true
                shuffleButton.setImageResource(R.drawable.ic_shuffle_green_24dp)
            }
        }

        loopButton.setOnClickListener {
            when {
                MonstercatPlayer.loop -> {
                    MonstercatPlayer.loop = false

                    MonstercatPlayer.loopSingle = true

                    loopButton.setImageResource(R.drawable.ic_repeat_one_green_24dp)
                }
                MonstercatPlayer.loopSingle -> {
                    MonstercatPlayer.loopSingle = false
                    loopButton.setImageResource(R.drawable.ic_repeat_24dp)
                }
                else -> {
                    MonstercatPlayer.loop = true
                    loopButton.setImageResource(R.drawable.ic_repeat_green_24dp)
                }
            }
        }
    }

}