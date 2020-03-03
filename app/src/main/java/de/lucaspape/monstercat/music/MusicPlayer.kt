package de.lucaspape.monstercat.music

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.media.session.MediaSession
import android.support.v4.media.session.MediaSessionCompat
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.SimpleExoPlayer
import de.lucaspape.monstercat.database.Song
import de.lucaspape.monstercat.database.helper.SongDatabaseHelper
import de.lucaspape.monstercat.music.notification.startPlayerService
import de.lucaspape.monstercat.music.notification.stopPlayerService
import de.lucaspape.monstercat.util.abandonAudioFocus
import de.lucaspape.monstercat.util.requestAudioFocus
import java.lang.ref.WeakReference
import kotlin.random.Random

internal var contextReference: WeakReference<Context>? = null

var exoPlayer: ExoPlayer? = null
    internal set

var nextExoPlayer: ExoPlayer? = null
    internal set

var songQueue = ArrayList<String>()
    internal set

internal var playlist = ArrayList<String>()
internal var playlistIndex = 0

var loop = false
var loopSingle = false
var shuffle = false
var crossfade = 12000

var mediaSession: MediaSessionCompat? = null
    internal set

private var sessionCreated = false

val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener {
    pause()
}

class NoisyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
            pause()
        }
    }
}

/**
 * Create mediaSession and listen for callbacks (pause, play buttons on headphones etc.)
 */
fun createMediaSession() {
    contextReference?.get()?.let {
        if (!sessionCreated) {
            exoPlayer = SimpleExoPlayer.Builder(it).build()


            mediaSession = MediaSessionCompat.fromMediaSession(
                it,
                MediaSession(it, "de.lucaspape.monstercat.music")
            )

            mediaSession?.setCallback(MediaSessionCallback())

            mediaSession?.isActive = true

            sessionCreated = true
        }
    }
}

/**
 * General control (public)
 */

fun next() {
    playNext()
}

fun previous() {
    playPrevious()
}

fun pause() {
    contextReference?.get()?.let { context ->
        exoPlayer?.playWhenReady = false

        setPlayButtonImage(context)

        abandonAudioFocus(context)
    }
}

private fun resume() {
    contextReference?.get()?.let { context ->
        if (exoPlayer?.isPlaying == false) {
            if (requestAudioFocus(context) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                exoPlayer?.playWhenReady = true

                val intentFilter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
                context.registerReceiver(
                    NoisyReceiver(),
                    intentFilter
                )
            }

            setPlayButtonImage(context)

            startPlayerService()
        }
    }
}

fun toggleMusic() {
    if (exoPlayer?.isPlaying == true) {
        pause()
    } else {
        resume()
    }
}

internal fun stop() {
    if (exoPlayer?.isPlaying == true) {

        hideTitle()

        setPlayButtonImage(contextReference?.get()!!)

        exoPlayer?.stop()

        contextReference?.get()?.let { context ->
            abandonAudioFocus(context)
        }
    }

    stopPlayerService()
}

internal fun nextSong(): String {
    if (loopSingle) {
        return try {
            playlist[playlistIndex]
        } catch (e: IndexOutOfBoundsException) {
            ""
        }
    } else {
        try {
            playlistIndex++
            return playlist[playlistIndex]
        } catch (e: IndexOutOfBoundsException) {
            try {
                //grab song from queue
                val queueIndex = if (shuffle) {
                    Random.nextInt(0, songQueue.size + 1)
                } else {
                    0
                }

                val nextSongId: String = songQueue[queueIndex]
                songQueue.removeAt(queueIndex)

                playlist.add(nextSongId)
                playlistIndex = playlist.indexOf(nextSongId)

                return nextSongId
            } catch (e: IndexOutOfBoundsException) {
                return if (loop) {
                    playlistIndex = 0
                    playlist[playlistIndex]
                } else {
                    ""
                }
            }
        }
    }

}

internal fun previousSong(): String {
    return try{
        playlistIndex--
        playlist[playlistIndex]
    }catch(e: IndexOutOfBoundsException){
        ""
    }
}

fun getCurrentSong(): Song? {
    contextReference?.get()?.let { context ->
        return try{
            val currentSongId = playlist[playlistIndex]
            val songDatabaseHelper = SongDatabaseHelper(context)

            songDatabaseHelper.getSong(context, currentSongId)
        }catch(e: IndexOutOfBoundsException){
            null
        }
    }

    return null
}

fun clearQueue() {
    songQueue = ArrayList()
}