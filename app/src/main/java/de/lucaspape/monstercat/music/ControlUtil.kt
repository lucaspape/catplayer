package de.lucaspape.monstercat.music

import android.content.Context
import android.content.IntentFilter
import android.media.AudioManager
import android.os.AsyncTask
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.Player
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.database.Song
import de.lucaspape.monstercat.download.addDownloadSong
import de.lucaspape.monstercat.music.notification.startPlayerService
import de.lucaspape.monstercat.music.notification.stopPlayerService
import de.lucaspape.monstercat.music.notification.updateNotification
import de.lucaspape.monstercat.twitch.Stream
import de.lucaspape.monstercat.util.*
import java.io.File
import java.lang.IndexOutOfBoundsException
import java.lang.ref.WeakReference

var updateLiveInfoAsync: UpdateLiveInfoAsync? = null
var waitForDownloadTask: AsyncTask<Void, Void, String>? = null

/**
 * Get current song and play it
 */
internal fun play() {
    //remove play next listener
    clearListener()

    updateLiveInfoAsync?.cancel(true)

    //start the player service (with its notification)
    startPlayerService()

    val song = getCurrentSong()

    song?.let {
        contextReference?.get()?.let { context ->
            val settings = Settings(context)
            val downloadStream = settings.getSetting("downloadStream")?.toBoolean()

            //This downloads the stream before playing it (and it pre-downloads the next stream)
            if (downloadStream == true) {
                if (song.isDownloadable) {
                    if (!File(song.downloadLocation).exists() && !File(song.streamDownloadLocation).exists()) {
                        addDownloadSong(
                            song.streamLocation,
                            song.streamDownloadLocation,
                            song.shownTitle
                        )
                    }

                    waitForDownloadTask?.cancel(true)

                    waitForDownloadTask = object : AsyncTask<Void, Void, String>() {
                        override fun doInBackground(vararg params: Void?): String? {
                            if (!File(song.downloadLocation).exists()) {
                                while (!File(song.streamDownloadLocation).exists()) {
                                    Thread.sleep(100)
                                }
                            }

                            return null
                        }

                        override fun onPostExecute(result: String?) {
                            playSong(context, song)

                            try {
                                val nextSong = playList[currentSong + 1]

                                if (!File(nextSong.downloadLocation).exists() && !File(nextSong.streamDownloadLocation).exists()) {
                                    addDownloadSong(
                                        nextSong.streamLocation,
                                        nextSong.streamDownloadLocation,
                                        nextSong.shownTitle
                                    )
                                }

                            } catch (e: IndexOutOfBoundsException) {

                            }
                        }

                    }

                    waitForDownloadTask?.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
                } else {
                    //No permission to download

                    playSong(context, song)
                    displayInfo(
                        context,
                        context.getString(R.string.errorDownloadNotAllowedFallbackStream)
                    )
                }

            } else {
                //Stream normally

                playSong(context, song)
            }

        }
    }
}

/**
 * Play specific song
 */
private fun playSong(context: Context, song: Song) {
    val settings = Settings(context)

    val primaryResolution = settings.getSetting("primaryCoverResolution")

    mediaPlayer?.release()
    mediaPlayer?.stop()

    //new exoplayer
    val exoPlayer = ExoPlayerFactory.newSimpleInstance(context)
    exoPlayer.audioAttributes = getAudioAttributes()

    //for play/pause button change
    exoPlayer.addListener(object : Player.EventListener {
        @Override
        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            setPlayButtonImage(context)
            updateNotification(
                song.title,
                song.version,
                song.artist,
                contextReference?.get()!!.filesDir.toString() + "/" + song.albumId + ".png" + primaryResolution
            )
        }
    })

    mediaPlayer = exoPlayer

    //request audio focus
    if (requestAudioFocus(context) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
        //if song ended play next
        registerNextListener()

        //dont stream if not allowed
        if (wifiConnected(context) == true || settings.getSetting("streamOverMobile") == "true" || File(
                song.getUrl()
            ).exists()
        ) {

            val mediaSource = song.getMediaSource()

            if (mediaSource != null) {
                mediaPlayer?.prepare(mediaSource)

                mediaPlayer?.playWhenReady = true

                //UI stuff
                setTitle(song.title, song.version, song.artist)
                startTextAnimation()
                setCover(song, context)
                setPlayButtonImage(context)
                startSeekBarUpdate()
            } else {
                //TODO msg
                displayInfo(context, "You cannot play this song")
            }
        }
    }
}

/**
 * Play specific stream
 */
fun playStream(stream: Stream) {
    clearListener()

    contextReference?.get()?.let { context ->
        val settings = Settings(context)

        startPlayerService()

        mediaPlayer?.release()
        mediaPlayer?.stop()

        //new exoplayer
        val exoPlayer = ExoPlayerFactory.newSimpleInstance(context)
        exoPlayer.audioAttributes = getAudioAttributes()

        //for play/pause button change
        exoPlayer.addListener(object : Player.EventListener {
            @Override
            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                setPlayButtonImage(context)

                updateNotification(
                    UpdateLiveInfoAsync.previousTitle,
                    "",
                    UpdateLiveInfoAsync.previousArtist,
                    context.filesDir.toString() + "/live.png"
                )
            }
        })

        mediaPlayer = exoPlayer

        //request audio focus
        if (requestAudioFocus(context) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            //only play stream if allowed
            if (wifiConnected(context) == true || settings.getSetting("streamOverMobile") == "true") {
                mediaPlayer?.prepare(stream.getMediaSource(context))

                mediaPlayer?.playWhenReady = true

                //UI stuff
                setTitle(stream.title, "", stream.artist)
                startTextAnimation()
                setPlayButtonImage(context)
                startSeekBarUpdate()

                //this will update the title and artist of the stream in the background (from an api)
                updateLiveInfoAsync?.cancel(true)

                updateLiveInfoAsync = UpdateLiveInfoAsync(WeakReference(context), stream)
                updateLiveInfoAsync?.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
            }

        }
    }
}

/**
 * Stop playback
 */
internal fun stop() {
    if (mediaPlayer?.isPlaying == true) {

        clearListener()

        hideTitle()

        setPlayButtonImage(contextReference?.get()!!)

        mediaPlayer?.stop()

        registerNextListener()

        contextReference?.get()?.let { context ->
            abandonAudioFocus(context)
        }
    }

    stopPlayerService()
}

/**
 * Pause playback
 */
fun pause() {
    clearListener()

    contextReference?.get()?.let { context ->
        try {
            mediaPlayer?.playWhenReady = false

            setPlayButtonImage(context)

            registerNextListener()

            abandonAudioFocus(context)

        } catch (e: IndexOutOfBoundsException) {
        }
    }
}

/**
 * Resume playback
 */
fun resume() {
    clearListener()

    contextReference?.get()?.let { context ->
        try {
            if (mediaPlayer?.isPlaying == false) {
                if (requestAudioFocus(context) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                    mediaPlayer!!.playWhenReady = true

                    val intentFilter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
                    context.registerReceiver(NoisyReceiver(), intentFilter)
                }

                setPlayButtonImage(context)

                registerNextListener()

                startPlayerService()
            }

        } catch (e: IndexOutOfBoundsException) {

        }
    }
}

/**
 * Next song
 */
fun next() {
    clearListener()

    if (!repeat) {
        currentSong++
    }

    play()
}

/**
 * Previous song
 */
fun previous() {
    if (currentSong != 0) {
        clearListener()

        currentSong--

        play()
    }
}

/**
 * Toggle pause/play
 */
fun toggleMusic() {
    if (mediaPlayer?.isPlaying == true) {
        pause()
    } else {
        resume()
    }
}

/**
 * Returns current song from playlist
 */
fun getCurrentSong(): Song? {
    return try {
        playList[currentSong]
    } catch (e: IndexOutOfBoundsException) {
        null
    }
}

private fun clearListener() {
    mediaPlayer?.addListener(object : Player.EventListener {
        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        }
    })
}

private fun registerNextListener() {
    mediaPlayer?.addListener(object : Player.EventListener {
        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            when (playbackState) {
                Player.STATE_ENDED -> next()
            }
        }
    })
}