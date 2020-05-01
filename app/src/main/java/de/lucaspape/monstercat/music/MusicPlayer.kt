package de.lucaspape.monstercat.music

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.media.session.MediaSession
import android.os.AsyncTask
import android.support.v4.media.session.MediaSessionCompat
import com.google.android.exoplayer2.SimpleExoPlayer
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.database.helper.SongDatabaseHelper
import de.lucaspape.monstercat.music.notification.startPlayerService
import de.lucaspape.monstercat.music.notification.stopPlayerService
import de.lucaspape.monstercat.music.save.PlayerSaveState
import de.lucaspape.monstercat.music.util.*
import de.lucaspape.monstercat.music.util.playSong
import de.lucaspape.monstercat.request.async.LoadRelatedTracksAsync
import de.lucaspape.monstercat.twitch.Stream
import de.lucaspape.util.Settings
import java.lang.ref.WeakReference
import kotlin.random.Random

//main exoPlayer
var exoPlayer: SimpleExoPlayer? = null
    internal set

//secondary exoPlayer -> allows crossFade and gapless playback
var preparedExoPlayer: SimpleExoPlayer? = null
    internal set

//queue for songs
var songQueue = ArrayList<String>()
    internal set

//priority queue for songs (will always be played back first)
var prioritySongQueue = ArrayList<String>()
    internal set

//playlist, contains playback history
internal var playlist = ArrayList<String>()
internal var playlistIndex = 0

//nextRandom needs to be prepared before next playing to allow crossfade
internal var nextRandom = -1

//playback control vars
var loop = false
var loopSingle = false
var shuffle = false
var crossfade = 12000
var volume: Float = 1.0f
    set(value) {
        exoPlayer?.audioComponent?.volume = value
        field = value
    }

var playRelatedSongsAfterPlaylistFinished = false

var mediaSession: MediaSessionCompat? = null
    internal set

private var sessionCreated = false

//updater which updates information about playing livestream (title, artist, coverImage)
internal var streamInfoUpdateAsync: StreamInfoUpdateAsync? = null

/**
 * Listener for audioFocusChange
 */
class AudioFocusChangeListener{
    companion object{
        @JvmStatic var audioFocusChangeListener:AudioManager.OnAudioFocusChangeListener? = null

        @JvmStatic fun getAudioFocusChangeListener(contextReference: WeakReference<Context>):AudioManager.OnAudioFocusChangeListener{
            if(audioFocusChangeListener == null){
                audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
                    contextReference.get()?.let { context ->
                        when (focusChange) {
                            AudioManager.AUDIOFOCUS_GAIN ->
                                resume(context)

                            AudioManager.AUDIOFOCUS_LOSS ->
                                pause(context)

                            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT ->
                                pause(context)
                        }
                    }
                }
            }

            return audioFocusChangeListener!!
        }
    }
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
            MediaSession(context, "de.lucaspape.monstercat.music")
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
        volume = it
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
internal fun next(context: Context) {
    playSong(
        context, nextSong(context),
        showNotification = true,
        requestAudioFocus = true,
        playWhenReady = true,
        progress = null
    )

    PlayerSaveState.saveMusicPlayerState(context)
}

/**
 * Play previous song
 */
internal fun previous(context: Context) {
    playSong(
        context, previousSong(),
        showNotification = true,
        requestAudioFocus = true,
        playWhenReady = true,
        progress = null
    )

    PlayerSaveState.saveMusicPlayerState(context)
}

/**
 * Pause playback
 */
internal fun pause(context: Context) {
    exoPlayer?.playWhenReady = false

    abandonAudioFocus(context)

    PlayerSaveState.saveMusicPlayerState(context)
}

/**
 * Resume playback
 */
internal fun resume(context: Context) {
    startPlayerService(context, currentSongId)

    //check if should resume livestream or song
    if (streamInfoUpdateAsync?.status == AsyncTask.Status.RUNNING) {
        playStream(
            context,
            Stream(
                context.getString(R.string.twitchClientID),
                context.getString(R.string.twitchChannel)
            )
        )
    } else {
        val intentFilter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)

        context.registerReceiver(
            NoisyReceiver(),
            intentFilter
        )

        playSong(
            context, currentSongId,
            showNotification = true,
            requestAudioFocus = true,
            playWhenReady = true,
            progress = currentPosition.toLong()
        )

        PlayerSaveState.saveMusicPlayerState(context)
    }
}

/**
 * Toggle playback (play/pause)
 */
internal fun toggleMusic(context: Context) {
    if (exoPlayer?.isPlaying == true) {
        pause(context)
    } else {
        resume(context)
    }
}

/**
 * Stop playback
 */
internal fun stop(context: Context) {
    if (exoPlayer?.isPlaying == true) {

        title = ""
        artist = ""

        exoPlayer?.stop()

        abandonAudioFocus(context)
        PlayerSaveState.saveMusicPlayerState(context)

    }

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
    } else if (playRelatedSongsAfterPlaylistFinished) {
        playRelatedSongs(context)
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

/**
 * Returns songId of currently playing song
 */
val currentSongId: String
    get() {
        return when {
            streamInfoUpdateAsync?.status == AsyncTask.Status.RUNNING -> {
                StreamInfoUpdateAsync.liveSongId
            }
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

/**
 * Fetch songs which are related to songs in playlist
 */
fun playRelatedSongs(context: Context) {
    Settings.getSettings(context)
        .getBoolean(context.getString(R.string.skipMonstercatSongsSetting))?.let {
            LoadRelatedTracksAsync(
                WeakReference(context), playlist, it,
                finishedCallback = { _, relatedIdArray ->
                    for (songId in relatedIdArray) {
                        songQueue.add(songId)
                    }

                    skipPreviousInPlaylist()
                    next(context)
                },
                errorCallback = {
                    //TODO handle error
                }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        }


}