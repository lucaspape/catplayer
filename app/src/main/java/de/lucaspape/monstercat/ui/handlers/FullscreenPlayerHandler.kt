package de.lucaspape.monstercat.ui.handlers

import android.view.View
import android.widget.*
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.music.*
import de.lucaspape.monstercat.music.util.*
import java.lang.ref.WeakReference

class FullscreenPlayerHandler(private val onSearch:(searchString:String?) -> Unit): Handler {
    private fun setupMusicPlayer(view: View) {
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

    private fun registerListeners(view: View) {
        //music control buttons
        val playButton = view.findViewById<ImageButton>(R.id.fullScreenPlay)
        val backButton = view.findViewById<ImageButton>(R.id.fullscreenPrev)
        val nextButton = view.findViewById<ImageButton>(R.id.fullscreenNext)
        val shuffleButton = view.findViewById<ImageButton>(R.id.fullscreenShuffle)
        val loopButton = view.findViewById<ImageButton>(R.id.fullscreenLoop)

        if (shuffle) {
            shuffleButton.setImageResource(R.drawable.ic_shuffle_green_24dp)
        } else {
            shuffleButton.setImageResource(R.drawable.ic_shuffle_24dp)
        }

        when {
            loop -> {
                loopButton.setImageResource(R.drawable.ic_repeat_green_24dp)
            }
            loopSingle -> {
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
            if (shuffle) {
                shuffle = false
                shuffleButton.setImageResource(R.drawable.ic_shuffle_24dp)
            } else {
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

        val titleTextView = view.findViewById<TextView>(R.id.fullscreenTitle)
        val artistTextView = view.findViewById<TextView>(R.id.fullscreenArtist)

        titleTextView.setOnClickListener {
            onSearch(titleTextView.text.toString())
        }

        artistTextView.setOnClickListener {
            onSearch(artistTextView.text.toString())
        }
    }

    override fun onBackPressed(view: View) {
    }

    override fun onPause(view: View) {
    }

    override val layout: Int = R.layout.activity_player_fullscreen

    override fun onCreate(view: View) {
        setupMusicPlayer(view)
        registerListeners(view)
    }
}