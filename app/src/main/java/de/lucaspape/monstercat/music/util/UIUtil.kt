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
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import de.lucaspape.monstercat.database.helper.SongDatabaseHelper
import de.lucaspape.monstercat.download.downloadCoverIntoImageReceiver
import de.lucaspape.monstercat.download.ImageReceiverInterface
import de.lucaspape.monstercat.download.downloadArtistImageIntoImageReceiver
import de.lucaspape.monstercat.download.downloadImageUrlIntoImageReceiver
import de.lucaspape.monstercat.music.*
import de.lucaspape.monstercat.ui.pauseButtonDrawable
import de.lucaspape.monstercat.ui.playButtonDrawable
import java.lang.ref.WeakReference

var textViewReference: WeakReference<TextView>? = null
    set(newTextView) {
        val titleWithArtist = "$title - $artist"
        newTextView?.get()?.text = titleWithArtist

        field = newTextView
    }

var seekBarReference: WeakReference<SeekBar>? = null
    set(newSeekBar) {
        newSeekBar?.get()?.progress = currentPosition

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
        field = newPlayButton

        exoPlayer?.isPlaying?.let { isPlaying ->
            playing = isPlaying
        }
    }

var fullscreenTitleReference: WeakReference<TextView>? = null
    set(newTitleTextView) {
        newTitleTextView?.get()?.text = title

        field = newTitleTextView
    }

var fullscreenArtistReference: WeakReference<TextView>? = null
    set(newArtistTextView) {
        newArtistTextView?.get()?.text = artist

        field = newArtistTextView
    }

var fullscreenSeekBarReference: WeakReference<SeekBar>? = null
    set(newSeekBar) {
        newSeekBar?.get()?.progress = currentPosition

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
        field = newPlayButton

        exoPlayer?.isPlaying?.let { isPlaying ->
            playing = isPlaying
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
    }

var duration = 0
    set(newInt) {
        seekBarReference?.get()?.max = newInt
        fullscreenSeekBarReference?.get()?.max = newInt

        field = newInt

        setSongMetadata()
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

var playing = false
    set(newBoolean) {
        if (newBoolean) {
            playButtonReference?.get()?.setImageURI(pauseButtonDrawable.toUri())

            fullscreenPlayButtonReference?.get()?.setImageURI(pauseButtonDrawable.toUri())

        } else {
            playButtonReference?.get()?.setImageURI(playButtonDrawable.toUri())
            fullscreenPlayButtonReference?.get()?.setImageURI(playButtonDrawable.toUri())
        }


        field = newBoolean
    }

internal fun setCover(context: Context, songId: String, callback: (bitmap: Bitmap) -> Unit) {
    SongDatabaseHelper(context).getSong(context, songId)?.let { song ->
        setCover(context, song.albumId, song.artistId, callback)
    }
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
                if (getCurrentAlbumId(context) == id) {
                    coverBitmap = bitmap

                    bitmap?.let {
                        callback(it)
                    }
                }
            }
        }

        override fun setDrawable(id: String, drawable: Drawable?) {
            if (id == albumId) {
                if (getCurrentAlbumId(context) == id) {
                    coverDrawable = drawable

                    drawable?.toBitmap()?.let {
                        callback(it)
                    }
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

internal fun setCustomCover(
    context: Context,
    coverId: String,
    coverUrl: String,
    callback: (bitmap: Bitmap) -> Unit
) {
    downloadImageUrlIntoImageReceiver(context, object :
        ImageReceiverInterface {
        override fun setBitmap(id: String, bitmap: Bitmap?) {
            if (id == coverId) {
                coverBitmap = bitmap

                bitmap?.let {
                    callback(it)
                }
            }
        }

        override fun setDrawable(id: String, drawable: Drawable?) {
            if (id == coverId) {
                coverDrawable = drawable

                drawable?.toBitmap()?.let {
                    callback(it)
                }
            }
        }
    }, false, coverId, coverUrl)
}

/**
 * SetPlayerState
 */
internal fun setPlayerState(progress: Long) {
    val stateBuilder = PlaybackStateCompat.Builder()

    val state: Int = if (exoPlayer?.isPlaying == true) {
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
private fun setSongMetadata() {
    val mediaMetadata = MediaMetadataCompat.Builder()
    mediaMetadata.putString(MediaMetadata.METADATA_KEY_ARTIST, artist)
    mediaMetadata.putString(MediaMetadata.METADATA_KEY_TITLE, title)
    mediaMetadata.putLong(MediaMetadata.METADATA_KEY_DURATION, duration.toLong())
    mediaMetadata.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, coverBitmap)
    mediaMetadata.putBitmap(MediaMetadata.METADATA_KEY_ART, coverBitmap)
    mediaSession?.setMetadata(mediaMetadata.build())
}