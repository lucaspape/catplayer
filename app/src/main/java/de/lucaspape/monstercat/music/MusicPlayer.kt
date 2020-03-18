package de.lucaspape.monstercat.music

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.media.session.MediaSession
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

                getCurrentSong()?.let { songId ->
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
    if (loopSingle) {
        //loop single
        return try {
            playlist[playlistIndex]
        } catch (e: IndexOutOfBoundsException) {
            ""
        }
    } else {
        when {
            prioritySongQueue.size > 0 -> {
                val songId = prioritySongQueue[0]
                prioritySongQueue.removeAt(0)
                playlist.add(playlistIndex, songId)
                return songId
            }
            playlist.size > playlistIndex + 1 -> {
                val songId = playlist[playlistIndex + 1]
                playlistIndex++
                return songId
            }
            else -> {
                try {
                    //next song is not in already in playlist

                    //grab song from queue, if shuffle queueIndex is random
                    val queueIndex = if (shuffle && songQueue.size > 0) {
                        if (nextRandom == -1) {
                            nextRandom = Random.nextInt(0, songQueue.size)
                        }

                        nextRandom
                    } else {
                        0
                    }

                    val songId = songQueue[queueIndex]

                    //add to playlist, remove from queue
                    songQueue.removeAt(queueIndex)
                    playlist.add(songId)

                    playlistIndex = playlist.indexOf(songId)

                    if (songQueue.size > 0) {
                        nextRandom = Random.nextInt(0, songQueue.size)
                    }

                    return songId
                } catch (e: IndexOutOfBoundsException) {
                    //queue is empty, if loop add every song from playlist back to queue and start again
                    if (loop) {
                        clearQueue()

                        //add every song from playlist to queue
                        for (songId in playlist) {
                            songQueue.add(songId)
                        }

                        clearPlaylist()

                        return try {
                            val songId = songQueue[0]

                            //add to playlist, remove from queue
                            songQueue.removeAt(0)
                            playlist.add(songId)

                            playlistIndex = playlist.indexOf(songId)

                            songId
                        } catch (e: IndexOutOfBoundsException) {
                            ""
                        }
                    } else {
                        //queue finished, no loop
                        return ""
                    }
                }
            }
        }
    }
}

private fun previousSong(): String {
    return try {
        playlistIndex--
        playlist[playlistIndex]
    } catch (e: IndexOutOfBoundsException) {
        ""
    }
}

/**
 * Return next song without making changes to vars, only for prediction
 */
fun getNextSong(): String {
    if (loopSingle) {
        //loop single
        return try {
            playlist[playlistIndex]
        } catch (e: IndexOutOfBoundsException) {
            ""
        }
    } else {
        when {
            prioritySongQueue.size > 0 -> {
                return prioritySongQueue[0]
            }
            playlist.size > playlistIndex + 1 -> {
                return playlist[playlistIndex + 1]
            }
            else -> {
                try {
                    //next song is not in already in playlist

                    //grab song from queue, if shuffle queueIndex is random
                    val queueIndex = if (shuffle && songQueue.size > 0) {
                        if (nextRandom == -1) {
                            nextRandom = Random.nextInt(0, songQueue.size)
                        }

                        nextRandom
                    } else {
                        0
                    }

                    return songQueue[queueIndex]
                } catch (e: IndexOutOfBoundsException) {
                    //queue is empty, if loop add every song from playlist back to queue and start again
                    return if (loop) {
                        try {
                            playlist[0]
                        } catch (e: IndexOutOfBoundsException) {
                            ""
                        }
                    } else {
                        //queue finished, no loop
                        ""
                    }
                }
            }
        }
    }
}

fun getCurrentSong(): String? {
    contextReference?.get()?.let { context ->
        return try {
            return playlist[playlistIndex]
        } catch (e: IndexOutOfBoundsException) {
            null
        }
    }

    return null
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