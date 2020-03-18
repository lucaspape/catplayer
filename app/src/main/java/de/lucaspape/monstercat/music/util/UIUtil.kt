package de.lucaspape.monstercat.music.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.media.MediaMetadata
import android.media.session.PlaybackState
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.download.downloadCoverIntoImageReceiver
import de.lucaspape.monstercat.download.ImageReceiverInterface
import de.lucaspape.monstercat.download.downloadArtistImageIntoImageReceiver
import de.lucaspape.monstercat.music.*
import de.lucaspape.monstercat.music.contextReference
import java.lang.ref.WeakReference

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
        if (fullscreenTitleReference?.get() != null) {
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
        if (fullscreenArtistReference?.get() != null) {
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

var fullscreenArtistImageViewReference: WeakReference<ImageView>? = null
    set(newImageView) {
        newImageView?.get()?.setImageDrawable(fullscreenArtistImageViewReference?.get()?.drawable)

        field = newImageView
    }

var title = ""
    set(newString) {
        val titleWithArtist = "$newString - $artist"

        textViewReference?.get()?.text = titleWithArtist
        fullscreenTitleReference?.get()?.text = newString
        field = newString
    }

var artist = ""
    set(newString) {
        val titleWithArtist = "$title - $newString"
        textViewReference?.get()?.text = titleWithArtist

        fullscreenArtistReference?.get()?.text = newString

        field = newString
    }

var currentPosition = 0
    set(newInt) {
        seekBarReference?.get()?.progress = newInt
        fullscreenSeekBarReference?.get()?.progress = newInt

        field = newInt

        setPlayerState()
    }

var duration = 0
    set(newInt) {
        seekBarReference?.get()?.max = newInt
        fullscreenSeekBarReference?.get()?.max = newInt

        field = newInt
    }

var coverBitmap: Bitmap? = null
    set(newBitmap) {
        barCoverImageReference?.get()?.setImageBitmap(newBitmap)
        fullscreenCoverReference?.get()?.setImageBitmap(newBitmap)

        field = newBitmap

        setSongMetadata()
    }

var coverDrawable: Drawable? = null
    set(newDrawable) {
        barCoverImageReference?.get()?.setImageDrawable(newDrawable)
        fullscreenCoverReference?.get()?.setImageDrawable(newDrawable)

        field = newDrawable
    }

var artistBitmap: Bitmap? = null
    set(newBitmap) {
        fullscreenArtistImageViewReference?.get()?.setImageBitmap(newBitmap)

        field = newBitmap
    }

var artistDrawable: Drawable? = null
    set(newDrawable) {
        fullscreenArtistImageViewReference?.get()?.setImageDrawable(newDrawable)

        field = newDrawable
    }

internal fun setCover(
    context: Context,
    albumId: String,
    artistId: String,
    callback: (bitmap: Bitmap) -> Unit
) {
    downloadCoverIntoImageReceiver(context, object :
        ImageReceiverInterface {
        override fun setBitmap(id: String, bitmap: Bitmap?) {
            if (id == albumId) {
                coverBitmap = bitmap

                bitmap?.let {
                    callback(it)
                }
            }
        }

        override fun setDrawable(id: String, drawable: Drawable?) {
            if (id == albumId) {
                coverDrawable = drawable

                drawable?.toBitmap()?.let {
                    callback(it)
                }
            }
        }
    }, albumId, false)

    downloadArtistImageIntoImageReceiver(context, object : ImageReceiverInterface {
        override fun setBitmap(id: String, bitmap: Bitmap?) {
            if (id == artistId) {
                artistBitmap = bitmap
            }
        }

        override fun setDrawable(id: String, drawable: Drawable?) {
            if (id == artistId) {
                artistDrawable = drawable
            }
        }
    }, artistId)
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
private fun setPlayerState() {
    val stateBuilder = PlaybackStateCompat.Builder()

    val state: Int = if (exoPlayer?.isPlaying == true) {
        PlaybackState.STATE_PLAYING
    } else {
        PlaybackState.STATE_PAUSED
    }

    stateBuilder.setState(state, currentPosition.toLong(), 1.0f)
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
private fun setSongMetadata() {
    val mediaMetadata = MediaMetadataCompat.Builder()
    mediaMetadata.putString(MediaMetadata.METADATA_KEY_ARTIST, artist)
    mediaMetadata.putString(MediaMetadata.METADATA_KEY_TITLE, title)
    mediaMetadata.putLong(MediaMetadata.METADATA_KEY_DURATION, duration.toLong())
    mediaMetadata.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, coverBitmap)
    mediaMetadata.putBitmap(MediaMetadata.METADATA_KEY_ART, coverBitmap)
    mediaSession?.setMetadata(mediaMetadata.build())
}