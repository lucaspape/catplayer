package de.lucaspape.monstercat.music

import android.content.Context
import android.graphics.Bitmap
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.PlaybackState
import android.os.Handler
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.download.downloadCoverIntoBitmap
import de.lucaspape.monstercat.download.downloadCoverIntoImageView
import java.lang.ref.WeakReference
import java.util.*

var textViewReference: WeakReference<TextView>? = null
    set(newTextView) {
        newTextView?.get()?.text = textViewReference?.get()?.text
        field = newTextView
    }

var seekBarReference: WeakReference<SeekBar>? = null
    set(newSeekBar) {
        seekBarReference?.get()?.progress?.let { newSeekBar?.get()?.progress = it }
        newSeekBar?.get()?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser)
                    exoPlayer?.seekTo(progress.toLong())
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
            }
        })

        field = newSeekBar
    }

var barCoverImageReference: WeakReference<ImageView>? = null
    set(newImageView) {
        newImageView?.get()?.setImageDrawable(barCoverImageReference?.get()?.drawable)

        field = newImageView
    }

var musicBarReference: WeakReference<androidx.appcompat.widget.Toolbar>? = null

var playButtonReference: WeakReference<ImageButton>? = null
    set(newPlayButton) {
        contextReference?.get()?.let { context ->
            if (exoPlayer?.isPlaying == true) {
                newPlayButton?.get()?.setImageDrawable(
                    ContextCompat.getDrawable(
                        context,
                        R.drawable.ic_pause_24dp
                    )
                )

            } else {
                newPlayButton?.get()?.setImageDrawable(
                    ContextCompat.getDrawable(
                        context,
                        R.drawable.ic_play_arrow_24dp
                    )
                )

            }
        }

        field = newPlayButton
    }

var fullscreenTitleReference: WeakReference<TextView>? = null
    set(newTitleTextView) {
        if (fullscreenTitleReference != null) {
            newTitleTextView?.get()?.text = fullscreenTitleReference?.get()?.text
        } else {
            val currentSong = getCurrentSong()

            val shownTitle = if (currentSong == null) {
                ""
            } else {
                "${currentSong.title} ${currentSong.version}"
            }

            newTitleTextView?.get()?.text = shownTitle
        }

        field = newTitleTextView
    }

var fullscreenArtistReference: WeakReference<TextView>? = null
    set(newArtistTextView) {
        if (fullscreenArtistReference != null) {
            newArtistTextView?.get()?.text = fullscreenArtistReference?.get()?.text
        } else {
            val currentSong = getCurrentSong()

            val artist = currentSong?.artist ?: ""

            newArtistTextView?.get()?.text = artist
        }

        field = newArtistTextView
    }

var fullscreenSeekBarReference: WeakReference<SeekBar>? = null
    set(newSeekBar) {
        seekBarReference?.get()?.progress?.let { newSeekBar?.get()?.progress = it }

        newSeekBar?.get()?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser)
                    exoPlayer?.seekTo(progress.toLong())
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
            }
        })

        field = newSeekBar
    }

var fullscreenCoverReference: WeakReference<ImageView>? = null
    set(newImageView) {
        newImageView?.get()?.setImageDrawable(barCoverImageReference?.get()?.drawable)

        field = newImageView
    }

var fullscreenPlayButtonReference: WeakReference<ImageButton>? = null
    set(newPlayButton) {
        contextReference?.get()?.let { context ->
            if (exoPlayer?.isPlaying == true) {
                newPlayButton?.get()?.setImageDrawable(
                    ContextCompat.getDrawable(
                        context,
                        R.drawable.ic_pause_24dp
                    )
                )

            } else {
                newPlayButton?.get()?.setImageDrawable(
                    ContextCompat.getDrawable(
                        context,
                        R.drawable.ic_play_arrow_24dp
                    )
                )

            }

            field = newPlayButton
        }
    }

internal fun setTitle(title: String, version: String, artist: String) {
    val fullText = "$title $version - $artist"
    val shownTitle = "$title $version"

    textViewReference?.get()?.text = fullText

    fullscreenTitleReference?.get()?.text = shownTitle

    fullscreenArtistReference?.get()?.text = artist
}

internal fun hideTitle() {
    val text = ""

    textViewReference?.get()?.text = text

    fullscreenTitleReference?.get()?.text = text

    fullscreenArtistReference?.get()?.text = text
}

private var currentProgressUpdaterId = ""

internal fun startSeekBarUpdate() {
    val seekBarUpdateHandler = Handler()

    contextReference?.get()?.let { context ->
        val id = UUID.randomUUID().toString()
        currentProgressUpdaterId = id

        val updateSeekBar = object : Runnable {
            override fun run() {

                exoPlayer?.duration?.toInt()?.let { duration ->
                    seekBarReference?.get()?.max = duration
                    fullscreenSeekBarReference?.get()?.max = duration
                }

                exoPlayer?.currentPosition?.toInt()?.let { currentPosition ->
                    seekBarReference?.get()?.progress = currentPosition
                    fullscreenSeekBarReference?.get()?.progress = currentPosition
                    setPlayerState(currentPosition)
                }

                exoPlayer?.duration?.let { duration ->
                    exoPlayer?.currentPosition?.let { currentPosition ->
                        val timeLeft = duration - currentPosition

                        if (timeLeft < crossfade && exoPlayer?.isPlaying == true) {
                            if (timeLeft >= 1) {
                                val nextVolume: Float = (crossfade.toFloat() - timeLeft) / crossfade
                                nextExoPlayer?.audioComponent?.volume = nextVolume

                                val currentVolume = 1 - nextVolume
                                exoPlayer?.audioComponent?.volume = currentVolume
                            }

                            nextExoPlayer?.playWhenReady = true
                        } else if (timeLeft < duration / 2 && exoPlayer?.isPlaying == true) {
                            prepareNextSong(context)
                        }
                    }
                }

                if (currentProgressUpdaterId == id) {
                    seekBarUpdateHandler.postDelayed(this, 50)
                }
            }
        }

        seekBarUpdateHandler.postDelayed(updateSeekBar, 0)
    }
}

internal fun setCover(
    context: Context,
    title: String,
    version: String,
    artist: String,
    albumId: String,
    callback: (bitmap: Bitmap) -> Unit
) {
    barCoverImageReference?.get()?.let {
        downloadCoverIntoImageView(context, it, albumId, false)
    }

    fullscreenCoverReference?.get()?.let {
        downloadCoverIntoImageView(context, it, albumId, false)
    }

    downloadCoverIntoBitmap(context, { bitmap ->
        exoPlayer?.duration?.let {
            setSongMetadata(
                title,
                version,
                artist,
                bitmap,
                it
            )
        }
        callback(bitmap)
    }, albumId, false)
}

internal fun setPlayButtonImage(context: Context) {
    if (exoPlayer?.isPlaying == true) {
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
internal fun setPlayerState(progress: Int) {
    val stateBuilder = PlaybackStateCompat.Builder()

    val state: Int = if (exoPlayer?.isPlaying == true) {
        PlaybackState.STATE_PLAYING
    } else {
        PlaybackState.STATE_PAUSED
    }

    stateBuilder.setState(state, progress.toLong(), 1.0f)
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
internal fun setSongMetadata(
    title: String,
    version: String,
    artist: String,
    cover: Bitmap,
    duration: Long
) {
    val mediaMetadata = MediaMetadataCompat.Builder()
    mediaMetadata.putString(MediaMetadata.METADATA_KEY_ARTIST, artist)
    mediaMetadata.putString(MediaMetadata.METADATA_KEY_TITLE, "$title $version")
    mediaMetadata.putLong(MediaMetadata.METADATA_KEY_DURATION, duration)
    mediaMetadata.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, cover)
    mediaMetadata.putBitmap(MediaMetadata.METADATA_KEY_ART, cover)
    mediaSession?.setMetadata(mediaMetadata.build())
}