package de.lucaspape.monstercat.music

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
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
import de.lucaspape.monstercat.music.MonstercatPlayer.Companion.contextReference
import de.lucaspape.monstercat.music.MonstercatPlayer.Companion.mediaPlayer
import de.lucaspape.monstercat.music.MonstercatPlayer.Companion.mediaSession
import java.lang.ref.WeakReference
import java.util.*

private var title = ""
private var version = ""
private var artist = ""

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
                    mediaPlayer?.seekTo(progress.toLong())
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
            if (mediaPlayer?.isPlaying == true) {
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
        if(fullscreenTitleReference != null){
            newTitleTextView?.get()?.text = fullscreenTitleReference?.get()?.text
        }else{
            val shownTitle = "$title $version"
            newTitleTextView?.get()?.text = shownTitle
        }

        field = newTitleTextView
    }

var fullscreenArtistReference: WeakReference<TextView>? = null
    set(newArtistTextView) {
        if(fullscreenArtistReference != null){
            newArtistTextView?.get()?.text = fullscreenArtistReference?.get()?.text
        }else{
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
                    mediaPlayer?.seekTo(progress.toLong())
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
            if (mediaPlayer?.isPlaying == true) {
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

internal fun setTitle(newTitle: String, newVersion: String, newArtist: String) {
    title = newTitle
    version = newVersion
    artist = newArtist

    val fullText = "$title $version - $artist"
    val shownTitle = "$title $version"

    textViewReference?.get()?.text = fullText

    fullscreenTitleReference?.get()?.text = shownTitle

    fullscreenArtistReference?.get()?.text = artist
}

internal fun hideTitle() {
    title = ""
    version = ""
    artist = ""

    val text = ""

    textViewReference?.get()?.text = text

    fullscreenTitleReference?.get()?.text = text

    fullscreenArtistReference?.get()?.text = text
}

private var currentSeekbarHandlerId = ""

@SuppressLint("ClickableViewAccessibility")
internal fun startSeekBarUpdate() {
    val seekBarUpdateHandler = Handler()

    val id = UUID.randomUUID().toString()
    currentSeekbarHandlerId = id

    val updateSeekBar = object : Runnable {
        override fun run() {
            mediaPlayer?.duration?.toInt()?.let { seekBarReference?.get()?.max = it }
            mediaPlayer?.currentPosition?.toInt()
                ?.let { seekBarReference?.get()?.progress = it }


            mediaPlayer?.duration?.toInt()?.let { fullscreenSeekBarReference?.get()?.max = it }
            mediaPlayer?.currentPosition?.toInt()
                ?.let { fullscreenSeekBarReference?.get()?.progress = it }


            mediaPlayer?.currentPosition?.let { setPlayerState(it) }

            if(currentSeekbarHandlerId == id){
                seekBarUpdateHandler.postDelayed(this, 50)
            }
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
            if (fromUser) {
                mediaPlayer?.seekTo(progress.toLong())
                setPlayerState(progress.toLong())
            }
        }

        override fun onStartTrackingTouch(seekBar: SeekBar) {
        }

        override fun onStopTrackingTouch(seekBar: SeekBar) {
        }
    })
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
        mediaPlayer?.duration?.let {
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