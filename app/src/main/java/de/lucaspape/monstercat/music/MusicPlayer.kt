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
import com.google.android.exoplayer2.SimpleExoPlayer
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.activities.musicPlayer
import de.lucaspape.monstercat.background.BackgroundService.Companion.streamInfoUpdateAsync
import de.lucaspape.monstercat.database.Song
import de.lucaspape.monstercat.database.helper.SongDatabaseHelper
import de.lucaspape.monstercat.download.addStreamDownloadSong
import de.lucaspape.monstercat.music.notification.startPlayerService
import de.lucaspape.monstercat.music.notification.stopPlayerService
import de.lucaspape.monstercat.twitch.Stream
import de.lucaspape.monstercat.util.*
import java.io.File
import java.lang.IndexOutOfBoundsException
import java.lang.ref.WeakReference
import kotlin.random.Random

class MusicPlayer() {
    constructor(context: Context) : this() {
        contextReference = WeakReference(context)
    }

    companion object {
        @JvmStatic
        internal var contextReference: WeakReference<Context>? = null

        @JvmStatic
        var exoPlayer: ExoPlayer? = null
            internal set

        @JvmStatic
        var songQueue = ArrayList<String>()

        @JvmStatic
        internal var playlist = ArrayList<String>()
        @JvmStatic
        internal var playlistIndex = 0

        @JvmStatic
        var loop = false
        @JvmStatic
        var loopSingle = false
        @JvmStatic
        var shuffle = false

        @JvmStatic
        var mediaSession: MediaSessionCompat? = null
            internal set

        @JvmStatic
        private var sessionCreated = false

        @JvmStatic
        val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener {
            musicPlayer.pause()
        }

        class NoisyReceiver : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                    musicPlayer.pause()
                }
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

    fun playNow() {
        playNext()
    }

    private fun playSong(context: Context, song: Song) {
        streamInfoUpdateAsync?.cancel(true)

        val settings = Settings(context)

        exoPlayer?.release()
        exoPlayer?.stop()

        //new exoplayer
        val newExoPlayer = SimpleExoPlayer.Builder(context).build()
        newExoPlayer.audioAttributes = getAudioAttributes()

        //for play/pause button change and if song ended
        newExoPlayer.addListener(getPlayerListener(context, song))

        exoPlayer = newExoPlayer

        if (requestAudioFocus(context) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            if (wifiConnected(context) == true || settings.getSetting("streamOverMobile") == "true" || File(
                    song.getUrl()
                ).exists()
            ) {
                val mediaSource = song.getMediaSource()

                if (mediaSource != null) {
                    exoPlayer?.prepare(mediaSource)

                    exoPlayer?.playWhenReady = true

                    //UI stuff
                    setTitle(song.title, song.version, song.artist)

                    setCover(context, song.title, song.version, song.artist, song.albumId) {
                        setPlayButtonImage(context)
                        startSeekBarUpdate()
                    }
                } else {
                    displayInfo(context, context.getString(R.string.songNotPlayableError))
                }
            }
        }
    }

    fun playStream(stream: Stream) {
        contextReference?.get()?.let { context ->
            streamInfoUpdateAsync?.cancel(true)

            val settings = Settings(context)

            startPlayerService()

            exoPlayer?.release()
            exoPlayer?.stop()

            //new exoplayer
            val newExoPlayer = SimpleExoPlayer.Builder(context).build()
            newExoPlayer.audioAttributes = getAudioAttributes()

            //for play/pause button change and if song ended
            newExoPlayer.addListener(getStreamPlayerListener(context))

            exoPlayer = newExoPlayer

            //request audio focus
            if (requestAudioFocus(context) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                //only play stream if allowed
                if (wifiConnected(context) == true || settings.getSetting("streamOverMobile") == "true") {
                    exoPlayer?.prepare(stream.getMediaSource(context))

                    exoPlayer?.playWhenReady = true

                    //UI stuff
                    setTitle(stream.title, stream.version, stream.artist)

                    setPlayButtonImage(context)
                    startSeekBarUpdate()

                    streamInfoUpdateAsync = StreamInfoUpdateAsync(WeakReference(context), stream)
                    streamInfoUpdateAsync?.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
                }
            }
        }
    }

    /**
     * Pre-downloads stream before playback
     */
    private fun preDownloadSongStream(
        context: Context,
        song: Song,
        nextSong: Song?,
        currentSongFinished: (song: Song) -> Unit
    ) {
        if (song.isDownloadable) {
            if (!File(song.downloadLocation).exists() && !File(song.streamDownloadLocation).exists()) {
                addStreamDownloadSong(context, song.songId) {
                    currentSongFinished(song)
                }
            } else {
                currentSongFinished(song)
            }
        } else {
            //No permission to download

            currentSongFinished(song)

            displayInfo(
                context,
                context.getString(R.string.errorDownloadNotAllowedFallbackStream)
            )
        }

        try {
            if (nextSong != null) {
                if (!File(nextSong.downloadLocation).exists() && !File(nextSong.streamDownloadLocation).exists()) {
                    addStreamDownloadSong(context, nextSong.songId) {}
                }
            }
        } catch (e: IndexOutOfBoundsException) {

        }
    }

    private fun playNext() {
        contextReference?.get()?.let { context ->
            streamInfoUpdateAsync?.cancel(true)
            startPlayerService()

            val songDatabaseHelper = SongDatabaseHelper(context)
            val song = songDatabaseHelper.getSong(context, nextSong())

            song?.let {
                val settings = Settings(context)
                val downloadStream = settings.getSetting("downloadStream")?.toBoolean()

                if (downloadStream == true) {
                    val nextSongId = try {
                        playlist[playlistIndex + 1]
                    } catch (e: IndexOutOfBoundsException) {
                        try {
                            songQueue[0]
                        } catch (e: IndexOutOfBoundsException) {
                            ""
                        }
                    }

                    val nextSong = songDatabaseHelper.getSong(context, nextSongId)

                    preDownloadSongStream(context, song, nextSong) { song ->
                        playSong(
                            context,
                            song
                        )
                    }

                } else {
                    playSong(context, song)
                }
            }
        }
    }

    private fun playPrevious() {
        contextReference?.get()?.let { context ->
            streamInfoUpdateAsync?.cancel(true)
            startPlayerService()

            val songDatabaseHelper = SongDatabaseHelper(context)
            val song = songDatabaseHelper.getSong(context, previousSong())

            song?.let {
                val settings = Settings(context)
                val downloadStream = settings.getSetting("downloadStream")?.toBoolean()

                if (downloadStream == true) {
                    val nextSongId = try {
                        playlist[playlistIndex + 1]
                    } catch (e: IndexOutOfBoundsException) {
                        try {
                            songQueue[0]
                        } catch (e: IndexOutOfBoundsException) {
                            ""
                        }
                    }

                    val nextSong = songDatabaseHelper.getSong(context, nextSongId)

                    preDownloadSongStream(context, song, nextSong) { song ->
                        playSong(
                            context,
                            song
                        )
                    }
                } else {
                    playSong(context, song)
                }
            }
        }
    }

    private fun nextSong(): String {
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

    private fun previousSong(): String {
        playlistIndex--
        return playlist[playlistIndex]
    }

    fun getCurrentSong(): Song? {
        contextReference?.get()?.let { context ->
            val currentSongId = playlist[playlistIndex]
            val songDatabaseHelper = SongDatabaseHelper(context)

            return songDatabaseHelper.getSong(context, currentSongId)
        }

        return null
    }

    fun clearQueue() {
        songQueue = ArrayList()
    }
}