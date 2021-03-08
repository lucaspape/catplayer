package de.lucaspape.monstercat.core.music

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.media.session.MediaSession
import android.support.v4.media.session.MediaSessionCompat
import com.google.android.exoplayer2.SimpleExoPlayer
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.core.database.helper.SongDatabaseHelper
import de.lucaspape.monstercat.core.music.notification.startPlayerService
import de.lucaspape.monstercat.core.music.notification.stopPlayerService
import de.lucaspape.monstercat.core.music.save.PlayerSaveState
import de.lucaspape.monstercat.core.music.util.*
import de.lucaspape.monstercat.core.util.Settings
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.log
import kotlin.random.Random

//main exoPlayer
var exoPlayer: SimpleExoPlayer? = null
    set(value) {
        currentListenerId = ""
        field?.playWhenReady = false
        field?.release()
        field?.stop()

        field = value
    }

//secondary exoPlayer -> allows crossFade and gapless playback
var preparedExoPlayer: SimpleExoPlayer? = null
    set(value) {
        preparedExoPlayer?.playWhenReady = false

        field = value
    }

//queue for songs
var songQueue = ArrayList<String>()

//priority queue for songs (will always be played back first)
var prioritySongQueue = LinkedList<String>()

var relatedSongQueue = ArrayList<String>()

private var loadedRelatedHash = 0

//playlist, contains playback history
var playlist = ArrayList<String>()
var playlistIndex = 0

var history = ArrayList<String>()

//nextRandom needs to be prepared before next playing to allow crossfade
var nextRandom = -1
var nextRelatedRandom = -1

//playback control vars
var loop = false
var loopSingle = false
var shuffle = false
var crossfade = 12000
var volume: Float = 1.0f
    set(value) {
        if (value > 1) {
            exoPlayer?.audioComponent?.volume = 1F
            field = 1F
        } else {
            exoPlayer?.audioComponent?.volume = value
            field = value
        }
    }

var playRelatedSongsAfterPlaylistFinished = false

var mediaSession: MediaSessionCompat? = null

private var sessionCreated = false

var connectSid = ""
var cid = ""

var retrieveRelatedSongs: (context: Context, callback: (relatedSongs: ArrayList<String>) -> Unit, errorCallback: () -> Unit) -> Unit =
    { _, _, _ -> }

var displayInfo: (context: Context, msg: String) -> Unit = { _, _ -> }

var openMainActivityIntent = Intent()

fun setupMusicPlayer(
    sRetrieveRelatedSongs: (context: Context, callback: (relatedSongs: ArrayList<String>) -> Unit, errorCallback: () -> Unit) -> Unit,
    sDisplayInfo: (context: Context, msg: String) -> Unit,
    sOpenMainActivityIntent: Intent
) {
    retrieveRelatedSongs = sRetrieveRelatedSongs
    displayInfo = sDisplayInfo
    openMainActivityIntent = sOpenMainActivityIntent
}

/**
 * Listener for headphones disconnected
 */
class NoisyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
            context?.let {
                pause(context)
            }
        }
    }
}

/**
 * Create mediaSession and listen for callbacks (pause, play buttons on headphones etc.)
 */
fun createMediaSession(context: Context) {
    if (!sessionCreated || mediaSession == null) {
        PlayerSaveState.restoreMusicPlayerState(context, false)

        mediaSession = MediaSessionCompat.fromMediaSession(
            context,
            MediaSession(context, "de.lucaspape.monstercat.core.music")
        )

        mediaSession?.setCallback(MediaSessionCallback(context))

        mediaSession?.isActive = true

        sessionCreated = true
    }
}

fun applyPlayerSettings(context: Context) {
    val settings = Settings.getSettings(context)

    settings.getInt(context.getString(R.string.crossfadeTimeSetting))?.let {
        crossfade = it
    }

    settings.getFloat(context.getString(R.string.volumeSetting))?.let {
        volume = 1 - log(100 - (it * 100), 100F)
    }

    settings.getBoolean(context.getString(R.string.playRelatedSetting))?.let {
        playRelatedSongsAfterPlaylistFinished = it
    }
}

/**
 * General control
 */

/**
 * Play next song
 */
fun next(context: Context) {
    playSong(
        context, nextSong(context),
        showNotification = true,
        playWhenReady = true,
        progress = null
    )

    PlayerSaveState.saveMusicPlayerState(context)
}

/**
 * Play previous song
 */
fun previous(context: Context) {
    playSong(
        context, previousSong(),
        showNotification = true,
        playWhenReady = true,
        progress = null
    )

    PlayerSaveState.saveMusicPlayerState(context)
}

/**
 * Pause playback
 */
fun pause(context: Context) {
    exoPlayer?.playWhenReady = false
    visiblePlaying = false

    PlayerSaveState.saveMusicPlayerState(context)
}

/**
 * Resume playback
 */
fun resume(context: Context) {
    startPlayerService(context, currentSongId)

    val intentFilter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)

    context.applicationContext.registerReceiver(
        NoisyReceiver(),
        intentFilter
    )

    playSong(
        context, currentSongId,
        showNotification = true,
        playWhenReady = true,
        progress = currentPosition.toLong()
    )

    PlayerSaveState.saveMusicPlayerState(context)
}

/**
 * Toggle playback (play/pause)
 */
fun toggleMusic(context: Context) {
    if (visiblePlaying) {
        pause(context)
    } else {
        resume(context)
    }
}

/**
 * Stop playback
 */
fun stop(context: Context) {
    if (exoPlayer?.isPlaying == true) {

        title = ""
        artist = ""

        exoPlayer?.stop()

        PlayerSaveState.saveMusicPlayerState(context)

    }

    visiblePlaying = false
    stopPlayerService(context)
}

/**
 * Returns next song and makes changes to vars
 */
private fun nextSong(context: Context): String {
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

        try {
            playlist.add(playlistIndex + 1, songId)
            playlistIndex++
        } catch (e: IndexOutOfBoundsException) {
            playlist.add(songId)
            skipPreviousInPlaylist()
        }

        //clear related because playlist change
        clearRelatedSongs()

        return songId
    } else if (playlist.size > playlistIndex + 1) {
        //get from playlist
        val songId = playlist[playlistIndex + 1]
        playlistIndex++

        //clear related because playlist change
        clearRelatedSongs()

        return songId
    } else if (songQueue.size > 0) {
        //get from queue

        val queueIndex = if (shuffle && songQueue.size > 0) {
            if (nextRandom == -1) {
                nextRandom = Random.nextInt(songQueue.size)
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
            nextRandom = Random.nextInt(songQueue.size)
        }

        //clear related because playlist change
        clearRelatedSongs()

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

        //clear related because playlist change
        clearRelatedSongs()

        return songId

        //if the priority queue changed lets re-fetch the related songs and let the user adapt them
    } else if (playRelatedSongsAfterPlaylistFinished && relatedSongQueue.size > 0) {
        //get from relatedQueue
        val queueIndex = if (shuffle && relatedSongQueue.size > 0) {
            if (nextRelatedRandom == -1) {
                nextRelatedRandom = Random.nextInt(relatedSongQueue.size)
            }

            nextRelatedRandom
        } else {
            0
        }

        val songId = relatedSongQueue[queueIndex]
        relatedSongQueue.removeAt(queueIndex)
        playlist.add(songId)

        skipPreviousInPlaylist()

        //prepare nextRandom
        if (relatedSongQueue.size > 0) {
            nextRelatedRandom = Random.nextInt(relatedSongQueue.size)
        }

        return songId

    } else if (playRelatedSongsAfterPlaylistFinished) {
        loadRelatedSongs(context, playAfter = true)

        return ""
    } else {
        return ""
    }
}

/**
 * Get previous song (and sets vars)
 */
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
val nextSongId: String
    get() {
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
                    nextRandom = Random.nextInt(songQueue.size)
                }

                nextRandom
            } else {
                0
            }

            return songQueue[queueIndex]
        } else if (loop && playlist.size > 0) {
            //if loop go back to beginning of playlist
            return playlist[0]
        } else if (playRelatedSongsAfterPlaylistFinished && relatedSongQueue.size > 0) {
            //get from queue

            val queueIndex = if (shuffle && relatedSongQueue.size > 0) {
                if (nextRelatedRandom == -1) {
                    nextRelatedRandom = Random.nextInt(relatedSongQueue.size)
                }

                nextRelatedRandom
            } else {
                0
            }

            return relatedSongQueue[queueIndex]
        } else {
            return ""
        }
    }

/**
 * Returns songId of currently playing song
 */
val currentSongId: String
    get() {
        return when {
            playlist.size > playlistIndex && playlistIndex >= 0 -> {
                playlist[playlistIndex]
            }
            else -> {
                ""
            }
        }
    }

/**
 * Get albumId of current song (needed for album cover)
 */
fun getCurrentAlbumId(context: Context): String {
    SongDatabaseHelper(context).getSong(context, currentSongId)?.let { song ->
        return song.albumId
    }

    return ""
}

/**
 * Empty queue
 */
fun clearQueue() {
    nextRandom = -1
    songQueue = ArrayList()
}

/**
 * Empty playlist
 */
fun clearPlaylist() {
    playlistIndex = 0
    playlist = ArrayList()
}

/**
 * Set index to most recent song added to playlist
 */
fun skipPreviousInPlaylist() {
    playlistIndex = playlist.size - 1
}

fun clearRelatedSongs() {
    relatedSongQueue = ArrayList()
    nextRelatedRandom = -1
    loadedRelatedHash = -1
}

/**
 * Fetch songs which are related to songs in playlist
 */
fun loadRelatedSongs(context: Context, playAfter: Boolean) {
    if (!loadingRelatedSongs && loadedRelatedHash != playlist.hashCode()) {
        loadingRelatedSongs = true

        retrieveRelatedSongs(context, {
            clearRelatedSongs()

            relatedSongQueue = it
            loadedRelatedHash = playlist.hashCode()

            loadingRelatedSongs = false

            if (playAfter) {
                skipPreviousInPlaylist()
                next(context)
            }
        }, {
            clearRelatedSongs()
            loadingRelatedSongs = false
        })
    }
}

fun removeFromPriorityQueue(index:Int){
    prioritySongQueue.removeAt(index)
}

fun removeFromQueue(index:Int){
    songQueue.removeAt(index)
    nextRandom = -1
}

fun removeFromRelatedQueue(index:Int){
    relatedSongQueue.removeAt(index)
    nextRelatedRandom = -1
}