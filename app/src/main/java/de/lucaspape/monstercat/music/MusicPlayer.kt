package de.lucaspape.monstercat.music

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.media.session.MediaSession
import android.os.AsyncTask
import android.support.v4.media.session.MediaSessionCompat
import com.google.android.exoplayer2.ExoPlayer
import de.lucaspape.monstercat.database.helper.SongDatabaseHelper
import de.lucaspape.monstercat.music.notification.startPlayerService
import de.lucaspape.monstercat.music.notification.stopPlayerService
import de.lucaspape.monstercat.music.notification.updateNotification
import de.lucaspape.monstercat.music.util.*
import de.lucaspape.monstercat.music.util.playSong
import java.lang.ref.WeakReference
import kotlin.random.Random

internal var contextReference: WeakReference<Context>? = null

var exoPlayer: ExoPlayer? = null
    internal set

var nextExoPlayer: ExoPlayer? = null
    internal set

var songQueue = ArrayList<String>()
    internal set

var prioritySongQueue = ArrayList<String>()
    internal set

internal var playlist = ArrayList<String>()
internal var playlistIndex = 0
internal var nextRandom = -1

var loop = false
var loopSingle = false
var shuffle = false
var crossfade = 12000

var listenerEnabled = false

var mediaSession: MediaSessionCompat? = null
    internal set

private var sessionCreated = false

var streamInfoUpdateAsync: StreamInfoUpdateAsync? = null

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
    contextReference?.get()?.let { context ->
        playSong(
            context, nextSong(),
            showNotification = true,
            requestAudioFocus = true,
            playWhenReady = true,
            progress = null
        )
    }
}

fun previous() {
    contextReference?.get()?.let { context ->
        playSong(
            context, previousSong(),
            showNotification = true,
            requestAudioFocus = true,
            playWhenReady = true,
            progress = null
        )
    }
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
                listenerEnabled = true

                exoPlayer?.playWhenReady = true

                val intentFilter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
                context.registerReceiver(
                    NoisyReceiver(),
                    intentFilter
                )

                setPlayButtonImage(context)

                val songId = getCurrentSong()

                startPlayerService(songId)

                setCover(
                    context,
                    songId
                ) {
                    setPlayButtonImage(
                        context
                    )
                    runSeekBarUpdate(
                        context,
                        true
                    )

                    updateNotification(context, songId, it)
                }

                SongDatabaseHelper(context).getSong(context, songId)?.let { song ->
                    //UI stuff
                    title = "${song.title} ${song.version}"
                    artist = song.artist
                }
            }
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

        title = ""
        artist = ""

        setPlayButtonImage(contextReference?.get()!!)

        exoPlayer?.stop()

        listenerEnabled = false

        contextReference?.get()?.let { context ->
            abandonAudioFocus(context)
        }
    }

    stopPlayerService()
}

/**
 * Returns next song and makes changes to vars
 */
private fun nextSong(): String {
    if (loopSingle && playlist.size >= playlistIndex) {
        //loop single
        return try {
            playlist[playlistIndex]
        } catch (e: java.lang.IndexOutOfBoundsException) {
            ""
        }
    } else if (prioritySongQueue.size > 0) {
        //get from priority list
        val songId = prioritySongQueue[0]
        prioritySongQueue.removeAt(0)
        playlist.add(songId)
        return songId
    } else if (playlist.size > playlistIndex + 1) {
        //get from playlist
        val songId = playlist[playlistIndex + 1]
        playlistIndex++
        return songId
    } else if (songQueue.size > 0) {
        //get from queue

        val queueIndex = if (shuffle && songQueue.size > 0) {
            if (nextRandom == -1) {
                nextRandom = Random.nextInt(0, songQueue.size)
            }

            nextRandom
        } else {
            0
        }

        val songId = songQueue[queueIndex]
        songQueue.removeAt(queueIndex)
        playlist.add(songId)

        skipPreviousInPlaylist()

        //prepare nextRandom
        if (songQueue.size > 0) {
            nextRandom = Random.nextInt(0, songQueue.size)
        }

        return songId
    } else if (loop && playlist.size > 0) {
        //if loop go back to beginning of playlist

        clearQueue()

        for (songId in playlist) {
            songQueue.add(songId)
        }

        clearPlaylist()

        val songId = songQueue[0]
        songQueue.removeAt(0)
        playlist.add(songId)

        playlistIndex = 0

        return songId
    } else {
        return ""
    }

}

private fun previousSong(): String {
    return try {
        val songId = playlist[playlistIndex - 1]
        playlistIndex--
        songId
    } catch (e: IndexOutOfBoundsException) {
        ""
    }
}

/**
 * Return next song without making changes to vars, only for prediction
 */
fun getNextSong(): String {
    if (loopSingle && playlist.size >= playlistIndex) {
        //loop single
        return try {
            playlist[playlistIndex]
        } catch (e: java.lang.IndexOutOfBoundsException) {
            ""
        }
    } else if (prioritySongQueue.size > 0) {
        //get from priority list
        return prioritySongQueue[0]
    } else if (playlist.size > playlistIndex + 1) {
        //get from playlist
        return playlist[playlistIndex + 1]
    } else if (songQueue.size > 0) {
        //get from queue

        val queueIndex = if (shuffle && songQueue.size > 0) {
            if (nextRandom == -1) {
                nextRandom = Random.nextInt(0, songQueue.size)
            }

            nextRandom
        } else {
            0
        }

        return songQueue[queueIndex]
    } else if (loop && playlist.size > 0) {
        //if loop go back to beginning of playlist
        return playlist[0]
    } else {
        return ""
    }
}

fun getCurrentSong(): String {
    return when {
        streamInfoUpdateAsync?.status == AsyncTask.Status.RUNNING -> {
            StreamInfoUpdateAsync.liveSongId
        }
        playlist.size > playlistIndex -> {
            playlist[playlistIndex]
        }
        else -> {
            ""
        }
    }
}

fun clearQueue() {
    nextRandom = -1
    songQueue = ArrayList()
}

fun clearPlaylist() {
    playlistIndex = 0
    playlist = ArrayList()
}

/**
 * Dont play from playlist, play from queue
 */
fun skipPreviousInPlaylist() {
    playlistIndex = playlist.size
}