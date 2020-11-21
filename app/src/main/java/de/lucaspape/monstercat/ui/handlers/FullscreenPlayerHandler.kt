package de.lucaspape.monstercat.ui.handlers

import android.view.View
import android.widget.*
import androidx.core.net.toUri
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.core.music.*
import de.lucaspape.monstercat.core.music.util.*
import de.lucaspape.monstercat.ui.abstract_items.content.CatalogItem
import de.lucaspape.monstercat.ui.pauseButtonDrawable
import de.lucaspape.monstercat.ui.playButtonDrawable

class FullscreenPlayerHandler(
    private val onSearch: (searchString: String?) -> Unit,
    private val closeFullscreen: () -> Unit
) : Handler {

    private fun bindPlayerUICallbacks(view: View) {
        val titleTextView = view.findViewById<TextView>(R.id.fullscreenTitle)
        val artistTextView = view.findViewById<TextView>(R.id.fullscreenArtist)
        val seekbar = view.findViewById<SeekBar>(R.id.fullscreenSeekBar)
        val barCoverImage = view.findViewById<ImageView>(R.id.fullscreenAlbumImage)
        val playButton = view.findViewById<ImageButton>(R.id.fullScreenPlay)

        val songTimePassed = view.findViewById<TextView>(R.id.songTimePassed)
        val songTimeMax = view.findViewById<TextView>(R.id.songTimeMax)

        titleChangedCallback = {
            titleTextView.text = title
        }

        titleTextView.text = title
        artistTextView.text = artist

        artistChangedCallback = {
            artistTextView.text = artist
        }

        seekbar.progress = currentPosition

        seekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser)
                    exoPlayer?.seekTo(progress.toLong())
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
            }
        })

        currentPositionChangedCallback = {
            seekbar.progress = currentPosition

            val minutes = currentPosition / 60000
            val seconds = (currentPosition % 60000) / 1000

            val text = if (seconds < 10) {
                "$minutes:0$seconds"
            } else {
                "$minutes:$seconds"
            }

            songTimePassed.text = text
        }

        durationChangedCallback = {
            seekbar.max = duration

            val minutes = duration / 60000
            val seconds = (duration % 60000) / 1000

            val text = if (seconds < 10) {
                "$minutes:0$seconds"
            } else {
                "$minutes:$seconds"
            }

            songTimeMax.text = text
        }

        barCoverImage.setImageBitmap(coverBitmap)

        playingChangedCallback = {
            if (playing) {
                playButton.setImageURI(pauseButtonDrawable.toUri())

            } else {
                playButton.setImageURI(playButtonDrawable.toUri())
            }
        }

        exoPlayer?.isPlaying?.let { isPlaying ->
            playing = isPlaying
        }

        coverBitmapChangedCallback = {
            barCoverImage.setImageBitmap(coverBitmap)
        }

        coverDrawableChangedCallback = {
            barCoverImage.setImageDrawable(coverDrawable)
        }

        setTagCallback = { target ->
            barCoverImage.tag = target
        }
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
        bindPlayerUICallbacks(view)
        registerListeners(view)
    }
}