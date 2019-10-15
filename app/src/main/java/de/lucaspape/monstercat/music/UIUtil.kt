package de.lucaspape.monstercat.music

import android.animation.ValueAnimator
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.ColorDrawable
import android.media.MediaMetadata
import android.media.session.PlaybackState
import android.os.Handler
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.database.Song
import de.lucaspape.monstercat.settings.Settings
import java.io.File
import java.lang.IllegalStateException
import java.lang.NullPointerException
import java.lang.ref.WeakReference

var textView1Reference: WeakReference<TextView>? = null
var textView2Reference: WeakReference<TextView>? = null
var seekBarReference: WeakReference<SeekBar>? = null
var barCoverImageReference: WeakReference<ImageView>? = null
var musicBarReference: WeakReference<androidx.appcompat.widget.Toolbar>? = null
var playButtonReference: WeakReference<ImageButton>? = null

var fullscreenTextView1Reference: WeakReference<TextView>? = null
var fullscreenTextView2Reference: WeakReference<TextView>? = null
var fullscreenSeekBarReference: WeakReference<SeekBar>? = null
var fullscreenCoverReference: WeakReference<ImageView>? = null
var fullscreenPlayButtonReference: WeakReference<ImageButton>? = null

/**
 * UI update methods
 */
fun setTextView(newTextView1: TextView, newTextView2: TextView) {
    try {
        newTextView1.text = textView1Reference!!.get()!!.text
        newTextView2.text = textView2Reference!!.get()!!.text
    } catch (e: NullPointerException) {

    }

    textView1Reference = WeakReference(newTextView1)
    textView2Reference = WeakReference(newTextView2)
}

fun setFullscreenTextView(newTextView1: TextView, newTextView2: TextView) {
    try {
        newTextView1.text = textView1Reference!!.get()!!.text
        newTextView2.text = textView2Reference!!.get()!!.text
    } catch (e: NullPointerException) {

    }

    fullscreenTextView1Reference = WeakReference(newTextView1)
    fullscreenTextView2Reference = WeakReference(newTextView2)
}

fun setSeekBar(newSeekBar: SeekBar) {
    try {
        newSeekBar.progress = seekBarReference!!.get()!!.progress
    } catch (e: NullPointerException) {

    }

    newSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
            if (fromUser)
                mediaPlayer.seekTo(progress)
        }

        override fun onStartTrackingTouch(seekBar: SeekBar) {
        }

        override fun onStopTrackingTouch(seekBar: SeekBar) {
        }
    })

    seekBarReference = WeakReference(newSeekBar)
}

fun setFullscreenSeekBar(newSeekBar: SeekBar) {
    try {
        newSeekBar.progress = seekBarReference!!.get()!!.progress
    } catch (e: NullPointerException) {

    }

    newSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
            if (fromUser)
                mediaPlayer.seekTo(progress)
        }

        override fun onStartTrackingTouch(seekBar: SeekBar) {
        }

        override fun onStopTrackingTouch(seekBar: SeekBar) {
        }
    })

    fullscreenSeekBarReference = WeakReference(newSeekBar)
}

fun setBarCoverImageView(newImageView: ImageView) {
    try {
        newImageView.setImageDrawable(barCoverImageReference!!.get()!!.drawable)
    } catch (e: NullPointerException) {

    }

    barCoverImageReference = WeakReference(newImageView)
}

fun setFullscreenCoverImageView(newImageView: ImageView) {
    try {
        newImageView.setImageDrawable(barCoverImageReference!!.get()!!.drawable)
    } catch (e: NullPointerException) {

    }

    fullscreenCoverReference = WeakReference(newImageView)
}

fun setMusicBar(newToolbar: androidx.appcompat.widget.Toolbar) {
    try {
        newToolbar.setBackgroundColor((musicBarReference!!.get()!!.background as ColorDrawable).color)
    } catch (e: NullPointerException) {

    }
    musicBarReference = WeakReference(newToolbar)
}

fun setPlayButton(newPlayButton: ImageButton) {
    if (playing) {
        newPlayButton.setImageDrawable(
            ContextCompat.getDrawable(
                contextReference!!.get()!!,
                R.drawable.ic_pause_white_24dp
            )
        )

    } else {
        newPlayButton.setImageDrawable(
            ContextCompat.getDrawable(
                contextReference!!.get()!!,
                R.drawable.ic_play_arrow_white_24dp
            )
        )

    }

    playButtonReference = WeakReference(newPlayButton)
}

fun setFullscreenPlayButton(newPlayButton: ImageButton) {
    if (playing) {
        newPlayButton.setImageDrawable(
            ContextCompat.getDrawable(
                contextReference!!.get()!!,
                R.drawable.ic_pause_black_24dp
            )
        )

    } else {
        newPlayButton.setImageDrawable(
            ContextCompat.getDrawable(
                contextReference!!.get()!!,
                R.drawable.ic_play_arrow_black_24dp
            )
        )

    }

    fullscreenPlayButtonReference = WeakReference(newPlayButton)
}

internal fun setTitle(title: String) {
    try {
        textView1Reference!!.get()!!.text = title
    } catch (e: NullPointerException) {

    }
    try {
        textView2Reference!!.get()!!.text = title
    } catch (e: NullPointerException) {

    }

    try {
        fullscreenTextView1Reference!!.get()!!.text = title
    } catch (e: NullPointerException) {

    }

    try {
        fullscreenTextView2Reference!!.get()!!.text = title
    } catch (e: NullPointerException) {

    }
}

internal fun startTextAnimation() {
    val valueAnimator = ValueAnimator.ofFloat(0.0f, 1.0f)
    valueAnimator.repeatCount = Animation.INFINITE
    valueAnimator.interpolator = LinearInterpolator()
    valueAnimator.duration = 9000L
    valueAnimator.addUpdateListener { animation ->
        val progress = animation.animatedValue as Float

        var width = 0


        try {
            width = textView1Reference!!.get()!!.width
        } catch (e: NullPointerException) {

        }


        try {
            width = fullscreenTextView1Reference!!.get()!!.width
        } catch (e: NullPointerException) {

        }


        val translationX = width * progress

        try {
            textView1Reference!!.get()!!.translationX = translationX
        } catch (e: NullPointerException) {

        }

        try {
            textView2Reference!!.get()!!.translationX = translationX - width
        } catch (e: NullPointerException) {
        }

        try {
            fullscreenTextView1Reference!!.get()!!.translationX = translationX
        } catch (e: NullPointerException) {

        }

        try {
            fullscreenTextView2Reference!!.get()!!.translationX = translationX - width
        } catch (e: NullPointerException) {

        }
    }

    valueAnimator.start()
}

internal fun startSeekBarUpdate() {
    val seekBarUpdateHandler = Handler()

    val updateSeekBar = object : Runnable {
        override fun run() {

            try {
                seekBarReference!!.get()!!.max = mediaPlayer.duration
                seekBarReference!!.get()!!.progress = mediaPlayer.currentPosition
            } catch (e: NullPointerException) {

            }

            try {
                fullscreenSeekBarReference!!.get()!!.max = mediaPlayer.duration
                fullscreenSeekBarReference!!.get()!!.progress = mediaPlayer.currentPosition
            } catch (e: NullPointerException) {

            }
            setPlayerState(mediaPlayer.currentPosition.toLong())

            seekBarUpdateHandler.postDelayed(this, 50)
        }
    }

    try {
        seekBarUpdateHandler.postDelayed(updateSeekBar, 0)

        seekBarReference!!.get()!!.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(
                seekBar: SeekBar,
                progress: Int,
                fromUser: Boolean
            ) {
                if (fromUser)
                    mediaPlayer.seekTo(progress)
                setPlayerState(progress.toLong())
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
            }
        })
    } catch (e: NullPointerException) {

    }

    try {
        fullscreenSeekBarReference!!.get()!!.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(
                seekBar: SeekBar,
                progress: Int,
                fromUser: Boolean
            ) {
                if (fromUser)
                    mediaPlayer.seekTo(progress)
                setPlayerState(progress.toLong())
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
            }
        })
    } catch (e: NullPointerException) {

    }
}

internal fun setCover(song: Song) {
    val settings = Settings(contextReference!!.get()!!)
    val primaryResolution = settings.getSetting("primaryCoverResolution")

    val coverFile =
        File(contextReference!!.get()!!.filesDir.toString() + "/" + song.albumId + ".png" + primaryResolution)

    if (coverFile.exists()) {
        val bitmap = BitmapFactory.decodeFile(coverFile.absolutePath)

        try {
            barCoverImageReference!!.get()!!.setImageBitmap(bitmap)
        } catch (e: NullPointerException) {

        }

        try {
            fullscreenCoverReference!!.get()!!.setImageBitmap(bitmap)
        } catch (e: NullPointerException) {

        }

        setSongMetadata(song.artist, song.title, bitmap, mediaPlayer.duration.toLong())
    } else {
        setSongMetadata(song.artist, song.title, null, mediaPlayer.duration.toLong())
    }
}

internal fun setPlayButtonImage() {
    if (playing) {
        try {
            playButtonReference!!.get()!!.setImageDrawable(
                ContextCompat.getDrawable(
                    contextReference!!.get()!!,
                    R.drawable.ic_pause_white_24dp
                )
            )

        } catch (e: NullPointerException) {

        }

        try {
            fullscreenPlayButtonReference!!.get()!!.setImageDrawable(
                ContextCompat.getDrawable(
                    contextReference!!.get()!!,
                    R.drawable.ic_pause_black_24dp
                )
            )

        } catch (e: NullPointerException) {

        }
    } else {
        try {
            playButtonReference!!.get()!!.setImageDrawable(
                ContextCompat.getDrawable(
                    contextReference!!.get()!!,
                    R.drawable.ic_play_arrow_white_24dp
                )
            )

        } catch (e: NullPointerException) {

        }

        try {
            fullscreenPlayButtonReference!!.get()!!.setImageDrawable(
                ContextCompat.getDrawable(
                    contextReference!!.get()!!,
                    R.drawable.ic_play_arrow_black_24dp
                )
            )

        } catch (e: NullPointerException) {

        }
    }
}

/**
 * SetPlayerState
 */
internal fun setPlayerState(progress: Long) {
    val stateBuilder = PlaybackStateCompat.Builder()

    val state: Int = if (playing) {
        PlaybackState.STATE_PLAYING
    } else {
        PlaybackState.STATE_PAUSED
    }

    stateBuilder.setState(state, progress, 1.0f)
    stateBuilder.setActions(
        PlaybackStateCompat.ACTION_PLAY +
                PlaybackStateCompat.ACTION_PAUSE +
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT +
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS +
                PlaybackStateCompat.ACTION_STOP +
                PlaybackStateCompat.ACTION_PLAY_PAUSE +
                PlaybackStateCompat.ACTION_SEEK_TO +
                PlaybackStateCompat.ACTION_FAST_FORWARD +
                PlaybackStateCompat.ACTION_REWIND
    )
    mediaSession!!.setPlaybackState(stateBuilder.build())
}

/**
 * Set song metadata
 */
internal fun setSongMetadata(artist: String, title: String, coverImage: Bitmap?, duration: Long) {
    val mediaMetadata = MediaMetadataCompat.Builder()
    mediaMetadata.putString(MediaMetadata.METADATA_KEY_ARTIST, artist)
    mediaMetadata.putString(MediaMetadata.METADATA_KEY_TITLE, title)
    mediaMetadata.putLong(MediaMetadata.METADATA_KEY_DURATION, duration)
    mediaMetadata.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, coverImage)
    mediaMetadata.putBitmap(MediaMetadata.METADATA_KEY_ART, coverImage)
    mediaSession!!.setMetadata(mediaMetadata.build())
}