package de.lucaspape.monstercat.music

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.BitmapFactory
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
import de.lucaspape.monstercat.util.Settings
import java.io.File
import java.lang.ref.WeakReference

private var textViewReference: WeakReference<TextView>? = null
private var seekBarReference: WeakReference<SeekBar>? = null
private var barCoverImageReference: WeakReference<ImageView>? = null
private var musicBarReference: WeakReference<androidx.appcompat.widget.Toolbar>? = null
private var playButtonReference: WeakReference<ImageButton>? = null

private var fullscreenTextView1Reference: WeakReference<TextView>? = null
private var fullscreenTextView2Reference: WeakReference<TextView>? = null
private var fullscreenSeekBarReference: WeakReference<SeekBar>? = null
private var fullscreenCoverReference: WeakReference<ImageView>? = null
private var fullscreenPlayButtonReference: WeakReference<ImageButton>? = null

/**
 * UI update methods
 */
fun setTextView(newTextView: TextView) {
    newTextView.text = textViewReference?.get()?.text

    textViewReference = WeakReference(newTextView)
}

fun setFullscreenTextView(newTextView1: TextView, newTextView2: TextView) {
    newTextView1.text = textViewReference?.get()?.text
    newTextView2.text = textViewReference?.get()?.text

    fullscreenTextView1Reference = WeakReference(newTextView1)
    fullscreenTextView2Reference = WeakReference(newTextView2)
}

fun setSeekBar(newSeekBar: SeekBar) {
    seekBarReference?.get()?.progress?.let { newSeekBar.progress = it }

    newSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
            if (fromUser)
                mediaPlayer?.seekTo(progress.toLong())
        }

        override fun onStartTrackingTouch(seekBar: SeekBar) {
        }

        override fun onStopTrackingTouch(seekBar: SeekBar) {
        }
    })

    seekBarReference = WeakReference(newSeekBar)
}

fun setFullscreenSeekBar(newSeekBar: SeekBar) {
    seekBarReference?.get()?.progress?.let { newSeekBar.progress = it }

    newSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
            if (fromUser)
                mediaPlayer?.seekTo(progress.toLong())
        }

        override fun onStartTrackingTouch(seekBar: SeekBar) {
        }

        override fun onStopTrackingTouch(seekBar: SeekBar) {
        }
    })

    fullscreenSeekBarReference = WeakReference(newSeekBar)
}

fun setBarCoverImageView(newImageView: ImageView) {
    newImageView.setImageDrawable(barCoverImageReference?.get()?.drawable)

    barCoverImageReference = WeakReference(newImageView)
}

fun setFullscreenCoverImageView(newImageView: ImageView) {
    newImageView.setImageDrawable(barCoverImageReference?.get()?.drawable)

    fullscreenCoverReference = WeakReference(newImageView)
}

fun setMusicBar(newToolbar: androidx.appcompat.widget.Toolbar) {
    musicBarReference = WeakReference(newToolbar)
}

fun setPlayButton(newPlayButton: ImageButton, context: Context) {
    if (mediaPlayer?.isPlaying == true) {
        newPlayButton.setImageDrawable(
            ContextCompat.getDrawable(
                context,
                R.drawable.ic_pause_24dp
            )
        )

    } else {
        newPlayButton.setImageDrawable(
            ContextCompat.getDrawable(
                context,
                R.drawable.ic_play_arrow_24dp
            )
        )

    }

    playButtonReference = WeakReference(newPlayButton)
}

fun setFullscreenPlayButton(newPlayButton: ImageButton, context: Context) {
    if (mediaPlayer?.isPlaying == true) {
        newPlayButton.setImageDrawable(
            ContextCompat.getDrawable(
                context,
                R.drawable.ic_pause_24dp
            )
        )

    } else {
        newPlayButton.setImageDrawable(
            ContextCompat.getDrawable(
                context,
                R.drawable.ic_play_arrow_24dp
            )
        )

    }

    fullscreenPlayButtonReference = WeakReference(newPlayButton)
}

internal fun setTitle(title: String, version: String, artist: String) {

    val text = "$title $version - $artist"

    textViewReference?.get()?.text = text

    fullscreenTextView1Reference?.get()?.text = text

    fullscreenTextView2Reference?.get()?.text = text
}

internal fun hideTitle() {
    val text = ""

    textViewReference?.get()?.text = text

    fullscreenTextView1Reference?.get()?.text = text

    fullscreenTextView2Reference?.get()?.text = text
}

internal fun startTextAnimation() {
    val valueAnimator = ValueAnimator.ofFloat(0.0f, 1.0f)
    valueAnimator.repeatCount = Animation.INFINITE
    valueAnimator.interpolator = LinearInterpolator()
    valueAnimator.duration = 9000L
    valueAnimator.addUpdateListener { animation ->
        val progress = animation.animatedValue as Float

        var width = 0

        fullscreenTextView1Reference?.get()?.width?.let { width = it }

        val translationX = width * progress

        fullscreenTextView1Reference?.get()?.translationX = translationX

        fullscreenTextView2Reference?.get()?.translationX = translationX - width
    }

    valueAnimator.start()
}

@SuppressLint("ClickableViewAccessibility")
internal fun startSeekBarUpdate() {
    val seekBarUpdateHandler = Handler()

    val updateSeekBar = object : Runnable {
        override fun run() {
            mediaPlayer?.duration?.toInt()?.let { seekBarReference?.get()?.max = it }
            mediaPlayer?.currentPosition?.toInt()
                ?.let { seekBarReference?.get()?.progress = it }


            mediaPlayer?.duration?.toInt()?.let { fullscreenSeekBarReference?.get()?.max = it }
            mediaPlayer?.currentPosition?.toInt()
                ?.let { fullscreenSeekBarReference?.get()?.progress = it }


            mediaPlayer?.currentPosition?.let { setPlayerState(it) }

            seekBarUpdateHandler.postDelayed(this, 50)
        }
    }

    seekBarUpdateHandler.postDelayed(updateSeekBar, 0)

    seekBarReference?.get()?.setOnTouchListener { _, _ -> true }

    fullscreenSeekBarReference?.get()?.setOnSeekBarChangeListener(object :
        SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(
            seekBar: SeekBar,
            progress: Int,
            fromUser: Boolean
        ) {
            if (fromUser)
                mediaPlayer?.seekTo(progress.toLong())
            setPlayerState(progress.toLong())
        }

        override fun onStartTrackingTouch(seekBar: SeekBar) {
        }

        override fun onStopTrackingTouch(seekBar: SeekBar) {
        }
    })
}

internal fun setCover(song: Song, context: Context) {
    val settings = Settings(context)
    val primaryResolution = settings.getSetting("primaryCoverResolution")

    val coverFile =
        File(context.filesDir.toString() + "/" + song.albumId + ".png" + primaryResolution)

    if (File(coverFile.absolutePath).exists()) {
        val bitmap = BitmapFactory.decodeFile(coverFile.absolutePath)

        barCoverImageReference?.get()?.setImageBitmap(bitmap)

        fullscreenCoverReference?.get()?.setImageBitmap(bitmap)

        mediaPlayer?.duration?.let { setSongMetadata(song, coverFile.absolutePath, it) }
    }
}

internal fun setCover(
    title: String,
    version: String,
    artist: String,
    coverLocation: String
) {
    val coverFile =
        File(coverLocation)

    if (File(coverFile.absolutePath).exists()) {
        val bitmap = BitmapFactory.decodeFile(coverFile.absolutePath)

        barCoverImageReference?.get()?.setImageBitmap(bitmap)

        fullscreenCoverReference?.get()?.setImageBitmap(bitmap)

        mediaPlayer?.duration?.let {
            setSongMetadata(
                title,
                version,
                artist,
                coverFile.absolutePath,
                it
            )
        }
    }
}

internal fun setPlayButtonImage(context: Context) {
    if (mediaPlayer?.isPlaying == true) {
        playButtonReference?.get()?.setImageDrawable(
            ContextCompat.getDrawable(
                context,
                R.drawable.ic_pause_24dp
            )
        )

        fullscreenPlayButtonReference?.get()?.setImageDrawable(
            ContextCompat.getDrawable(
                context,
                R.drawable.ic_pause_24dp
            )
        )

    } else {
        playButtonReference?.get()?.setImageDrawable(
            ContextCompat.getDrawable(
                context,
                R.drawable.ic_play_arrow_24dp
            )
        )
        fullscreenPlayButtonReference?.get()?.setImageDrawable(
            ContextCompat.getDrawable(
                context,
                R.drawable.ic_play_arrow_24dp
            )
        )
    }
}

/**
 * SetPlayerState
 */
internal fun setPlayerState(progress: Long) {
    val stateBuilder = PlaybackStateCompat.Builder()

    val state: Int = if (mediaPlayer?.isPlaying == true) {
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
    mediaSession?.setPlaybackState(stateBuilder.build())
}

/**
 * Set song metadata
 */
internal fun setSongMetadata(song: Song, coverLocation: String, duration: Long) {
    val coverImage = BitmapFactory.decodeFile(coverLocation)

    val mediaMetadata = MediaMetadataCompat.Builder()
    mediaMetadata.putString(MediaMetadata.METADATA_KEY_ARTIST, song.artist)
    mediaMetadata.putString(MediaMetadata.METADATA_KEY_TITLE, "${song.title} ${song.version}")
    mediaMetadata.putLong(MediaMetadata.METADATA_KEY_DURATION, duration)
    mediaMetadata.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, coverImage)
    mediaMetadata.putBitmap(MediaMetadata.METADATA_KEY_ART, coverImage)
    mediaSession?.setMetadata(mediaMetadata.build())
}

internal fun setSongMetadata(
    title: String,
    version: String,
    artist: String,
    coverLocation: String,
    duration: Long
) {
    val coverImage = BitmapFactory.decodeFile(coverLocation)

    val mediaMetadata = MediaMetadataCompat.Builder()
    mediaMetadata.putString(MediaMetadata.METADATA_KEY_ARTIST, artist)
    mediaMetadata.putString(MediaMetadata.METADATA_KEY_TITLE, "$title $version")
    mediaMetadata.putLong(MediaMetadata.METADATA_KEY_DURATION, duration)
    mediaMetadata.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, coverImage)
    mediaMetadata.putBitmap(MediaMetadata.METADATA_KEY_ART, coverImage)
    mediaSession?.setMetadata(mediaMetadata.build())
}