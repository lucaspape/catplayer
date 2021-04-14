package de.lucaspape.monstercat.core.music.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.media.MediaMetadata
import android.media.session.PlaybackState
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.graphics.drawable.toBitmap
import com.squareup.picasso.Target
import de.lucaspape.monstercat.core.database.helper.SongDatabaseHelper
import de.lucaspape.monstercat.core.download.downloadCoverIntoImageReceiver
import de.lucaspape.monstercat.core.download.ImageReceiverInterface
import de.lucaspape.monstercat.core.download.downloadArtistImageIntoImageReceiver
import de.lucaspape.monstercat.core.music.*
import java.lang.RuntimeException

var titleChangedCallback = {}
var artistChangedCallback = {}
var currentPositionChangedCallback = {}
var durationChangedCallback = {}
var coverBitmapChangedCallback = {}
var coverDrawableChangedCallback = {}
var playingChangedCallback = {}
var artistBitmapChangedCallback = {}
var artistDrawableChangedCallback = {}
var setTagCallback = { _: Target -> }
var loadingRelatedChangedCallback = {}
var lyricsChangedCallback = {}

var title = ""
    set(newString) {
        field = newString

        titleChangedCallback()
    }

var artist = ""
    set(newString) {
        field = newString

        artistChangedCallback()
    }

var currentPosition:Long = 0
    set(newLong) {
        field = newLong

        currentPositionChangedCallback()
    }

var duration:Long = 0
    set(newLong) {
        if(newLong > 0){
            field = newLong
        }else{
            field = 0
        }
        
        setSongMetadata()

        durationChangedCallback()
    }

var coverBitmap: Bitmap? = null
    set(newBitmap) {
        field = newBitmap

        setSongMetadata()

        coverBitmapChangedCallback()
    }

var coverDrawable: Drawable? = null
    set(newDrawable) {
        field = newDrawable

        coverDrawableChangedCallback()
    }

var artistBitmap: Bitmap? = null
    set(newBitmap) {
        field = newBitmap
        artistBitmapChangedCallback()
    }

var artistDrawable: Drawable? = null
    set(newDrawable) {
        field = newDrawable
        artistDrawableChangedCallback()
    }

var visiblePlaying = false
    set(newBoolean) {
        field = newBoolean
        playingChangedCallback()
    }

var loadingRelatedSongs = false
    set(newBoolean) {
        field = newBoolean
        loadingRelatedChangedCallback()
    }

var currentLyricsIndex = 0
    set(value) {
        field = value

        lyricsChangedCallback()
    }

var lyricTimeCodesArray = emptyArray<Int>()
var lyricTextArray = emptyArray<String>()
    set(value) {
        field = value

        lyricsChangedCallback()
    }

fun setCover(context: Context, songId: String, callback: (bitmap: Bitmap) -> Unit) {
    SongDatabaseHelper(context).getSong(songId)?.let { song ->
        setCover(context, song.albumId, song.artistId, callback)
    }
}

fun setCover(
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

        override fun setTag(target: Target) {
            setTagCallback(target)
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

        override fun setTag(target: Target) {
        }
    }, artistId)
}

/**
 * SetPlayerState
 */
fun setPlayerState(progress: Long) {
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
    try {
        val mediaMetadata = MediaMetadataCompat.Builder()
        mediaMetadata.putString(MediaMetadata.METADATA_KEY_ARTIST, artist)
        mediaMetadata.putString(MediaMetadata.METADATA_KEY_TITLE, title)
        mediaMetadata.putLong(MediaMetadata.METADATA_KEY_DURATION, duration)

        coverBitmap?.let {
            mediaMetadata.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, it)
            mediaMetadata.putBitmap(MediaMetadata.METADATA_KEY_ART, it)
        }

        mediaSession?.setMetadata(mediaMetadata.build())
    } catch (e: RuntimeException) {
        println("Failed to set song metadata.")
    }
}