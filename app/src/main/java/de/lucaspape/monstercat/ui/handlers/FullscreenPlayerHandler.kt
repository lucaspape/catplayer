package de.lucaspape.monstercat.ui.handlers

import android.view.View
import android.widget.*
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.music.*
import de.lucaspape.monstercat.music.util.*
import de.lucaspape.monstercat.ui.abstract_items.content.CatalogItem
import java.lang.ref.WeakReference

class FullscreenPlayerHandler(
    private val onSearch: (searchString: String?) -> Unit,
    private val closeFullscreen: () -> Unit
) : Handler {
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
        val closeButton = view.findViewById<ImageButton>(R.id.fullscreenBack)
        val fullscreenMenuButton = view.findViewById<ImageButton>(R.id.fullscreenMenuButton)

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
            toggleMusic(view.context)
        }

        nextButton.setOnClickListener {
            next(view.context)
        }

        backButton.setOnClickListener {
            previous(view.context)
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

        closeButton.setOnClickListener {
            closeFullscreen()
        }

        fullscreenMenuButton.setOnClickListener {
            CatalogItem.showContextMenu(view, arrayListOf(currentSongId), 0)
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
        closeFullscreen()
    }

    override fun onPause(view: View) {
    }

    override val layout: Int = R.layout.activity_player_fullscreen

    override fun onCreate(view: View) {
        setupMusicPlayer(view)
        registerListeners(view)
    }
}