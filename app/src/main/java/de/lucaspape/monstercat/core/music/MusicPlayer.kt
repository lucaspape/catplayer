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
import de.lucaspape.monstercat.core.database.helper.FilterDatabaseHelper
import de.lucaspape.monstercat.core.database.helper.SongDatabaseHelper
import de.lucaspape.monstercat.core.database.objects.Song
import de.lucaspape.monstercat.core.music.notification.startPlayerService
import de.lucaspape.monstercat.core.music.notification.stopPlayerService
import de.lucaspape.monstercat.core.music.save.PlayerSaveState
import de.lucaspape.monstercat.core.music.util.*
import de.lucaspape.monstercat.core.util.Settings
import de.lucaspape.monstercat.request.async.loadLyrics
import de.lucaspape.monstercat.ui.activities.login
import de.lucaspape.monstercat.util.Listener
import de.lucaspape.monstercat.util.loggedIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.math.log
import kotlin.random.Random

val scope = CoroutineScope(Dispatchers.Default)

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
    set(value) {
        field = value
        playlistChangedCallback()
    }

//songs that are played longer than 30 seconds are saved in here. Used for related songs
var history = ArrayList<String>()

var playlistChangedCallback: () -> Unit = {}

//nextRandom needs to be prepared before next playing to allow crossfade
var nextRandom = -1
var nextRelatedRandom = -1

var randomSeed = 0
var relatedRandomSeed = 0

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

private var filters = HashMap<String, ArrayList<String>>()

fun setupMusicPlayer(
    context: Context,
    sRetrieveRelatedSongs: (context: Context, callback: (relatedSongs: ArrayList<String>) -> Unit, errorCallback: () -> Unit) -> Unit,
    sDisplayInfo: (context: Context, msg: String) -> Unit,
    sOpenMainActivityIntent: Intent
) {
    retrieveRelatedSongs = sRetrieveRelatedSongs
    displayInfo = sDisplayInfo
    openMainActivityIntent = sOpenMainActivityIntent

    setupPlayerListeners(context)
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
fun createMediaSession(context: Context, force: Boolean) {
    if (!sessionCreated || mediaSession == null) {
        if (!loggedIn && !force) {
            login(context)
        } else {
            PlayerSaveState.restore(context, false)

            mediaSession = MediaSessionCompat.fromMediaSession(
                context,
                MediaSession(context, "de.lucaspape.monstercat.core.music")
            )

            mediaSession?.setCallback(MediaSessionCallback(context))

            mediaSession?.isActive = true

            sessionCreated = true
        }
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

    applyFilterSettings(context)
}

fun applyFilterSettings(context: Context) {
    filters = HashMap()

    val filterDatabaseHelper = FilterDatabaseHelper(context)

    filterDatabaseHelper.getAllFilters().let {
        it.forEach { filter ->
            when (filter.filterType) {
                "special" -> addSpecialFilter(filter.filter)
                "artist" -> addArtistToFilters(filter.filter)
                "title" -> addTitleFilter(filter.filter)
            }
        }
    }
}

fun setupPlayerListeners(context: Context) {
    val positionListener = Listener({
        exoPlayer?.duration?.let {
            duration = it
        }

        exoPlayer?.currentPosition?.let {
            currentPosition = it
            setPlayerState(it)
        }
    }, false)

    val lyricsListener = Listener({
        if (loadedLyricsId == currentSongId) {
            //calculate current timecode

            try {
                var timeCodeIndex = 0

                for ((index, value) in lyricTimeCodesArray.withIndex()) {
                    if (value * 1000 < currentPosition) {
                        timeCodeIndex = index
                    }
                }

                if (currentLyricsIndex != timeCodeIndex) {
                    currentLyricsIndex = timeCodeIndex
                }

            } catch (e: java.lang.IndexOutOfBoundsException) {
                currentLyricsIndex = 0
            }
        } else {
            //load lyrics

            if (!loadingLyrics) {
                loadingLyrics = true

                scope.launch {
                    loadLyrics(context, currentSongId, {
                        loadedLyricsId = it

                        loadingLyrics = false
                    }, {
                        loadedLyricsId = it

                        currentLyricsIndex = 0
                        lyricTextArray = emptyArray()
                        lyricTimeCodesArray = emptyArray()

                        loadingLyrics = false
                    })
                }
            }

        }
    }, false)

    val crossFadeListener = Listener({
        val timeLeft = duration - currentPosition

        if (timeLeft < duration / 2 && exoPlayer?.isPlaying == true) {
            if (nextSongId != "") {
                prepareSong(context, nextSongId, {}, {})
            } else if (playRelatedSongsAfterPlaylistFinished) {
                loadRelatedSongs(context, playAfter = false)
            }
        }

        if (timeLeft < crossfade && exoPlayer?.isPlaying == true && nextSongId == preparedExoPlayerSongId) {
            if (timeLeft >= 1) {
                val crossVolume = 1 - log(
                    100 - ((crossfade.toFloat() - timeLeft) / crossfade * 100),
                    100.toFloat()
                )

                val higherVolume = crossVolume * volume
                val lowerVolume = volume - higherVolume

                if (higherVolume > 0.toFloat() && higherVolume.isFinite()) {
                    preparedExoPlayer?.audioComponent?.volume = higherVolume
                }

                if (lowerVolume > 0.toFloat() && lowerVolume.isFinite()) {
                    exoPlayer?.audioComponent?.volume = lowerVolume
                }
            }

            preparedExoPlayer?.playWhenReady = true
        } else if (exoPlayer?.isPlaying == false) {
            preparedExoPlayer?.playWhenReady = false
        }
    }, false)

    val historyListener = Listener({
        //add current song to history after 30 seconds
        if (currentPosition > 30 * 1000) {
            try {
                if (history[history.size - 1] != currentSongId) {
                    history.add(currentSongId)
                }
            } catch (e: java.lang.IndexOutOfBoundsException) {
                history.add(currentSongId)
            }
        }
    }, false)

    playerPositionChangedListeners.add(crossFadeListener)
    playerPositionChangedListeners.add(historyListener)
    playerPositionChangedListeners.add(positionListener)
    playerPositionChangedListeners.add(lyricsListener)
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

    PlayerSaveState.save(context)
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

    PlayerSaveState.save(context)
}

/**
 * Pause playback
 */
fun pause(context: Context) {
    exoPlayer?.playWhenReady = false
    visiblePlaying = false

    PlayerSaveState.save(context)
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
        progress = currentPosition
    )

    PlayerSaveState.save(context)
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

        PlayerSaveState.save(context)

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
                nextRandom = Random(randomSeed).nextInt(songQueue.size)
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
            nextRandom = Random(randomSeed).nextInt(songQueue.size)
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
                nextRelatedRandom = Random(relatedRandomSeed).nextInt(relatedSongQueue.size)
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
            nextRelatedRandom = Random(relatedRandomSeed).nextInt(relatedSongQueue.size)
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

val previousSongId: String
    get() {
        return try {
            playlist[playlistIndex - 1]
        } catch (e: java.lang.IndexOutOfBoundsException) {
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
                    nextRandom = Random(randomSeed).nextInt(songQueue.size)
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
                    nextRelatedRandom = Random(relatedRandomSeed).nextInt(relatedSongQueue.size)
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
    SongDatabaseHelper(context).getSong(currentSongId)?.let { song ->
        return song.albumId
    }

    return ""
}

/**
 * Empty queue
 */
fun clearQueue() {
    nextRandom = -1
    randomSeed = Random.nextInt(0, 999999999)
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
    relatedRandomSeed = Random.nextInt(0, 999999999)
    nextRelatedRandom = -1
    loadedRelatedHash = -1
}

var lastLoad: Long = 0

/**
 * Fetch songs which are related to songs in playlist
 */
fun loadRelatedSongs(context: Context, playAfter: Boolean) {
    if (!loadingRelatedSongs && loadedRelatedHash != playlist.hashCode() && System.currentTimeMillis() - lastLoad > 5000) {
        lastLoad = System.currentTimeMillis()

        loadingRelatedSongs = true

        retrieveRelatedSongs(context, {
            clearRelatedSongs()

            val filteredList = ArrayList<String>()

            val songDatabaseHelper = SongDatabaseHelper(context)

            it.forEach { songId ->
                songDatabaseHelper.getSong(songId)?.let { song ->
                    if (!filter(song)) {
                        filteredList.add(songId)
                    }
                }
            }

            relatedSongQueue = filteredList

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

fun filter(song: Song): Boolean {
    var filter = false

    filters["artists"]?.let {
        it.forEach { filterArtist ->
            if (song.artist.contains(filterArtist, ignoreCase = true)) {
                filter = true
            }
        }
    }

    filters["titles"]?.let {
        it.forEach { filterTitle ->
            if (song.shownTitle.contains(filterTitle, ignoreCase = true)) {
                filter = true
            }
        }
    }

    filters["special"]?.let {
        it.forEach { special ->
            when (special) {
                "creatorFriendly" -> {
                    if (!song.creatorFriendly) {
                        filter = true
                    }
                }

                "explicit" -> {
                    if (song.explicit) {
                        filter = true
                    }
                }
            }
        }
    }

    return filter
}

fun addToPriorityQueue(songId: String) {
    prioritySongQueue.add(songId)
}

fun pushToPriorityQueue(songId: String) {
    prioritySongQueue.push(songId)
}

fun addToQueue(context: Context, songId: String) {
    SongDatabaseHelper(context).getSong(songId)?.let {
        addToQueue(it)
    }
}

fun addToQueue(song: Song) {
    if (!filter(song)) {
        songQueue.add(song.songId)
    }
}

fun removeFromPriorityQueue(index: Int) {
    prioritySongQueue.removeAt(index)
}

fun removeFromQueue(index: Int) {
    songQueue.removeAt(index)
    nextRandom = -1
}

fun removeFromRelatedQueue(index: Int) {
    relatedSongQueue.removeAt(index)
    nextRelatedRandom = -1
}

private fun addArtistToFilters(artistName: String) {
    var artistFilters = filters["artists"]

    if (artistFilters == null) {
        artistFilters = ArrayList()
    }

    artistFilters.add(artistName)

    filters["artists"] = artistFilters
}

private fun addTitleFilter(title: String) {
    var titleFilters = filters["titles"]

    if (titleFilters == null) {
        titleFilters = ArrayList()
    }

    titleFilters.add(title)

    filters["titles"] = titleFilters
}

private fun addSpecialFilter(specialFilter: String) {
    var specialFilters = filters["special"]

    if (specialFilters == null) {
        specialFilters = ArrayList()
    }

    specialFilters.add(specialFilter)

    filters["special"] = specialFilters
}