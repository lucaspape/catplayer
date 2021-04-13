package de.lucaspape.monstercat.ui.pages

import android.content.Context
import android.text.Spanned
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.net.toUri
import androidx.core.text.HtmlCompat
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.core.music.*
import de.lucaspape.monstercat.core.music.notification.hideLoadingRelatedSongsNotification
import de.lucaspape.monstercat.core.music.notification.showLoadingRelatedNotification
import de.lucaspape.monstercat.core.music.util.*
import de.lucaspape.monstercat.ui.abstract_items.content.CatalogItem
import de.lucaspape.monstercat.ui.pages.util.Page
import de.lucaspape.monstercat.ui.pauseButtonDrawable
import de.lucaspape.monstercat.ui.playButtonDrawable
import java.lang.IndexOutOfBoundsException
import java.util.*

class FullscreenPlayerPage(
    private val onSearch: (searchString: String?) -> Unit,
    private val closeFullscreen: () -> Unit
) : Page() {

    constructor() : this({}, {})

    companion object {
        @JvmStatic
        val fullscreenPlayerPageName = "fullscreen-player"
    }

    private fun bindPlayerUICallbacks(view: View) {
        val titleTextView = view.findViewById<TextView>(R.id.fullscreenTitle)
        val artistTextView = view.findViewById<TextView>(R.id.fullscreenArtist)
        val seekbar = view.findViewById<SeekBar>(R.id.fullscreenSeekBar)
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

        seekbar.progress = currentPosition.toInt()

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
            seekbar.progress = currentPosition.toInt()

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
            seekbar.max = duration.toInt()

            val minutes = duration / 60000
            val seconds = (duration % 60000) / 1000

            val text = if (seconds < 10) {
                "$minutes:0$seconds"
            } else {
                "$minutes:$seconds"
            }

            songTimeMax.text = text
        }

        playingChangedCallback = {
            if (visiblePlaying) {
                playButton.setImageURI(pauseButtonDrawable.toUri())

            } else {
                playButton.setImageURI(playButtonDrawable.toUri())
            }
        }

        playingChangedCallback()

        loadingRelatedChangedCallback = {
            if (loadingRelatedSongs) {
                showLoadingRelatedNotification(view.context)
            } else {
                hideLoadingRelatedSongsNotification(view.context)
            }
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

        val viewPager = view.findViewById<ViewPager>(R.id.fullscreenViewPager)
        viewPager.adapter = ViewPagerAdapter(view.context)

        view.findViewById<Button>(R.id.fullscreenLyricsButton).setOnClickListener {
            viewPager.currentItem = 1
        }
    }

    override fun onBackPressed(view: View): Boolean {
        closeFullscreen()
        return false
    }

    override fun onPause(view: View) {
    }

    override val layout: Int = R.layout.activity_player_fullscreen

    override fun onCreate(view: View) {
        bindPlayerUICallbacks(view)
        registerListeners(view)
    }

    override val pageName: String = fullscreenPlayerPageName
}

class ViewPagerAdapter(private val context: Context) : PagerAdapter() {
    override fun getCount(): Int {
        return 2
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view == `object` as ConstraintLayout
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val layoutInflater =
            context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        return when (position) {
            0 -> {
                val item = layoutInflater.inflate(R.layout.fullscreen_image_item, container, false)

                val fullscreenImageView = item.findViewById<ImageView>(R.id.fullscreenAlbumImage)

                fullscreenImageView?.setImageBitmap(coverBitmap)

                coverBitmapChangedCallback = {
                    fullscreenImageView?.setImageBitmap(coverBitmap)
                }

                coverDrawableChangedCallback = {
                    fullscreenImageView?.setImageDrawable(coverDrawable)
                }

                setTagCallback = { target ->
                    fullscreenImageView?.tag = target
                }

                Objects.requireNonNull(container).addView(item)

                item
            }
            1 -> {
                val item = layoutInflater.inflate(R.layout.fullscreen_lyrics_item, container, false)

                val fullscreenLyricsView = item.findViewById<TextView>(R.id.fullscreenLyrics)

                fullscreenLyricsView.text = getSpannableLyricText(context)

                lyricsChangedCallback = {
                    fullscreenLyricsView.text = getSpannableLyricText(context)
                }

                Objects.requireNonNull(container).addView(item)

                item
            }
            else -> {
                layoutInflater.inflate(R.layout.fullscreen_image_item, container, false)
            }
        }
    }

    private fun getSpannableLyricText(context: Context): Spanned {
        val previousLyric = try {
            lyricTextArray[currentLyricsIndex - 1].replace("\n", "<br><br>")
        } catch (e: IndexOutOfBoundsException) {
            ""
        }

        var currentLyric = try {
            lyricTextArray[currentLyricsIndex].replace("\n", "<br><br>")
        } catch (e: IndexOutOfBoundsException) {
            ""
        }

        val nextLyric = try {
            lyricTextArray[currentLyricsIndex + 1].replace("\n", "<br><br>")
        } catch (e: IndexOutOfBoundsException) {
            ""
        }

        if (previousLyric == "" && currentLyric == "" && nextLyric == "") {
            currentLyric = context.getString(R.string.noLyricsAvailable)
        } else if ((currentLyric != "" && previousLyric != "") || (currentLyric != "" && nextLyric != "")) {
            currentLyric = "<h1>$currentLyric</h1>"
        }

        return HtmlCompat.fromHtml(
            "$previousLyric <br> $currentLyric <br> $nextLyric",
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )
    }
}